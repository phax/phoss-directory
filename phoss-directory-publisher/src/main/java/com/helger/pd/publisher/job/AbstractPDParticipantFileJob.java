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

import java.io.File;
import java.time.Duration;
import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ESuccess;
import com.helger.base.timing.StopWatch;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.util.PDTIOHelper;
import com.helger.io.file.FileIOError;
import com.helger.io.file.FileOperationManager;
import com.helger.io.file.FilenameHelper;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.io.WebFileIO;
import com.helger.photon.mgrs.longrun.AbstractLongRunningJobRunnable;
import com.helger.photon.mgrs.longrun.LongRunningJobResult;
import com.helger.photon.security.lock.SingleRunLock;
import com.helger.text.IMultilingualText;
import com.helger.web.scope.mgr.WebScoped;

/**
 * Base class for the long running jobs that work on an uploaded file with participant identifiers.
 * It takes care of the common lifecycle: run in a Web Scope, delete the uploaded file afterwards
 * and release the lock that the triggering page acquired.<br>
 * The caller must acquire the lock before starting such a job - the job releases it when it is
 * done.
 *
 * @author Philip Helger
 * @since 0.17.2
 */
public abstract class AbstractPDParticipantFileJob extends AbstractLongRunningJobRunnable
{
  /** The name of the directory below the data path, in which the uploaded files are stored */
  public static final String UPLOAD_DIRECTORY = "participant-upload";

  /** The maximum number of entries that are listed in a job result */
  public static final int MAX_RESULT_DETAILS = 100;

  /** The phase that is appended to the job type to form the audit action of the job start */
  public static final String AUDIT_PHASE_START = "start";
  /** The phase that is appended to the job type to form the audit action of the job end */
  public static final String AUDIT_PHASE_END = "end";

  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractPDParticipantFileJob.class);

  private final String m_sUserID;
  private final File m_aUploadedFile;
  private final SingleRunLock m_aLock;
  private ESuccess m_eSuccess = ESuccess.FAILURE;

  protected AbstractPDParticipantFileJob (@NonNull @Nonempty final String sJobType,
                                          @NonNull final IMultilingualText aJobDesc,
                                          @NonNull @Nonempty final String sUserID,
                                          @NonNull final File aUploadedFile,
                                          @NonNull final SingleRunLock aLock)
  {
    super (sJobType, aJobDesc, () -> sUserID);
    ValueEnforcer.notEmpty (sUserID, "UserID");
    ValueEnforcer.notNull (aUploadedFile, "UploadedFile");
    ValueEnforcer.notNull (aLock, "Lock");
    m_sUserID = sUserID;
    m_aUploadedFile = aUploadedFile;
    m_aLock = aLock;
  }

  /**
   * Get the audit action of a participant file job.
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
   * @return The directory in which all uploaded files are stored. Never <code>null</code>. The
   *         directory may not yet exist.
   */
  @NonNull
  public static File getUploadDirectory ()
  {
    return WebFileIO.getDataIO ().getFile (UPLOAD_DIRECTORY);
  }

  /**
   * Create the upload directory (if it is not yet present) and return a new unique file in it, to
   * which the uploaded data can be written.
   *
   * @param sFilenamePrefix
   *        The prefix to be used for the created filename. May not be <code>null</code> nor empty.
   * @return The file to write the uploaded data to. It does not yet exist. Never <code>null</code>.
   */
  @NonNull
  public static File createUploadFile (@NonNull @Nonempty final String sFilenamePrefix)
  {
    ValueEnforcer.notEmpty (sFilenamePrefix, "FilenamePrefix");

    final FileIOError aError = WebFileIO.getDataIO ().createDirectory (UPLOAD_DIRECTORY, true);
    if (aError.isFailure ())
      throw new IllegalStateException ("Failed to create the upload directory: " + aError.toString ());

    final String sFilename = sFilenamePrefix +
                             PDTIOHelper.getCurrentLocalDateTimeForFilename () +
                             "-" +
                             UUID.randomUUID ().toString ();

    // Ensure the resulting name is a valid filename on all platforms
    final String sSecureFilename = FilenameHelper.getAsSecureValidASCIIFilename (sFilename);
    if (sSecureFilename == null)
      throw new IllegalStateException ("Failed to create a valid upload filename from '" + sFilename + "'");

    return new File (getUploadDirectory (), sSecureFilename);
  }

  /**
   * Append at most {@link #MAX_RESULT_DETAILS} entries to the provided result text, so that the job
   * result stays small enough to be persisted. Such a file may well contain tens of thousands of
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
   * @return The uploaded file this job works on. Never <code>null</code>.
   */
  @NonNull
  protected final File getUploadedFile ()
  {
    return m_aUploadedFile;
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
  protected abstract LongRunningJobResult createParticipantJobResult ();

  @NonNull
  public final LongRunningJobResult createLongRunningJobResult ()
  {
    // The base class swallows the exception, so the outcome must be remembered here
    final LongRunningJobResult ret = createParticipantJobResult ();
    m_eSuccess = ESuccess.SUCCESS;
    return ret;
  }

  @Override
  public void run ()
  {
    // The overall duration is part of every audit item, so that the audit trail alone shows how
    // long the job took
    final StopWatch aSWTotal = StopWatch.createdStarted ();

    // The job runs in a worker thread, so the user that triggered it is passed explicitly
    AuditHelper.onAuditExecuteSuccess (getAuditAction (getJobType (), AUDIT_PHASE_START),
                                       m_sUserID,
                                       m_aUploadedFile.getName (),
                                       Long.valueOf (m_aUploadedFile.length ()),
                                       aSWTotal.getDuration ());

    // A Web Scope is needed for storing the job result
    try (final WebScoped w = new WebScoped ())
    {
      super.run ();
    }
    finally
    {
      final Duration aTotalDuration = aSWTotal.stopAndGetDuration ();
      if (m_eSuccess.isSuccess ())
        AuditHelper.onAuditExecuteSuccess (getAuditAction (getJobType (), AUDIT_PHASE_END),
                                           m_sUserID,
                                           m_aUploadedFile.getName (),
                                           aTotalDuration);
      else
        AuditHelper.onAuditExecuteFailure (getAuditAction (getJobType (), AUDIT_PHASE_END),
                                           m_sUserID,
                                           m_aUploadedFile.getName (),
                                           aTotalDuration);

      // The uploaded file is of no use anymore
      if (FileOperationManager.INSTANCE.deleteFileIfExisting (m_aUploadedFile).isFailure ())
        LOGGER.warn ("Failed to delete the uploaded file '" + m_aUploadedFile.getAbsolutePath () + "'");

      // Always release, even if the job failed
      m_aLock.release ();
    }
  }
}
