/*
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
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
package com.helger.pd.client;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.io.resourceprovider.ReadableResourceProviderChain;
import com.helger.commons.string.StringHelper;
import com.helger.commons.system.SystemProperties;
import com.helger.config.Config;
import com.helger.config.ConfigFactory;
import com.helger.config.IConfig;
import com.helger.config.source.MultiConfigurationValueProvider;
import com.helger.config.source.res.ConfigurationSourceProperties;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.KeyStoreHelper;
import com.helger.security.keystore.LoadedKey;
import com.helger.security.keystore.LoadedKeyStore;

/**
 * This class manages the configuration properties of the Peppol Directory
 * client using the global {@link IConfig} interface.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class PDClientConfiguration
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDClientConfiguration.class);

  static
  {
    // Since 0.9.0
    if (StringHelper.hasText (SystemProperties.getPropertyValueOrNull ("peppol.pd.client.properties.path")))
      throw new InitializationException ("The system property 'peppol.pd.client.properties.path' is no longer supported." +
                                         " See https://github.com/phax/ph-commons#ph-config for alternatives." +
                                         " Consider using the system property 'config.file' instead.");
    if (StringHelper.hasText (SystemProperties.getPropertyValueOrNull ("pd.client.properties.path")))
      throw new InitializationException ("The system property 'pd.client.properties.path' is no longer supported." +
                                         " See https://github.com/phax/ph-commons#ph-config for alternatives." +
                                         " Consider using the system property 'config.file' instead.");
    if (StringHelper.hasText (System.getenv ().get ("DIRECTORY_CLIENT_CONFIG")))
      throw new InitializationException ("The environment variable 'DIRECTORY_CLIENT_CONFIG' is no longer supported." +
                                         " See https://github.com/phax/ph-commons#ph-config for alternatives." +
                                         " Consider using the environment variable 'CONFIG_FILE' instead.");
  }

  /**
   * @return The configuration value provider for phase4 that contains backward
   *         compatibility support.
   */
  @Nonnull
  public static MultiConfigurationValueProvider createSMPClientValueProvider ()
  {
    // Start with default setup
    final MultiConfigurationValueProvider ret = ConfigFactory.createDefaultValueProvider ();

    final ReadableResourceProviderChain aResourceProvider = ConfigFactory.createDefaultResourceProviderChain ();

    IReadableResource aRes;
    final int nBasePrio = ConfigFactory.APPLICATION_PROPERTIES_PRIORITY;

    // Lower priority than the standard files
    aRes = aResourceProvider.getReadableResourceIf ("private-pd-client.properties", IReadableResource::exists);
    if (aRes != null)
    {
      LOGGER.warn ("The support for the properties file 'private-pd-client.properties' is deprecated. Place the properties in 'application.properties' instead.");
      ret.addConfigurationSource (new ConfigurationSourceProperties (aRes, StandardCharsets.UTF_8), nBasePrio - 1);
    }

    aRes = aResourceProvider.getReadableResourceIf ("pd-client.properties", IReadableResource::exists);
    if (aRes != null)
    {
      LOGGER.warn ("The support for the properties file 'pd-client.properties' is deprecated. Place the properties in 'application.properties' instead.");
      ret.addConfigurationSource (new ConfigurationSourceProperties (aRes, StandardCharsets.UTF_8), nBasePrio - 2);
    }

    return ret;
  }

  public static final EKeyStoreType DEFAULT_TRUSTSTORE_TYPE = EKeyStoreType.JKS;
  public static final int DEFAULT_CONNECTION_TIMEOUT_MS = 5_000;
  public static final int DEFAULT_REQUEST_TIMEOUT_MS = 10_000;

  private static final IConfig CONFIG = Config.create (createSMPClientValueProvider ());

  /**
   * Reload the Directory configuration from the source again.
   *
   * @since 0.8.1
   */
  public static void reloadConfiguration ()
  {
    if (CONFIG.reloadAllResourceBasedConfigurationValues ().isSuccess ())
      LOGGER.info ("Successfully re-read the resource based configuration sources");
    else
      LOGGER.warn ("Failed to reload at least one of the resource based configuration sources");
  }

  private PDClientConfiguration ()
  {}

  /**
   * @return The global config file for the SMP client.
   */
  @Nonnull
  public static IConfig getConfig ()
  {
    return CONFIG;
  }

  /**
   * @return The type to the keystore. This is usually JKS. Property
   *         <code>keystore.type</code>.
   */
  @Nonnull
  public static EKeyStoreType getKeyStoreType ()
  {
    final String sType = getConfig ().getAsString ("keystore.type");
    return EKeyStoreType.getFromIDCaseInsensitiveOrDefault (sType, EKeyStoreType.JKS);
  }

  /**
   * @return The key store location as specified in the configuration file by
   *         the property <code>keystore.path</code>.
   */
  @Nullable
  public static String getKeyStorePath ()
  {
    return getConfig ().getAsString ("keystore.path");
  }

  /**
   * @return The keystore password as specified in the configuration file by the
   *         property <code>keystore.password</code>.
   */
  @Nullable
  public static String getKeyStorePassword ()
  {
    return getConfig ().getAsString ("keystore.password");
  }

  /**
   * @return The loaded key store and never <code>null</code>.
   */
  @Nonnull
  public static LoadedKeyStore loadKeyStore ()
  {
    return KeyStoreHelper.loadKeyStore (getKeyStoreType (), getKeyStorePath (), getKeyStorePassword ());
  }

  /**
   * @return The private key alias as specified in the configuration file by the
   *         property <code>keystore.key.alias</code>.
   */
  @Nullable
  public static String getKeyStoreKeyAlias ()
  {
    return getConfig ().getAsString ("keystore.key.alias");
  }

  /**
   * @return The private key password as specified in the configuration file by
   *         the property <code>keystore.key.password</code>.
   */
  @Nullable
  public static char [] getKeyStoreKeyPassword ()
  {
    return getConfig ().getAsCharArray ("keystore.key.password");
  }

  /**
   * @param aKeyStore
   *        The key store to be used. May not be <code>null</code>.
   * @return The loaded key and never <code>null</code>.
   */
  @Nonnull
  public static LoadedKey <KeyStore.PrivateKeyEntry> loadPrivateKey (@Nonnull final KeyStore aKeyStore)
  {
    return KeyStoreHelper.loadPrivateKey (aKeyStore, getKeyStorePath (), getKeyStoreKeyAlias (), getKeyStoreKeyPassword ());
  }

  /**
   * @return The type to the truststore. This is usually JKS. Property
   *         <code>truststore.type</code>.
   * @since 0.6.0
   */
  @Nonnull
  public static EKeyStoreType getTrustStoreType ()
  {
    final String sType = getConfig ().getAsString ("truststore.type");
    return EKeyStoreType.getFromIDCaseInsensitiveOrDefault (sType, DEFAULT_TRUSTSTORE_TYPE);
  }

  /**
   * @return The trust store location as specified in the configuration file by
   *         the property <code>truststore.path</code>. Defaults to
   *         <code>null</code>
   * @since 0.5.1
   */
  @Nullable
  public static String getTrustStorePath ()
  {
    return getConfig ().getAsString ("truststore.path");
  }

  /**
   * @return The trust store password as specified in the configuration file by
   *         the property <code>truststore.password</code>. Defaults to
   *         <code>null</code>.
   * @since 0.5.1
   */
  @Nullable
  public static String getTrustStorePassword ()
  {
    return getConfig ().getAsString ("truststore.password");
  }

  /**
   * @return The loaded trust store and never <code>null</code>.
   */
  @Nonnull
  public static LoadedKeyStore loadTrustStore ()
  {
    return KeyStoreHelper.loadKeyStore (getTrustStoreType (), getTrustStorePath (), getTrustStorePassword ());
  }

  /**
   * During handshaking, if the URL's hostname and the server's identification
   * hostname mismatch, the verification mechanism can call back to implementers
   * of this interface to determine if this connection should be allowed.
   *
   * @return <code>true</code> if hostname checking is disabled (the default),
   *         or <code>false</code> if it is enabled.
   * @since 0.5.1
   */
  public static boolean isHttpsHostnameVerificationDisabled ()
  {
    return getConfig ().getAsBoolean ("https.hostname-verification.disabled", true);
  }

  /**
   * @return The proxy host to be used for "http" calls. May be
   *         <code>null</code>.
   * @see #getHttpsProxyHost()
   */
  @Nullable
  public static String getHttpProxyHost ()
  {
    return getConfig ().getAsString ("http.proxyHost");
  }

  /**
   * @return The proxy port to be used for "http" calls. Defaults to 0.
   * @see #getHttpsProxyPort()
   */
  public static int getHttpProxyPort ()
  {
    return getConfig ().getAsInt ("http.proxyPort", 0);
  }

  /**
   * @return The proxy host to be used for "https" calls. May be
   *         <code>null</code>.
   * @see #getHttpProxyHost()
   */
  @Nullable
  public static String getHttpsProxyHost ()
  {
    return getConfig ().getAsString ("https.proxyHost");
  }

  /**
   * @return The proxy port to be used for "https" calls. Defaults to 0.
   * @see #getHttpProxyPort()
   */
  public static int getHttpsProxyPort ()
  {
    return getConfig ().getAsInt ("https.proxyPort", 0);
  }

  /**
   * @return The username for proxy calls. Valid for https and https proxy. May
   *         be <code>null</code>.
   * @since 0.6.0
   */
  @Nullable
  public static String getProxyUsername ()
  {
    return getConfig ().getAsString ("proxy.username");
  }

  /**
   * @return The password for proxy calls. Valid for https and https proxy. May
   *         be <code>null</code>.
   * @since 0.6.0
   */
  @Nullable
  public static String getProxyPassword ()
  {
    return getConfig ().getAsString ("proxy.password");
  }

  /**
   * @return Connection timeout in milliseconds. Defaults to 5000 (=5 seconds).
   *         0 means "indefinite", -1 means "system default".
   * @since 0.6.0
   */
  public static int getConnectTimeoutMS ()
  {
    return getConfig ().getAsInt ("connect.timeout.ms", DEFAULT_CONNECTION_TIMEOUT_MS);
  }

  /**
   * @return Request/read/socket timeout in milliseconds. Defaults to 10000 (=10
   *         seconds). 0 means "indefinite", -1 means "system default".
   * @since 0.6.0
   */
  public static int getRequestTimeoutMS ()
  {
    return getConfig ().getAsInt ("request.timeout.ms", DEFAULT_REQUEST_TIMEOUT_MS);
  }
}
