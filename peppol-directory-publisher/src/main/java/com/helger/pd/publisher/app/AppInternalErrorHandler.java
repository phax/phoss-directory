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
package com.helger.pd.publisher.app;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.email.EmailAddress;
import com.helger.photon.core.app.error.InternalErrorBuilder;
import com.helger.photon.core.app.error.InternalErrorSettings;
import com.helger.photon.core.app.error.callback.AbstractErrorCallback;
import com.helger.photon.core.mgr.PhotonCoreManager;
import com.helger.photon.core.smtp.CNamedSMTPSettings;
import com.helger.photon.core.smtp.NamedSMTPSettings;
import com.helger.smtp.settings.ISMTPSettings;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

public final class AppInternalErrorHandler extends AbstractErrorCallback
{
  @Override
  protected void onError (@Nonnull final Throwable t,
                          @Nullable final IRequestWebScopeWithoutResponse aRequestScope,
                          @Nonnull @Nonempty final String sErrorCode,
                          @Nullable final Map <String, String> aCustomAttrs)
  {
    new InternalErrorBuilder ().setThrowable (t)
                               .setRequestScope (aRequestScope)
                               .addErrorMessage (sErrorCode)
                               .addCustomData (aCustomAttrs)
                               .handle ();
  }

  public static void doSetup ()
  {
    // Set global internal error handlers
    new AppInternalErrorHandler ().install ();

    final NamedSMTPSettings aNamedSettings = PhotonCoreManager.getSMTPSettingsMgr ()
                                                              .getSettings (CNamedSMTPSettings.NAMED_SMTP_SETTINGS_DEFAULT_ID);
    final ISMTPSettings aSMTPSettings = aNamedSettings == null ? null : aNamedSettings.getSMTPSettings ();
    InternalErrorSettings.setSMTPSenderAddress (new EmailAddress ("pd@helger.com", AppCommonUI.getApplicationTitle ()));
    InternalErrorSettings.setSMTPReceiverAddress (new EmailAddress ("philip@helger.com", "Philip"));
    InternalErrorSettings.setSMTPSettings (aSMTPSettings);
    InternalErrorSettings.setFallbackLocale (AppCommonUI.DEFAULT_LOCALE);
  }
}
