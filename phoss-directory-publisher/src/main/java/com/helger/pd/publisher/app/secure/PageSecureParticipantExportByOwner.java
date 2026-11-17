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

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.io.nonblocking.NonBlockingByteArrayOutputStream;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsSortedSet;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.http.CHttp;
import com.helger.io.file.FilenameHelper;
import com.helger.pd.indexer.storage.PDStoredMetaData;
import com.helger.pd.publisher.exportall.ExportAllHandlerParticipantCSV;
import com.helger.pd.publisher.exportall.ExportAllHandlerParticipantJSON;
import com.helger.pd.publisher.exportall.ExportAllHandlerParticipantXML;
import com.helger.pd.publisher.exportall.ExportAllManager;
import com.helger.pd.publisher.exportall.IExportAllHandler;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.photon.ajax.decl.AjaxFunctionDeclaration;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.photon.bootstrap5.button.BootstrapButton;
import com.helger.photon.bootstrap5.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap5.form.BootstrapForm;
import com.helger.photon.bootstrap5.form.BootstrapFormGroup;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * Export all participant IDs of a single owner, in the same formats as the export of all
 * participants.
 *
 * @author Philip Helger
 */
public final class PageSecureParticipantExportByOwner extends AbstractAppWebPage
{
  public static final String FIELD_OWNER_ID = "ownerid";

  private static final Logger LOGGER = LoggerFactory.getLogger (PageSecureParticipantExportByOwner.class);

  private static final AjaxFunctionDeclaration AJAX_DOWNLOAD_XML;
  private static final AjaxFunctionDeclaration AJAX_DOWNLOAD_JSON;
  private static final AjaxFunctionDeclaration AJAX_DOWNLOAD_CSV;

  @NonNull
  private static String _getFilename (@NonNull @Nonempty final String sOwnerID, @NonNull @Nonempty final String sExt)
  {
    return "directory-export-participants-" + FilenameHelper.getAsSecureValidASCIIFilename (sOwnerID) + sExt;
  }

  private static void _download (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                 @NonNull final PhotonUnifiedResponse aAjaxResponse,
                                 @NonNull final IExportAllHandler aHandler,
                                 @NonNull @Nonempty final String sExt) throws Exception
  {
    final String sOwnerID = aRequestScope.params ().getAsStringTrimmed (FIELD_OWNER_ID);
    if (StringHelper.isEmpty (sOwnerID))
    {
      aAjaxResponse.setStatus (CHttp.HTTP_BAD_REQUEST);
      return;
    }

    LOGGER.info ("Starting to export all participants of owner '" + sOwnerID + "' as " + aHandler.getDisplayName ());
    final ICommonsSortedSet <String> aParticipantIDs = ExportAllManager.getAllStoredParticipantIDsOfOwner (sOwnerID);
    try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
    {
      ExportAllManager.exportParticipants (aParticipantIDs, aHandler, aBAOS);
      aAjaxResponse.binary (aBAOS, aHandler.getContentType (), _getFilename (sOwnerID, sExt));
    }
    LOGGER.info ("Finished exporting " +
                 aParticipantIDs.size () +
                 " participants of owner '" +
                 sOwnerID +
                 "' as " +
                 aHandler.getDisplayName ());
  }

  static
  {
    // The S3 filename of the handlers is not used, as the result is not uploaded
    AJAX_DOWNLOAD_XML = addAjax ( (req,
                                   res) -> _download (req,
                                                      res,
                                                      new ExportAllHandlerParticipantXML ("Participants of owner as XML",
                                                                                          "unused.xml"),
                                                      ".xml"));
    AJAX_DOWNLOAD_JSON = addAjax ( (req,
                                    res) -> _download (req,
                                                       res,
                                                       new ExportAllHandlerParticipantJSON ("Participants of owner as JSON",
                                                                                            "unused.json"),
                                                       ".json"));
    AJAX_DOWNLOAD_CSV = addAjax ( (req,
                                   res) -> _download (req,
                                                      res,
                                                      new ExportAllHandlerParticipantCSV ("Participants of owner as CSV",
                                                                                          "unused.csv"),
                                                      ".csv"));
  }

