package com.helger.pd.settings;

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
public class PDConfiguredTrustStore
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
