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
import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.util.PDTIOHelper;
import com.helger.io.file.FileIOError;
import com.helger.io.file.FileOperationManager;
import com.helger.io.file.FilenameHelper;
import com.helger.photon.io.WebFileIO;
import com.helger.photon.security.lock.SingleRunLock;
import com.helger.text.IMultilingualText;

/**
 * Base class for the long running jobs that work on an uploaded file with participant identifiers.
 * On top of the common lifecycle it deletes the uploaded file when the job is done.
 *
 * @author Philip Helger
 * @since 0.17.2
 */
public abstract class AbstractPDParticipantFileJob extends AbstractPDLongRunningJob
{
  /** The name of the directory below the data path, in which the uploaded files are stored */
  public static final String UPLOAD_DIRECTORY = "participant-upload";

  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractPDParticipantFileJob.class);

  private final File m_aUploadedFile;

  protected AbstractPDParticipantFileJob (@NonNull @Nonempty final String sJobType,
                                          @NonNull final IMultilingualText aJobDesc,
                                          @NonNull @Nonempty final String sUserID,
                                          @NonNull final File aUploadedFile,
                                          @NonNull final SingleRunLock aLock)
  {
    super (sJobType, aJobDesc, sUserID, aLock);
    ValueEnforcer.notNull (aUploadedFile, "UploadedFile");
    m_aUploadedFile = aUploadedFile;
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
   * @return The uploaded file this job works on. Never <code>null</code>.
   */
  @NonNull
  protected final File getUploadedFile ()
  {
    return m_aUploadedFile;
  }

  @Override
  @NonNull
  @ReturnsMutableCopy
  protected ICommonsList <Object> getAuditArgs ()
  {
    return new CommonsArrayList <> (m_aUploadedFile.getName ());
  }

  @Override
  @NonNull
  @ReturnsMutableCopy
  protected ICommonsList <Object> getAuditStartArgs ()
  {
    final ICommonsList <Object> ret = getAuditArgs ();
    ret.add (Long.valueOf (m_aUploadedFile.length ()));
    return ret;
  }

  @Override
  protected void onJobFinished ()
  {
    // The uploaded file is of no use anymore
    if (FileOperationManager.INSTANCE.deleteFileIfExisting (m_aUploadedFile).isFailure ())
      LOGGER.warn ("Failed to delete the uploaded file '" + m_aUploadedFile.getAbsolutePath () + "'");
  }
}
