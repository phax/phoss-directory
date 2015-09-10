/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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
package com.helger.pyp.lucene;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.lucene.analysis.Analyzer;
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
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.callback.IThrowingCallable;
import com.helger.commons.callback.IThrowingRunnable;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.state.ESuccess;
import com.helger.photon.basic.app.io.WebFileIO;

/**
 * The singleton wrapper around the Lucene index to be used in PYP.
 *
 * @author Philip Helger
 */
public final class PYPLucene implements Closeable
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PYPLucene.class);

  private final Lock m_aLock = new ReentrantLock ();
  private final Directory m_aDir;
  private final Analyzer m_aAnalyzer;
  private final IndexWriter m_aIndexWriter;
  private DirectoryReader m_aIndexReader;
  private IndexSearcher m_aSearcher;
  private final AtomicBoolean m_aClosing = new AtomicBoolean (false);
  private final AtomicInteger m_aWriterChanges = new AtomicInteger (0);

  public PYPLucene () throws IOException
  {
    // Where to store the index files
    final Path aPath = WebFileIO.getDataIO ().getFile ("lucene-index").toPath ();
    m_aDir = FSDirectory.open (aPath);

    // Analyzer to use
    m_aAnalyzer = new StandardAnalyzer ();

    final IndexWriterConfig aWriterConfig = new IndexWriterConfig (m_aAnalyzer);
    aWriterConfig.setOpenMode (OpenMode.CREATE_OR_APPEND);

    m_aIndexWriter = new IndexWriter (m_aDir, aWriterConfig);

    // Reader and searcher are opened on demand

    s_aLogger.info ("Lucene index operating on " + aPath);
  }

  public void close () throws IOException
  {
    // Avoid double closing
    if (!m_aClosing.getAndSet (true))
    {
      m_aLock.lock ();
      try
      {
        // Start closing
        StreamHelper.close (m_aIndexReader);
        if (m_aIndexWriter != null && m_aIndexWriter.isOpen ())
          m_aIndexWriter.commit ();
        StreamHelper.close (m_aIndexWriter);
        StreamHelper.close (m_aDir);
        s_aLogger.info ("Closed Lucene reader/writer/directory");
      }
      finally
      {
        m_aLock.unlock ();
      }
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

  @Nonnull
  public Analyzer getAnalyzer ()
  {
    _checkClosing ();
    return m_aAnalyzer;
  }

  @Nonnull
  private IndexWriter _getWriter ()
  {
    _checkClosing ();
    return m_aIndexWriter;
  }

  @Nullable
  private IndexReader _getReader () throws IOException
  {
    _checkClosing ();
    try
    {
      // Commit only if a reader is requested
      if (m_aWriterChanges.intValue () > 0)
      {
        s_aLogger.info ("Lazily committing " + m_aWriterChanges.intValue () + " changes to the Lucene index");
        _getWriter ().commit ();
        m_aWriterChanges.set (0);
      }

      // Is a new reader required?
      final DirectoryReader aNewReader = m_aIndexReader != null ? DirectoryReader.openIfChanged (m_aIndexReader)
                                                                : DirectoryReader.open (m_aDir);
      if (aNewReader != null)
      {
        // Something changed in the index
        m_aIndexReader = aNewReader;
        m_aSearcher = null;

        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Contents of index changed. Creating new searcher");
      }
      return m_aIndexReader;
    }
    catch (final IndexNotFoundException ex)
    {
      // No such index
      return null;
    }
  }

  @Nullable
  public Document getDocument (final int nDocID) throws IOException
  {
    _checkClosing ();

    final IndexReader aReader = _getReader ();
    if (aReader == null)
      return null;
    return aReader.document (nDocID);
  }

  @Nullable
  public IndexSearcher getSearcher () throws IOException
  {
    _checkClosing ();
    if (m_aSearcher == null)
    {
      final IndexReader aReader = _getReader ();
      if (aReader == null)
        return null;
      m_aSearcher = new IndexSearcher (aReader);
    }
    return m_aSearcher;
  }

  /**
   * Updates a document by first deleting the document(s) containing
   * <code>term</code> and then adding the new document. The delete and then add
   * are atomic as seen by a reader on the same index (flush may happen only
   * after the add).
   *
   * @param aDelTerm
   *        the term to identify the document(s) to be deleted. May be
   *        <code>null</code>.
   * @param aDoc
   *        the document to be added May not be <code>null</code>.
   * @throws CorruptIndexException
   *         if the index is corrupt
   * @throws IOException
   *         if there is a low-level IO error
   */
  public void updateDocument (@Nullable final Term aDelTerm,
                              @Nonnull final Iterable <? extends IndexableField> aDoc) throws IOException
  {
    _getWriter ().updateDocument (aDelTerm, aDoc);
    m_aWriterChanges.incrementAndGet ();
  }

  /**
   * Atomically deletes documents matching the provided delTerm and adds a block
   * of documents with sequentially assigned document IDs, such that an external
   * reader will see all or none of the documents.
   *
   * @param aDelTerm
   *        the term to identify the document(s) to be deleted. May be
   *        <code>null</code>.
   * @param aDocs
   *        the documents to be added. May not be <code>null</code>.
   * @throws CorruptIndexException
   *         if the index is corrupt
   * @throws IOException
   *         if there is a low-level IO error
   */
  public void updateDocuments (@Nullable final Term aDelTerm,
                               @Nonnull final Iterable <? extends Iterable <? extends IndexableField>> aDocs) throws IOException
  {
    _getWriter ().updateDocuments (aDelTerm, aDocs);
    m_aWriterChanges.incrementAndGet ();
  }

  /**
   * Deletes the document(s) containing any of the terms. All given deletes are
   * applied and flushed atomically at the same time.
   *
   * @param terms
   *        array of terms to identify the documents to be deleted
   * @throws CorruptIndexException
   *         if the index is corrupt
   * @throws IOException
   *         if there is a low-level IO error
   */
  public void deleteDocuments (final Term... terms) throws IOException
  {
    _getWriter ().deleteDocuments (terms);
    m_aWriterChanges.incrementAndGet ();
  }

  /**
   * @param aRunnable
   *        Callback to be executed
   * @return {@link ESuccess#FAILURE} if the index is just closing
   * @throws IOException
   *         may be thrown by the callback
   */
  @Nonnull
  public ESuccess runAtomic (@Nonnull final IThrowingRunnable <IOException> aRunnable) throws IOException
  {
    m_aLock.lock ();
    try
    {
      if (isClosing ())
        return ESuccess.FAILURE;
      aRunnable.run ();
    }
    finally
    {
      m_aLock.unlock ();
    }
    return ESuccess.SUCCESS;
  }

  /**
   * @param aRunnable
   *        Callback to be executed
   * @return <code>null</code> if the index is just closing
   * @throws IOException
   *         may be thrown by the callback
   */
  @Nonnull
  public <T> T runAtomic (@Nonnull final IThrowingCallable <T, IOException> aRunnable) throws IOException
  {
    m_aLock.lock ();
    try
    {
      if (isClosing ())
        return null;
      return aRunnable.call ();
    }
    finally
    {
      m_aLock.unlock ();
    }
  }
}
