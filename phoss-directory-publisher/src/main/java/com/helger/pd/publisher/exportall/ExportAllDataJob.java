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
package com.helger.pd.publisher.exportall;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.string.StringImplode;
import com.helger.base.timing.StopWatch;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsTreeSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSortedSet;
import com.helger.datetime.helper.PDTFactory;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.io.PhotonWorkerPool;
import com.helger.quartz.DisallowConcurrentExecution;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.JobExecutionException;
import com.helger.web.scope.util.AbstractScopeAwareJob;

import jakarta.annotation.Nullable;

/**
 * Job to export all BCs regularly to disk.
 *
 * @author Philip Helger
 */
@DisallowConcurrentExecution
public final class ExportAllDataJob extends AbstractScopeAwareJob
{
  /** The audit action logged when the export of all data starts */
  public static final String AUDIT_ACTION_START = "export-all-start";
  /** The audit action logged when all participant IDs were gathered from the search index */
  public static final String AUDIT_ACTION_PARTICIPANT_IDS = "export-all-participant-ids";
  /** The audit action logged when all export formats were created and uploaded */
  public static final String AUDIT_ACTION_FORMATS = "export-all-formats";
  /** The audit action logged when the export of all data is finished */
  public static final String AUDIT_ACTION_END = "export-all-end";

  private static final Logger LOGGER = LoggerFactory.getLogger (ExportAllDataJob.class);

  public static final class ExportAllStatus
  {
    private final AtomicBoolean m_aRunning = new AtomicBoolean (false);
    private LocalDateTime m_aStartDT;
    private String m_sCurrentStatus;
    private LocalDateTime m_aLastStatusChangeDT;
    private final ICommonsList <String> m_aFailedStatus = new CommonsArrayList <> ();

    boolean start ()
    {
      if (m_aRunning.getAndSet (true))
        return false;
      m_aStartDT = PDTFactory.getCurrentLocalDateTime ();
      return true;
    }

    public boolean isRunning ()
    {
      return m_aRunning.get ();
    }

    @Nullable
    public LocalDateTime getExportAllBusinessCardsStartDT ()
    {
      // Start in background
      return m_aStartDT;
    }

    void setCurrentStatus (@Nullable final String sStatus)
    {
      m_sCurrentStatus = sStatus;
      m_aLastStatusChangeDT = PDTFactory.getCurrentLocalDateTime ();
    }

    @Nullable
    public String getCurrentStatus ()
    {
      return m_sCurrentStatus;
    }

    @Nullable
    public LocalDateTime getLastStatusChangeDT ()
    {
      return m_aLastStatusChangeDT;
    }

    void rememberFailedStatus (@NonNull final String sStatus)
    {
      m_aFailedStatus.add (sStatus);
    }

    @NonNull
    public ICommonsList <String> getAllFailedStatus ()
    {
      return m_aFailedStatus.getClone ();
    }

    void end ()
    {
      m_aStartDT = null;
      m_sCurrentStatus = null;
      m_aFailedStatus.clear ();
      m_aRunning.set (false);
    }
  }

  private static final ExportAllStatus EXPORT_STATUS = new ExportAllStatus ();

  @NonNull
  public static ExportAllStatus getExportStatus ()
  {
    return EXPORT_STATUS;
  }

  public static void exportAllBusinessCardsInBackground ()
  {
    // Start in background
    PhotonWorkerPool.getInstance ().runThrowing ("ExportAllBusinessCards", ExportAllDataJob::exportAllBusinessCards);
  }

