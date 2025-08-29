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
package com.helger.pd.client;

import java.security.KeyStore;

import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.equals.EqualsHelper;
import com.helger.config.ConfigFactory;
import com.helger.config.IConfig;
import com.helger.config.fallback.ConfigWithFallback;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.httpclient.HttpClientSettings;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.KeyStoreHelper;
import com.helger.security.keystore.LoadedKey;
import com.helger.security.keystore.LoadedKeyStore;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * This class manages the configuration properties of the Peppol Directory client using the global
 * {@link IConfig} interface.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class PDClientConfiguration
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDClientConfiguration.class);

  public static final EKeyStoreType DEFAULT_TRUSTSTORE_TYPE = EKeyStoreType.JKS;

  private static final IConfigWithFallback DEFAULT_CONFIG = new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ());
  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();
  @GuardedBy ("RW_LOCK")
  private static IConfigWithFallback s_aConfig = DEFAULT_CONFIG;

  private PDClientConfiguration ()
  {}

  /**
   * @return The current global configuration. Never <code>null</code>.
   */
  @Nonnull
  public static IConfigWithFallback getConfig ()
  {
    // Inline for performance
    RW_LOCK.readLock ().lock ();
    try
    {
      return s_aConfig;
    }
    finally
    {
      RW_LOCK.readLock ().unlock ();
    }
  }

  /**
   * Overwrite the global configuration. This is only needed for testing.
   *
   * @param aNewConfig
   *        The configuration to use globally. May not be <code>null</code>.
   * @return The old value of {@link IConfig}. Never <code>null</code>.
   */
  @Nonnull
  public static IConfigWithFallback setConfig (@Nonnull final IConfigWithFallback aNewConfig)
  {
    ValueEnforcer.notNull (aNewConfig, "NewConfig");
    final IConfigWithFallback ret;
    RW_LOCK.writeLock ().lock ();
    try
    {
      ret = s_aConfig;
      s_aConfig = aNewConfig;
    }
    finally
    {
      RW_LOCK.writeLock ().unlock ();
    }

    if (!EqualsHelper.identityEqual (ret, aNewConfig))
      LOGGER.info ("The PDClient configuration provider was changed to " + aNewConfig);
    return ret;
  }

  /**
   * Reload the Directory configuration from the source again.
   *
   * @since 0.8.1
   */
  public static void reloadConfiguration ()
  {
    if (getConfig ().reloadAllResourceBasedConfigurationValues ().isSuccess ())
      LOGGER.info ("Successfully re-read the resource based configuration sources");
    else
      LOGGER.warn ("Failed to reload at least one of the resource based configuration sources");
  }

  /**
   * @return The type to the keystore. This is usually JKS. Property <code>keystore.type</code>.
   */
  @Nonnull
  public static EKeyStoreType getKeyStoreType ()
  {
    final String sType = getConfig ().getAsString ("pdclient.keystore.type");
    return EKeyStoreType.getFromIDCaseInsensitiveOrDefault (sType, EKeyStoreType.JKS);
  }

  /**
   * @return The key store location as specified in the configuration file by the property
   *         <code>keystore.path</code>.
   */
  @Nullable
  public static String getKeyStorePath ()
  {
    return getConfig ().getAsString ("pdclient.keystore.path");
  }

  /**
   * @return The keystore password as specified in the configuration file by the property
   *         <code>keystore.password</code>.
   */
  @Nullable
  public static char [] getKeyStorePassword ()
  {
    return getConfig ().getAsCharArray ("pdclient.keystore.password");
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
   * @return The private key alias as specified in the configuration file by the property
   *         <code>keystore.key.alias</code>.
   */
  @Nullable
  public static String getKeyStoreKeyAlias ()
  {
    return getConfig ().getAsString ("pdclient.keystore.key.alias");
  }

  /**
   * @return The private key password as specified in the configuration file by the property
   *         <code>keystore.key.password</code>.
   */
  @Nullable
  public static char [] getKeyStoreKeyPassword ()
  {
    final String ret = getConfig ().getAsString ("pdclient.keystore.key.password");
    return ret == null ? null : ret.toCharArray ();
  }

  /**
   * @param aKeyStore
   *        The key store to be used. May not be <code>null</code>.
   * @return The loaded key and never <code>null</code>.
   */
  @Nonnull
  public static LoadedKey <KeyStore.PrivateKeyEntry> loadPrivateKey (@Nonnull final KeyStore aKeyStore)
  {
    return KeyStoreHelper.loadPrivateKey (aKeyStore,
                                          getKeyStorePath (),
                                          getKeyStoreKeyAlias (),
                                          getKeyStoreKeyPassword ());
  }

  /**
   * @return The type to the truststore. This is usually JKS. Property <code>truststore.type</code>.
   * @since 0.6.0
   */
  @Nonnull
  public static EKeyStoreType getTrustStoreType ()
  {
    final String sType = getConfig ().getAsString ("pdclient.truststore.type");
    return EKeyStoreType.getFromIDCaseInsensitiveOrDefault (sType, DEFAULT_TRUSTSTORE_TYPE);
  }

  /**
   * @return The trust store location as specified in the configuration file by the property
   *         <code>truststore.path</code>. Defaults to <code>null</code>
   * @since 0.5.1
   */
  @Nullable
  public static String getTrustStorePath ()
  {
    return getConfig ().getAsString ("pdclient.truststore.path");
  }

  /**
   * @return The trust store password as specified in the configuration file by the property
   *         <code>truststore.password</code>. Defaults to <code>null</code>.
   * @since 0.5.1
   */
  @Nullable
  public static char [] getTrustStorePassword ()
  {
    return getConfig ().getAsCharArray ("pdclient.truststore.password");
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
   * @return The proxy host to be used for "http" calls. May be <code>null</code>.
   */
  @Nullable
  public static String getHttpProxyHost ()
  {
    return getConfig ().getAsStringOrFallback ("http.proxy.host", "http.proxyHost");
  }

  /**
   * @return The proxy port to be used for "http" calls. Defaults to 0.
   */
  public static int getHttpProxyPort ()
  {
    return getConfig ().getAsIntOrFallback ("http.proxy.port", -1, 0, "http.proxyPort");
  }

  /**
   * @return The username for proxy calls. Valid for https and https proxy. May be
   *         <code>null</code>.
   * @since 0.6.0
   */
  @Nullable
  public static String getProxyUsername ()
  {
    return getConfig ().getAsStringOrFallback ("http.proxy.username", "proxy.username");
  }

  /**
   * @return The password for proxy calls. Valid for https and https proxy. May be
   *         <code>null</code>.
   * @since 0.6.0
   */
  @Nullable
  public static char [] getProxyPassword ()
  {
    final String ret = getConfig ().getAsStringOrFallback ("http.proxy.password", "proxy.password");
    return ret == null ? null : ret.toCharArray ();
  }

  /**
   * @return Connection timeout in milliseconds. Defaults to 5000 (=5 seconds). 0 means
   *         "indefinite", -1 means "system default".
   * @since 0.6.0
   */
  @Nonnull
  public static Timeout getConnectTimeout ()
  {
    final long nMillis = getConfig ().getAsLongOrFallback ("http.connect.timeout.ms", -1, -1, "connect.timeout.ms");
    if (nMillis >= 0)
      return Timeout.ofMilliseconds (nMillis);
    return HttpClientSettings.DEFAULT_CONNECT_TIMEOUT;
  }

  /**
   * @return Request/read/socket timeout in milliseconds. Defaults to 10000 (=10 seconds). 0 means
   *         "indefinite", -1 means "system default".
   * @since 0.6.0
   */
  @Nonnull
  public static Timeout getResponseTimeout ()
  {
    final long nMillis = getConfig ().getAsLongOrFallback ("http.response.timeout.ms",
                                                           -1,
                                                           -1,
                                                           "http.request.timeout.ms",
                                                           "request.timeout.ms");
    if (nMillis >= 0)
      return Timeout.ofMilliseconds (nMillis);
    return HttpClientSettings.DEFAULT_RESPONSE_TIMEOUT;
  }

  /**
   * During handshaking, if the URL's hostname and the server's identification hostname mismatch,
   * the verification mechanism can call back to implementers of this interface to determine if this
   * connection should be allowed.
   *
   * @return <code>true</code> if hostname checking is disabled (the default), or <code>false</code>
   *         if it is enabled.
   * @since 0.5.1
   */
  public static boolean isHttpsHostnameVerificationDisabled ()
  {
    return getConfig ().getAsBoolean ("https.hostname-verification.disabled", true);
  }
}
