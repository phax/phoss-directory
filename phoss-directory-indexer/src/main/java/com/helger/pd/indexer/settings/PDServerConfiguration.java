/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.settings;

import java.net.URI;
import java.net.URL;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.CheckForSigned;
import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.UsedViaReflection;
import com.helger.base.debug.GlobalDebug;
import com.helger.base.exception.InitializationException;
import com.helger.base.string.StringHelper;
import com.helger.base.system.SystemProperties;
import com.helger.base.url.URLHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.config.Config;
import com.helger.config.ConfigFactory;
import com.helger.config.IConfig;
import com.helger.config.source.MultiConfigurationValueProvider;
import com.helger.peppol.sml.ESMPAPIType;
import com.helger.peppolid.factory.BDXR1IdentifierFactory;
import com.helger.peppolid.factory.BDXR2IdentifierFactory;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.factory.SimpleIdentifierFactory;
import com.helger.scope.singleton.AbstractGlobalSingleton;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.smpclient.url.BDXLURLProvider;
import com.helger.smpclient.url.ISMPURLProvider;
import com.helger.smpclient.url.PeppolNaptrURLProvider;

import jakarta.annotation.Nullable;

/**
 * This class manages the configuration properties of the Peppol Directory Server. The order of the
 * properties file resolving is as follows:
 * <ol>
 * <li>Check for the value of the system property
 * <code>peppol.directory.server.properties.path</code></li>
 * <li>Check for the value of the system property <code>directory.server.properties.path</code></li>
 * <li>The filename <code>private-pd.properties</code> in the root of the classpath</li>
 * <li>The filename <code>pd.properties</code> in the root of the classpath</li>
 * </ol>
 *
 * @author Philip Helger
 */
@Immutable
public final class PDServerConfiguration extends AbstractGlobalSingleton
{
  static
  {
    // Since 0.9.0
    if (StringHelper.isNotEmpty (SystemProperties.getPropertyValueOrNull ("peppol.directory.server.properties.path")))
      throw new InitializationException ("The system property 'peppol.directory.server.properties.path' is no longer supported." +
                                         " See https://github.com/phax/ph-commons#ph-config for alternatives." +
                                         " Consider using the system property 'config.file' instead.");
    if (StringHelper.isNotEmpty (SystemProperties.getPropertyValueOrNull ("directory.server.properties.path")))
      throw new InitializationException ("The system property 'directory.server.properties.path' is no longer supported." +
                                         " See https://github.com/phax/ph-commons#ph-config for alternatives." +
                                         " Consider using the system property 'config.file' instead.");
    if (StringHelper.isNotEmpty (System.getenv ().get ("DIRECTORY_SERVER_CONFIG")))
      throw new InitializationException ("The environment variable 'DIRECTORY_SERVER_CONFIG' is no longer supported." +
                                         " See https://github.com/phax/ph-commons#ph-config for alternatives." +
                                         " Consider using the environment variable 'CONFIG_FILE' instead.");
  }

  /**
   * @return The configuration value provider for phase4 that contains backward compatibility
   *         support.
   */
  @NonNull
  public static MultiConfigurationValueProvider createConfigValueProvider ()
  {
    // Start with default setup
    final MultiConfigurationValueProvider ret = ConfigFactory.createDefaultValueProvider ();

    // Nothing atm

    return ret;
  }

  private static final IConfig DEFAULT_INSTANCE = Config.create (createConfigValueProvider ());

  @Deprecated (forRemoval = false)
  @UsedViaReflection
  private PDServerConfiguration ()
  {}

  /**
   * @return The global config file for the Peppol Directory server. Never <code>null</code>.
   */
  @NonNull
  public static IConfig getConfig ()
  {
    return DEFAULT_INSTANCE;
  }

  /**
   * Read value of <code>global.debug</code>
   *
   * @return The global debug flag to be used in {@link com.helger.base.debug.GlobalDebug}.
   */
  @Nullable
  public static String getGlobalDebug ()
  {
    return getConfig ().getAsString ("global.debug");
  }

  /**
   * Read value of <code>global.production</code>
   *
   * @return The global production flag to be used in {@link com.helger.base.debug.GlobalDebug}.
   */
  @Nullable
  public static String getGlobalProduction ()
  {
    return getConfig ().getAsString ("global.production");
  }

  /**
   * Read value of <code>webapp.datapath</code>
   *
   * @return The data path where the server will store it's data.
   */
  @Nullable
  public static String getDataPath ()
  {
    return getConfig ().getAsString ("webapp.datapath");
  }

  /**
   * Read value of <code>webapp.checkfileaccess</code>. Defaults to <code>true</code>.
   *
   * @return <code>true</code> to perform a readability check on all files in the web application
   *         directory to check for invalid OS user/access rights.
   */
  public static boolean isCheckFileAccess ()
  {
    return getConfig ().getAsBoolean ("webapp.checkfileaccess", false);
  }