  public static void exportAllBusinessCards ()
  {
    // Avoid running it in parallel
    if (EXPORT_STATUS.start ())
    {
      final StopWatch aSW = StopWatch.createdStarted ();
      // The overall duration is part of every audit item, so that the audit trail alone shows how
      // long the export took up to that point
      final StopWatch aSWTotal = StopWatch.createdStarted ();

      final LocalDateTime aStartDT = EXPORT_STATUS.getExportAllBusinessCardsStartDT ();
      final String sLogPrefix = "[EXPORT-ALL-JOB | " + aStartDT + "] ";

      AuditHelper.onAuditExecuteSuccess (AUDIT_ACTION_START, aStartDT, aSWTotal.getDuration ());

      try
      {
        // Step 1: gather all participant IDs with a single index query
        aSW.restart ();
        LOGGER.info (sLogPrefix + "Starting to gather all participant IDs from the index");
        ICommonsSortedSet <String> aAllParticipantIDs = new CommonsTreeSet <> ();
        try
        {
          EXPORT_STATUS.setCurrentStatus ("getAllStoredParticipantIDs");
          aAllParticipantIDs = ExportAllManager.getAllStoredParticipantIDs ();
        }
        catch (final IOException ex)
        {
          LOGGER.error (sLogPrefix + "Error gathering all participant IDs from the index", ex);
          EXPORT_STATUS.rememberFailedStatus ("getAllStoredParticipantIDs");
          AuditHelper.onAuditExecuteFailure (AUDIT_ACTION_PARTICIPANT_IDS,
                                             aSW.getDuration (),
                                             aSWTotal.getDuration (),
                                             ex.getMessage ());

          // We can't continue
          throw new UncheckedIOException (ex);
        }
        finally
        {
          aSW.stop ();
          LOGGER.info (sLogPrefix +
                       "Finished gathering all participant IDs (" +
                       aAllParticipantIDs.size () +
                       ") from the index after " +
                       aSW.getDuration () +
                       " milliseconds");
        }
        AuditHelper.onAuditExecuteSuccess (AUDIT_ACTION_PARTICIPANT_IDS,
                                           Integer.valueOf (aAllParticipantIDs.size ()),
                                           aSW.getDuration (),
                                           aSWTotal.getDuration ());

        // Step 2: create all export formats in a single pass, so that the index needs to be
        // queried only once per participant
        final ICommonsList <IExportAllHandler> aHandlers = ExportAllManager.createAllEnabledExportHandlers ();

        aSW.restart ();
        LOGGER.info (sLogPrefix + "Start exporting " + aHandlers.size () + " export format(s)");
        try
        {
          EXPORT_STATUS.setCurrentStatus ("exportAll");
          for (final String sFailedHandlerName : ExportAllManager.exportAll (aAllParticipantIDs,
                                                                             aHandlers,
                                                                             EXPORT_STATUS::setCurrentStatus))
          {
            EXPORT_STATUS.rememberFailedStatus (sFailedHandlerName);
          }
        }
        catch (final Throwable t)
        {
          LOGGER.error (sLogPrefix + "Error creating the export formats", t);
          EXPORT_STATUS.rememberFailedStatus ("exportAll");
        }
        finally
        {
          aSW.stop ();
          LOGGER.info (sLogPrefix +
                       "Finished exporting all export format(s) after " +
                       aSW.getDuration () +
                       " milliseconds");
        }

        // Only step 2 can fail without throwing, so the remembered status is exactly what failed
        // in this step
        final ICommonsList <String> aFailed = EXPORT_STATUS.getAllFailedStatus ();
        if (aFailed.isEmpty ())
          AuditHelper.onAuditExecuteSuccess (AUDIT_ACTION_FORMATS,
                                             Integer.valueOf (aHandlers.size ()),
                                             aSW.getDuration (),
                                             aSWTotal.getDuration ());
        else
          AuditHelper.onAuditExecuteFailure (AUDIT_ACTION_FORMATS,
                                             Integer.valueOf (aHandlers.size ()),
                                             aSW.getDuration (),
                                             aSWTotal.getDuration (),
                                             StringImplode.getImploded (", ", aFailed));
      }
      finally
      {
        // The status is cleared by "end", so it must be evaluated before
        final ICommonsList <String> aFailed = EXPORT_STATUS.getAllFailedStatus ();
        final Duration aTotalDuration = aSWTotal.stopAndGetDuration ();
        if (aFailed.isEmpty ())
          AuditHelper.onAuditExecuteSuccess (AUDIT_ACTION_END, aTotalDuration);
        else
          AuditHelper.onAuditExecuteFailure (AUDIT_ACTION_END,
                                             aTotalDuration,
                                             StringImplode.getImploded (", ", aFailed));

        EXPORT_STATUS.end ();
      }
    }
    else
    {
      LOGGER.warn ("Export is already running, so avoiding a parallel run");
    }
  }

  @Override
  protected void onExecute (@NonNull final JobDataMap aJobDataMap, @NonNull final IJobExecutionContext aContext)
                                                                                                                 throws JobExecutionException
  {
    try
    {
      exportAllBusinessCards ();
    }
    catch (final RuntimeException ex)
    {
      throw new JobExecutionException ("Error exporting all business cards", ex);
    }
  }
}
