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

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.security.keystore.EKeyStoreType;

/**
 * A single truststore as found in the pd.properties configuration file.
 *
 * @author Philip Helger
 */
@Immutable
public class PDConfiguredTrustStore
{
  private final EKeyStoreType m_eType;
  private final String m_sPath;
  private final String m_sPassword;
  private final String m_sAlias;

  public PDConfiguredTrustStore (@NonNull final EKeyStoreType eType,
                                 @NonNull @Nonempty final String sPath,
                                 @NonNull final String sPassword,
                                 @NonNull @Nonempty final String sAlias)
  {
    ValueEnforcer.notNull (eType, "Type");
    ValueEnforcer.notEmpty (sPath, "Path");
    ValueEnforcer.notNull (sPassword, "Password");
    ValueEnforcer.notEmpty (sAlias, "Alias");
    m_eType = eType;
    m_sPath = sPath;
    m_sPassword = sPassword;
    m_sAlias = sAlias;
  }

  @NonNull
  public EKeyStoreType getType ()
  {
    return m_eType;
  }

  @NonNull
  @Nonempty
  public String getPath ()
  {
    return m_sPath;
  }

  @NonNull
  public String getPassword ()
  {
    return m_sPassword;
  }

  @NonNull
  @Nonempty
  public String getAlias ()
  {
    return m_sAlias;
  }
}
