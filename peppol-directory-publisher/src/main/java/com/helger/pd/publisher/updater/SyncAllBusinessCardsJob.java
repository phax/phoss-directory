/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.updater;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.ICommonsSortedSet;
import com.helger.pd.indexer.index.EIndexerWorkItemType;
import com.helger.pd.indexer.mgr.PDIndexerManager;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.photon.basic.audit.AuditHelper;
import com.helger.quartz.DisallowConcurrentExecution;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.JobExecutionException;
import com.helger.web.scope.util.AbstractScopeAwareJob;

/**
 * Job to update all BCs regularly from the source SMP.
 *
 * @author Philip Helger
 */
@DisallowConcurrentExecution
public final class SyncAllBusinessCardsJob extends AbstractScopeAwareJob
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SyncAllBusinessCardsJob.class);

  public static void syncAllBusinessCards ()
  {
    LOGGER.info ("Start synchronizing business cards");
    final PDIndexerManager aIndexerMgr = PDMetaManager.getIndexerMgr ();
    // Queue a work item to re-scan all
    final ICommonsSortedSet <IParticipantIdentifier> aAll = PDMetaManager.getStorageMgr ()
                                                                         .getAllContainedParticipantIDs ();
    for (final IParticipantIdentifier aParticipantID : aAll)
    {
      aIndexerMgr.queueWorkItem (aParticipantID, EIndexerWorkItemType.SYNC, "sync-job", "localhost");
    }
    LOGGER.info ("Finished synchronizing of " + aAll.size () + " business cards");
    AuditHelper.onAuditExecuteSuccess ("sync-bc-started", Integer.valueOf (aAll.size ()));
  }

  @Override
  protected void onExecute (@Nonnull final JobDataMap aJobDataMap,
                            @Nonnull final IJobExecutionContext aContext) throws JobExecutionException
  {
    syncAllBusinessCards ();
  }
}
