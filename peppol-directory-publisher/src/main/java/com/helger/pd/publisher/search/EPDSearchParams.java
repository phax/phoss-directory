package com.helger.pd.publisher.search;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;

/**
 * The PEPPOL Directory specific search terms
 *
 * @author Philip Helger
 */
public enum EPDSearchParams
{
  PARTICIPANT_ID ("participant", EPDSearchDataType.STRING_CI),
  NAME ("name", EPDSearchDataType.STRING_CI),
  COUNTRY ("country", EPDSearchDataType.STRING_CI),
  GEO_INFO ("geoinfo", EPDSearchDataType.STRING_CI),
  IDENTIFIER ("identifier", EPDSearchDataType.STRING_CS),
  REGISTRATION_DATE ("regdate", EPDSearchDataType.DATE),
  DOCUMENT_TYPE ("doctype", EPDSearchDataType.STRING_CS);

  private final String m_sParamName;
  private final EPDSearchDataType m_eDataType;

  private EPDSearchParams (@Nonnull @Nonempty final String sParamName, @Nonnull final EPDSearchDataType eDataType)
  {
    m_sParamName = sParamName;
    m_eDataType = eDataType;
  }

  @Nonnull
  @Nonempty
  public String getParamName ()
  {
    return m_sParamName;
  }

  @Nonnull
  public EPDSearchDataType getDataType ()
  {
    return m_eDataType;
  }
}
