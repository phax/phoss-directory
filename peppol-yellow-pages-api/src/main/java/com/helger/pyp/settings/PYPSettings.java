/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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
package com.helger.pyp.settings;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.scope.singleton.AbstractGlobalSingleton;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.utils.KeyStoreHelper;
import com.helger.settings.IMutableSettings;
import com.helger.settings.ISettings;
import com.helger.settings.exchange.properties.SettingsPersistenceProperties;

/**
 * This class provides access to the settings as contained in the
 * <code>webapp.properties</code> file.
 *
 * @author Philip Helger
 */
public class PYPSettings extends AbstractGlobalSingleton
{
  /** The name of the file containing the settings */
  public static final String FILENAME = "pyp.properties";
  private static final IMutableSettings s_aSettings;

  static
  {
    s_aSettings = new SettingsPersistenceProperties ().readSettings (new ClassPathResource (FILENAME));
  }

  @Deprecated
  @UsedViaReflection
  private PYPSettings ()
  {}

  @Nonnull
  public static ISettings getSettingsObject ()
  {
    return s_aSettings;
  }

  @Nullable
  public static String getGlobalDebug ()
  {
    return s_aSettings.getStringValue ("global.debug");
  }

  @Nullable
  public static String getGlobalProduction ()
  {
    return s_aSettings.getStringValue ("global.production");
  }

  @Nullable
  public static String getDataPath ()
  {
    return s_aSettings.getStringValue ("webapp.datapath");
  }

  public static boolean isCheckFileAccess ()
  {
    return s_aSettings.getBooleanValue ("webapp.checkfileaccess", true);
  }

  public static boolean isTestVersion ()
  {
    return s_aSettings.getBooleanValue ("webapp.testversion", GlobalDebug.isDebugMode ());
  }

  @Nullable
  public static String getClientCertIssuer ()
  {
    return s_aSettings.getStringValue ("clientcert.issuer");
  }

  @Nullable
  public static String getClientCertIssuerAlternative ()
  {
    return s_aSettings.getStringValue ("clientcert-alt.issuer");
  }

  @Nonnull
  public static String getTruststoreLocation ()
  {
    return s_aSettings.getStringValue ("truststore.path", KeyStoreHelper.TRUSTSTORE_PRODUCTION_CLASSPATH);
  }

  @Nonnull
  public static String getTruststorePassword ()
  {
    return s_aSettings.getStringValue ("truststore.password", KeyStoreHelper.TRUSTSTORE_PASSWORD);
  }

  @Nonnull
  public static String getTruststoreAlias ()
  {
    return s_aSettings.getStringValue ("truststore.alias", KeyStoreHelper.TRUSTSTORE_PRODUCTION_ALIAS_SMP);
  }

  @Nullable
  public static String getTruststoreLocationAlternative ()
  {
    return s_aSettings.getStringValue ("truststore-alt.path", KeyStoreHelper.TRUSTSTORE_PILOT_CLASSPATH);
  }

  @Nullable
  public static String getTruststorePasswordAlternative ()
  {
    return s_aSettings.getStringValue ("truststore-alt.password", KeyStoreHelper.TRUSTSTORE_PASSWORD);
  }

  @Nullable
  public static String getTruststoreAliasAlternative ()
  {
    return s_aSettings.getStringValue ("truststore-alt.alias", KeyStoreHelper.TRUSTSTORE_PILOT_ALIAS_SMP);
  }

  /**
   * @return The maximum number of hours a retry will happen. If not provided 24
   *         hours is the default value.
   */
  @Nonnegative
  public static int getReIndexMaxRetryHours ()
  {
    final int ret = s_aSettings.getIntValue ("reindex.maxretryhours", 24);
    if (ret < 0)
      throw new IllegalStateException ("The reindex.maxretryhours property must be >= 0!");
    return ret;
  }

  /**
   * @return The number of minutes between retries. Defaults to 5.
   */
  @Nonnegative
  public static int getReIndexRetryMinutes ()
  {
    final int ret = s_aSettings.getIntValue ("reindex.retryminutes", 5);
    if (ret <= 0)
      throw new IllegalStateException ("The reindex.retryminutes property must be > 0!");
    return ret;
  }

  /**
   * @return The SML to be used. Never <code>null</code>. Defaults to
   *         {@link ESML#DIGIT_PRODUCTION}.
   */
  @Nonnull
  public static ESML getSMLToUse ()
  {
    final String sID = s_aSettings.getStringValue ("sml.id");
    return ESML.getFromIDOrDefault (sID, ESML.DIGIT_PRODUCTION);
  }
}
