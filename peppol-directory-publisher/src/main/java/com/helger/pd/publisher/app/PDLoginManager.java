/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.app;

import javax.annotation.Nonnull;

import com.helger.css.property.CCSSProperties;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.textlevel.HCSmall;
import com.helger.pd.CDirectory;
import com.helger.pd.publisher.CPDPublisher;
import com.helger.photon.bootstrap3.base.BootstrapContainer;
import com.helger.photon.bootstrap3.grid.BootstrapRow;
import com.helger.photon.bootstrap3.uictrls.ext.BootstrapLoginHTMLProvider;
import com.helger.photon.bootstrap3.uictrls.ext.BootstrapLoginManager;
import com.helger.photon.core.app.context.ISimpleWebExecutionContext;
import com.helger.photon.core.app.html.IHTMLProvider;
import com.helger.security.authentication.credentials.ICredentialValidationResult;

public final class PDLoginManager extends BootstrapLoginManager
{
  public PDLoginManager ()
  {
    super (CPDPublisher.getApplicationTitle () + " Administration - Login");
    setRequiredRoleIDs (AppSecurity.REQUIRED_ROLE_IDS_CONFIG);
  }

  @Override
  protected IHTMLProvider createLoginScreen (final boolean bLoginError,
                                             @Nonnull final ICredentialValidationResult aLoginResult)
  {
    return new BootstrapLoginHTMLProvider (bLoginError, aLoginResult, getPageTitle ())
    {
      @Override
      protected void onAfterContainer (final ISimpleWebExecutionContext aSWEC,
                                       final BootstrapContainer aContainer,
                                       final BootstrapRow aRow,
                                       final HCDiv aContentCol)
      {
        final HCDiv aDiv = new HCDiv ().addStyle (CCSSProperties.MARGIN_TOP.newValue ("1rem"));
        aDiv.addChild (new HCSmall ().addChild (CPDPublisher.getApplicationTitleWithVersion () +
                                                " / " +
                                                CDirectory.APPLICATION_TIMESTAMP));
        aContentCol.addChild (aDiv);
      }
    };
  }
}
