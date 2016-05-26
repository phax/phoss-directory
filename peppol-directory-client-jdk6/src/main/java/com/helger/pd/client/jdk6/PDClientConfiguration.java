/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.pd.client.jdk6;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.system.SystemProperties;
import com.helger.peppol.utils.ConfigFile;

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
  private static final Logger s_aLogger = LoggerFactory.getLogger (PDClientConfiguration.class);
  private static final ConfigFile s_aConfigFile;

  static
  {
    final List <String> aFilePaths = new ArrayList <String> ();
    // Check if the system property is present
    String sPropertyPath = SystemProperties.getPropertyValue ("peppol.pd.client.properties.path");
    if (StringHelper.hasText (sPropertyPath))
      aFilePaths.add (sPropertyPath);
    sPropertyPath = SystemProperties.getPropertyValue ("pd.client.properties.path");
    if (StringHelper.hasText (sPropertyPath))
      aFilePaths.add (sPropertyPath);

    // Use the default paths
    aFilePaths.add ("private-pd-client.properties");
    aFilePaths.add ("pd-client.properties");

    s_aConfigFile = new ConfigFile (ArrayHelper.newArray (aFilePaths, String.class));
    if (s_aConfigFile.isRead ())
      s_aLogger.info ("Read PEPPOL Directory client properties from " + s_aConfigFile.getReadResource ().getPath ());
    else
      s_aLogger.warn ("Failed to read PEPPOL Directory client properties from any of the paths: " + aFilePaths);
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
    return s_aConfigFile.getString ("keystore.path");
  }

  /**
   * @return The keystore password as specified in the configuration file by the
   *         property <code>keystore.password</code>.
   */
  @Nullable
  public static String getKeyStorePassword ()
  {
    return s_aConfigFile.getString ("keystore.password");
  }

  /**
   * @return The private key alias as specified in the configuration file by the
   *         property <code>keystore.key.alias</code>.
   */
  @Nullable
  public static String getKeyStoreKeyAlias ()
  {
    return s_aConfigFile.getString ("keystore.key.alias");
  }

  /**
   * @return The private key password as specified in the configuration file by
   *         the property <code>keystore.key.password</code>.
   */
  @Nullable
  public static char [] getKeyStoreKeyPassword ()
  {
    return s_aConfigFile.getCharArray ("keystore.key.password");
  }

  /**
   * @return The proxy host to be used for "http" calls. May be
   *         <code>null</code>.
   * @see #getHttpsProxyHost()
   */
  @Nullable
  public static String getHttpProxyHost ()
  {
    return s_aConfigFile.getString ("http.proxyHost");
  }

  /**
   * @return The proxy port to be used for "http" calls. Defaults to 0.
   * @see #getHttpsProxyPort()
   */
  public static int getHttpProxyPort ()
  {
    return s_aConfigFile.getInt ("http.proxyPort", 0);
  }

  /**
   * @return The proxy host to be used for "https" calls. May be
   *         <code>null</code>.
   * @see #getHttpProxyHost()
   */
  @Nullable
  public static String getHttpsProxyHost ()
  {
    return s_aConfigFile.getString ("https.proxyHost");
  }

  /**
   * @return The proxy port to be used for "https" calls. Defaults to 0.
   * @see #getHttpProxyPort()
   */
  public static int getHttpsProxyPort ()
  {
    return s_aConfigFile.getInt ("https.proxyPort", 0);
  }
}