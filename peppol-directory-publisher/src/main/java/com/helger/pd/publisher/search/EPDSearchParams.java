package com.helger.pd.publisher.search;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;

public enum EPDSearchParams
{
  GENERIC_QUERY ("q", null),
  PARTICIPANT_ID ("participant", EPDMatchType.EXACT_MATCH_CI),
  NAME ("name", EPDMatchType.PARTIAL_MATCH_CI),
  COUNTRY ("country", EPDMatchType.EXACT_MATCH_CI),
  GEO_INFO ("geoinfo", EPDMatchType.PARTIAL_MATCH_CI),
  IDENTIFIER ("identifier", EPDMatchType.EXACT_MATCH_CS),
  REGISTRATION_DATE ("regdate", null),
  DOCUMENT_TYPE ("doctype", EPDMatchType.EXACT_MATCH_CS),
  RESULT_PAGE_INDEX ("resultPageIndex", null),
  RESULT_PAGE_COUNT ("resultPageCount", null);

  private final String m_sParamName;
  private final EPDMatchType m_eMatchType;

  private EPDSearchParams (@Nonnull @Nonempty final String sParamName, @Nullable final EPDMatchType eMatchType)
  {
    m_sParamName = sParamName;
    m_eMatchType = eMatchType;
  }

  @Nonnull
  @Nonempty
  public String getParamName ()
  {
    return m_sParamName;
  }

  @Nullable
  public EPDMatchType getMatchType ()
  {
    return m_eMatchType;
  }
}
