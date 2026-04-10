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
package com.helger.pd.indexer.shadow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.style.OverrideOnDemand;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.datetime.helper.PDTFactory;
import com.helger.http.CHttp;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.settings.PDServerConfiguration;
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

import jakarta.annotation.Nonnull;
import jakarta.servlet.ServletContext;

/**
 * Quartz job that dispatches pending shadow events from the outbox to the downstream replicator
 * service. Runs every minute.
 * <p>
 * Events that fail with retryable errors (network issues, 5xx) remain in the queue for retry.
 * Events that fail with non-retryable errors (4xx) are moved to the dead-letter queue for manual
 * investigation.
 * </p>
 *
 * @author Mikael Aksamit
 */
@DisallowConcurrentExecution
public class ShadowEventDispatcherJob extends AbstractScopeAwareJob
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ShadowEventDispatcherJob.class);
  private final ServletContext m_aSC;

  public ShadowEventDispatcherJob ()
  {
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
  protected void onExecute (@Nonnull final JobDataMap aJobDataMap, @Nonnull final IJobExecutionContext aContext)
                                                                                                                 throws JobExecutionException
  {
    if (!PDServerConfiguration.isIndexerShadowingEnabled ())
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Indexer shadowing is disabled - skipping dispatcher run");
      return;
    }

    final String sDownstreamURL = PDServerConfiguration.getIndexerShadowingURL ();
    if (StringHelper.isEmpty (sDownstreamURL))
    {
      LOGGER.warn ("Indexer shadowing is enabled but URL is not configured - skipping dispatcher run");
      return;
    }

    final ShadowEventList aEventList = PDMetaManager.getShadowEventList ();
    if (aEventList == null)
    {
      LOGGER.error ("ShadowEventList not initialized - skipping dispatcher run");
      return;
    }

    final FailedShadowEventList aFailedEventList = PDMetaManager.getFailedShadowEventList ();
    if (aFailedEventList == null)
    {
      LOGGER.error ("FailedShadowEventList not initialized - skipping dispatcher run");
      return;
    }

    final ICommonsList <IShadowEvent> aPendingEvents = aEventList.getAllEvents ();
    if (aPendingEvents.isEmpty ())
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("No pending shadow events to dispatch");
      return;
    }

    LOGGER.info ("Dispatching " + aPendingEvents.size () + " shadow event(s) to " + sDownstreamURL);

    int nSuccessCount = 0;
    int nRetryableFailureCount = 0;
    int nNonRetryableFailureCount = 0;

    for (final IShadowEvent aEvent : aPendingEvents)
    {
      try
      {
        final int nStatusCode = ShadowEventSender.sendEvent (sDownstreamURL, aEvent);

        if (nStatusCode >= CHttp.HTTP_OK && nStatusCode < CHttp.HTTP_MULTIPLE_CHOICES)
        {
          // 2xx
          aEventList.removeEvent (aEvent.getEventID ());
          nSuccessCount++;
          LOGGER.info ("Successfully dispatched shadow event " + aEvent.getEventID ());
        }
        else
          if (nStatusCode >= CHttp.HTTP_BAD_REQUEST && nStatusCode < CHttp.HTTP_INTERNAL_SERVER_ERROR)
          {
            // 4xx
            aEventList.removeEvent (aEvent.getEventID ());
            aFailedEventList.addFailedEvent ((ShadowEvent) aEvent);
            nNonRetryableFailureCount++;
            LOGGER.error ("Shadow event " +
                          aEvent.getEventID () +
                          " rejected with HTTP " +
                          nStatusCode +
                          " - moved to DLQ");
          }
          else
          {
            nRetryableFailureCount++;
            LOGGER.warn ("Shadow event " + aEvent.getEventID () + " failed with HTTP " + nStatusCode + " - will retry");
          }
      }
      catch (final Exception ex)
      {
        nRetryableFailureCount++;
        LOGGER.warn ("Shadow event " + aEvent.getEventID () + " failed with exception - will retry", ex);
      }
    }

    LOGGER.info ("Shadow event dispatch complete: " +
                 nSuccessCount +
                 " sent, " +
                 nRetryableFailureCount +
                 " retryable failures, " +
                 nNonRetryableFailureCount +
                 " moved to DLQ");
  }

  @Nonnull
  public static TriggerKey schedule (@Nonnull final SimpleScheduleBuilder aScheduleBuilder)
  {
    ValueEnforcer.notNull (aScheduleBuilder, "ScheduleBuilder");

    final ICommonsMap <String, Object> aJobDataMap = new CommonsHashMap <> ();

    return GlobalQuartzScheduler.getInstance ()
                                .scheduleJob (ShadowEventDispatcherJob.class.getName (),
                                              JDK8TriggerBuilder.newTrigger ()
                                                                .startAt (PDTFactory.getCurrentLocalDateTime ()
                                                                                    .plusSeconds (5))
                                                                .withSchedule (aScheduleBuilder),
                                              ShadowEventDispatcherJob.class,
                                              aJobDataMap);
  }
}
