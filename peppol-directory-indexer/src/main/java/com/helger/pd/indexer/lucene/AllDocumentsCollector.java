/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.SimpleCollector;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.string.ToStringGenerator;

/**
 * A Lucene {@link Collector} that always collects all {@link Document} objects.
 *
 * @author Philip Helger
 */
public class AllDocumentsCollector extends SimpleCollector
{
  private final ILuceneDocumentProvider m_aDocumentProvider;
  private final ObjIntConsumer <Document> m_aConsumer;
  private int m_nDocBase = 0;

  /**
   * Constructor
   *
   * @param aDocumentProvider
   *        The overall Document provider. May not be <code>null</code>.
   * @param aConsumer
   *        The consumer that will take the Lucene {@link Document} objects. May
   *        not be <code>null</code>.
   */
  public AllDocumentsCollector (@Nonnull final ILuceneDocumentProvider aDocumentProvider,
                                @Nonnull final ObjIntConsumer <Document> aConsumer)
  {
    m_aDocumentProvider = ValueEnforcer.notNull (aDocumentProvider, "DocumentProvider");
    m_aConsumer = ValueEnforcer.notNull (aConsumer, "Consumer");
  }

  public boolean needsScores ()
  {
    return false;
  }

  @Override
  protected void doSetNextReader (@Nonnull final LeafReaderContext aCtx)
  {
    // Important to remember the current document base
    m_nDocBase = aCtx.docBase;
  }

  @Override
  public void collect (final int nDocID) throws IOException
  {
    final int nAbsoluteDocID = m_nDocBase + nDocID;
    // Resolve document
    final Document aDoc = m_aDocumentProvider.getDocument (nAbsoluteDocID);
    if (aDoc == null)
      throw new IllegalStateException ("Failed to resolve Lucene Document with ID " + nAbsoluteDocID);
    // Pass to Consumer
    m_aConsumer.accept (aDoc, nAbsoluteDocID);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Consumer", m_aConsumer).toString ();
  }
}
