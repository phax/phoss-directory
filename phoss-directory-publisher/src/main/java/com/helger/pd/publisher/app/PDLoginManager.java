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
package com.helger.pd.publisher.app;

import java.time.Duration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.textlevel.HCSmall;
import com.helger.pd.indexer.CDirectoryVersion;
import com.helger.pd.publisher.CPDPublisher;
import com.helger.photon.app.html.IHTMLProvider;
import com.helger.photon.bootstrap4.CBootstrapCSS;
import com.helger.photon.bootstrap4.uictrls.ext.BootstrapLoginHTMLProvider;
import com.helger.photon.bootstrap4.uictrls.ext.BootstrapLoginManager;
import com.helger.photon.core.execcontext.ISimpleWebExecutionContext;
import com.helger.security.authentication.credentials.ICredentialValidationResult;

public final class PDLoginManager extends BootstrapLoginManager
{
  public PDLoginManager ()
  {
    super (CPDPublisher.getApplicationTitle () + " Administration - Login");
    setRequiredRoleIDs (AppSecurity.REQUIRED_ROLE_IDS_CONFIG);
    setFailedLoginWaitingTime (Duration.ofSeconds (1));
  }

  @Override
  protected IHTMLProvider createLoginScreen (final boolean bLoginError,
                                             @Nonnull final ICredentialValidationResult aLoginResult)
  {
    return new BootstrapLoginHTMLProvider (bLoginError, aLoginResult, getPageTitle ())
    {
      @Override
      @Nullable
      protected IHCNode createFormFooter (@Nonnull final ISimpleWebExecutionContext aSWEC)
      {
        final HCDiv aDiv = new HCDiv ().addClass (CBootstrapCSS.D_FLEX).addClass (CBootstrapCSS.MT_5);
        aDiv.addChild (new HCSmall ().addChild (CPDPublisher.getApplicationTitleWithVersion () +
                                                " / " +
                                                CDirectoryVersion.BUILD_TIMESTAMP));
        return aDiv;
      }
    };
  }
}
