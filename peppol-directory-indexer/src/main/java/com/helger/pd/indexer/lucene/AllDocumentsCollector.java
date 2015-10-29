package com.helger.pd.indexer.lucene;

import java.io.IOException;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.SimpleCollector;

import com.helger.commons.ValueEnforcer;

/**
 * A Lucene {@link Collector} that always collets all {@link Document} objects.
 *
 * @author Philip Helger
 */
public class AllDocumentsCollector extends SimpleCollector
{
  private final ILuceneDocumentProvider m_aDocProvider;
  private final Consumer <Document> m_aConsumer;

  public AllDocumentsCollector (@Nonnull final ILuceneDocumentProvider aDocProvider,
                                @Nonnull final Consumer <Document> aConsumer)
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
