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
  PARTICIPANT_ID ("participant", ESearchDataType.STRING_CI),
  NAME ("name", ESearchDataType.STRING_CI),
  COUNTRY ("country", ESearchDataType.STRING_CI),
  GEO_INFO ("geoinfo", ESearchDataType.STRING_CI),
  IDENTIFIER ("identifier", ESearchDataType.STRING_CS),
  REGISTRATION_DATE ("regdate", ESearchDataType.DATE),
  DOCUMENT_TYPE ("doctype", ESearchDataType.STRING_CS);

  private final String m_sParamName;
  private final ESearchDataType m_eDataType;

  private EPDSearchParams (@Nonnull @Nonempty final String sParamName, @Nonnull final ESearchDataType eDataType)
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
  public ESearchDataType getDataType ()
  {
    return m_eDataType;
  }
}
