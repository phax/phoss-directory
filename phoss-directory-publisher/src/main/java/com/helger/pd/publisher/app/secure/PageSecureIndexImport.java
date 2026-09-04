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
import com.helger.base.string.StringImplode;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.io.file.FileOperationManager;
import com.helger.pd.indexer.businesscard.IPDBusinessCardProvider;
import com.helger.pd.indexer.businesscard.SMPBusinessCardProvider;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.publisher.job.PDIndexImportJob;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.photon.bootstrap5.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap5.form.BootstrapForm;
import com.helger.photon.bootstrap5.form.BootstrapFormGroup;
import com.helger.photon.bootstrap5.uictrls.ext.BootstrapFileUpload;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.io.PhotonWorkerPool;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.web.fileupload.IFileItem;

public final class PageSecureIndexImport extends AbstractAppWebPage
{
  public static final String FIELD_FILE = "file";
  private static final Logger LOGGER = LoggerFactory.getLogger (PageSecureIndexImport.class);

  public PageSecureIndexImport (@NonNull @Nonempty final String sID)
  {
    super (sID, "Import participants");
  }

  /**
   * Store the uploaded file below the data path and start the import job for it. Reading the file
   * and queueing tens of thousands of participants must not happen in an HTTP thread.
   *
   * @param aWPEC
   *        The current execution context. May not be <code>null</code>.
   * @param aFile
   *        The uploaded file. May not be <code>null</code>.
   */
  private void _startImport (@NonNull final WebPageExecutionContext aWPEC, @NonNull final IFileItem aFile)
  {
    File aImportFile = null;
    try
    {
      aImportFile = PDIndexImportJob.createImportFile ();
      if (aFile.write (aImportFile).isFailure ())
        throw new IllegalStateException ("Failed to store the uploaded file in '" +
                                         aImportFile.getAbsolutePath () +
                                         "'");

      LOGGER.info ("Stored the uploaded file '" +
                   aFile.getNameSecure () +
                   "' with " +
                   aImportFile.length () +
                   " bytes in '" +
                   aImportFile.getAbsolutePath () +
                   "'");

      PhotonWorkerPool.getInstance ()
                      .run (PDIndexImportJob.JOB_TYPE,
                            new PDIndexImportJob (aImportFile, aWPEC.getLoggedInUserID ()));
    }
    catch (final Exception ex)
    {
      // The job was never started, so it can neither delete the file nor release the lock
      if (aImportFile != null)
        FileOperationManager.INSTANCE.deleteFileIfExisting (aImportFile);
      PDIndexImportJob.LOCK.release ();

      LOGGER.error ("Failed to start the participant import", ex);
      aWPEC.postRedirectGetInternal (error ("Failed to start the participant import: " + ex.getMessage ()));
    }

    aWPEC.postRedirectGetInternal (success ("The import of the participants is now running in the background. " +
                                            "The result is shown on the \"Long running jobs\" page as soon as it is finished."));
  }

  @Override
  protected void fillContent (final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final FormErrorList aFormErrors = new FormErrorList ();

    {
      final IPDBusinessCardProvider aBCProv = PDMetaManager.getBusinessCardProvider ();
      if (aBCProv instanceof final SMPBusinessCardProvider aSMPBCProv)
      {
        aNodeList.addChild (info ("The following SMLs are crawled for entries: " +
                                  StringImplode.imploder ()
                                               .source (aSMPBCProv.getAllSMLsToUse (), ISMLInfo::getDisplayName)
                                               .separator (", ")
                                               .build ()));
      }
    }

    if (PDIndexImportJob.LOCK.isRunning ())
      aNodeList.addChild (warn ("An import is currently running in the background. " +
                                "The result is shown on the \"Long running jobs\" page as soon as it is finished."));

    final boolean bIsFormSubmitted = aWPEC.hasAction (CPageParam.ACTION_PERFORM);
    if (bIsFormSubmitted)
    {
      final IFileItem aFile = aWPEC.params ().getAsFileItem (FIELD_FILE);
      if (aFile == null || StringHelper.isEmpty (aFile.getNameSecure ()))
        aFormErrors.addFieldError (FIELD_FILE, "No file was selected");

      if (aFormErrors.isEmpty ())
      {
        // Only a single import may run at a time, because it queues a lot of work items
        if (PDIndexImportJob.LOCK.tryAcquire (aWPEC.getLoggedInUserID ()))
          _startImport (aWPEC, aFile);
        else
          aWPEC.postRedirectGetInternal (warn ("Another import is already running in the background. Please wait until it is finished."));
      }
    }
    final BootstrapForm aForm = aNodeList.addAndReturnChild (getUIHandler ().createFormFileUploadSelf (aWPEC,
                                                                                                       bIsFormSubmitted));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Import file")
                                                 .setCtrl (new BootstrapFileUpload (FIELD_FILE, aDisplayLocale))
                                                 .setHelpText ("Select a file that was created from a full XML export to index of all them manually. " +
                                                               "The import runs in the background - the result is shown on the \"Long running jobs\" page.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_FILE)));

    final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (new BootstrapButtonToolbar (aWPEC));
    aToolbar.addHiddenField (CPageParam.PARAM_ACTION, CPageParam.ACTION_PERFORM);
    aToolbar.addSubmitButton ("Import all", EDefaultIcon.YES);
  }
}
