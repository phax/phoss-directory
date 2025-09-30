/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.StringHelper;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.businesscard.IPDBusinessCardProvider;
import com.helger.pd.indexer.businesscard.SMPBusinessCardProvider;
import com.helger.pd.indexer.index.EIndexerWorkItemType;
import com.helger.pd.indexer.mgr.PDIndexerManager;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.storage.CPDStorage;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppolid.CIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.form.BootstrapForm;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;

public final class PageSecureIndexManually extends AbstractAppWebPage
{
  public static final String FIELD_PARTICIPANT_ID = "participantid";

  public PageSecureIndexManually (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Manually index participant");
  }

  @Override
  protected void fillContent (final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();
    final FormErrorList aFormErrors = new FormErrorList ();

    {
      final IPDBusinessCardProvider aBCProv = PDMetaManager.getBusinessCardProvider ();
      if (aBCProv instanceof SMPBusinessCardProvider)
      {
        final SMPBusinessCardProvider aSMPBCProv = (SMPBusinessCardProvider) aBCProv;
        if (aSMPBCProv.isFixedSMP ())
        {
          aNodeList.addChild (info ("Fixed SMP URI " + aSMPBCProv.getFixedSMPURI () + " is used."));
        }
        else
        {
          aNodeList.addChild (info ("The following SMLs are crawled for entries: " +
                                    StringHelper.getImplodedMapped (", ", aSMPBCProv.getAllSMLsToUse (), ISMLInfo::getDisplayName)));
        }
      }
    }

    if (aWPEC.hasAction (CPageParam.ACTION_PERFORM))
    {
      final String sParticipantID = aWPEC.params ().getAsString (FIELD_PARTICIPANT_ID);
      final IParticipantIdentifier aParticipantID = aIdentifierFactory.parseParticipantIdentifier (sParticipantID);

      if (StringHelper.hasNoText (sParticipantID))
        aFormErrors.addFieldError (FIELD_PARTICIPANT_ID, "A participant ID must be provided.");
      else
        if (aParticipantID == null)
          aFormErrors.addFieldError (FIELD_PARTICIPANT_ID, "The provided participant ID is syntactically invalid.");

      if (aFormErrors.isEmpty ())
      {
        if (PDMetaManager.getIndexerMgr ()
                         .queueWorkItem (aParticipantID,
                                         EIndexerWorkItemType.CREATE_UPDATE,
                                         CPDStorage.OWNER_MANUALLY_TRIGGERED,
                                         PDIndexerManager.HOST_LOCALHOST)
                         .isChanged ())
        {
          aWPEC.postRedirectGetInternal (success ("The indexing of participant ID '" + sParticipantID + "' was successfully triggered!"));
        }
        else
        {
          aWPEC.postRedirectGetInternal (warn ("Participant ID '" + sParticipantID + "' is already in the indexing queue!"));
        }
      }
    }

    final BootstrapForm aForm = aNodeList.addAndReturnChild (getUIHandler ().createFormSelf (aWPEC));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Participant ID")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_PARTICIPANT_ID,
                                                                                         PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME +
                                                                                                               CIdentifier.URL_SCHEME_VALUE_SEPARATOR)))
                                                 .setHelpText (span ().addChild ("Enter the fully qualified Peppol participant ID (including the scheme) you want to index.\nExample identifier layout: ")
                                                                      .addChild (code (aIdentifierFactory.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                                                                       "9999:test")
                                                                                                         .getURIEncoded ())))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_PARTICIPANT_ID)));

    final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (new BootstrapButtonToolbar (aWPEC));
    aToolbar.addHiddenField (CPageParam.PARAM_ACTION, CPageParam.ACTION_PERFORM);
    aToolbar.addSubmitButton ("Add to queue", EDefaultIcon.YES);
  }
}
