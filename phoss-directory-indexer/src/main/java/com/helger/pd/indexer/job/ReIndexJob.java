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
package com.helger.pd.indexer.job;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.datetime.PDTFactory;
import com.helger.pd.indexer.mgr.PDIndexerManager;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.quartz.DisallowConcurrentExecution;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.JobExecutionException;
import com.helger.quartz.SimpleScheduleBuilder;
import com.helger.quartz.TriggerKey;
import com.helger.schedule.quartz.GlobalQuartzScheduler;
import com.helger.schedule.quartz.trigger.JDK8TriggerBuilder;
import com.helger.servlet.mock.MockHttpServletRequest;
import com.helger.servlet.mock.OfflineHttpServletRequest;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.web.scope.util.AbstractScopeAwareJob;

/**
 * A Quartz job that is scheduled to re-index existing entries that failed to
 * re-index previously.
 *
 * @author Philip Helger
 */
@DisallowConcurrentExecution
public class ReIndexJob extends AbstractScopeAwareJob
{
  private final ServletContext m_aSC;

  /**
   * Public no argument constructor must be available.
   */
  public ReIndexJob ()
  {
    // Save to avoid global scope access
    m_aSC = WebScopeManager.getGlobalScope ().getServletContext ();
  }

  @Override
  @Nonnull
  @OverrideOnDemand
  protected MockHttpServletRequest createMockHttpServletRequest ()
  {
    return new OfflineHttpServletRequest (m_aSC, false);
  }

  @Override
  protected void onExecute (@Nonnull final JobDataMap aJobDataMap,
                            @Nonnull final IJobExecutionContext aContext) throws JobExecutionException
  {
    final PDIndexerManager aIndexerMgr = PDMetaManager.getIndexerMgr ();

    // First expire all old entries
    aIndexerMgr.expireOldEntries ();

    // Re-index all items now
    aIndexerMgr.reIndexParticipantData ();
  }

  /**
   * @param aScheduleBuilder
   *        The schedule builder to be used. May not be <code>null</code>.
   *        Example:
   *        <code>SimpleScheduleBuilder.repeatMinutelyForever (1)</code>
   * @return The created trigger key for further usage. Never <code>null</code>.
   */
  @Nonnull
  public static TriggerKey schedule (@Nonnull final SimpleScheduleBuilder aScheduleBuilder)
  {
    ValueEnforcer.notNull (aScheduleBuilder, "ScheduleBuilder");

    final ICommonsMap <String, Object> aJobDataMap = new CommonsHashMap <> ();

    return GlobalQuartzScheduler.getInstance ()
                                .scheduleJob (ReIndexJob.class.getName (),
                                              JDK8TriggerBuilder.newTrigger ()
                                                                .startAt (PDTFactory.getCurrentLocalDateTime ().plusSeconds (5))
                                                                .withSchedule (aScheduleBuilder),
                                              ReIndexJob.class,
                                              aJobDataMap);
  }
}
