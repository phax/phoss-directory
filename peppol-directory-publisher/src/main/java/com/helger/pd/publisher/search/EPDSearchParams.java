package com.helger.pd.publisher.search;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;

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
  private final EPDSearchDataType m_eSearchType;

  private EPDSearchParams (@Nonnull @Nonempty final String sParamName, @Nullable final EPDSearchDataType eSearchType)
  {
    m_sParamName = sParamName;
    m_eSearchType = eSearchType;
  }

  @Nonnull
  @Nonempty
  public String getParamName ()
  {
    return m_sParamName;
  }

  @Nullable
  public EPDSearchDataType getSearchType ()
  {
    return m_eSearchType;
  }
}
