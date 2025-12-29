/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
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
public final class PDLucene implements Closeable, ILuceneDocumentProvider, ILuceneAnalyzerProvider
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDLucene.class);

  private final Directory m_aDir;
  private final Analyzer m_aAnalyzer;
  // IndexWriter is thread-safe
  private final IndexWriter m_aIndexWriter;
  private DirectoryReader m_aDirectoryReader;
  private IndexReader m_aSearchReader;
  private IndexSearcher m_aSearcher;
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

    // Reader and searcher are opened on demand

    LOGGER.info ("Lucene index operating on " + aPath);
  }

  public void close () throws IOException
  {
    // Avoid double closing
    if (!m_aClosing.getAndSet (true))
    {
      // Start closing
      StreamHelper.close (m_aDirectoryReader);

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
      LOGGER.info ("Closed Lucene reader/writer/directory");
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

  @Nullable
  public DirectoryReader getDirectoryReader () throws IOException
  {
    _checkClosing ();

    try
    {
      // Commit the writer changes only if a reader is requested
      if (m_aWriterChanges.intValue () > 0)
      {
        LOGGER.info ("Lazily committing " + m_aWriterChanges.intValue () + " changes to the Lucene index");
        final long nSeqNum = _getWriter ().commit ();
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Committed up to seq# " + nSeqNum);
        m_aWriterChanges.set (0);
      }

      // Is a new reader required because the index changed?
      final DirectoryReader aNewReader = m_aDirectoryReader != null ? DirectoryReader.openIfChanged (m_aDirectoryReader)
                                                                    : DirectoryReader.open (m_aDir);
      if (aNewReader != null)
      {
        // Something changed in the index
        m_aDirectoryReader = aNewReader;
        m_aSearcher = null;

        if (LOGGER.isDebugEnabled ())
        {
          LOGGER.debug ("Contents of index changed. Creating new index reader");
          LOGGER.debug ("Using DirectoryReader " + aNewReader.toString ());
        }
      }
      return m_aDirectoryReader;
    }
    catch (final IndexNotFoundException ex)
    {
      // No such index
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("No such index", ex);
      return null;
    }
  }

  /**
   * Get the Lucene {@link Document} matching the specified ID
   *
   * @param nDocID
   *        Document ID
   * @return <code>null</code> if no reader could be obtained or no such document exists.
   * @throws IOException
   *         On IO error
   */
  @Nullable
  public Document getDocument (final int nDocID) throws IOException
  {
    _checkClosing ();

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("getDocument(" + nDocID + ")");

    final DirectoryReader aReader = getDirectoryReader ();
    if (aReader == null)
      return null;

    try
    {
      return aReader.document (nDocID);
    }
    catch (final IllegalArgumentException ex)
    {
      // Happens in the wild
      LOGGER.error ("Failed to retrieve document with ID " + nDocID + ": " + ex.getMessage ());
      return null;
    }
  }

  /**
   * Get a searcher on this index.
   *
   * @return <code>null</code> if no reader or no searcher could be obtained
   * @throws IOException
   *         On IO error
   */
  @Nullable
  public IndexSearcher getSearcher () throws IOException
  {
    _checkClosing ();

    final DirectoryReader aReader = getDirectoryReader ();
    if (aReader == null)
    {
      // Index not readable
      LOGGER.warn ("Index not readable");
      return null;
    }

    if (m_aSearchReader == aReader)
    {
      // Reader did not change - use cached searcher
    }
    else
    {
      // Create new searcher only if necessary
      m_aSearchReader = aReader;
      m_aSearcher = new IndexSearcher (aReader);
    }
    return m_aSearcher;
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
