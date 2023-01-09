/*
 * Copyright (C) 2015-2023 Philip Helger (www.helger.com)
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

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.security.keystore.EKeyStoreType;

/**
 * A single truststore as found in the pd.properties configuration file.
 *
 * @author Philip Helger
 */
@Immutable
public class PDConfiguredTrustStore implements Serializable
{
  private final EKeyStoreType m_eType;
  private final String m_sPath;
  private final String m_sPassword;
  private final String m_sAlias;

  public PDConfiguredTrustStore (@Nonnull final EKeyStoreType eType,
                                 @Nonnull @Nonempty final String sPath,
                                 @Nonnull final String sPassword,
                                 @Nonnull @Nonempty final String sAlias)
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

  @Nonnull
  public EKeyStoreType getType ()
  {
    return m_eType;
  }

  @Nonnull
  @Nonempty
  public String getPath ()
  {
    return m_sPath;
  }

  @Nonnull
  public String getPassword ()
  {
    return m_sPassword;
  }

  @Nonnull
  @Nonempty
  public String getAlias ()
  {
    return m_sAlias;
  }
}
