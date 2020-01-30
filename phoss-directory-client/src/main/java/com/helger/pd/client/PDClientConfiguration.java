/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.settings.exchange.configfile.ConfigFile;
import com.helger.settings.exchange.configfile.ConfigFileBuilder;

/**
 * This class manages the configuration properties of the Peppol Directory
 * client. The order of the properties file resolving is as follows:
 * <ol>
 * <li>Check for the value of the system property
 * <code>peppol.pd.client.properties.path</code></li>
 * <li>Check for the value of the system property
 * <code>pd.client.properties.path</code></li>
 * <li>The filename <code>private-pd-client.properties</code> in the root of the
 * classpath</li>
 * <li>The filename <code>pd-client.properties</code> in the root of the
 * classpath</li>
 * </ol>
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class PDClientConfiguration
{
  public static final String SYSTEM_PROPERTY_PRIMARY = "peppol.pd.client.properties.path";
  public static final String SYSTEM_PROPERTY_SECONDARY = "pd.client.properties.path";
  public static final String PROPERTY_FILE_PRIMARY = "private-pd-client.properties";
  public static final String PROPERTY_FILE_SECONDARY = "pd-client.properties";

  public static final EKeyStoreType DEFAULT_TRUSTSTORE_TYPE = EKeyStoreType.JKS;
  public static final int DEFAULT_CONNECTION_TIMEOUT_MS = 5_000;
  public static final int DEFAULT_REQUEST_TIMEOUT_MS = 10_000;

  private static final Logger LOGGER = LoggerFactory.getLogger (PDClientConfiguration.class);

  private static final SimpleReadWriteLock s_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("s_aRWLock")
  private static ConfigFile s_aConfigFile;

  /**
   * Reload the Directory configuration from the source again.
   *
   * @since 0.8.1
   */
  public static void reloadConfiguration ()
  {
    final ConfigFileBuilder aCFB = new ConfigFileBuilder ().addPathFromEnvVar ("DIRECTORY_CLIENT_CONFIG")
                                                           .addPathFromSystemProperty (SYSTEM_PROPERTY_PRIMARY)
                                                           .addPathFromSystemProperty (SYSTEM_PROPERTY_SECONDARY)
                                                           .addPath (PROPERTY_FILE_PRIMARY)
                                                           .addPath (PROPERTY_FILE_SECONDARY);

    final ConfigFile aConfigFile = aCFB.build ();
    if (aConfigFile.isRead ())
      LOGGER.info ("Read Peppol Directory client properties from " + aConfigFile.getReadResource ().getPath ());
    else
      LOGGER.warn ("Failed to read Peppol Directory client properties from " + aCFB.getAllPaths ());

    // Remember globally
    s_aRWLock.writeLocked ( () -> s_aConfigFile = aConfigFile);
  }

  static
  {
    reloadConfiguration ();
  }

  private PDClientConfiguration ()
  {}

  /**
   * @return The global config file for the SMP client.
   */
  @Nonnull
  public static ConfigFile getConfigFile ()
  {
    s_aRWLock.readLock ().lock ();
    try
    {
      return s_aConfigFile;
    }
    finally
    {
      s_aRWLock.readLock ().unlock ();
    }
  }

  /**
   * @return The type to the keystore. This is usually JKS. Property
   *         <code>keystore.type</code>.
   */
  @Nonnull
  public static EKeyStoreType getKeyStoreType ()
  {
    final String sType = getConfigFile ().getAsString ("keystore.type");
    return EKeyStoreType.getFromIDCaseInsensitiveOrDefault (sType, EKeyStoreType.JKS);
  }

  /**
   * @return The key store location as specified in the configuration file by
   *         the property <code>keystore.path</code>.
   */
  @Nullable
  public static String getKeyStorePath ()
  {
    return getConfigFile ().getAsString ("keystore.path");
  }

  /**
   * @return The keystore password as specified in the configuration file by the
   *         property <code>keystore.password</code>.
   */
  @Nullable
  public static String getKeyStorePassword ()
  {
    return getConfigFile ().getAsString ("keystore.password");
  }

  /**
   * @return The private key alias as specified in the configuration file by the
   *         property <code>keystore.key.alias</code>.
   */
  @Nullable
  public static String getKeyStoreKeyAlias ()
  {
    return getConfigFile ().getAsString ("keystore.key.alias");
  }

  /**
   * @return The private key password as specified in the configuration file by
   *         the property <code>keystore.key.password</code>.
   */
  @Nullable
  public static char [] getKeyStoreKeyPassword ()
  {
    return getConfigFile ().getAsCharArray ("keystore.key.password");
  }

  /**
   * @return The type to the truststore. This is usually JKS. Property
   *         <code>truststore.type</code>.
   * @since 0.6.0
   */
  @Nonnull
  public static EKeyStoreType getTrustStoreType ()
  {
    final String sType = getConfigFile ().getAsString ("truststore.type");
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
    return getConfigFile ().getAsString ("truststore.path");
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
    return getConfigFile ().getAsString ("truststore.password");
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
    return getConfigFile ().getAsBoolean ("https.hostname-verification.disabled", true);
  }

  /**
   * @return The proxy host to be used for "http" calls. May be
   *         <code>null</code>.
   * @see #getHttpsProxyHost()
   */
  @Nullable
  public static String getHttpProxyHost ()
  {
    return getConfigFile ().getAsString ("http.proxyHost");
  }

  /**
   * @return The proxy port to be used for "http" calls. Defaults to 0.
   * @see #getHttpsProxyPort()
   */
  public static int getHttpProxyPort ()
  {
    return getConfigFile ().getAsInt ("http.proxyPort", 0);
  }

  /**
   * @return The proxy host to be used for "https" calls. May be
   *         <code>null</code>.
   * @see #getHttpProxyHost()
   */
  @Nullable
  public static String getHttpsProxyHost ()
  {
    return getConfigFile ().getAsString ("https.proxyHost");
  }

  /**
   * @return The proxy port to be used for "https" calls. Defaults to 0.
   * @see #getHttpProxyPort()
   */
  public static int getHttpsProxyPort ()
  {
    return getConfigFile ().getAsInt ("https.proxyPort", 0);
  }

  /**
   * @return The username for proxy calls. Valid for https and https proxy. May
   *         be <code>null</code>.
   * @since 0.6.0
   */
  @Nullable
  public static String getProxyUsername ()
  {
    return getConfigFile ().getAsString ("proxy.username");
  }

  /**
   * @return The password for proxy calls. Valid for https and https proxy. May
   *         be <code>null</code>.
   * @since 0.6.0
   */
  @Nullable
  public static String getProxyPassword ()
  {
    return getConfigFile ().getAsString ("proxy.password");
  }

  /**
   * @return Connection timeout in milliseconds. Defaults to 5000 (=5 seconds).
   *         0 means "indefinite", -1 means "system default".
   * @since 0.6.0
   */
  public static int getConnectTimeoutMS ()
  {
    return getConfigFile ().getAsInt ("connect.timeout.ms", DEFAULT_CONNECTION_TIMEOUT_MS);
  }

  /**
   * @return Request/read/socket timeout in milliseconds. Defaults to 10000 (=10
   *         seconds). 0 means "indefinite", -1 means "system default".
   * @since 0.6.0
   */
  public static int getRequestTimeoutMS ()
  {
    return getConfigFile ().getAsInt ("request.timeout.ms", DEFAULT_REQUEST_TIMEOUT_MS);
  }
}
