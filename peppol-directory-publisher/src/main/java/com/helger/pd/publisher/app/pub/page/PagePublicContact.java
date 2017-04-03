/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.app.pub.page;

import java.util.Locale;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.email.EmailAddress;
import com.helger.commons.email.EmailAddressHelper;
import com.helger.commons.string.StringHelper;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCHiddenField;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.publisher.CPDPublisher;
import com.helger.pd.publisher.app.pub.CMenuPublic;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.photon.bootstrap3.EBootstrapIcon;
import com.helger.photon.bootstrap3.alert.BootstrapInfoBox;
import com.helger.photon.bootstrap3.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap3.button.BootstrapButton;
import com.helger.photon.bootstrap3.button.BootstrapSubmitButton;
import com.helger.photon.bootstrap3.button.EBootstrapButtonType;
import com.helger.photon.bootstrap3.form.BootstrapForm;
import com.helger.photon.bootstrap3.form.BootstrapFormGroup;
import com.helger.photon.core.app.error.InternalErrorSettings;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.html.select.HCExtSelect;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.autosize.HCTextAreaAutosize;
import com.helger.smtp.data.EEmailType;
import com.helger.smtp.data.EmailData;
import com.helger.smtp.scope.ScopedMailAPI;

public final class PagePublicContact extends AbstractAppWebPage
{
  private static final String FIELD_NAME = "name";
  private static final String FIELD_EMAIL = "email";
  private static final String FIELD_TOPIC = "reason";
  private static final String FIELD_TEXT = "topic";

  public PagePublicContact (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Contact form");
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    boolean bShowForm = true;
    final FormErrorList aFormErrors = new FormErrorList ();
    if (aWPEC.hasAction (CPageParam.ACTION_PERFORM))
    {
      final String sName = StringHelper.trim (aWPEC.getAttributeAsString (FIELD_NAME));
      final String sEmail = StringHelper.trim (aWPEC.getAttributeAsString (FIELD_EMAIL));
      final String sTopic = aWPEC.getAttributeAsString (FIELD_TOPIC);
      final String sText = StringHelper.trim (aWPEC.getAttributeAsString (FIELD_TEXT));

      if (StringHelper.hasNoText (sName))
        aFormErrors.addFieldError (FIELD_NAME, "Your name must be provided.");
      if (StringHelper.hasNoText (sEmail))
        aFormErrors.addFieldError (FIELD_EMAIL, "Your email address must be provided.");
      else
        if (!EmailAddressHelper.isValid (sEmail))
          aFormErrors.addFieldError (FIELD_EMAIL, "The provided email address is invalid.");
      if (StringHelper.hasNoText (sText))
        aFormErrors.addFieldError (FIELD_TEXT, "A message text must be provided.");

      if (aFormErrors.isEmpty ())
      {
        final EmailData aEmailData = new EmailData (EEmailType.TEXT);
        aEmailData.setFrom (CPDPublisher.EMAIL_SENDER);
        aEmailData.setTo (new EmailAddress ("support@peppol.eu"), new EmailAddress ("pd@helger.com"));
        aEmailData.setReplyTo (new EmailAddress (sEmail, sName));
        aEmailData.setSubject ("[PEPPOL Directory] Contact Form - " + sName);

        final StringBuilder aSB = new StringBuilder ();
        aSB.append ("Contact form from PEPPOL Directory was filled out.\n\n");
        aSB.append ("Name: ").append (sName).append ("\n");
        aSB.append ("Email: ").append (sEmail).append ("\n");
        aSB.append ("Topic: ").append (sTopic).append ("\n");
        aSB.append ("Text:\n").append (sText).append ("\n");
        aEmailData.setBody (aSB.toString ());

        ScopedMailAPI.getInstance ().queueMail (InternalErrorSettings.getSMTPSettings (), aEmailData);

        aNodeList.addChild (new BootstrapSuccessBox ().addChild ("Thank you for your message. We will come back to you asap."));
        bShowForm = false;
      }
    }

    if (bShowForm)
    {
      aNodeList.addChild (new BootstrapInfoBox ().addChild ("Alternatively write an email to support[at]peppol.eu or pd[at]helger.com - usually using the below form is more effective!"));

      final BootstrapForm aForm = aNodeList.addAndReturnChild (getUIHandler ().createFormSelf (aWPEC));
      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Your name")
                                                   .setCtrl (new HCEdit (new RequestField (FIELD_NAME)))
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_NAME)));
      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Your email address")
                                                   .setCtrl (new HCEdit (new RequestField (FIELD_EMAIL)))
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_EMAIL)));

      final HCExtSelect aSelect = new HCExtSelect (new RequestField (FIELD_TOPIC));
      aSelect.addOption ("SMP integration");
      aSelect.addOption ("Website");
      aSelect.addOption ("REST service");
      aSelect.addOption ("General question");
      aSelect.addOptionPleaseSelect (aDisplayLocale);
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Topic")
                                                   .setCtrl (aSelect)
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_TOPIC)));

      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Your message")
                                                   .setCtrl (new HCTextAreaAutosize (new RequestField (FIELD_TEXT)).setRows (5))
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_TEXT)));

      aForm.addChild (new HCHiddenField (CPageParam.PARAM_ACTION, CPageParam.ACTION_PERFORM));
      aForm.addChild (new BootstrapSubmitButton ().addChild ("Send message").setIcon (EBootstrapIcon.SEND));
      aForm.addChild (new BootstrapButton (EBootstrapButtonType.DEFAULT).addChild ("No thanks")
                                                                        .setIcon (EDefaultIcon.CANCEL)
                                                                        .setOnClick (aWPEC.getLinkToMenuItem (CMenuPublic.MENU_SEARCH)));
    }
  }
}
