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
package com.helger.pd.client;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.settings.exchange.configfile.ConfigFile;
import com.helger.settings.exchange.configfile.ConfigFileBuilder;

/**
 * This class manages the configuration properties of the PEPPOL Directory
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
@Immutable
public final class PDClientConfiguration
{
  public static final String SYSTEM_PROPERTY_PRIMARY = "peppol.pd.client.properties.path";
  public static final String SYSTEM_PROPERTY_SECONDARY = "pd.client.properties.path";
  public static final String PROPERTY_FILE_PRIMARY = "private-pd-client.properties";
  public static final String PROPERTY_FILE_SECONDARY = "pd-client.properties";

  public static final String DEFAULT_TRUSTSTORE_PATH = "pd-client.truststore.jks";
  public static final String DEFAULT_TRUSTSTORE_PASSWORD = "peppol";

  private static final Logger s_aLogger = LoggerFactory.getLogger (PDClientConfiguration.class);
  private static final ConfigFile s_aConfigFile;

  static
  {
    final ConfigFileBuilder aCFB = new ConfigFileBuilder ().addPathFromSystemProperty (SYSTEM_PROPERTY_PRIMARY)
                                                           .addPathFromSystemProperty (SYSTEM_PROPERTY_SECONDARY)
                                                           .addPath (PROPERTY_FILE_PRIMARY)
                                                           .addPath (PROPERTY_FILE_SECONDARY);

    s_aConfigFile = aCFB.build ();
    if (s_aConfigFile.isRead ())
      s_aLogger.info ("Read PEPPOL Directory client properties from " + s_aConfigFile.getReadResource ().getPath ());
    else
      s_aLogger.warn ("Failed to read PEPPOL Directory client properties from " + aCFB.getAllPaths ());
  }

  private PDClientConfiguration ()
  {}

  /**
   * @return The global config file for the SMP client.
   */
  @Nonnull
  public static ConfigFile getConfigFile ()
  {
    return s_aConfigFile;
  }

  /**
   * @return The key store location as specified in the configuration file by
   *         the property <code>keystore.path</code>.
   */
  @Nullable
  public static String getKeyStorePath ()
  {
    return s_aConfigFile.getAsString ("keystore.path");
  }

  /**
   * @return The keystore password as specified in the configuration file by the
   *         property <code>keystore.password</code>.
   */
  @Nullable
  public static String getKeyStorePassword ()
  {
    return s_aConfigFile.getAsString ("keystore.password");
  }

  /**
   * @return The private key alias as specified in the configuration file by the
   *         property <code>keystore.key.alias</code>.
   */
  @Nullable
  public static String getKeyStoreKeyAlias ()
  {
    return s_aConfigFile.getAsString ("keystore.key.alias");
  }

  /**
   * @return The private key password as specified in the configuration file by
   *         the property <code>keystore.key.password</code>.
   */
  @Nullable
  public static char [] getKeyStoreKeyPassword ()
  {
    return s_aConfigFile.getAsCharArray ("keystore.key.password");
  }

  /**
   * @return The trust store location as specified in the configuration file by
   *         the property <code>truststore.path</code>. Defaults to
   *         {@value #DEFAULT_TRUSTSTORE_PATH}.
   * @since 0.5.1
   */
  @Nullable
  public static String getTrustStorePath ()
  {
    return s_aConfigFile.getAsString ("truststore.path", DEFAULT_TRUSTSTORE_PATH);
  }

  /**
   * @return The trust store password as specified in the configuration file by
   *         the property <code>truststore.password</code>. Defaults to
   *         {@value #DEFAULT_TRUSTSTORE_PASSWORD}.
   * @since 0.5.1
   */
  @Nullable
  public static String getTrustStorePassword ()
  {
    return s_aConfigFile.getAsString ("truststore.password", DEFAULT_TRUSTSTORE_PASSWORD);
  }

  /**
   * @return The proxy host to be used for "http" calls. May be
   *         <code>null</code>.
   * @see #getHttpsProxyHost()
   */
  @Nullable
  public static String getHttpProxyHost ()
  {
    return s_aConfigFile.getAsString ("http.proxyHost");
  }

  /**
   * @return The proxy port to be used for "http" calls. Defaults to 0.
   * @see #getHttpsProxyPort()
   */
  public static int getHttpProxyPort ()
  {
    return s_aConfigFile.getAsInt ("http.proxyPort", 0);
  }

  /**
   * @return The proxy host to be used for "https" calls. May be
   *         <code>null</code>.
   * @see #getHttpProxyHost()
   */
  @Nullable
  public static String getHttpsProxyHost ()
  {
    return s_aConfigFile.getAsString ("https.proxyHost");
  }

  /**
   * @return The proxy port to be used for "https" calls. Defaults to 0.
   * @see #getHttpProxyPort()
   */
  public static int getHttpsProxyPort ()
  {
    return s_aConfigFile.getAsInt ("https.proxyPort", 0);
  }
}
