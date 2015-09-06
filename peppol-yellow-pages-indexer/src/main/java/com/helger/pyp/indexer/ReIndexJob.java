package com.helger.pyp.indexer;

import javax.annotation.Nonnull;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.ScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.photon.core.job.AbstractPhotonJob;
import com.helger.schedule.quartz.GlobalQuartzScheduler;

@DisallowConcurrentExecution
public class ReIndexJob extends AbstractPhotonJob
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (ReIndexJob.class);

  /**
   * Public no argument constructor must be available.
   */
  public ReIndexJob ()
  {}

  @Override
  protected void onExecute (@Nonnull final JobExecutionContext aContext) throws JobExecutionException
  {}

  /**
   * @param aScheduleBuilder
   *        The schedule builder to be used. May not be <code>null</code>.
   *        Example:
   *        <code>SimpleScheduleBuilder.repeatMinutelyForever (60)</code>
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
                                                                           .startNow ()
                                                                           .withSchedule (aScheduleBuilder),
                                                             ReIndexJob.class,
                                                             null);
  }
}
