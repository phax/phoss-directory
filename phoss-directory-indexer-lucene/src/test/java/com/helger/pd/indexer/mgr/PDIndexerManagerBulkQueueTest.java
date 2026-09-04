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
package com.helger.pd.indexer.mgr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.pd.indexer.index.EIndexerWorkItemType;
import com.helger.pd.indexer.lucene.PDLuceneIndexerTestRule;
import com.helger.pd.indexer.mgr.PDIndexerManager.BulkQueueResult;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;

/**
 * Test class for
 * {@link PDIndexerManager#queueWorkItems(java.util.Collection, EIndexerWorkItemType, String, String)}.
 * <br>
 * A Business Card provider that never finds anything is installed on purpose, so that no work item
 * can ever be indexed successfully. That keeps every queued item in the "unique items" set for the
 * whole test and makes the queued/not-queued accounting deterministic. Setting it explicitly is
 * required, because {@link PDMetaManager} keeps the provider in a static field that outlives the
 * global scope of a single test.
 *
 * @author Philip Helger
 */
public final class PDIndexerManagerBulkQueueTest
{
  private static final String OWNER_ID = "unit-test";

  @Rule
  public final TestRule m_aRule = new PDLuceneIndexerTestRule ();

  @Before
  public void setUpBusinessCardProvider ()
  {
    PDMetaManager.setBusinessCardProvider ((_, _) -> null);
  }

  @Test
  public void testBulkQueueing ()
  {
    PDMetaManager.getInstance ();
    final PDIndexerManager aIndexerMgr = PDMetaManager.getIndexerMgr ();

    final ICommonsList <IParticipantIdentifier> aParticipantIDs = new CommonsArrayList <> ();
    for (int i = 0; i < 20; ++i)
      aParticipantIDs.add (PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:bulk" +
                                                                                                          i));

    // All of them are new
    final BulkQueueResult aResult1 = aIndexerMgr.queueWorkItems (aParticipantIDs,
                                                                 EIndexerWorkItemType.CREATE_UPDATE,
                                                                 OWNER_ID,
                                                                 PDIndexerManager.HOST_LOCALHOST);
    assertEquals (aParticipantIDs.size (), aResult1.getQueuedCount ());
    assertEquals (0, aResult1.getNotQueuedCount ());
    assertEquals (aParticipantIDs, aResult1.getAllQueued ());
    assertTrue (aResult1.getAllNotQueued ().isEmpty ());

    // The very same ones are already in the queue/re-index list
    final BulkQueueResult aResult2 = aIndexerMgr.queueWorkItems (aParticipantIDs,
                                                                 EIndexerWorkItemType.CREATE_UPDATE,
                                                                 OWNER_ID,
                                                                 PDIndexerManager.HOST_LOCALHOST);
    assertEquals (0, aResult2.getQueuedCount ());
    assertEquals (aParticipantIDs.size (), aResult2.getNotQueuedCount ());
    assertEquals (aParticipantIDs, aResult2.getAllNotQueued ());

    // A different work item type is a different work item
    final BulkQueueResult aResult3 = aIndexerMgr.queueWorkItems (aParticipantIDs,
                                                                 EIndexerWorkItemType.SYNC,
                                                                 OWNER_ID,
                                                                 PDIndexerManager.HOST_LOCALHOST);
    assertEquals (aParticipantIDs.size (), aResult3.getQueuedCount ());
    assertEquals (0, aResult3.getNotQueuedCount ());
  }

  @Test
  public void testBulkQueueingWithDuplicatesInTheInput ()
  {
    PDMetaManager.getInstance ();
    final PDIndexerManager aIndexerMgr = PDMetaManager.getIndexerMgr ();

    final IParticipantIdentifier aPI1 = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:dup1");
    final IParticipantIdentifier aPI2 = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:dup2");

    final BulkQueueResult aResult = aIndexerMgr.queueWorkItems (new CommonsArrayList <> (aPI1, aPI2, aPI1, aPI2, aPI1),
                                                                EIndexerWorkItemType.CREATE_UPDATE,
                                                                OWNER_ID,
                                                                PDIndexerManager.HOST_LOCALHOST);
    assertEquals (2, aResult.getQueuedCount ());
    assertEquals (new CommonsArrayList <> (aPI1, aPI2), aResult.getAllQueued ());
    assertEquals (3, aResult.getNotQueuedCount ());
    assertEquals (new CommonsArrayList <> (aPI1, aPI2, aPI1), aResult.getAllNotQueued ());
  }

  @Test
  public void testBulkQueueingEmptyList ()
  {
    PDMetaManager.getInstance ();
    final PDIndexerManager aIndexerMgr = PDMetaManager.getIndexerMgr ();

    final BulkQueueResult aResult = aIndexerMgr.queueWorkItems (new CommonsArrayList <> (),
                                                                EIndexerWorkItemType.CREATE_UPDATE,
                                                                OWNER_ID,
                                                                PDIndexerManager.HOST_LOCALHOST);
    assertEquals (0, aResult.getQueuedCount ());
    assertEquals (0, aResult.getNotQueuedCount ());
  }
}