  public PageSecureParticipantExportByOwner (@NonNull @Nonempty final String sID)
  {
    super (sID, "Export participants by owner");
  }

  @Override
  protected void fillContent (@NonNull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final IRequestWebScopeWithoutResponse aRequestScope = aWPEC.getRequestScope ();
    final FormErrorList aFormErrors = new FormErrorList ();

    if (aWPEC.hasAction (CPageParam.ACTION_PERFORM))
    {
      final String sOwnerID = aWPEC.params ().getAsStringTrimmed (FIELD_OWNER_ID);

      if (StringHelper.isEmpty (sOwnerID))
        aFormErrors.addFieldError (FIELD_OWNER_ID, "An owner ID must be provided.");
      else
      {
        int nCount = -1;
        try
        {
          nCount = ExportAllManager.getAllStoredParticipantIDsOfOwner (sOwnerID).size ();
        }
        catch (final IOException ex)
        {
          LOGGER.error ("Error searching all participants of owner '" + sOwnerID + "'", ex);
        }

        if (nCount < 0)
          aNodeList.addChild (error ("Error searching all participants of owner '" + sOwnerID + "'"));
        else
          if (nCount == 0)
            aFormErrors.addFieldError (FIELD_OWNER_ID, "No participant is owned by '" + sOwnerID + "'.");
          else
          {
            final HCDiv aDiv = div ("Found " +
                                    nCount +
                                    " " +
                                    (nCount == 1 ? "participant" : "participants") +
                                    " owned by ").addChild (code (sOwnerID));
            final String sSeatNumber = PDStoredMetaData.getOwnerIDSeatNumber (sOwnerID);
            if (sSeatNumber != null)
              aDiv.addChild (" (seat number " + sSeatNumber + ")");
            aNodeList.addChild (success (aDiv));

            final BootstrapButtonToolbar aToolbar = aNodeList.addAndReturnChild (new BootstrapButtonToolbar (aWPEC));
            aToolbar.addChild (new BootstrapButton ().addChild ("Download Participants (XML, live)")
                                                     .setOnClick (AJAX_DOWNLOAD_XML.getInvocationURL (aRequestScope)
                                                                                   .add (FIELD_OWNER_ID, sOwnerID))
                                                     .setIcon (EDefaultIcon.SAVE_ALL));
            aToolbar.addChild (new BootstrapButton ().addChild ("Download Participants (JSON, live)")
                                                     .setOnClick (AJAX_DOWNLOAD_JSON.getInvocationURL (aRequestScope)
                                                                                    .add (FIELD_OWNER_ID, sOwnerID))
                                                     .setIcon (EDefaultIcon.SAVE_ALL));
            aToolbar.addChild (new BootstrapButton ().addChild ("Download Participants (CSV, live)")
                                                     .setOnClick (AJAX_DOWNLOAD_CSV.getInvocationURL (aRequestScope)
                                                                                   .add (FIELD_OWNER_ID, sOwnerID))
                                                     .setIcon (EDefaultIcon.SAVE_ALL));
          }
      }
    }

    final BootstrapForm aForm = aNodeList.addAndReturnChild (getUIHandler ().createFormSelf (aWPEC));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Owner ID")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_OWNER_ID)))
                                                 .setHelpText (span ().addChild ("Enter the certificate subject of the owner exactly as stored, without spaces and without a serial number. Participants stored with the old owner ID layout including the serial number are found as well.\nExample owner ID layout: ")
                                                                      .addChild (code ("CN=POP000000,O=Example Company,C=AT")))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_OWNER_ID)));

    final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (new BootstrapButtonToolbar (aWPEC));
    aToolbar.addHiddenField (CPageParam.PARAM_ACTION, CPageParam.ACTION_PERFORM);
    aToolbar.addSubmitButton ("Search participants", EDefaultIcon.MAGNIFIER);
  }
}
