/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.timing.StopWatch;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsTreeSet;
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

import jakarta.annotation.Nonnull;
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

    void rememberFailedStatus ()
    {
      m_aFailedStatus.add (m_sCurrentStatus);
    }

    @Nonnull
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

  @Nonnull
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

      final String sLogPrefix = "[EXPORT-ALL-JOB | " + EXPORT_STATUS.getExportAllBusinessCardsStartDT () + "] ";

      try
      {
        aSW.restart ();
        LOGGER.info (sLogPrefix + "Starting to gather all participant IDs from Lucene");
        ICommonsSortedSet <String> aAllParticipantIDs = new CommonsTreeSet <> ();
        try
        {
          EXPORT_STATUS.setCurrentStatus ("getAllStoredParticipantIDs");
          aAllParticipantIDs = ExportAllManager.getAllStoredParticipantIDs ();
        }
        catch (final IOException ex)
        {
          LOGGER.error (sLogPrefix + "Error gathering all participant IDs from Lucene", ex);
          EXPORT_STATUS.rememberFailedStatus ();

          // We can't continue
          throw new UncheckedIOException (ex);
        }
        finally
        {
          aSW.stop ();
          LOGGER.info (sLogPrefix +
                       "Finished gathering all participant IDs (" +
                       aAllParticipantIDs.size () +
                       ") from Lucene after " +
                       aSW.getDuration () +
                       " milliseconds");
        }

        if (CPDPublisher.EXPORT_BUSINESS_CARDS_XML)
        {
          aSW.restart ();
          LOGGER.info (sLogPrefix + "Start exporting business cards as XML (full)");
          try
          {
            EXPORT_STATUS.setCurrentStatus ("writeFileBusinessCardXMLFull");
            ExportAllManager.writeFileBusinessCardXMLFull (aAllParticipantIDs);
          }
          catch (final Throwable t)
          {
            LOGGER.error (sLogPrefix + "Error exporting business cards as XML (full)", t);
            EXPORT_STATUS.rememberFailedStatus ();
          }
          finally
          {
            aSW.stop ();
            LOGGER.info (sLogPrefix +
                         "Finished exporting business cards as XML (full) after " +
                         aSW.getDuration () +
                         " milliseconds");
          }

          aSW.restart ();
          LOGGER.info (sLogPrefix + "Start exporting business cards as XML (no doc types)");
          try
          {
            EXPORT_STATUS.setCurrentStatus ("writeFileBusinessCardXMLNoDocTypes");
            ExportAllManager.writeFileBusinessCardXMLNoDocTypes (aAllParticipantIDs);
          }
          catch (final Throwable t)
          {
            LOGGER.error (sLogPrefix + "Error exporting business cards as XML (no doc types)", t);
            EXPORT_STATUS.rememberFailedStatus ();
          }
          finally
          {
            aSW.stop ();
            LOGGER.info (sLogPrefix +
                         "Finished exporting business cards as XML (no doc types) after " +
                         aSW.getDuration () +
                         " milliseconds");
          }
        }

        if (CPDPublisher.EXPORT_BUSINESS_CARDS_JSON)
        {
          aSW.restart ();
          LOGGER.info (sLogPrefix + "Start exporting business cards as JSON");
          try
          {
            EXPORT_STATUS.setCurrentStatus ("writeFileBusinessCardJSON");
            ExportAllManager.writeFileBusinessCardJSON (aAllParticipantIDs);
          }
          catch (final Throwable t)
          {
            LOGGER.error (sLogPrefix + "Error exporting business cards as JSON", t);
            EXPORT_STATUS.rememberFailedStatus ();
          }
          finally
          {
            aSW.stop ();
            LOGGER.info (sLogPrefix +
                         "Finished exporting business cards as JSON after " +
                         aSW.getDuration () +
                         " milliseconds");
          }
        }

        if (CPDPublisher.EXPORT_BUSINESS_CARDS_CSV)
        {
          aSW.restart ();
          LOGGER.info (sLogPrefix + "Start exporting business cards as CSV");
          try
          {
            EXPORT_STATUS.setCurrentStatus ("writeFileBusinessCardCSV");
            ExportAllManager.writeFileBusinessCardCSV (aAllParticipantIDs);
          }
          catch (final Throwable t)
          {
            LOGGER.error (sLogPrefix + "Error exporting business cards as CSV", t);
            EXPORT_STATUS.rememberFailedStatus ();
          }
          finally
          {
            aSW.stop ();
            LOGGER.info (sLogPrefix +
                         "Finished exporting business cards as CSV after " +
                         aSW.getDuration () +
                         " milliseconds");
          }
        }

        if (CPDPublisher.EXPORT_PARTICIPANTS_XML)
        {
          aSW.restart ();
          LOGGER.info (sLogPrefix + "Start exporting participants as XML");
          try
          {
            EXPORT_STATUS.setCurrentStatus ("writeFileParticipantXML");
            ExportAllManager.writeFileParticipantXML (aAllParticipantIDs);
          }
          catch (final Throwable t)
          {
            LOGGER.error (sLogPrefix + "Error exporting participants cards as XML", t);
            EXPORT_STATUS.rememberFailedStatus ();
          }
          finally
          {
            aSW.stop ();
            LOGGER.info (sLogPrefix +
                         "Finished exporting participants as XML after " +
                         aSW.getDuration () +
                         " milliseconds");
          }
        }

        if (CPDPublisher.EXPORT_PARTICIPANTS_JSON)
        {
          aSW.restart ();
          LOGGER.info (sLogPrefix + "Start exporting participants as JSON");
          try
          {
            EXPORT_STATUS.setCurrentStatus ("writeFileParticipantJSON");
            ExportAllManager.writeFileParticipantJSON (aAllParticipantIDs);
          }
          catch (final Throwable t)
          {
            LOGGER.error (sLogPrefix + "Error exporting participants cards as JSON", t);
            EXPORT_STATUS.rememberFailedStatus ();
          }
          finally
          {
            aSW.stop ();
            LOGGER.info (sLogPrefix +
                         "Finished exporting participants as JSON after " +
                         aSW.getDuration () +
                         " milliseconds");
          }
        }

        if (CPDPublisher.EXPORT_PARTICIPANTS_CSV)
        {
          aSW.restart ();
          LOGGER.info (sLogPrefix + "Start exporting participants as CSV");
          try
          {
            EXPORT_STATUS.setCurrentStatus ("writeFileParticipantCSV");
            ExportAllManager.writeFileParticipantCSV (aAllParticipantIDs);
          }
          catch (final Throwable t)
          {
            LOGGER.error (sLogPrefix + "Error exporting participants cards as CSV", t);
            EXPORT_STATUS.rememberFailedStatus ();
          }
          finally
          {
            aSW.stop ();
            LOGGER.info (sLogPrefix +
                         "Finished exporting participants as CSV after " +
                         aSW.getDuration () +
                         " milliseconds");
          }
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
  protected void onExecute (@Nonnull final JobDataMap aJobDataMap, @Nonnull final IJobExecutionContext aContext)
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
