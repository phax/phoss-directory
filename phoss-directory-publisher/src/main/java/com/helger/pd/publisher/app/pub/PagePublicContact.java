/**
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.app.pub;

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
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.button.BootstrapSubmitButton;
import com.helger.photon.bootstrap4.form.BootstrapForm;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.interror.InternalErrorSettings;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.html.google.HCReCaptchaV2;
import com.helger.photon.uicore.html.google.ReCaptchaServerSideValidator;
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
  private static final String FIELD_CAPTCHA = "captcha";

  public PagePublicContact (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Contact us");
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    final boolean bShowForm = true;
    final FormErrorList aFormErrors = new FormErrorList ();
    if (aWPEC.hasAction (CPageParam.ACTION_PERFORM))
    {
      final String sName = StringHelper.trim (aWPEC.params ().getAsString (FIELD_NAME));
      final String sEmail = StringHelper.trim (aWPEC.params ().getAsString (FIELD_EMAIL));
      final String sTopic = aWPEC.params ().getAsString (FIELD_TOPIC);
      final String sText = StringHelper.trim (aWPEC.params ().getAsString (FIELD_TEXT));
      final String sReCaptcha = StringHelper.trim (aWPEC.params ().getAsString ("g-recaptcha-response"));

      if (StringHelper.hasNoText (sName))
        aFormErrors.addFieldError (FIELD_NAME, "Your name must be provided.");
      if (StringHelper.hasNoText (sEmail))
        aFormErrors.addFieldError (FIELD_EMAIL, "Your email address must be provided.");
      else
        if (!EmailAddressHelper.isValid (sEmail))
          aFormErrors.addFieldError (FIELD_EMAIL, "The provided email address is invalid.");
      if (StringHelper.hasNoText (sText))
        aFormErrors.addFieldError (FIELD_TEXT, "A message text must be provided.");

      if (aFormErrors.isEmpty () || StringHelper.hasText (sReCaptcha))
      {
        if (!CaptchaSessionSingleton.getInstance ().isChecked ())
        {
          // Check only if no other errors occurred
          if (ReCaptchaServerSideValidator.check ("6LfZFS0UAAAAAONDJHyDnuUUvMB_oNmJxz9Utxza", sReCaptcha).isFailure ())
            aFormErrors.addFieldError (FIELD_CAPTCHA, "Please confirm you are not a robot!");
          else
            CaptchaSessionSingleton.getInstance ().setChecked ();
        }
      }

      if (aFormErrors.isEmpty ())
      {
        final EmailData aEmailData = new EmailData (EEmailType.TEXT);
        aEmailData.setFrom (CPDPublisher.EMAIL_SENDER);
        aEmailData.to ().add (new EmailAddress ("pd@helger.com"));
        aEmailData.replyTo ().add (new EmailAddress (sEmail, sName));
        aEmailData.setSubject ("[" + CPDPublisher.getApplication () + "] Contact Form - " + sName);

        final StringBuilder aSB = new StringBuilder ();
        aSB.append ("Contact form from " + CPDPublisher.getApplication () + " was filled out.\n\n");
        aSB.append ("Name: ").append (sName).append ("\n");
        aSB.append ("Email: ").append (sEmail).append ("\n");
        aSB.append ("Topic: ").append (sTopic).append ("\n");
        aSB.append ("Text:\n").append (sText).append ("\n");
        aEmailData.setBody (aSB.toString ());

        ScopedMailAPI.getInstance ().queueMail (InternalErrorSettings.getSMTPSettings (), aEmailData);

        aWPEC.postRedirectGetInternal (success ("Thank you for your message. We will come back to you asap."));
      }
    }

    if (bShowForm)
    {
      aNodeList.addChild (info ("Alternatively write an email to pd[at]helger.com - usually using the below form is more effective!"));
      aNodeList.addChild (warn ("Please don't request any change of data via this contact form - contact your service provider instead. Thank you."));

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
      aSelect.addOption ("SMP Statement of use");
      aSelect.addOption ("General question");
      aSelect.addOptionPleaseSelect (aDisplayLocale);
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Topic")
                                                   .setCtrl (aSelect)
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_TOPIC)));

      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Your message")
                                                   .setCtrl (new HCTextAreaAutosize (new RequestField (FIELD_TEXT)).setRows (5))
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_TEXT)));

      if (!CaptchaSessionSingleton.getInstance ().isChecked ())
      {
        // Add visible Captcha
        aForm.addFormGroup (new BootstrapFormGroup ().setCtrl (HCReCaptchaV2.create ("6LfZFS0UAAAAAJaqpHJdFS_xxY7dqMQjXoBIQWOD",
                                                                                     aDisplayLocale))
                                                     .setErrorList (aFormErrors.getListOfField (FIELD_CAPTCHA)));
      }

      aForm.addChild (new HCHiddenField (CPageParam.PARAM_ACTION, CPageParam.ACTION_PERFORM));

      aForm.addChild (new BootstrapSubmitButton ().addChild ("Send message").setIcon (EDefaultIcon.YES));
      aForm.addChild (new BootstrapButton ().addChild ("No thanks")
                                            .setIcon (EDefaultIcon.CANCEL)
                                            .setOnClick (aWPEC.getLinkToMenuItem (CMenuPublic.MENU_SEARCH_SIMPLE)));
    }
  }
}
