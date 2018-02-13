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
package com.helger.pd.settings;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.utils.PeppolKeyStoreHelper;
import com.helger.peppol.utils.PeppolKeyStoreHelper.Config2010;
import com.helger.peppol.utils.PeppolKeyStoreHelper.Config2018;
import com.helger.scope.singleton.AbstractGlobalSingleton;
import com.helger.security.keystore.EKeyStoreType;
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
   * @return The global config file for the PEPPOL Directory server. Never
   *         <code>null</code>.
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
   * Read value of <code>global.debug</code>
   *
   * @return The global debug flag to be used in
   *         {@link com.helger.commons.debug.GlobalDebug}.
   */
  @Nullable
  public static String getGlobalDebug ()
  {
    return s_aConfigFile.getAsString ("global.debug");
  }

  /**
   * Read value of <code>global.production</code>
   *
   * @return The global production flag to be used in
   *         {@link com.helger.commons.debug.GlobalDebug}.
   */
  @Nullable
  public static String getGlobalProduction ()
  {
    return s_aConfigFile.getAsString ("global.production");
  }

  /**
   * Read value of <code>webapp.datapath</code>
   *
   * @return The data path where the server will store it's data.
   */
  @Nullable
  public static String getDataPath ()
  {
    return s_aConfigFile.getAsString ("webapp.datapath");
  }

  /**
   * Read value of <code>webapp.checkfileaccess</code>. Defaults to
   * <code>true</code>.
   *
   * @return <code>true</code> to perform a readability check on all files in
   *         the web application directory to check for invalid OS user/access
   *         rights.
   */
  public static boolean isCheckFileAccess ()
  {
    return s_aConfigFile.getAsBoolean ("webapp.checkfileaccess", false);
  }

  /**
   * Read value of <code>webapp.testversion</code>. Defaults to
   * <code>GlobalDebug.isDebugMode ()</code>.
   *
   * @return <code>true</code> if this is a test version. Usually has only
   *         relevance on the UI for presentational purposes.
   */
  public static boolean isTestVersion ()
  {
    return s_aConfigFile.getAsBoolean ("webapp.testversion", GlobalDebug.isDebugMode ());
  }

  /**
   * @return <code>true</code> if all paths should be forced to the ROOT ("/")
   *         context, <code>false</code> if the context should remain as it is.
   *         Property <code>webapp.forceroot</code>.
   */
  public static boolean isForceRoot ()
  {
    return s_aConfigFile.getAsBoolean ("webapp.forceroot", false);
  }

  /**
   * Read value of <code>indexer.clientcert.validation</code>. Defaults to
   * <code>true</code>.
   *
   * @return <code>true</code> if client certificate validation is enabled (only
   *         suitable if https is used), <code>false</code> otherwise.
   */
  public static boolean isClientCertificateValidationActive ()
  {
    return s_aConfigFile.getAsBoolean ("indexer.clientcert.validation", true);
  }

  /**
   * Read value of <code>clientcert.issuer.x</code> values, where "x" is an
   * ascending number starting from 1.
   *
   * @return The list of potential issuers of the expected client certificates.
   *         Never <code>null</code> but maybe empty.
   */
  @Nonnull
  public static ICommonsList <String> getAllClientCertIssuer ()
  {
    final ICommonsList <String> ret = new CommonsArrayList <> ();

    int nIndex = 1;
    while (true)
    {
      final String sValue = s_aConfigFile.getAsString ("clientcert.issuer." + nIndex);
      if (StringHelper.hasNoText (sValue))
        break;

      // Present - try next
      ret.add (sValue);
      ++nIndex;
    }
    return ret;
  }

  /**
   * @return The type to the truststore. This is usually JKS. Property
   *         <code>truststore.type</code>.
   * @since 0.6.0
   */
  @Nonnull
  public static EKeyStoreType getTrustStoreType ()
  {
    final String sType = s_aConfigFile.getAsString ("truststore.type");
    return EKeyStoreType.getFromIDCaseInsensitiveOrDefault (sType, PeppolKeyStoreHelper.TRUSTSTORE_TYPE);
  }

  /**
   * Read value of <code>truststore.path</code>. Defaults to
   * <code>{@link PeppolKeyStoreHelper#TRUSTSTORE_COMPLETE_CLASSPATH}</code>.
   *
   * @return The truststore location path.
   */
  @Nonnull
  public static String getTrustStorePath ()
  {
    return s_aConfigFile.getAsString ("truststore.path", PeppolKeyStoreHelper.TRUSTSTORE_COMPLETE_CLASSPATH);
  }

  /**
   * Read value of <code>truststore.password</code>. Defaults to
   * <code>{@link PeppolKeyStoreHelper#TRUSTSTORE_PASSWORD}</code>.
   *
   * @return The truststore password.
   */
  @Nonnull
  public static String getTrustStorePassword ()
  {
    return s_aConfigFile.getAsString ("truststore.password", PeppolKeyStoreHelper.TRUSTSTORE_PASSWORD);
  }

  /**
   * Read value of <code>truststore.alias</code>. Defaults to
   * <code>{@link Config2010#TRUSTSTORE_PRODUCTION_ALIAS_SMP}</code>.
   *
   * @return The truststore password.
   */
  @Nonnull
  public static String getTrustStoreAlias ()
  {
    return s_aConfigFile.getAsString ("truststore.alias",
                                      PeppolKeyStoreHelper.Config2010.TRUSTSTORE_PRODUCTION_ALIAS_SMP);
  }

  /**
   * @return The type to the truststore. This is usually JKS. Property
   *         <code>truststore-alt.type</code>.
   * @since 0.6.0
   */
  @Nonnull
  public static EKeyStoreType getTrustStoreTypeAlternative ()
  {
    final String sType = s_aConfigFile.getAsString ("truststore-alt.type");
    return EKeyStoreType.getFromIDCaseInsensitiveOrDefault (sType, PeppolKeyStoreHelper.TRUSTSTORE_TYPE);
  }

  /**
   * Read value of <code>truststore-alt.path</code>. Defaults to
   * <code>{@link PeppolKeyStoreHelper#TRUSTSTORE_COMPLETE_CLASSPATH}</code>.
   *
   * @return The alternative truststore location path.
   */
  @Nullable
  public static String getTrustStorePathAlternative ()
  {
    return s_aConfigFile.getAsString ("truststore-alt.path", PeppolKeyStoreHelper.TRUSTSTORE_COMPLETE_CLASSPATH);
  }

  /**
   * Read value of <code>truststore-alt.password</code>. Defaults to
   * <code>{@link PeppolKeyStoreHelper#TRUSTSTORE_PASSWORD}</code>.
   *
   * @return The alternative truststore password.
   */
  @Nullable
  public static String getTrustStorePasswordAlternative ()
  {
    return s_aConfigFile.getAsString ("truststore-alt.password", PeppolKeyStoreHelper.TRUSTSTORE_PASSWORD);
  }

  /**
   * Read value of <code>truststore-alt.alias</code>. Defaults to
   * <code>{@link Config2018#TRUSTSTORE_PRODUCTION_ALIAS_SMP}</code>.
   *
   * @return The alternative truststore password.
   */
  @Nullable
  public static String getTrustStoreAliasAlternative ()
  {
    return s_aConfigFile.getAsString ("truststore-alt.alias",
                                      PeppolKeyStoreHelper.Config2018.TRUSTSTORE_PRODUCTION_ALIAS_SMP);
  }

  /**
   * Read value of <code>reindex.maxretryhours</code>. Defaults to
   * <code>24</code>.
   *
   * @return The maximum number of hours a retry will happen. Always &ge; 0.
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
   * Read value of <code>reindex.retryminutes</code>. Defaults to
   * <code>5</code>.
   *
   * @return The number of minutes between retries. Always &ge; 0.
   */
  @Nonnegative
  public static int getReIndexRetryMinutes ()
  {
    final int ret = s_aConfigFile.getAsInt ("reindex.retryminutes", 5);
    if (ret <= 0)
      throw new IllegalStateException ("The reindex.retryminutes property must be > 0!");
    return ret;
  }

  /**
   * Read value of <code>http.proxyHost</code>.
   *
   * @return The optional proxy host to use. May be an IP address or a host name
   *         WITHOUT a port number or a user.
   */
  @Nullable
  public static String getProxyHost ()
  {
    return s_aConfigFile.getAsString ("http.proxyHost");
  }

  /**
   * Read value of <code>http.proxyPort</code>. Defaults to <code>0</code>.
   *
   * @return The proxy port to use. Only relevant is {@link #getProxyHost()} is
   *         present.
   */
  public static int getProxyPort ()
  {
    return s_aConfigFile.getAsInt ("http.proxyPort", 0);
  }

  /**
   * Read value of <code>http.proxyUsername</code>.
   *
   * @return The optional proxy username to use. Maybe <code>null</code>.
   */
  @Nullable
  public static String getProxyUsername ()
  {
    return s_aConfigFile.getAsString ("http.proxyUsername");
  }

  /**
   * Read value of <code>http.proxyPassword</code>.
   *
   * @return The optional proxy password to use. Maybe <code>null</code>.
   */
  @Nullable
  public static String getProxyPassword ()
  {
    return s_aConfigFile.getAsString ("http.proxyPassword");
  }

  @Nullable
  public static ISMLInfo getSMLToUse ()
  {
    final String sSMLID = s_aConfigFile.getAsString ("sml.id");
    final ESML eSML = ESML.getFromIDOrNull (sSMLID);
    if (eSML == null && sSMLID != null)
      s_aLogger.warn ("The provided SML-ID '" +
                      sSMLID +
                      "' is invalid. Valid values are: " +
                      StringHelper.getImplodedMapped (", ", ESML.values (), ESML::getID));
    return eSML;
  }
}
