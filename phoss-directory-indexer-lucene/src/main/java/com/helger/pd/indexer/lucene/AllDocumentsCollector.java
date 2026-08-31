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

import java.io.IOException;
import java.util.function.ObjIntConsumer;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;
import org.jspecify.annotations.NonNull;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;

/**
 * A Lucene {@link Collector} that always collects all {@link Document} objects.
 *
 * @author Philip Helger
 */
public class AllDocumentsCollector extends SimpleCollector
{
  private final ObjIntConsumer <Document> m_aConsumer;
  private LeafReader m_aLeafReader;
  private int m_nDocBase = 0;

  /**
   * Constructor
   *
   * @param aConsumer
   *        The consumer that will take the Lucene {@link Document} objects. May
   *        not be <code>null</code>.
   */
  public AllDocumentsCollector (@NonNull final ObjIntConsumer <Document> aConsumer)
  {
    m_aConsumer = ValueEnforcer.notNull (aConsumer, "Consumer");
  }

  public boolean needsScores ()
  {
    return false;
  }

  @Override
  protected void doSetNextReader (@NonNull final LeafReaderContext aCtx)
  {
    /*
     * Important to remember the current leaf reader and the current document base. The document
     * must be resolved from exactly this reader, because the document ID is only valid for it (see
     * security advisory GHSA-8qhv-6p5x-2437).
     */
    m_aLeafReader = aCtx.reader ();
    m_nDocBase = aCtx.docBase;
  }

  @Override
  public void collect (final int nDocID) throws IOException
  {
    // Resolve document relative to the leaf reader currently being searched
    final Document aDoc = m_aLeafReader.document (nDocID);

    // Pass to Consumer, using the absolute document ID
    m_aConsumer.accept (aDoc, m_nDocBase + nDocID);
  }

  // Lucene 8
  @Override
  public ScoreMode scoreMode ()
  {
    return ScoreMode.COMPLETE_NO_SCORES;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Consumer", m_aConsumer).getToString ();
  }
}
