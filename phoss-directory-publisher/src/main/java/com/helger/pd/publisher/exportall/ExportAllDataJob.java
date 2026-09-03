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
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.timing.StopWatch;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSortedSet;
import com.helger.datetime.helper.PDTFactory;
import com.helger.pd.publisher.CPDPublisher;
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

  private static void _collectResult (@NonNull final String sStatus,
                                      @NonNull final Future <ICommonsList <String>> aFuture)
  {
    try
    {
      for (final String sFailedStatus : aFuture.get ())
        EXPORT_STATUS.rememberFailedStatus (sFailedStatus);
    }
    catch (final InterruptedException ex)
    {
      Thread.currentThread ().interrupt ();
      LOGGER.error ("Interrupted while waiting for export group '" + sStatus + "'", ex);
      EXPORT_STATUS.rememberFailedStatus (sStatus);
    }
    catch (final ExecutionException ex)
    {
      LOGGER.error ("Export group '" + sStatus + "' failed", ex.getCause ());
      EXPORT_STATUS.rememberFailedStatus (sStatus);
    }
  }

  public static void exportAllBusinessCards ()
  {
    // Avoid running it in parallel
    if (EXPORT_STATUS.start ())
    {
      final StopWatch aSW = StopWatch.createdStarted ();

      final String sLogPrefix = "[EXPORT-ALL-JOB | " + EXPORT_STATUS.getExportAllBusinessCardsStartDT () + "] ";

      try
      {
        aSW.restart ();
        LOGGER.info (sLogPrefix + "Starting to gather all participant IDs from the index");
        final ICommonsSortedSet <String> aAllParticipantIDs;
        try
        {
          EXPORT_STATUS.setCurrentStatus ("getAllStoredParticipantIDs");
          aAllParticipantIDs = ExportAllManager.getAllStoredParticipantIDs ();
        }
        catch (final IOException ex)
        {
          LOGGER.error (sLogPrefix + "Error gathering all participant IDs from the index", ex);
          EXPORT_STATUS.rememberFailedStatus ("getAllStoredParticipantIDs");

          // We can't continue
          throw new UncheckedIOException (ex);
        }
        aSW.stop ();
        LOGGER.info (sLogPrefix +
                     "Finished gathering all participant IDs (" +
                     aAllParticipantIDs.size () +
                     ") from the index after " +
                     aSW.getDuration () +
                     " milliseconds");

        aSW.restart ();
        LOGGER.info (sLogPrefix + "Start writing all export files in parallel");
        EXPORT_STATUS.setCurrentStatus ("writeAllExportFiles");
        try (final ExecutorService aExecutor = Executors.newVirtualThreadPerTaskExecutor ())
        {
          final Future <ICommonsList <String>> aBusinessCardFuture = aExecutor.submit ( () -> ExportAllManager.writeAllBusinessCardFiles (aAllParticipantIDs,
                                                                                                                                          CPDPublisher.EXPORT_BUSINESS_CARDS_XML,
                                                                                                                                          CPDPublisher.EXPORT_BUSINESS_CARDS_JSON,
                                                                                                                                          CPDPublisher.EXPORT_BUSINESS_CARDS_CSV));
          final Future <ICommonsList <String>> aParticipantFuture = aExecutor.submit ( () -> ExportAllManager.writeAllParticipantFiles (aAllParticipantIDs,
                                                                                                                                        CPDPublisher.EXPORT_PARTICIPANTS_XML,
                                                                                                                                        CPDPublisher.EXPORT_PARTICIPANTS_JSON,
                                                                                                                                        CPDPublisher.EXPORT_PARTICIPANTS_CSV));

          _collectResult ("writeAllBusinessCardFiles", aBusinessCardFuture);
          _collectResult ("writeAllParticipantFiles", aParticipantFuture);
        }
        finally
        {
          aSW.stop ();
          LOGGER.info (sLogPrefix +
                       "Finished writing all export files after " +
                       aSW.getDuration () +
                       " milliseconds");
        }
      }
      finally
      {
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
