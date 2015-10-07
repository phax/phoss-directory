package com.helger.pyp.lucene;

import java.io.IOException;

import javax.annotation.Nullable;

import org.apache.lucene.document.Document;

/**
 * {@link Document} retrieval interface
 *
 * @author Philip Helger
 */
public interface ILuceneDocumentProvider
{
  /**
   * Get the Lucene document from the document ID
   *
   * @param nDocID
   *        Internal Lucene Document ID
   * @return The Document or <code>null</code>.
   * @throws IOException
   *         In case of a Lucene error
   */
  @Nullable
  Document getDocument (int nDocID) throws IOException;
}
