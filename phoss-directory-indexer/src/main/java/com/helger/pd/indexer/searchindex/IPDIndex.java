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
package com.helger.pd.indexer.searchindex;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.CheckForSigned;
import com.helger.annotation.Nonempty;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.pd.indexer.searchindex.query.IPDIndexQuery;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryTerm;

import jakarta.annotation.Nullable;

/**
 * The abstraction of the search index used by the Peppol Directory. It hides the underlying search
 * engine (like Apache Lucene) from all callers. All documents and queries are expressed with the
 * search engine independent types from this package.
 *
 * @author Philip Helger
 * @since 0.16.0
 */
public interface IPDIndex extends Closeable
{
  /**
   * @return <code>true</code> if the index is currently shutting down. In that case no more index
   *         access is possible.
   */
  boolean isClosing ();

  /**
   * Get implementation specific information on the underlying index, to be displayed in the
   * administration UI.
   *
   * @return An ordered map from display name to display value. Never <code>null</code> but maybe
   *         empty.
   * @throws IOException
   *         On index error
   */
  @NonNull
  ICommonsOrderedMap <String, String> getIndexInformation () throws IOException;

  /**
   * Split a user provided query string into the terms that are relevant for querying, using the
   * same rules that were used when the field was indexed. This will e.g. remove ":" from a word.
   *
   * @param sFieldName
   *        Index field name the query string is used for. May neither be <code>null</code> nor
   *        empty.
   * @param sQueryString
   *        The user provided query string. May neither be <code>null</code> nor empty.
   * @return The non-<code>null</code> list of all terms.
   * @throws IOException
   *         On index error
   */
  @NonNull
  ICommonsList <String> getSplitIntoTerms (@NonNull @Nonempty String sFieldName,
                                           @NonNull @Nonempty String sQueryString) throws IOException;

  /**
   * Atomically delete all documents matching the provided query and add the provided documents, so
   * that a reader either sees all or none of the new documents.
   *
   * @param aDeleteQuery
   *        The query to identify the documents to be deleted. May be <code>null</code> to only add
   *        documents. Only an exact match query is supported here, because that is the only thing
   *        that can be performed atomically by all supported search engines.
   * @param aDocs
   *        The documents to be added. May not be <code>null</code> but maybe empty.
   * @throws IOException
   *         On index error
   */
  void updateDocuments (@Nullable PDIndexQueryTerm aDeleteQuery, @NonNull List <PDIndexDocument> aDocs)
                                                                                                       throws IOException;

  /**
   * Delete all documents matching the provided query. All deletions are applied atomically at the
   * same time.
   *
   * @param aQuery
   *        The query to identify the documents to be deleted. May not be <code>null</code>.
   * @throws IOException
   *         On index error
   */
  void deleteDocuments (@NonNull IPDIndexQuery aQuery) throws IOException;

  /**
   * Count all documents matching the provided query.
   *
   * @param aQuery
   *        Query to execute. May not be <code>null</code>.
   * @return The number of matching documents. Always &ge; 0.
   * @throws IOException
   *         On index error
   */
  @CheckForSigned
  int getCount (@NonNull IPDIndexQuery aQuery) throws IOException;

  /**
   * Search all documents matching the provided query and pass each of them to the provided
   * {@link Consumer}.<br>
   * If a positive maximum result count is provided, the documents must be passed on ordered by
   * descending relevance, so that the queries built by
   * {@link com.helger.pd.indexer.storage.PDQueryManager} can influence the result ordering.
   * Documents with the same relevance as well as all documents of an unlimited search are passed on
   * in an implementation defined order.
   *
   * @param aQuery
   *        Query to execute. May not be <code>null</code>.
   * @param nMaxResultCount
   *        Maximum number of results. Values &le; 0 mean all.
   * @param aConsumer
   *        The consumer of the matching documents. May not be <code>null</code>.
   * @return The total number of documents matching the query, independent of the provided maximum
   *         result count. Always &ge; 0. Every search engine determines this number as a side
   *         effect of the search itself, so that no separate {@link #getCount(IPDIndexQuery)} call
   *         is necessary.
   * @throws IOException
   *         On index error
   */
  @CheckForSigned
  int searchAll (@NonNull IPDIndexQuery aQuery,
                 @CheckForSigned int nMaxResultCount,
                 @NonNull Consumer <? super PDIndexDocument> aConsumer) throws IOException;
}
