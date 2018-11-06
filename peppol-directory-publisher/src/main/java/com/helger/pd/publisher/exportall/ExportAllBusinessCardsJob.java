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
package com.helger.pd.publisher.exportall;

import java.io.IOException;
import java.util.concurrent.locks.Lock;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.concurrent.SimpleLock;
import com.helger.pd.indexer.storage.EQueryMode;
import com.helger.poi.excel.WorkbookCreationHelper;
import com.helger.quartz.DisallowConcurrentExecution;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.JobExecutionException;
import com.helger.web.scope.util.AbstractScopeAwareJob;
import com.helger.xml.microdom.IMicroDocument;

/**
 * Job to export all BCs regularly to disk.
 *
 * @author Philip Helger
 */
@DisallowConcurrentExecution
public final class ExportAllBusinessCardsJob extends AbstractScopeAwareJob
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ExportAllBusinessCardsJob.class);

  private static final Lock s_aLock = new SimpleLock ();

  public static void exportAllBusinessCards () throws IOException
  {
    // Avoid running it in parallel
    s_aLock.lock ();
    try
    {
      LOGGER.info ("Start exporting business cards as XML");
      try
      {
        final IMicroDocument aDoc = ExportAllManager.getAllContainedBusinessCardsAsXML (EQueryMode.NON_DELETED_ONLY);
        ExportAllManager.writeFileXML (aDoc);
      }
      finally
      {
        LOGGER.info ("Finished exporting business cards as XML");
      }

      LOGGER.info ("Start exporting business cards as Excel");
      try
      {
        final WorkbookCreationHelper aWBCH = ExportAllManager.getAllContainedBusinessCardsAsExcel (EQueryMode.NON_DELETED_ONLY);
        ExportAllManager.writeFileExcel (aWBCH::writeTo);
      }
      finally
      {
        LOGGER.info ("Finished exporting business cards as Excel");
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
