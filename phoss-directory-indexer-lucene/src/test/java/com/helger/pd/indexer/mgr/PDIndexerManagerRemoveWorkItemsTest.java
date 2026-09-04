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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.pd.indexer.index.EIndexerWorkItemType;
import com.helger.pd.indexer.lucene.PDLuceneIndexerTestRule;
import com.helger.pd.indexer.mgr.PDIndexerManager.RemoveWorkItemsResult;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;

/**
 * Test class for {@link PDIndexerManager#removeWorkItems(java.util.Collection)}.<br>
 * A Business Card provider that never finds anything is installed on purpose, so that no work item
 * can ever be indexed successfully. That keeps every queued item tracked for the whole test - it is
 * either in the work queue or in the re-index list - and makes the assertions deterministic. The
 * exact distribution over the two is not asserted, because the collectors work the queue
 * concurrently.
 *
 * @author Philip Helger
 */
public final class PDIndexerManagerRemoveWorkItemsTest
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
  public void testRemoveWorkItems ()
  {
    PDMetaManager.getInstance ();
    final PDIndexerManager aIndexerMgr = PDMetaManager.getIndexerMgr ();

    final ICommonsList <IParticipantIdentifier> aKept = new CommonsArrayList <> ();
    final ICommonsList <IParticipantIdentifier> aRemoved = new CommonsArrayList <> ();
    for (int i = 0; i < 10; ++i)
    {
      aKept.add (PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:keep" + i));
      aRemoved.add (PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:remove" + i));
    }

    final ICommonsList <IParticipantIdentifier> aAll = new CommonsArrayList <> (aKept);
    aAll.addAll (aRemoved);
    assertEquals (aAll.size (),
                  aIndexerMgr.queueWorkItems (aAll,
                                              EIndexerWorkItemType.CREATE_UPDATE,
                                              OWNER_ID,
                                              PDIndexerManager.HOST_LOCALHOST).getQueuedCount ());

    aIndexerMgr.removeWorkItems (aRemoved);

    // The removed ones are no longer tracked, so they can be queued again
    assertEquals (aRemoved.size (),
                  aIndexerMgr.queueWorkItems (aRemoved,
                                              EIndexerWorkItemType.CREATE_UPDATE,
                                              OWNER_ID,
                                              PDIndexerManager.HOST_LOCALHOST).getQueuedCount ());

    // The other ones were left alone and are therefore still tracked
    assertEquals (0,
                  aIndexerMgr.queueWorkItems (aKept,
                                              EIndexerWorkItemType.CREATE_UPDATE,
                                              OWNER_ID,
                                              PDIndexerManager.HOST_LOCALHOST).getQueuedCount ());
  }

  @Test
  public void testRemoveWorkItemsOfUnknownParticipants ()
  {
    PDMetaManager.getInstance ();
    final PDIndexerManager aIndexerMgr = PDMetaManager.getIndexerMgr ();

    final RemoveWorkItemsResult aResult = aIndexerMgr.removeWorkItems (new CommonsArrayList <> (PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:unknown")));
    assertEquals (0, aResult.getTotalCount ());
    assertEquals (0, aResult.getQueueCount ());
    assertEquals (0, aResult.getReIndexListCount ());
    assertEquals (0, aResult.getDeadListCount ());
  }

  @Test
  public void testRemoveWorkItemsEmptyList ()
  {
    PDMetaManager.getInstance ();
    final PDIndexerManager aIndexerMgr = PDMetaManager.getIndexerMgr ();

    final RemoveWorkItemsResult aResult = aIndexerMgr.removeWorkItems (new CommonsArrayList <> ());
    assertEquals (0, aResult.getTotalCount ());
  }
}
