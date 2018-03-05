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
package com.helger.pd.publisher;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.email.EmailAddress;
import com.helger.commons.email.IEmailAddress;
import com.helger.commons.url.ISimpleURL;
import com.helger.commons.url.SimpleURL;
import com.helger.pd.settings.PDServerConfiguration;

@Immutable
public final class CPDPublisher
{
  private CPDPublisher ()
  {}

  // Email sender - depends on the used SMTP server
  public static final IEmailAddress EMAIL_SENDER = new EmailAddress ("no-reply@helger.com");

  // APP Name - like "PEPPOL Directory"
  public static final String APP_NAME_BASIC = PDServerConfiguration.getAppName ();
  public static final String APP_NAME = APP_NAME_BASIC + (PDServerConfiguration.isTestVersion () ? " [TEST]" : "");

  private static ISimpleURL s_aLogoImageURL = new SimpleURL ("/imgs/peppol.png");

  public static void setLogoImageURL (@Nonnull @Nonempty final String sLogoImageURL)
  {
    ValueEnforcer.notEmpty (sLogoImageURL, "LogoImageURL");
    s_aLogoImageURL = new SimpleURL (sLogoImageURL);
  }

  @Nonnull
  public static ISimpleURL getLogoImageURL ()
  {
    return s_aLogoImageURL;
  }
}
