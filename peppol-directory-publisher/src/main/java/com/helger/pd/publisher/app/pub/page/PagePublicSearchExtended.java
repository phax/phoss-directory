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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.publisher.search.EPDSearchField;
import com.helger.pd.publisher.search.ui.HCSearchOperatorSelect;
import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.photon.bootstrap3.form.BootstrapFormGroup;
import com.helger.photon.bootstrap3.form.BootstrapViewForm;
import com.helger.photon.bootstrap3.uictrls.datetimepicker.BootstrapDateTimePicker;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.form.RequestFieldDate;
import com.helger.photon.uicore.html.select.HCCountrySelect;
import com.helger.photon.uicore.page.WebPageExecutionContext;

public final class PagePublicSearchExtended extends AbstractPagePublicSearch
{
  private static final String PREFIX_OPERATOR = "op";

  private static final Logger s_aLogger = LoggerFactory.getLogger (PagePublicSearchExtended.class);

  public PagePublicSearchExtended (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Extended Search");
  }

  @Nonnull
  private static IHCNode _createCtrl (@Nonnull final EPDSearchField eField, @Nonnull final Locale aDisplayLocale)
  {
    final String sFieldName = eField.getFieldName ();
    switch (eField)
    {
      case COUNTRY:
        return new HCCountrySelect (new RequestField (sFieldName), aDisplayLocale);
      case REGISTRATION_DATE:
        return new BootstrapDateTimePicker (new RequestFieldDate (sFieldName, aDisplayLocale));
      default:
        // Default to String
        return new HCEdit (new RequestField (sFieldName));
    }
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();

    final BootstrapViewForm aViewForm = new BootstrapViewForm ();
    // Add all search fields
    for (final EPDSearchField eField : EPDSearchField.values ())
    {
      aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel (eField.getDisplayText (aDisplayLocale))
                                                       .setCtrl (new HCSearchOperatorSelect (new RequestField (PREFIX_OPERATOR +
                                                                                                               eField.getFieldName ()),
                                                                                             eField.getDataType (),
                                                                                             aDisplayLocale),
                                                                 _createCtrl (eField, aDisplayLocale)));
    }
    aNodeList.addChild (aViewForm);
  }
}
