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

import java.net.URI;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.URLHelper;
import com.helger.peppol.url.EsensURLProvider;
import com.helger.peppol.url.IPeppolURLProvider;
import com.helger.peppol.url.PeppolURLProvider;
import com.helger.peppol.utils.PeppolKeyStoreHelper;
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
  private static final Logger LOGGER = LoggerFactory.getLogger (PDServerConfiguration.class);
  private static final ConfigFile s_aConfigFile;

  static
  {
    final ConfigFileBuilder aCFB = new ConfigFileBuilder ().addPathFromSystemProperty ("peppol.directory.server.properties.path")
                                                           .addPathFromSystemProperty ("directory.server.properties.path")
                                                           .addPath ("private-pd.properties")
                                                           .addPath ("pd.properties");

    s_aConfigFile = aCFB.build ();
    if (s_aConfigFile.isRead ())
      LOGGER.info ("Read PEPPOL Directory server properties from " + s_aConfigFile.getReadResource ().getPath ());
    else
      LOGGER.warn ("Failed to read PEPPOL Directory server properties from " + aCFB.getAllPaths ());
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
    return getConfigFile ().getSettings ();
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
    return getConfigFile ().getAsString ("global.debug");
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
    return getConfigFile ().getAsString ("global.production");
  }

  /**
   * Read value of <code>webapp.datapath</code>
   *
   * @return The data path where the server will store it's data.
   */
  @Nullable
  public static String getDataPath ()
  {
    return getConfigFile ().getAsString ("webapp.datapath");
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
    return getConfigFile ().getAsBoolean ("webapp.checkfileaccess", false);
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
    return getConfigFile ().getAsBoolean ("webapp.testversion", GlobalDebug.isDebugMode ());
  }

  /**
   * @return <code>true</code> if all paths should be forced to the ROOT ("/")
   *         context, <code>false</code> if the context should remain as it is.
   *         Property <code>webapp.forceroot</code>.
   */
  public static boolean isForceRoot ()
  {
    return getConfigFile ().getAsBoolean ("webapp.forceroot", false);
  }

  @Nonnull
  @Nonempty
  public static String getAppName ()
  {
    return getConfigFile ().getAsString ("webapp.appname", "PEPPOL Directory");
  }

  @Nullable
  public static String getVendorName ()
  {
    return getConfigFile ().getAsString ("webapp.vendor.name", "OpenPEPPOL AISBL");
  }

  @Nullable
  public static String getVendorURL ()
  {
    return getConfigFile ().getAsString ("webapp.vendor.url", "http://peppol.eu");
  }

  @Nullable
  public static String getLogoImageURL ()
  {
    return getConfigFile ().getAsString ("webapp.logo.image.path");
  }

  @Nullable
  public static String getSearchUIMode ()
  {
    return getConfigFile ().getAsString ("webapp.search.ui");
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
    return getConfigFile ().getAsBoolean ("indexer.clientcert.validation", true);
  }

  /**
   * Read value of <code>clientcert.issuer.X</code> values, where "X" is an
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
      final String sValue = getConfigFile ().getAsString ("clientcert.issuer." + nIndex);
      if (StringHelper.hasNoText (sValue))
        break;

      // Present - try next
      ret.add (sValue);
      ++nIndex;
    }
    return ret;
  }

  /**
   * @return A list of trust stores configured. Property names are
   *         <code>truststore.X.type</code>, <code>truststore.X.path</code>,
   *         <code>truststore.X.password</code>,
   *         <code>truststore.X.alias</code>, where "X" is an ascending number
   *         starting from 1.
   * @since 0.6.0
   */
  @Nonnull
  public static ICommonsList <PDConfiguredTrustStore> getAllTrustStores ()
  {
    final ICommonsList <PDConfiguredTrustStore> ret = new CommonsArrayList <> ();

    int nIndex = 1;
    while (true)
    {
      final String sPrefix = "truststore." + nIndex;

      final String sType = getConfigFile ().getAsString (sPrefix + ".type");
      final EKeyStoreType eType = EKeyStoreType.getFromIDCaseInsensitiveOrDefault (sType,
                                                                                   PeppolKeyStoreHelper.TRUSTSTORE_TYPE);
      final String sPath = getConfigFile ().getAsString (sPrefix + ".path");
      final String sPassword = getConfigFile ().getAsString (sPrefix + ".password");
      final String sAlias = getConfigFile ().getAsString (sPrefix + ".alias");

      if (StringHelper.hasNoText (sPath) || StringHelper.hasNoText (sPassword) || StringHelper.hasNoText (sAlias))
        break;

      // Present - try next
      ret.add (new PDConfiguredTrustStore (eType, sPath, sPassword, sAlias));
      ++nIndex;
    }
    return ret;
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
    final int ret = getConfigFile ().getAsInt ("reindex.maxretryhours", 24);
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
    final int ret = getConfigFile ().getAsInt ("reindex.retryminutes", 5);
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
    return getConfigFile ().getAsString ("http.proxyHost");
  }

  /**
   * Read value of <code>http.proxyPort</code>. Defaults to <code>0</code>.
   *
   * @return The proxy port to use. Only relevant is {@link #getProxyHost()} is
   *         present.
   */
  public static int getProxyPort ()
  {
    return getConfigFile ().getAsInt ("http.proxyPort", 0);
  }

  /**
   * Read value of <code>http.proxyUsername</code>.
   *
   * @return The optional proxy username to use. Maybe <code>null</code>.
   */
  @Nullable
  public static String getProxyUsername ()
  {
    return getConfigFile ().getAsString ("http.proxyUsername");
  }

  /**
   * Read value of <code>http.proxyPassword</code>.
   *
   * @return The optional proxy password to use. Maybe <code>null</code>.
   */
  @Nullable
  public static String getProxyPassword ()
  {
    return getConfigFile ().getAsString ("http.proxyPassword");
  }

  /**
   * @return The fixed SMP URI to be used to retrieve business cards. This
   *         document should only be used when setting up a new network and
   *         SML/DNS are not (yet) available in the system. Because it is
   *         special, it is not documented.
   */
  @Nullable
  public static URI getFixedSMPURI ()
  {
    final String sSMPURI = getConfigFile ().getAsString ("smp.uri");
    return URLHelper.getAsURI (sSMPURI);
  }

  /**
   * @return The URL provider to be used, if an SML is used. If a direct SMP is
   *         configured, this does not matter.
   */
  @Nonnull
  public static IPeppolURLProvider getURLProvider ()
  {
    final String sSMLURLProvider = getConfigFile ().getAsString ("sml.urlprovider");
    if ("esens".equalsIgnoreCase (sSMLURLProvider))
      return EsensURLProvider.INSTANCE;

    // Default is PEPPOL
    return PeppolURLProvider.INSTANCE;
  }

  @Nonnull
  public static EPDSMPMode getSMPMode ()
  {
    final String sSMPMode = getConfigFile ().getAsString ("smp.mode");
    if ("oasis-bdxr-v1".equalsIgnoreCase (sSMPMode))
      return EPDSMPMode.OASIS_BDXR_v1;

    // Default is PEPPOL
    return EPDSMPMode.PEPPOL;
  }
}