  /**
   * Read value of <code>webapp.testversion</code>. Defaults to
   * <code>GlobalDebug.isDebugMode ()</code>.
   *
   * @return <code>true</code> if this is a test version. Usually has only relevance on the UI for
   *         presentational purposes.
   */
  public static boolean isTestVersion ()
  {
    return getConfig ().getAsBoolean ("webapp.testversion", GlobalDebug.isDebugMode ());
  }

  /**
   * @return <code>true</code> if all paths should be forced to the ROOT ("/") context,
   *         <code>false</code> if the context should remain as it is. Property
   *         <code>webapp.forceroot</code>.
   */
  public static boolean isForceRoot ()
  {
    return getConfig ().getAsBoolean ("webapp.forceroot", false);
  }

  @NonNull
  @Nonempty
  public static String getAppName ()
  {
    return getConfig ().getAsString ("webapp.appname", "Peppol Directory");
  }

  @Nullable
  public static String getAppLogoImagePath ()
  {
    // Will be ignored if not present
    return getConfig ().getAsString ("webapp.applogo.image.path");
  }

  @Nullable
  public static String getVendorName ()
  {
    return getConfig ().getAsString ("webapp.vendor.name", "OpenPeppol AISBL");
  }

  @Nullable
  public static String getVendorURL ()
  {
    return getConfig ().getAsString ("webapp.vendor.url", "https://peppol.org");
  }

  @Nullable
  public static String getLogoImageURL ()
  {
    return getConfig ().getAsString ("webapp.logo.image.path");
  }

  @Nullable
  public static String getSearchUIMode ()
  {
    return getConfig ().getAsString ("webapp.search.ui");
  }

  public static boolean isWebAppShowContactLink ()
  {
    // true for backwards compatibility
    return getConfig ().getAsBoolean ("webapp.contact.show", true);
  }

  @Nullable
  public static URL getWebAppContactExternalURL ()
  {
    final String sURL = getConfig ().getAsString ("webapp.contact.external.url");
    return StringHelper.isNotEmpty (sURL) ? URLHelper.getAsURL (sURL, false) : null;
  }

  @Nullable
  public static String getWebAppContactTitle (@Nullable final String sDefaultTitle)
  {
    return getConfig ().getAsString ("webapp.contact.title", sDefaultTitle);
  }

  @Nullable
  public static String getWebAppAPIAllowOrigin ()
  {
    return getConfig ().getAsString ("webapp.api.allow.origin");
  }

  /**
   * Read value of <code>indexer.clientcert.validation</code>. Defaults to <code>true</code>.
   *
   * @return <code>true</code> if client certificate validation is enabled (only suitable if https
   *         is used), <code>false</code> otherwise.
   */
  public static boolean isClientCertificateValidationActive ()
  {
    return getConfig ().getAsBoolean ("indexer.clientcert.validation", true);
  }

  /**
   * Read value of <code>clientcert.issuer.X</code> values, where "X" is an ascending number
   * starting from 1.
   *
   * @return The list of potential issuers of the expected client certificates. Never
   *         <code>null</code> but maybe empty.
   */
  @NonNull
  public static ICommonsList <String> getAllClientCertIssuer ()
  {
    final ICommonsList <String> ret = new CommonsArrayList <> ();

    int nIndex = 1;
    while (true)
    {
      final String sValue = getConfig ().getAsString ("clientcert.issuer." + nIndex);
      if (StringHelper.isEmpty (sValue))
        break;

      // Present - try next
      ret.add (sValue);
      ++nIndex;
    }
    return ret;
  }

  /**
   * @return A list of trust stores configured. Property names are <code>truststore.X.type</code>,
   *         <code>truststore.X.path</code>, <code>truststore.X.password</code>,
   *         <code>truststore.X.alias</code>, where "X" is an ascending number starting from 1.
   * @since 0.6.0
   */
  @NonNull
  public static ICommonsList <PDConfiguredTrustStore> getAllTrustStores ()
  {
    final ICommonsList <PDConfiguredTrustStore> ret = new CommonsArrayList <> ();

    int nIndex = 1;
    while (true)
    {
      final String sPrefix = "truststore." + nIndex;

      final String sType = getConfig ().getAsString (sPrefix + ".type");
      final EKeyStoreType eType = EKeyStoreType.getFromIDCaseInsensitiveOrNull (sType);
      final String sPath = getConfig ().getAsString (sPrefix + ".path");
      final String sPassword = getConfig ().getAsString (sPrefix + ".password");
      final String sAlias = getConfig ().getAsString (sPrefix + ".alias");

      if (eType == null ||
          StringHelper.isEmpty (sPath) ||
          StringHelper.isEmpty (sPassword) ||
          StringHelper.isEmpty (sAlias))
        break;

      // Present - try next
      ret.add (new PDConfiguredTrustStore (eType, sPath, sPassword, sAlias));
      ++nIndex;
    }
    return ret;
  }

