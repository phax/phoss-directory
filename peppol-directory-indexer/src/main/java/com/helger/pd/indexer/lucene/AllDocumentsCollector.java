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
package com.helger.pd.indexer.lucene;

import java.io.IOException;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.SimpleCollector;

import com.helger.commons.ValueEnforcer;

/**
 * A Lucene {@link Collector} that always collects all {@link Document} objects.
 *
 * @author Philip Helger
 */
public class AllDocumentsCollector extends SimpleCollector
{
  private final ILuceneDocumentProvider m_aDocProvider;
  private final Consumer <Document> m_aConsumer;

  public AllDocumentsCollector (@Nonnull final ILuceneDocumentProvider aDocProvider, @Nonnull final Consumer <Document> aConsumer)
  {
    m_aDocProvider = ValueEnforcer.notNull (aDocProvider, "DocProvider");
    m_aConsumer = ValueEnforcer.notNull (aConsumer, "Consumer");
  }

  public boolean needsScores ()
  {
    return false;
  }

  @Override
  public void collect (final int nDocID) throws IOException
  {
    final Document aDoc = m_aDocProvider.getDocument (nDocID);
    m_aConsumer.accept (aDoc);
  }
}
