package com.helger.pyp.indexer.job;

import java.util.Date;

import javax.annotation.Nonnull;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.ScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.photon.core.job.AbstractPhotonJob;
import com.helger.pyp.indexer.mgr.IndexerManager;
import com.helger.pyp.indexer.mgr.PYPMetaManager;
import com.helger.schedule.quartz.GlobalQuartzScheduler;

/**
 * A Quartz job that is scheduled to re-index existing entries that failed to
 * re-index previously.
 *
 * @author Philip Helger
 */
@DisallowConcurrentExecution
public class ReIndexJob extends AbstractPhotonJob
{
  /**
   * Public no argument constructor must be available.
   */
  public ReIndexJob ()
  {}

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
