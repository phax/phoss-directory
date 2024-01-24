/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.StringHelper;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
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

public final class PageSecureDeleteManually extends AbstractAppWebPage
{
  public static final String FIELD_PARTICIPANT_ID = "participantid";

  public PageSecureDeleteManually (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Manually delete participant");
  }

  @Override
  protected void fillContent (final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();
    final FormErrorList aFormErrors = new FormErrorList ();

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
        int nDeleted = 0;
        try
        {
          nDeleted = PDMetaManager.getStorageMgr ().deleteEntry (aParticipantID, null, false);
        }
        catch (final IOException ex)
        {
          // ignore
          nDeleted = -1;
        }
        if (nDeleted > 0)
          aWPEC.postRedirectGetInternal (success ("The participant ID '" +
                                                  aParticipantID.getURIEncoded () +
                                                  "' was deleted (" +
                                                  nDeleted +
                                                  " rows)"));
        else
          aWPEC.postRedirectGetInternal (error ("Error deleting participant ID '" + aParticipantID.getURIEncoded () + "'"));
      }
    }

    final BootstrapForm aForm = aNodeList.addAndReturnChild (getUIHandler ().createFormSelf (aWPEC));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Participant ID")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_PARTICIPANT_ID,
                                                                                         PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME +
                                                                                                               CIdentifier.URL_SCHEME_VALUE_SEPARATOR)))
                                                 .setHelpText (span ().addChild ("Enter the fully qualified Peppol participant ID (including the scheme) you want to delete.\nExample identifier layout: ")
                                                                      .addChild (code (aIdentifierFactory.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                                                                       "9999:test")
                                                                                                         .getURIEncoded ())))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_PARTICIPANT_ID)));

    final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (new BootstrapButtonToolbar (aWPEC));
    aToolbar.addHiddenField (CPageParam.PARAM_ACTION, CPageParam.ACTION_PERFORM);
    aToolbar.addSubmitButton ("Delete from index", EDefaultIcon.DELETE);
  }
}
