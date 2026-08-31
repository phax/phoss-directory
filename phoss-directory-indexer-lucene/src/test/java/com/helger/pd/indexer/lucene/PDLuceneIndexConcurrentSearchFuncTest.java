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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.NonNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.pd.indexer.conformance.PDConformanceTestData;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryMatchAll;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.pd.indexer.storage.field.PDField;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * Test that a search never returns the document of a different participant, even if the index is
 * modified while the search is running. See security advisory GHSA-8qhv-6p5x-2437.
 *
 * @author Philip Helger
 */
public final class PDLuceneIndexConcurrentSearchFuncTest
{
  private static final int PARTICIPANT_COUNT = 100;
  private static final int SEARCH_THREAD_COUNT = 4;
  private static final int SEARCHES_PER_THREAD = 100;
  private static final int MAX_RESULT_COUNT = 10;

  @Rule
  public final TestRule m_aRule = new PDLuceneIndexerTestRule ();

  @NonNull
  private static IParticipantIdentifier _createParticipantID (final int nIndex)
  {
    return PDMetaManager.getIdentifierFactory ()
                        .createParticipantIdentifier ("iso6523-actorid-upis", "9915:test" + nIndex);
  }

  @Test
  public void testSearchResultBelongsToTheQueriedParticipant () throws Exception
  {
    final ConcurrentLinkedQueue <String> aErrors = new ConcurrentLinkedQueue <> ();

    try (final PDLuceneIndex aIndex = new PDLuceneIndex ())
    {
      // Fill the index
      for (int i = 0; i < PARTICIPANT_COUNT; ++i)
      {
        final IParticipantIdentifier aParticipantID = _createParticipantID (i);
        aIndex.updateDocuments (PDField.PARTICIPANT_ID.getExactMatchQuery (aParticipantID),
                                PDConformanceTestData.createMockIndexDocuments (aParticipantID));
      }
      assertEquals (PARTICIPANT_COUNT * 2, aIndex.getCount (PDIndexQueryMatchAll.INSTANCE));

      // Constantly modify the index, so that the index reader is reopened and the internal
      // document IDs are reassigned by the segment merges
      final AtomicBoolean aWriterRunning = new AtomicBoolean (true);
      final Thread aWriterThread = new Thread ( () -> {
        int nIndex = 0;
        while (aWriterRunning.get ())
        {
          final IParticipantIdentifier aParticipantID = _createParticipantID (nIndex % PARTICIPANT_COUNT);
          try
          {
            aIndex.updateDocuments (PDField.PARTICIPANT_ID.getExactMatchQuery (aParticipantID),
                                    PDConformanceTestData.createMockIndexDocuments (aParticipantID));
          }
          catch (final Exception ex)
          {
            aErrors.add ("Indexing failed: " + ex.getClass ().getName () + " - " + ex.getMessage ());
            break;
          }
          nIndex++;
        }
      }, "pd-lucene-writer");

      // Search in parallel to the modifications
      final Thread [] aSearchThreads = new Thread [SEARCH_THREAD_COUNT];
      for (int nThread = 0; nThread < SEARCH_THREAD_COUNT; ++nThread)
      {
        final int nThreadIndex = nThread;
        aSearchThreads[nThread] = new Thread ( () -> {
          for (int i = 0; i < SEARCHES_PER_THREAD; ++i)
          {
            final IParticipantIdentifier aParticipantID = _createParticipantID ((nThreadIndex * SEARCHES_PER_THREAD + i) %
                                                                               PARTICIPANT_COUNT);
            try
            {
              aIndex.searchAll (PDField.PARTICIPANT_ID.getExactMatchQuery (aParticipantID),
                                MAX_RESULT_COUNT,
                                aDoc -> {
                                  final IParticipantIdentifier aFoundID = PDStoredBusinessEntity.create (aDoc)
                                                                                                .getParticipantID ();
                                  if (!aParticipantID.equals (aFoundID))
                                    aErrors.add ("Search for '" +
                                                 aParticipantID.getURIEncoded () +
                                                 "' returned the document of '" +
                                                 (aFoundID == null ? "null" : aFoundID.getURIEncoded ()) +
                                                 "'");
                                });
            }
            catch (final Exception ex)
            {
              aErrors.add ("Search failed: " + ex.getClass ().getName () + " - " + ex.getMessage ());
              break;
            }
          }
        }, "pd-lucene-search-" + nThread);
      }

      aWriterThread.start ();
      for (final Thread aSearchThread : aSearchThreads)
        aSearchThread.start ();
      for (final Thread aSearchThread : aSearchThreads)
        aSearchThread.join ();
      aWriterRunning.set (false);
      aWriterThread.join ();
    }

    assertTrue (aErrors.size () + " error(s) occurred: " + aErrors, aErrors.isEmpty ());
  }
}
