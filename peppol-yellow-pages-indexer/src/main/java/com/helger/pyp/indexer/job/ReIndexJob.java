/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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
package com.helger.pyp.indexer.job;

import java.util.Date;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.ScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.photon.core.job.AbstractPhotonJob;
import com.helger.pyp.indexer.mgr.IndexerManager;
import com.helger.pyp.indexer.mgr.PYPMetaManager;
import com.helger.schedule.quartz.GlobalQuartzScheduler;
import com.helger.web.mock.MockHttpServletRequest;
import com.helger.web.mock.OfflineHttpServletRequest;
import com.helger.web.scope.mgr.WebScopeManager;

/**
 * A Quartz job that is scheduled to re-index existing entries that failed to
 * re-index previously.
 *
 * @author Philip Helger
 */
@DisallowConcurrentExecution
public class ReIndexJob extends AbstractPhotonJob
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
  protected void onExecute (@Nonnull final JobExecutionContext aContext) throws JobExecutionException
  {
    final IndexerManager aIndexerMgr = PYPMetaManager.getIndexerMgr ();

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
   * @param sApplicationID
   *        The internal application ID to be used. May neither be
   *        <code>null</code> nor empty.
   * @return The created trigger key for further usage. Never <code>null</code>.
   */
  @Nonnull
  public static TriggerKey schedule (@Nonnull final ScheduleBuilder <SimpleTrigger> aScheduleBuilder,
                                     @Nonnull @Nonempty final String sApplicationID)
  {
    ValueEnforcer.notNull (aScheduleBuilder, "ScheduleBuilder");

    setApplicationScopeID (sApplicationID);
    return GlobalQuartzScheduler.getInstance ().scheduleJob (ReIndexJob.class.getName (),
                                                             TriggerBuilder.newTrigger ()
                                                                           .startAt (new Date (new Date ().getTime () +
                                                                                               5000))
                                                                           .withSchedule (aScheduleBuilder),
                                                             ReIndexJob.class,
                                                             null);
  }
}
