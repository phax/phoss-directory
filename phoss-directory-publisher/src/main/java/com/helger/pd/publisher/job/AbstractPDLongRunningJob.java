/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.job;

import java.time.Duration;
import java.util.List;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ESuccess;
import com.helger.base.timing.StopWatch;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.mgrs.longrun.AbstractLongRunningJobRunnable;
import com.helger.photon.mgrs.longrun.LongRunningJobResult;
import com.helger.photon.security.lock.SingleRunLock;
import com.helger.text.IMultilingualText;
import com.helger.web.scope.mgr.WebScoped;

/**
 * Base class for all long running jobs of the publisher. It takes care of the common lifecycle: run
 * in a Web Scope, write an audit item for the start and the end of the job and release the lock
 * that the triggering page acquired.<br>
 * The caller must acquire the lock before starting such a job - the job releases it when it is
 * done.
 *
 * @author Philip Helger
 * @since 0.18.3
 */
public abstract class AbstractPDLongRunningJob extends AbstractLongRunningJobRunnable
{
  /** The maximum number of entries that are listed in a job result */
  public static final int MAX_RESULT_DETAILS = 100;

  /** The phase that is appended to the job type to form the audit action of the job start */
  public static final String AUDIT_PHASE_START = "start";
  /** The phase that is appended to the job type to form the audit action of the job end */
  public static final String AUDIT_PHASE_END = "end";

  private final String m_sUserID;
  private final SingleRunLock m_aLock;
  private ESuccess m_eSuccess = ESuccess.FAILURE;

  protected AbstractPDLongRunningJob (@NonNull @Nonempty final String sJobType,
                                      @NonNull final IMultilingualText aJobDesc,
                                      @NonNull @Nonempty final String sUserID,
                                      @NonNull final SingleRunLock aLock)
  {
    super (sJobType, aJobDesc, () -> sUserID);
    ValueEnforcer.notEmpty (sUserID, "UserID");
    ValueEnforcer.notNull (aLock, "Lock");
    m_sUserID = sUserID;
    m_aLock = aLock;
  }

  /**
   * Get the audit action of a long running job.
   *
   * @param sJobType
   *        The type of the job. Neither <code>null</code> nor empty.
   * @param sPhase
   *        The phase - either {@link #AUDIT_PHASE_START} or {@link #AUDIT_PHASE_END}. Neither
   *        <code>null</code> nor empty.
   * @return The audit action - e.g. <code>index-delete-start</code>. Neither <code>null</code> nor
   *         empty.
   */
  @NonNull
  @Nonempty
  public static String getAuditAction (@NonNull @Nonempty final String sJobType, @NonNull @Nonempty final String sPhase)
  {
    return sJobType + "-" + sPhase;
  }

  /**
   * Append at most {@link #MAX_RESULT_DETAILS} entries to the provided result text, so that the job
   * result stays small enough to be persisted. Such a job may well work on tens of thousands of
   * participants, and listing all of them is of no use to anybody.
   *
   * @param aSB
   *        The result text to append to. May not be <code>null</code>.
   * @param sTitle
   *        The headline to use. May not be <code>null</code>.
   * @param aEntries
   *        The entries to be listed. May not be <code>null</code>.
   */
  protected static void appendDetails (@NonNull final StringBuilder aSB,
                                       @NonNull final String sTitle,
                                       @NonNull final ICommonsList <String> aEntries)
  {
    if (aEntries.isEmpty ())
      return;

    aSB.append ('\n').append (sTitle).append (" (").append (aEntries.size ()).append ("):\n");
    for (final String sEntry : aEntries.subList (0, Math.min (MAX_RESULT_DETAILS, aEntries.size ())))
      aSB.append ("  ").append (sEntry).append ('\n');
    if (aEntries.size () > MAX_RESULT_DETAILS)
      aSB.append ("  ... and ").append (aEntries.size () - MAX_RESULT_DETAILS).append (" more\n");
  }

  /**
   * @return The maximum number of entries that are listed in a job result.
   */
  @Nonnegative
  protected static int getMaxResultDetails ()
  {
    return MAX_RESULT_DETAILS;
  }

  /**
   * @return The ID of the user that triggered this job. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  protected final String getUserID ()
  {
    return m_sUserID;
  }

  /**
   * @return The job specific arguments that are added to the audit items of this job - e.g. the
   *         name of the processed file. The user ID and the duration are added by this class. May
   *         not be <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  protected abstract ICommonsList <Object> getAuditArgs ();

  /**
   * @return The job specific arguments that are added to the audit item of the job start. By
   *         default these are the same as the ones of {@link #getAuditArgs()}. May not be
   *         <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  protected ICommonsList <Object> getAuditStartArgs ()
  {
    return getAuditArgs ();
  }

  /**
   * Create the result of this job. This is the method the derived classes have to implement -
   * {@link #createLongRunningJobResult ()} only wraps it, so that the outcome becomes part of the
   * audit trail.
   *
   * @return The results of this job for asynchronous retrieval by the user. Never
   *         <code>null</code>.
   */
  @NonNull
  protected abstract LongRunningJobResult createJobResult ();

  @NonNull
  public final LongRunningJobResult createLongRunningJobResult ()
  {
    // The base class swallows the exception, so the outcome must be remembered here
    final LongRunningJobResult ret = createJobResult ();
    m_eSuccess = ESuccess.SUCCESS;
    return ret;
  }

  /**
   * Called after the job has ended and after the audit item of the job end was written, but before
   * the lock is released. Does nothing by default.
   */
  protected void onJobFinished ()
  {}

  @NonNull
  @ReturnsMutableCopy
  private Object [] _getAuditArgs (@NonNull final List <Object> aJobArgs, @NonNull final Duration aDuration)
  {
    // The job runs in a worker thread, so the user that triggered it is passed explicitly
    final ICommonsList <Object> ret = new CommonsArrayList <> (m_sUserID);
    ret.addAll (aJobArgs);
    ret.add (aDuration);
    return ret.toArray ();
  }

  @Override
  public void run ()
  {
    // The overall duration is part of every audit item, so that the audit trail alone shows how
    // long the job took
    final StopWatch aSWTotal = StopWatch.createdStarted ();

    AuditHelper.onAuditExecuteSuccess (getAuditAction (getJobType (), AUDIT_PHASE_START),
                                       _getAuditArgs (getAuditStartArgs (), aSWTotal.getDuration ()));

    // A Web Scope is needed for storing the job result
    try (final WebScoped w = new WebScoped ())
    {
      super.run ();
    }
    finally
    {
      final String sAuditAction = getAuditAction (getJobType (), AUDIT_PHASE_END);
      final Object [] aAuditArgs = _getAuditArgs (getAuditArgs (), aSWTotal.stopAndGetDuration ());
      if (m_eSuccess.isSuccess ())
        AuditHelper.onAuditExecuteSuccess (sAuditAction, aAuditArgs);
      else
        AuditHelper.onAuditExecuteFailure (sAuditAction, aAuditArgs);

      onJobFinished ();

      // Always release, even if the job failed
      m_aLock.release ();
    }
  }
}
