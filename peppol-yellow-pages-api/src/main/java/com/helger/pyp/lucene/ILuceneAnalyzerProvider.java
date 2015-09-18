package com.helger.pyp.lucene;

import java.io.IOException;

import javax.annotation.Nullable;

import org.apache.lucene.analysis.Analyzer;

/**
 * {@link Analyzer} retrieval interface
 *
 * @author Philip Helger
 */
public interface ILuceneAnalyzerProvider
{
  /**
   * Get the Lucene Analyzer to use
   *
   * @return The Analyzer to use. May be <code>null</code> if the underyling
   *         Analyzer is already closed
   * @throws IOException
   *         In case of a Lucene error
   */
  @Nullable
  Analyzer getAnalyzer () throws IOException;
}
