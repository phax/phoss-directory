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
package com.helger.pd.publisher.app.secure;

import java.io.File;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringHelper;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.io.file.FileOperationManager;
import com.helger.pd.publisher.job.AbstractPDParticipantFileJob;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.photon.bootstrap5.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap5.form.BootstrapForm;
import com.helger.photon.bootstrap5.form.BootstrapFormGroup;
import com.helger.photon.bootstrap5.uictrls.ext.BootstrapFileUpload;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.io.PhotonWorkerPool;
import com.helger.photon.security.lock.SingleRunLock;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.web.fileupload.IFileItem;

/**
 * Base class for the pages that take a file with participant identifiers and hand it over to a long
 * running job. Reading such a file and working through tens of thousands of participants must not
 * happen in an HTTP thread.
 *
 * @author Philip Helger
 * @since 0.17.2
 */
public abstract class AbstractPageSecureParticipantUpload extends AbstractAppWebPage
{
  public static final String FIELD_FILE = "file";

  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractPageSecureParticipantUpload.class);

  protected AbstractPageSecureParticipantUpload (@NonNull @Nonempty final String sID, @NonNull final String sName)
  {
    super (sID, sName);
  }

  /**
   * @return The lock that ensures that only a single such job runs at a time. May not be
   *         <code>null</code>.
   */
  @NonNull
  protected abstract SingleRunLock getLock ();

  /**
   * @return The type of the long running job to be started. May neither be <code>null</code> nor
   *         empty.
   */
  @NonNull
  @Nonempty
  protected abstract String getJobType ();

  /**
   * @return The name of the activity, as it is used in the messages shown to the user - e.g.
   *         "import". May neither be <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  protected abstract String getActivityName ();

  /**
   * @return The help text of the file upload field. May neither be <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  protected abstract String getFileHelpText ();

  /**
   * @return The label of the submit button. May neither be <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  protected abstract String getSubmitButtonText ();

  /**
   * @return A new unique file to which the uploaded data can be written. Never <code>null</code>.
   */
  @NonNull
  protected abstract File createUploadFile ();

  /**
   * Create the job that handles the uploaded file.
   *
   * @param aUploadedFile
   *        The file with the uploaded data. Never <code>null</code>.
   * @param sUserID
   *        The ID of the user that triggered the job. Neither <code>null</code> nor empty.
   * @return The job to be run. May not be <code>null</code>.
   */
  @NonNull
  protected abstract AbstractPDParticipantFileJob createJob (@NonNull File aUploadedFile,
                                                             @NonNull @Nonempty String sUserID);

  /**
   * Add page specific content above the upload form. Does nothing by default.
   *
   * @param aWPEC
   *        The current execution context. Never <code>null</code>.
   * @param aNodeList
   *        The node list to add the content to. Never <code>null</code>.
   */
  protected void addAdditionalInfo (@NonNull final WebPageExecutionContext aWPEC, @NonNull final HCNodeList aNodeList)
  {}

  /**
   * Store the uploaded file below the data path and start the job for it.
   *
   * @param aWPEC
   *        The current execution context. May not be <code>null</code>.
   * @param aFile
   *        The uploaded file. May not be <code>null</code>.
   */
  private void _startJob (@NonNull final WebPageExecutionContext aWPEC, @NonNull final IFileItem aFile)
  {
    File aUploadedFile = null;
    try
    {
      aUploadedFile = createUploadFile ();
      if (aFile.write (aUploadedFile).isFailure ())
        throw new IllegalStateException ("Failed to store the uploaded file in '" +
                                         aUploadedFile.getAbsolutePath () +
                                         "'");

      LOGGER.info ("Stored the uploaded file '" +
                   aFile.getNameSecure () +
                   "' with " +
                   aUploadedFile.length () +
                   " bytes in '" +
                   aUploadedFile.getAbsolutePath () +
                   "'");

      PhotonWorkerPool.getInstance ()
                      .run (getJobType (), createJob (aUploadedFile, aWPEC.getLoggedInUserID ()));
    }
    catch (final Exception ex)
    {
      // The job was never started, so it can neither delete the file nor release the lock
      if (aUploadedFile != null)
        FileOperationManager.INSTANCE.deleteFileIfExisting (aUploadedFile);
      getLock ().release ();

      LOGGER.error ("Failed to start the participant " + getActivityName (), ex);
      aWPEC.postRedirectGetInternal (error ("Failed to start the participant " +
                                            getActivityName () +
                                            ": " +
                                            ex.getMessage ()));
    }

    aWPEC.postRedirectGetInternal (success ("The " +
                                            getActivityName () +
                                            " is now running in the background. " +
                                            "The result is shown on the \"Long running jobs\" page as soon as it is finished."));
  }

  @Override
  protected void fillContent (final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final FormErrorList aFormErrors = new FormErrorList ();

    addAdditionalInfo (aWPEC, aNodeList);

    if (getLock ().isRunning ())
      aNodeList.addChild (warn ("A " +
                                getActivityName () +
                                " is currently running in the background. " +
                                "The result is shown on the \"Long running jobs\" page as soon as it is finished."));

    final boolean bIsFormSubmitted = aWPEC.hasAction (CPageParam.ACTION_PERFORM);
    if (bIsFormSubmitted)
    {
      final IFileItem aFile = aWPEC.params ().getAsFileItem (FIELD_FILE);
      if (aFile == null || StringHelper.isEmpty (aFile.getNameSecure ()))
        aFormErrors.addFieldError (FIELD_FILE, "No file was selected");

      if (aFormErrors.isEmpty ())
      {
        // Only a single job may run at a time, because it works on a lot of participants
        if (getLock ().tryAcquire (aWPEC.getLoggedInUserID ()))
          _startJob (aWPEC, aFile);
        else
          aWPEC.postRedirectGetInternal (warn ("Another " +
                                               getActivityName () +
                                               " is already running in the background. Please wait until it is finished."));
      }
    }

    final BootstrapForm aForm = aNodeList.addAndReturnChild (getUIHandler ().createFormFileUploadSelf (aWPEC,
                                                                                                       bIsFormSubmitted));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("File")
                                                 .setCtrl (new BootstrapFileUpload (FIELD_FILE, aDisplayLocale))
                                                 .setHelpText (getFileHelpText ())
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_FILE)));

    final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (new BootstrapButtonToolbar (aWPEC));
    aToolbar.addHiddenField (CPageParam.PARAM_ACTION, CPageParam.ACTION_PERFORM);
    aToolbar.addSubmitButton (getSubmitButtonText (), EDefaultIcon.YES);
  }
}
