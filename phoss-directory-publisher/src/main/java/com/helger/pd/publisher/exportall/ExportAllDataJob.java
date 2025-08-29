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
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.timing.StopWatch;
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

  private static final AtomicBoolean EXPORT_RUNNING = new AtomicBoolean (false);
  private static LocalDateTime EXPORT_START_DT;

  public static boolean isExportCurrentlyRunning ()
  {
    return EXPORT_RUNNING.get ();
  }

  @Nullable
  public static LocalDateTime getExportAllBusinessCardsStartDT ()
  {
    // Start in background
    return EXPORT_START_DT;
  }

  public static void exportAllBusinessCardsInBackground ()
  {
    // Start in background
    PhotonWorkerPool.getInstance ().runThrowing ("ExportAllBusinessCards", ExportAllDataJob::exportAllBusinessCards);
  }

  public static void exportAllBusinessCards () throws IOException
  {
    // Avoid running it in parallel
    if (!EXPORT_RUNNING.getAndSet (true))
    {
      EXPORT_START_DT = PDTFactory.getCurrentLocalDateTime ();
      final StopWatch aSW = StopWatch.createdStarted ();

      try
      {
        LOGGER.info ("Start exporting business cards as XML (full)");
        try
        {
          ExportAllManager.writeFileBusinessCardXMLFull ();
        }
        finally
        {
          LOGGER.info ("Finished exporting business cards as XML (full) after " +
                       aSW.stopAndGetMillis () +
                       " milliseconds");
        }

        aSW.restart ();
        LOGGER.info ("Start exporting business cards as XML (no doc types)");
        try
        {
          ExportAllManager.writeFileBusinessCardXMLNoDocTypes ();
        }
        finally
        {
          LOGGER.info ("Finished exporting business cards as XML (no doc types) after " +
                       aSW.stopAndGetMillis () +
                       " milliseconds");
        }

        if (CPDPublisher.EXPORT_BUSINESS_CARDS_JSON)
        {
          aSW.restart ();
          LOGGER.info ("Start exporting business cards as JSON");
          try
          {
            ExportAllManager.writeFileBusinessCardJSON ();
          }
          finally
          {
            LOGGER.info ("Finished exporting business cards as JSON after " +
                         aSW.stopAndGetMillis () +
                         " milliseconds");
          }
        }

        if (CPDPublisher.EXPORT_BUSINESS_CARDS_EXCEL)
        {
          aSW.restart ();
          LOGGER.info ("Start exporting business cards as Excel");
          try
          {
            ExportAllManager.writeFileBusinessCardExcel ();
          }
          finally
          {
            LOGGER.info ("Finished exporting business cards as Excel after " +
                         aSW.stopAndGetMillis () +
                         " milliseconds");
          }
        }

        if (CPDPublisher.EXPORT_BUSINESS_CARDS_CSV)
        {
          aSW.restart ();
          LOGGER.info ("Start exporting business cards as CSV");
          try
          {
            ExportAllManager.writeFileBusinessCardCSV ();
          }
          finally
          {
            LOGGER.info ("Finished exporting business cards as CSV after " + aSW.stopAndGetMillis () + " milliseconds");
          }
        }

        if (CPDPublisher.EXPORT_PARTICIPANTS_XML)
        {
          aSW.restart ();
          LOGGER.info ("Start exporting participants as XML");
          try
          {
            ExportAllManager.writeFileParticipantXML ();
          }
          finally
          {
            LOGGER.info ("Finished exporting participants as XML after " + aSW.stopAndGetMillis () + " milliseconds");
          }
        }

        if (CPDPublisher.EXPORT_PARTICIPANTS_JSON)
        {
          aSW.restart ();
          LOGGER.info ("Start exporting participants as JSON");
          try
          {
            ExportAllManager.writeFileParticipantJSON ();
          }
          finally
          {
            LOGGER.info ("Finished exporting participants as JSON after " + aSW.stopAndGetMillis () + " milliseconds");
          }
        }

        if (CPDPublisher.EXPORT_PARTICIPANTS_CSV)
        {
          aSW.restart ();
          LOGGER.info ("Start exporting participants as CSV");
          try
          {
            ExportAllManager.writeFileParticipantCSV ();
          }
          finally
          {
            LOGGER.info ("Finished exporting participants as CSV after " + aSW.stopAndGetMillis () + " milliseconds");
          }
        }
      }
      finally
      {
        EXPORT_START_DT = null;
        EXPORT_RUNNING.set (false);
      }
    }
    else
    {
      LOGGER.info ("Export is already running, so avoiding a parallel run");
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
    catch (final IOException ex)
    {
      throw new JobExecutionException ("Error exporting all business cards", ex);
    }
  }
}
