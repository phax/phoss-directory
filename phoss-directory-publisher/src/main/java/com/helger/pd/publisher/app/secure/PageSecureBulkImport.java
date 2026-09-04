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

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringImplode;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.businesscard.IPDBusinessCardProvider;
import com.helger.pd.indexer.businesscard.SMPBusinessCardProvider;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.publisher.job.PDIndexImportJob;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.photon.security.lock.SingleRunLock;
import com.helger.photon.uicore.page.WebPageExecutionContext;

public final class PageSecureBulkImport extends AbstractPageSecureParticipantUpload
{
  public PageSecureBulkImport (@NonNull @Nonempty final String sID)
  {
    super (sID, "Bulk import participants");
  }

  @Override
  @NonNull
  protected SingleRunLock getLock ()
  {
    return PDIndexImportJob.LOCK;
  }

  @Override
  @NonNull
  @Nonempty
  protected String getJobType ()
  {
    return PDIndexImportJob.JOB_TYPE;
  }

  @Override
  @NonNull
  @Nonempty
  protected String getActivityName ()
  {
    return "participant import";
  }

  @Override
  @NonNull
  @Nonempty
  protected String getFileHelpText ()
  {
    return "Select the file with the participants to be indexed. " +
           "Either an XML file that was created from a full XML export, or a text file with one participant ID per line " +
           "(e.g. iso6523-actorid-upis::9915:test) - empty lines and lines starting with '#' are ignored. " +
           "The import runs in the background - the result is shown on the \"Long running jobs\" page.";
  }

  @Override
  @NonNull
  @Nonempty
  protected String getSubmitButtonText ()
  {
    return "Import all";
  }

  @Override
  @NonNull
  protected File createUploadFile ()
  {
    return PDIndexImportJob.createUploadFile ();
  }

  @Override
  @NonNull
  protected PDIndexImportJob createJob (@NonNull final File aUploadedFile, @NonNull @Nonempty final String sUserID)
  {
    return new PDIndexImportJob (aUploadedFile, sUserID);
  }

  @Override
  protected void addAdditionalInfo (@NonNull final WebPageExecutionContext aWPEC, @NonNull final HCNodeList aNodeList)
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
}
