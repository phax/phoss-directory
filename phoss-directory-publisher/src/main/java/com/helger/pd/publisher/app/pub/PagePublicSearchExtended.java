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
package com.helger.pd.publisher.app.pub;

import java.util.Locale;

import com.helger.annotation.Nonempty;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.publisher.search.EPDSearchField;
import com.helger.pd.publisher.search.ESearchOperator;
import com.helger.pd.publisher.ui.HCSearchOperatorSelect;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.form.BootstrapViewForm;
import com.helger.photon.bootstrap4.grid.BootstrapRow;
import com.helger.photon.bootstrap4.uictrls.datetimepicker.BootstrapDateTimePicker;
import com.helger.photon.bootstrap4.uictrls.datetimepicker.EBootstrap4DateTimePickerMode;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.html.select.HCCountrySelect;
import com.helger.photon.uicore.page.WebPageExecutionContext;

import jakarta.annotation.Nonnull;

public final class PagePublicSearchExtended extends AbstractPagePublicSearch
{
  private static final String PREFIX_OPERATOR = "op-";
  private static final String PREFIX_SPECIAL = "special-";

  public PagePublicSearchExtended (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Extended Search [DEBUG ONLY]");
  }

  @Nonnull
  private static HCNodeList _createCtrl (@Nonnull final EPDSearchField eField, @Nonnull final Locale aDisplayLocale)
  {
    final String sFieldName = eField.getFieldName ();
    final HCNodeList ret = new HCNodeList ();
    switch (eField)
    {
      case COUNTRY:
        ret.addChild (new HCCountrySelect (new RequestField (PREFIX_SPECIAL + sFieldName), aDisplayLocale));
        break;
      case REGISTRATION_DATE:
        ret.addChild (BootstrapDateTimePicker.create (PREFIX_SPECIAL + sFieldName, aDisplayLocale, EBootstrap4DateTimePickerMode.DATE));
        break;
    }
    // Default to String
    ret.addChild (new HCEdit (new RequestField (sFieldName)));
    return ret;
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    aNodeList.addChild (info ("This is a placeholder page - has no effect yet!"));

    final BootstrapViewForm aViewForm = new BootstrapViewForm ();
    // Add all search fields
    for (final EPDSearchField eField : EPDSearchField.values ())
    {
      final HCSearchOperatorSelect aSelect = new HCSearchOperatorSelect (new RequestField (PREFIX_OPERATOR + eField.getFieldName (),
                                                                                           ESearchOperator.EQ.getID ()),
                                                                         eField.getDataType (),
                                                                         aDisplayLocale);
      final HCNodeList aCtrl = _createCtrl (eField, aDisplayLocale);
      final BootstrapRow aRow = new BootstrapRow ();
      aRow.createColumn (2).addChild (aSelect);
      aRow.createColumn (10).addChild (aCtrl);
      aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel (eField.getDisplayText (aDisplayLocale)).setCtrl (aRow));
    }
    aNodeList.addChild (aViewForm);
  }
}
