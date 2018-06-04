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

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.pd.indexer.mgr.PDMetaManager;
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
  private static final Logger s_aLogger = LoggerFactory.getLogger (ExportAllBusinessCardsJob.class);

  public static void exportAllBusinessCards () throws IOException
  {
    s_aLogger.info ("Start exporting business cards");
    try
    {
      final IMicroDocument aDoc = PDMetaManager.getStorageMgr ().getAllContainedBusinessCardsAsXML ();
      ExportAllManager.writeFile (aDoc);
    }
    finally
    {
      s_aLogger.info ("Finished exporting business cards");
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
