/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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
package com.jelger.pyp.indexer.page;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.css.property.CCSSProperties;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.photon.bootstrap3.alert.BootstrapInfoBox;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.jelger.pyp.indexer.app.AppCommonUI;
import com.jelger.pyp.indexer.app.CApp;

public final class PagePublicLogin extends AbstractAppWebPage
{
  public PagePublicLogin (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Login");
  }

  @Override
  @Nullable
  public String getHeaderText (@Nonnull final WebPageExecutionContext aWPEC)
  {
    return null;
  }

  @Override
  protected void fillContent (final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();

    aNodeList.addChild (AppCommonUI.createViewLoginForm (aWPEC, null, true));
    aNodeList.addChild (new BootstrapInfoBox ().addChild ("Demo login ")
                                               .addChild (new HCCode ().addChild (CApp.USER_ADMINISTRATOR_LOGINNAME))
                                               .addChild (" with password ")
                                               .addChild (new HCCode ().addChild (CApp.USER_ADMINISTRATOR_PASSWORD))
                                               .addStyle (CCSSProperties.MARGIN_TOP.newValue ("2em")));
  }
}
