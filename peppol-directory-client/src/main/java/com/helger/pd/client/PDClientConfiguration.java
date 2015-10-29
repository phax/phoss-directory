/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
 */
package com.helger.pd.client;

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
 * This class manages the configuration properties of the PYP client. The order
 * of the properties file resolving is as follows:
 * <ol>
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
    final String sPropertyPath = SystemProperties.getPropertyValue ("pd.client.properties.path");
    if (StringHelper.hasText (sPropertyPath))
      aFilePaths.add (sPropertyPath);

    // Use the default paths
    aFilePaths.add ("private-pd-client.properties");
    aFilePaths.add ("pd-client.properties");

    s_aConfigFile = new ConfigFile (ArrayHelper.newArray (aFilePaths, String.class));
    if (s_aConfigFile.isRead ())
      s_aLogger.info ("Read PYP client properties from " + s_aConfigFile.getReadResource ().getPath ());
    else
      s_aLogger.warn ("Failed to read PYP client properties from any of the paths: " + aFilePaths);
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
}
