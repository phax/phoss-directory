/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
import java.util.concurrent.locks.Lock;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.concurrent.SimpleLock;
import com.helger.commons.timing.StopWatch;
import com.helger.pd.indexer.storage.EQueryMode;
import com.helger.pd.publisher.CPDPublisher;
import com.helger.quartz.DisallowConcurrentExecution;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.JobExecutionException;
import com.helger.web.scope.util.AbstractScopeAwareJob;

/**
 * Job to export all BCs regularly to disk.
 *
 * @author Philip Helger
 */
@DisallowConcurrentExecution
public final class ExportAllDataJob extends AbstractScopeAwareJob
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ExportAllDataJob.class);

  private static final Lock s_aLock = new SimpleLock ();

  public static void exportAllBusinessCards () throws IOException
  {
    // Avoid running it in parallel
    s_aLock.lock ();
    try
    {
      final StopWatch aSW = StopWatch.createdStarted ();
      LOGGER.info ("Start exporting business cards as XML (full)");
      try
      {
        ExportAllManager.writeFileBusinessCardXMLFull (EQueryMode.NON_DELETED_ONLY);
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
        ExportAllManager.writeFileBusinessCardXMLNoDocTypes (EQueryMode.NON_DELETED_ONLY);
      }
      finally
      {
        LOGGER.info ("Finished exporting business cards as XML (no doc types) after " +
                     aSW.stopAndGetMillis () +
                     " milliseconds");
      }

      if (CPDPublisher.EXPORT_BUSINESS_CARDS_EXCEL)
      {
        aSW.restart ();
        LOGGER.info ("Start exporting business cards as Excel");
        try
        {
          ExportAllManager.writeFileBusinessCardExcel (EQueryMode.NON_DELETED_ONLY);
        }
        finally
        {
          LOGGER.info ("Finished exporting business cards as Excel after " + aSW.stopAndGetMillis () + " milliseconds");
        }
      }

      if (CPDPublisher.EXPORT_BUSINESS_CARDS_CSV)
      {
        aSW.restart ();
        LOGGER.info ("Start exporting business cards as CSV");
        try
        {
          ExportAllManager.writeFileBusinessCardCSV (EQueryMode.NON_DELETED_ONLY);
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
          ExportAllManager.writeFileParticipantXML (EQueryMode.NON_DELETED_ONLY);
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
          ExportAllManager.writeFileParticipantJSON (EQueryMode.NON_DELETED_ONLY);
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
          ExportAllManager.writeFileParticipantCSV (EQueryMode.NON_DELETED_ONLY);
        }
        finally
        {
          LOGGER.info ("Finished exporting participants as CSV after " + aSW.stopAndGetMillis () + " milliseconds");
        }
      }
    }
    finally
    {
      s_aLock.unlock ();
    }
  }

  @Override
  protected void onExecute (@Nonnull final JobDataMap aJobDataMap,
                            @Nonnull final IJobExecutionContext aContext) throws JobExecutionException
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
