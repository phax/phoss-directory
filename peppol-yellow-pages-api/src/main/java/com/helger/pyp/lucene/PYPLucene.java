package com.helger.pyp.lucene;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    try
    {
      m_aIndexReader = DirectoryReader.open (m_aDir);
    }
    catch (final IndexNotFoundException ex)
    {
      // empty index
    }

    s_aLogger.info ("Lucene index operating on " + aPath);
  }

  public void close () throws IOException
  {
    m_aClosing.set (true);
    m_aLock.lock ();
    try
    {
      // Start closing
      StreamHelper.close (m_aIndexReader);
      if (m_aIndexWriter != null)
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
  public IndexReader getReader ()
  {
    _checkClosing ();
    return m_aIndexReader;
  }

  @Nonnull
  public IndexWriter getWriter ()
  {
    _checkClosing ();
    return m_aIndexWriter;
  }

  @Nullable
  public IndexSearcher getSearcher () throws IOException
  {
    _checkClosing ();
    try
    {
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
      if (m_aSearcher == null)
        m_aSearcher = new IndexSearcher (m_aIndexReader);
      return m_aSearcher;
    }
    catch (final IndexNotFoundException ex)
    {
      // No such index
      return null;
    }
  }

  @Nonnull
  public ESuccess runLocked (@Nonnull final IThrowingRunnable <IOException> aRunnable) throws IOException
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
}