  /**
   * Read value of <code>reindex.maxretryhours</code>. Defaults to <code>24</code>.
   *
   * @return The maximum number of hours a retry will happen. Always &ge; 0.
   */
  @Nonnegative
  public static int getReIndexMaxRetryHours ()
  {
    final int ret = getConfig ().getAsInt ("reindex.maxretryhours", 24);
    if (ret < 0)
      throw new IllegalStateException ("The reindex.maxretryhours property must be >= 0!");
    return ret;
  }

  /**
   * Read value of <code>reindex.retryminutes</code>. Defaults to <code>5</code>.
   *
   * @return The number of minutes between retries. Always &ge; 0.
   */
  @Nonnegative
  public static int getReIndexRetryMinutes ()
  {
    final int ret = getConfig ().getAsInt ("reindex.retryminutes", 5);
    if (ret <= 0)
      throw new IllegalStateException ("The reindex.retryminutes property must be > 0!");
    return ret;
  }

  /**
   * Read value of <code>http.proxyHost</code>.
   *
   * @return The optional proxy host to use. May be an IP address or a host name WITHOUT a port
   *         number or a user.
   */
  @Nullable
  public static String getProxyHost ()
  {
    return getConfig ().getAsString ("http.proxyHost");
  }

  /**
   * Read value of <code>http.proxyPort</code>. Defaults to <code>0</code>.
   *
   * @return The proxy port to use. Only relevant is {@link #getProxyHost()} is present.
   */
  public static int getProxyPort ()
  {
    return getConfig ().getAsInt ("http.proxyPort", 0);
  }

  /**
   * Read value of <code>http.proxyUsername</code>.
   *
   * @return The optional proxy username to use. Maybe <code>null</code>.
   */
  @Nullable
  public static String getProxyUsername ()
  {
    return getConfig ().getAsString ("http.proxyUsername");
  }

  /**
   * Read value of <code>http.proxyPassword</code>.
   *
   * @return The optional proxy password to use. Maybe <code>null</code>.
   */
  @Nullable
  public static char [] getProxyPassword ()
  {
    return getConfig ().getAsCharArray ("http.proxyPassword");
  }

  /**
   * @return The fixed SMP URI to be used to retrieve business cards. This document should only be
   *         used when setting up a new network and SML/DNS are not (yet) available in the system.
   *         Because it is special, it is not documented.
   */
  @Nullable
  public static URI getFixedSMPURI ()
  {
    final String sSMPURI = getConfig ().getAsString ("smp.uri");
    return URLHelper.getAsURI (sSMPURI);
  }

  /**
   * @return The URL provider to be used, if an SML is used. If a direct SMP is configured, this
   *         does not matter.
   */
  @NonNull
  public static ISMPURLProvider getURLProvider ()
  {
    final String sSMLURLProvider = getConfig ().getAsString ("sml.urlprovider");
    if ("esens".equalsIgnoreCase (sSMLURLProvider) || "bdxl".equalsIgnoreCase (sSMLURLProvider))
      return BDXLURLProvider.INSTANCE;

    // Default is Peppol
    return PeppolNaptrURLProvider.INSTANCE;
  }

  @NonNull
  public static ESMPAPIType getSMPMode ()
  {
    final String sSMPMode = getConfig ().getAsString ("smp.mode");
    if ("oasis-bdxr-v1".equalsIgnoreCase (sSMPMode))
      return ESMPAPIType.OASIS_BDXR_V1;
    if ("oasis-bdxr-v2".equalsIgnoreCase (sSMPMode))
      return ESMPAPIType.OASIS_BDXR_V2;

    // Default is Peppol
    return ESMPAPIType.PEPPOL;
  }

  public static boolean isSMPTLSTrustAll ()
  {
    return getConfig ().getAsBoolean ("smp.tls.trust-all", false);
  }

  @NonNull
  public static IIdentifierFactory getIdentifierFactory ()
  {
    final String sSMPMode = getConfig ().getAsString ("identifier.type");
    if ("oasis-bdxr-v1".equalsIgnoreCase (sSMPMode))
      return BDXR1IdentifierFactory.INSTANCE;
    if ("oasis-bdxr-v2".equalsIgnoreCase (sSMPMode))
      return BDXR2IdentifierFactory.INSTANCE;
    if ("simple".equalsIgnoreCase (sSMPMode))
      return SimpleIdentifierFactory.INSTANCE;

    // Default is Peppol
    return PeppolIdentifierFactory.INSTANCE;
  }

  @CheckForSigned
  public static long getRESTAPIMaxRequestsPerSecond ()
  {
    return getConfig ().getAsLong ("rest.limit.requestspersecond", -1);
  }

  public static boolean isSyncAllBusinessCards ()
  {
    return getConfig ().getAsBoolean ("sync.businesscards", false);
  }

  @Nullable
  public static String getS3BucketName ()
  {
    return getConfig ().getAsString ("aws.export.s3.bucket");
  }

  @Nullable
  public static String getS3WebsiteURLWithTrailingSlash ()
  {
    return getConfig ().getAsString ("aws.export.s3.publicurl");
  }
}
