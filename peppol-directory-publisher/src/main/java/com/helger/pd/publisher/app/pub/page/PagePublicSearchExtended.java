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
import com.helger.commons.locale.country.CountryCache;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.publisher.search.EPDSearchParams;
import com.helger.pd.publisher.ui.HCSearchOperatorSelect;
import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.photon.bootstrap3.form.BootstrapFormGroup;
import com.helger.photon.bootstrap3.form.BootstrapViewForm;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.html.select.HCCountrySelect;
import com.helger.photon.uicore.page.WebPageExecutionContext;

public final class PagePublicSearchExtended extends AbstractPagePublicSearch
{
  private static final String PREFIX_OPERATOR = "op";
  public static final String FIELD_PARTICIPANT_ID = EPDSearchParams.PARTICIPANT_ID.getParamName ();
  public static final String FIELD_NAME = EPDSearchParams.NAME.getParamName ();
  public static final String FIELD_COUNTRY = EPDSearchParams.COUNTRY.getParamName ();
  public static final String FIELD_GEO_INFO = EPDSearchParams.GEO_INFO.getParamName ();
  public static final String FIELD_IDENTIFIER = EPDSearchParams.IDENTIFIER.getParamName ();
  public static final String FIELD_REGISTRATION_DATE = EPDSearchParams.REGISTRATION_DATE.getParamName ();

  public static final String PARAM_MAX = "max";
  private static final Logger s_aLogger = LoggerFactory.getLogger (PagePublicSearchExtended.class);

  public PagePublicSearchExtended (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Extended Search");
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();

    final String sName = aWPEC.getAttributeAsString (FIELD_NAME);
    final String sCountry = aWPEC.getAttributeAsString (FIELD_COUNTRY);
    final Locale aCountry = CountryCache.getInstance ().getCountry (sCountry);

    final BootstrapViewForm aViewForm = new BootstrapViewForm ();
    aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Name")
                                                     .setCtrl (new HCSearchOperatorSelect (new RequestField (PREFIX_OPERATOR +
                                                                                                             FIELD_NAME),
                                                                                           EPDSearchParams.NAME.getDataType (),
                                                                                           aDisplayLocale),
                                                               new HCEdit (new RequestField (FIELD_NAME))));
    aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Country")
                                                     .setCtrl (new HCCountrySelect (new RequestField (FIELD_COUNTRY),
                                                                                    aDisplayLocale)));
    aNodeList.addChild (aViewForm);
  }
}
