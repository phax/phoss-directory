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
package com.helger.pd.settings;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.scope.singleton.AbstractGlobalSingleton;
import com.helger.peppol.utils.PeppolKeyStoreHelper;
import com.helger.settings.ISettings;
import com.helger.settings.exchange.configfile.ConfigFile;
import com.helger.settings.exchange.configfile.ConfigFileBuilder;

/**
 * This class manages the configuration properties of the PEPPOL Directory
 * Server. The order of the properties file resolving is as follows:
 * <ol>
 * <li>Check for the value of the system property
 * <code>peppol.directory.server.properties.path</code></li>
 * <li>Check for the value of the system property
 * <code>directory.server.properties.path</code></li>
 * <li>The filename <code>private-pd.properties</code> in the root of the
 * classpath</li>
 * <li>The filename <code>pd.properties</code> in the root of the classpath</li>
 * </ol>
 *
 * @author Philip Helger
 */
@Immutable
public final class PDServerConfiguration extends AbstractGlobalSingleton
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PDServerConfiguration.class);
  private static final ConfigFile s_aConfigFile;

  static
  {
    final ConfigFileBuilder aCFB = new ConfigFileBuilder ().addPathFromSystemProperty ("peppol.directory.server.properties.path")
                                                           .addPathFromSystemProperty ("directory.server.properties.path")
                                                           .addPath ("private-pd.properties")
                                                           .addPath ("pd.properties");

    s_aConfigFile = aCFB.build ();
    if (s_aConfigFile.isRead ())
      s_aLogger.info ("Read PEPPOL Directory server properties from " + s_aConfigFile.getReadResource ().getPath ());
    else
      s_aLogger.warn ("Failed to read PEPPOL Directory server properties from " + aCFB.getAllPaths ());
  }

  @Deprecated
  @UsedViaReflection
  private PDServerConfiguration ()
  {}

  /**
   * @return The global config file for the PEPPOL Directory server.
   */
  @Nonnull
  public static ConfigFile getConfigFile ()
  {
    return s_aConfigFile;
  }

  /**
   * @return The underlying settings object. Use it to query non-standard
   *         settings. Never <code>null</code>.
   */
  @Nonnull
  public static ISettings getSettingsObject ()
  {
    return s_aConfigFile.getSettings ();
  }

  /**
   * @return The global debug flag to be used in
   *         {@link com.helger.commons.debug.GlobalDebug}.
   */
  @Nullable
  public static String getGlobalDebug ()
  {
    return s_aConfigFile.getAsString ("global.debug");
  }

  /**
   * @return The global production flag to be used in
   *         {@link com.helger.commons.debug.GlobalDebug}.
   */
  @Nullable
  public static String getGlobalProduction ()
  {
    return s_aConfigFile.getAsString ("global.production");
  }

  /**
   * @return The data path where photon will store it's data.
   */
  @Nullable
  public static String getDataPath ()
  {
    return s_aConfigFile.getAsString ("webapp.datapath");
  }

  /**
   * @return <code>true</code> to perform a readability check on all files in
   *         the web application directory to check for invalid OS user/access
   *         rights.
   */
  public static boolean isCheckFileAccess ()
  {
    return s_aConfigFile.getAsBoolean ("webapp.checkfileaccess", true);
  }

  /**
   * @return <code>true</code> if this is a test version. Usually has only
   *         relevance on the UI for presentational purposes.
   */
  public static boolean isTestVersion ()
  {
    return s_aConfigFile.getAsBoolean ("webapp.testversion", GlobalDebug.isDebugMode ());
  }

  public static boolean isClientCertificateValidationActive ()
  {
    return s_aConfigFile.getAsBoolean ("indexer.clientcert.validation", true);
  }

  /**
   * @return The issuer of the expected client certificate of the issuer.
   */
  @Nullable
  public static String getClientCertIssuer ()
  {
    return s_aConfigFile.getAsString ("clientcert.issuer");
  }

  /**
   * @return The alternative (other) issuer of the expected client certificate
   *         of the issuer.
   */
  @Nullable
  public static String getClientCertIssuerAlternative ()
  {
    return s_aConfigFile.getAsString ("clientcert-alt.issuer");
  }

  @Nonnull
  public static String getTruststoreLocation ()
  {
    return s_aConfigFile.getAsString ("truststore.path", PeppolKeyStoreHelper.TRUSTSTORE_PRODUCTION_CLASSPATH);
  }

  @Nonnull
  public static String getTruststorePassword ()
  {
    return s_aConfigFile.getAsString ("truststore.password", PeppolKeyStoreHelper.TRUSTSTORE_PASSWORD);
  }

  @Nonnull
  public static String getTruststoreAlias ()
  {
    return s_aConfigFile.getAsString ("truststore.alias", PeppolKeyStoreHelper.TRUSTSTORE_PRODUCTION_ALIAS_SMP);
  }

  @Nullable
  public static String getTruststoreLocationAlternative ()
  {
    return s_aConfigFile.getAsString ("truststore-alt.path", PeppolKeyStoreHelper.TRUSTSTORE_PILOT_CLASSPATH);
  }

  @Nullable
  public static String getTruststorePasswordAlternative ()
  {
    return s_aConfigFile.getAsString ("truststore-alt.password", PeppolKeyStoreHelper.TRUSTSTORE_PASSWORD);
  }

  @Nullable
  public static String getTruststoreAliasAlternative ()
  {
    return s_aConfigFile.getAsString ("truststore-alt.alias", PeppolKeyStoreHelper.TRUSTSTORE_PILOT_ALIAS_SMP);
  }

  /**
   * @return The maximum number of hours a retry will happen. If not provided 24
   *         hours is the default value.
   */
  @Nonnegative
  public static int getReIndexMaxRetryHours ()
  {
    final int ret = s_aConfigFile.getAsInt ("reindex.maxretryhours", 24);
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
    final int ret = s_aConfigFile.getAsInt ("reindex.retryminutes", 5);
    if (ret <= 0)
      throw new IllegalStateException ("The reindex.retryminutes property must be > 0!");
    return ret;
  }

  @Nullable
  public static String getProxyHost ()
  {
    return s_aConfigFile.getAsString ("http.proxyHost");
  }

  public static int getProxyPort ()
  {
    return s_aConfigFile.getAsInt ("http.proxyPort", 0);
  }
}
