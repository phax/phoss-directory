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
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.publisher.job.PDIndexDeleteJob;
import com.helger.photon.security.lock.SingleRunLock;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.icon.IIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;

public final class PageSecureBulkDelete extends AbstractPageSecureParticipantUpload
{
  public PageSecureBulkDelete (@NonNull @Nonempty final String sID)
  {
    super (sID, "Bulk delete participants");
  }

  @Override
  @NonNull
  protected SingleRunLock getLock ()
  {
    return PDIndexDeleteJob.LOCK;
  }

  @Override
  @NonNull
  @Nonempty
  protected String getJobType ()
  {
    return PDIndexDeleteJob.JOB_TYPE;
  }

  @Override
  @NonNull
  @Nonempty
  protected String getActivityName ()
  {
    return "participant deletion";
  }

  @Override
  @NonNull
  @Nonempty
  protected String getFileHelpText ()
  {
    return "Select the file with the participants to be deleted. " +
           "Either an XML file that was created from a full XML export, or a text file with one participant ID per line " +
           "(e.g. iso6523-actorid-upis::9915:test) - empty lines and lines starting with '#' are ignored. " +
           "The deletion runs in the background - the result is shown on the \"Long running jobs\" page.";
  }

  @Override
  @NonNull
  @Nonempty
  protected String getSubmitButtonText ()
  {
    return "Delete all";
  }

  @Override
  @Nullable
  protected IIcon getSubmitButtonIcon ()
  {
    return EDefaultIcon.DELETE;
  }

  @Override
  @NonNull
  protected File createUploadFile ()
  {
    return PDIndexDeleteJob.createUploadFile ();
  }

  @Override
  @NonNull
  protected PDIndexDeleteJob createJob (@NonNull final File aUploadedFile, @NonNull @Nonempty final String sUserID)
  {
    return new PDIndexDeleteJob (aUploadedFile, sUserID);
  }

  @Override
  protected void addAdditionalInfo (@NonNull final WebPageExecutionContext aWPEC, @NonNull final HCNodeList aNodeList)
  {
    aNodeList.addChild (warn ("The listed participants are removed from the search index no matter who owns them, " +
                              "exactly like on the \"Manually delete participant\" page. " +
                              "All their pending indexer work items are withdrawn as well, so that a queued " +
                              "create/update does not put them back into the index."));
  }
}
