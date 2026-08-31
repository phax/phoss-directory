/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.pd.indexer.lucene;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.stream.StreamHelper;
import com.helger.photon.io.WebFileIO;

import jakarta.annotation.Nullable;

/**
 * The singleton wrapper around the Lucene index to be used in Peppol Directory.
 *
 * @author Philip Helger
 */
public final class PDLucene implements Closeable, ILuceneAnalyzerProvider
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDLucene.class);

  private final Directory m_aDir;
  private final Analyzer m_aAnalyzer;
  // IndexWriter is thread-safe
  private final IndexWriter m_aIndexWriter;
  // SearcherManager is thread-safe
  private final SearcherManager m_aSearcherManager;
  private final AtomicBoolean m_aClosing = new AtomicBoolean (false);
  private final AtomicInteger m_aWriterChanges = new AtomicInteger (0);

  @NonNull
  public static File getLuceneIndexDir ()
  {
    return WebFileIO.getDataIO ().getFile ("lucene-index");
  }

  @NonNull
  public static Analyzer createAnalyzer ()
  {
    if (false)
    {
      // Only lowercasing, no stop words
      return new SimpleAnalyzer ();
    }
    return new StandardAnalyzer ();
  }

  /**
   * Default constructor using a {@link StandardAnalyzer}.
   *
   * @throws IOException
   *         On IO error
   */
  public PDLucene () throws IOException
  {
    this (PDLucene::createAnalyzer);
  }

  /**
   * Constructor with a custom analyzer provider.
   *
   * @param aAnalyzerProvider
   *        The analyzer provider. May not be <code>null</code>.
   * @throws IOException
   *         On IO error
   */
  public PDLucene (@NonNull final Supplier <? extends Analyzer> aAnalyzerProvider) throws IOException
  {
    ValueEnforcer.notNull (aAnalyzerProvider, "AnalyzerProvider");

    // Where to store the index files
    final Path aPath = getLuceneIndexDir ().toPath ();
    m_aDir = FSDirectory.open (aPath);

    // Analyzer to use
    m_aAnalyzer = aAnalyzerProvider.get ();

    // Create the index writer
    final IndexWriterConfig aWriterConfig = new IndexWriterConfig (m_aAnalyzer);
    aWriterConfig.setOpenMode (OpenMode.CREATE_OR_APPEND);
    m_aIndexWriter = new IndexWriter (m_aDir, aWriterConfig);

    /*
     * Create the searcher manager. It ensures that a searcher that is in use is not closed
     * underneath the user, so that the internal Lucene document IDs of a search result stay valid
     * until the searcher is released again.
     */
    m_aSearcherManager = new SearcherManager (m_aIndexWriter, new SearcherFactory ());

    LOGGER.info ("Lucene index operating on " + aPath);
  }

  public void close () throws IOException
  {
    // Avoid double closing
    if (!m_aClosing.getAndSet (true))
    {
      // Start closing
      StreamHelper.close (m_aSearcherManager);

      // Ensure to commit the writer in case of pending changes
      if (m_aIndexWriter != null && m_aIndexWriter.isOpen ())
      {
        final long nSeqNum = m_aIndexWriter.commit ();
        if (nSeqNum >= 0)
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Committed up to seq# " + nSeqNum);
      }
      StreamHelper.close (m_aIndexWriter);
      StreamHelper.close (m_aDir);
      StreamHelper.close (m_aAnalyzer);
      LOGGER.info ("Closed Lucene searcher/writer/directory");
    }
  }

  public boolean isClosing ()
  {
    return m_aClosing.get ();
  }

  private void _checkClosing ()
  {
    if (isClosing ())
      throw new IllegalStateException ("The Lucene index is shutting down so no access is possible");
  }

  /**
   * @return The analyzer to be used for all Lucene based actions
   */
  @NonNull
  public Analyzer getAnalyzer ()
  {
    _checkClosing ();

    return m_aAnalyzer;
  }

  @NonNull
  private IndexWriter _getWriter ()
  {
    _checkClosing ();

    return m_aIndexWriter;
  }

  /**
   * Commit all pending writer changes and reopen the searcher, if the index changed in the
   * meantime.
   *
   * @throws IOException
   *         On IO error
   */
  private void _commitAndRefresh () throws IOException
  {
    // Commit the writer changes only if a searcher is requested
    final int nChanges = m_aWriterChanges.intValue ();
    if (nChanges > 0)
    {
      LOGGER.info ("Lazily committing " + nChanges + " changes to the Lucene index");
      final long nSeqNum = _getWriter ().commit ();
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Committed up to seq# " + nSeqNum);
      // Only subtract what was committed - don't lose changes made in parallel
      m_aWriterChanges.addAndGet (-nChanges);
    }

    // Is a new searcher required because the index changed?
    if (m_aSearcherManager.maybeRefresh ())
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Contents of index changed. Created a new index searcher");
  }

  /**
   * Acquire the {@link IndexSearcher} for a single search operation. All the Lucene documents of a
   * single search result must be resolved with exactly this searcher, because the internal Lucene
   * document IDs are only valid for the index reader that created them. The returned searcher must
   * be released with {@link #releaseSearcher(IndexSearcher)} in a <code>finally</code> block.
   *
   * @return The searcher to be used. Never <code>null</code>.
   * @throws IOException
   *         On IO error
   * @see #releaseSearcher(IndexSearcher)
   */
  @NonNull
  public IndexSearcher acquireSearcher () throws IOException
  {
    _checkClosing ();

    _commitAndRefresh ();
    return m_aSearcherManager.acquire ();
  }

  /**
   * Release a searcher previously acquired with {@link #acquireSearcher()}.
   *
   * @param aSearcher
   *        The searcher to be released. May be <code>null</code>.
   * @throws IOException
   *         On IO error
   * @see #acquireSearcher()
   */
  public void releaseSearcher (@Nullable final IndexSearcher aSearcher) throws IOException
  {
    // Note: no closing check, because a searcher must be released in all cases
    if (aSearcher != null)
      m_aSearcherManager.release (aSearcher);
  }

  /**
   * Updates a document by first deleting the document(s) containing <code>term</code> and then
   * adding the new document. The delete and then add are atomic as seen by a reader on the same
   * index (flush may happen only after the add).
   *
   * @param aDelTerm
   *        the term to identify the document(s) to be deleted. May be <code>null</code>.
   * @param aDoc
   *        the document to be added May not be <code>null</code>.
   * @throws CorruptIndexException
   *         if the index is corrupt
   * @throws IOException
   *         if there is a low-level IO error
   */
  public void updateDocument (@Nullable final Term aDelTerm, @NonNull final Iterable <? extends IndexableField> aDoc)
                                                                                                                      throws IOException
  {
    _checkClosing ();

    final long nSeqNum = _getWriter ().updateDocument (aDelTerm, aDoc);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Last seq# after updateDocument is " + nSeqNum);
    m_aWriterChanges.incrementAndGet ();
  }

  /**
   * Atomically deletes documents matching the provided delTerm and adds a block of documents with
   * sequentially assigned document IDs, such that an external reader will see all or none of the
   * documents.
   *
   * @param aDelTerm
   *        the term to identify the document(s) to be deleted. May be <code>null</code>.
   * @param aDocs
   *        the documents to be added. May not be <code>null</code>.
   * @throws CorruptIndexException
   *         if the index is corrupt
   * @throws IOException
   *         if there is a low-level IO error
   */
  public void updateDocuments (@Nullable final Term aDelTerm,
                               @NonNull final Iterable <? extends Iterable <? extends IndexableField>> aDocs) throws IOException
  {
    _checkClosing ();

    final long nSeqNum;
    if (false)
    {
      // Delete and than add
      _getWriter ().deleteDocuments (aDelTerm);
      nSeqNum = _getWriter ().updateDocuments (null, aDocs);
    }
    else
    {
      // Update directly
      nSeqNum = _getWriter ().updateDocuments (aDelTerm, aDocs);
    }
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Last seq# after updateDocuments is " + nSeqNum);
    m_aWriterChanges.incrementAndGet ();
  }

  /**
   * Deletes the document(s) containing any of the terms. All given deletes are applied and flushed
   * atomically at the same time.
   *
   * @param aTerms
   *        array of terms to identify the documents to be deleted
   * @throws CorruptIndexException
   *         if the index is corrupt
   * @throws IOException
   *         if there is a low-level IO error
   */
  public void deleteDocuments (final Term... aTerms) throws IOException
  {
    _checkClosing ();

    final long nSeqNum = _getWriter ().deleteDocuments (aTerms);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Last seq# after deleteDocuments is " + nSeqNum);
    m_aWriterChanges.incrementAndGet ();
  }

  /**
   * Deletes the document(s) containing any of the queries. All given deletes are applied and
   * flushed atomically at the same time.
   *
   * @param aQueries
   *        array of queries to identify the documents to be deleted
   * @throws CorruptIndexException
   *         if the index is corrupt
   * @throws IOException
   *         if there is a low-level IO error
   */
  public void deleteDocuments (final Query... aQueries) throws IOException
  {
    _checkClosing ();

    final long nSeqNum = _getWriter ().deleteDocuments (aQueries);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Last seq# after deleteDocuments is " + nSeqNum);
    m_aWriterChanges.incrementAndGet ();
  }
}
