/*
 * Copyright (C) 2015-2022 Philip Helger (www.helger.com)
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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTFromString;
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.commons.state.EChange;
import com.helger.pd.indexer.index.EIndexerWorkItemType;
import com.helger.pd.indexer.mgr.PDIndexerManager;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.storage.CPDStorage;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.photon.app.io.WebFileIO;
import com.helger.photon.audit.AuditHelper;
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

  private static final LocalDateTime INITIAL_SYNC = PDTFactory.createLocalDateTime (2018, Month.NOVEMBER, 7, 12, 0, 0);

  @Nonnull
  private static File _getLastSyncFile ()
  {
    return WebFileIO.getDataIO ().getFile ("last-sync.dat");
  }

  @Nonnull
  public static LocalDateTime getLastSync ()
  {
    final String sPayload = SimpleFileIO.getFileAsString (_getLastSyncFile (), StandardCharsets.ISO_8859_1);
    final LocalDateTime ret = PDTFromString.getLocalDateTimeFromString (sPayload, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    return ret == null ? INITIAL_SYNC : ret;
  }

  public static void _setLastSync (@Nonnull final LocalDateTime aLastSyncDT)
  {
    final String sPayload = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format (aLastSyncDT);
    SimpleFileIO.writeFile (_getLastSyncFile (), sPayload, StandardCharsets.ISO_8859_1);
  }

  @Nonnull
  public static EChange syncAllBusinessCards (final boolean bForceSync)
  {
    final LocalDateTime aNow = PDTFactory.getCurrentLocalDateTime ();
    if (!bForceSync)
    {
      // Only sync every 2 weeks
      if (aNow.isBefore (getLastSync ().plusWeeks (2)))
      {
        return EChange.UNCHANGED;
      }
    }

    LOGGER.info ("Start synchronizing business cards" + (bForceSync ? " (forced)" : ""));
    final PDIndexerManager aIndexerMgr = PDMetaManager.getIndexerMgr ();
    // Queue a work item to re-scan all
    final Set <IParticipantIdentifier> aAll = PDMetaManager.getStorageMgr ().getAllContainedParticipantIDs ().keySet ();
    for (final IParticipantIdentifier aParticipantID : aAll)
    {
      aIndexerMgr.queueWorkItem (aParticipantID, EIndexerWorkItemType.SYNC, CPDStorage.OWNER_SYNC_JOB, PDIndexerManager.HOST_LOCALHOST);
    }
    LOGGER.info ("Finished synchronizing of " + aAll.size () + " business cards");
    AuditHelper.onAuditExecuteSuccess ("sync-bc-started", Integer.valueOf (aAll.size ()), aNow, Boolean.valueOf (bForceSync));
    _setLastSync (aNow);

    return EChange.CHANGED;
  }

  @Override
  protected void onExecute (@Nonnull final JobDataMap aJobDataMap,
                            @Nonnull final IJobExecutionContext aContext) throws JobExecutionException
  {
    // Ignore result - not forced
    syncAllBusinessCards (false);
  }
}
