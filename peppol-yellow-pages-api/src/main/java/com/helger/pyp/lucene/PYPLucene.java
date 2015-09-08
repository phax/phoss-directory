package com.helger.pyp.lucene;

import java.io.IOException;
import java.nio.file.Path;

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

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.scope.IScope;
import com.helger.commons.scope.singleton.AbstractGlobalSingleton;
import com.helger.photon.basic.app.io.WebFileIO;

public class PYPLucene extends AbstractGlobalSingleton
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PYPLucene.class);

  private final Directory m_aDir;
  private final Analyzer m_aAnalyzer;
  private final IndexWriter m_aIndexWriter;
  private DirectoryReader m_aIndexReader;
  private IndexSearcher m_aSearcher;

  @Deprecated
  @UsedViaReflection
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

  @Nonnull
  public static PYPLucene getInstance ()
  {
    return getGlobalSingleton (PYPLucene.class);
  }

  @Override
  protected void onDestroy (@Nonnull final IScope aScopeInDestructions) throws IOException
  {
    StreamHelper.close (m_aIndexReader);
    StreamHelper.close (m_aIndexWriter);
    StreamHelper.close (m_aDir);
  }

  @Nonnull
  public static Analyzer getAnalyzer ()
  {
    return getInstance ().m_aAnalyzer;
  }

  @Nonnull
  public static IndexReader getReader ()
  {
    return getInstance ().m_aIndexReader;
  }

  @Nonnull
  public static IndexWriter getWriter ()
  {
    return getInstance ().m_aIndexWriter;
  }

  @Nullable
  public IndexSearcher getSearcher () throws IOException
  {
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
}
