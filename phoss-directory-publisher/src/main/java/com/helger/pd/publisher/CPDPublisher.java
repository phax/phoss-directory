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
package com.helger.pd.publisher;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.email.EmailAddress;
import com.helger.base.email.IEmailAddress;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.pd.indexer.CDirectoryVersion;
import com.helger.pd.indexer.settings.PDServerConfiguration;
import com.helger.url.ISimpleURL;
import com.helger.url.SimpleURL;

import jakarta.annotation.Nonnull;

@NotThreadSafe
public final class CPDPublisher
{
  // Email sender - depends on the used SMTP server
  public static final IEmailAddress EMAIL_SENDER = new EmailAddress ("no-reply@helger.com");

  public static final boolean EXPORT_BUSINESS_CARDS_XML = true;
  public static final boolean EXPORT_BUSINESS_CARDS_JSON = true;
  public static final boolean EXPORT_BUSINESS_CARDS_CSV = true;

  public static final boolean EXPORT_PARTICIPANTS_XML = true;
  public static final boolean EXPORT_PARTICIPANTS_JSON = true;
  public static final boolean EXPORT_PARTICIPANTS_CSV = true;

  // APP Name - like "Peppol Directory"
  private static final String APPLICATION_TITLE = PDServerConfiguration.getAppName ();

  private static ISimpleURL s_aLogoImageURL = new SimpleURL ("/imgs/peppol/peppol.png");

  private CPDPublisher ()
  {}

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

  @Nonnull
  @Nonempty
  public static String getApplication ()
  {
    return APPLICATION_TITLE;
  }

  @Nonnull
  @Nonempty
  public static String getApplicationTitle ()
  {
    return APPLICATION_TITLE + (PDServerConfiguration.isTestVersion () ? " [TEST]" : "");
  }

  @Nonnull
  @Nonempty
  public static String getApplicationTitleWithVersion ()
  {
    return getApplicationTitle () + " v" + CDirectoryVersion.BUILD_VERSION;
  }
}
