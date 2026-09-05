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
package com.helger.pd.publisher.search;

import java.time.LocalDate;
import java.util.Locale;
import java.util.function.Function;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.searchindex.query.EPDIndexQueryOccur;
import com.helger.pd.indexer.searchindex.query.IPDIndexQuery;
import com.helger.pd.indexer.storage.PDQueryManager;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.text.display.IHasDisplayText;

import jakarta.annotation.Nullable;

/**
 * The Peppol Directory specific search fields. The fields here should be conform to the fields in
 * {@link PDStoredBusinessEntity}.
 *
 * @author Philip Helger
 */
public enum EPDSearchField implements IHasID <String>, IHasDisplayText
{
  GENERIC ("q",
           EPDSearchFieldName.GENERIC,
           ESearchDataType.STRING_CS,
           Object.class,
           sQuery -> PDQueryManager.getGenericQuery (PDMetaManager.getIndex (), sQuery)),
  PARTICIPANT_ID ("participant",
                  EPDSearchFieldName.PARTICIPANT_ID,
                  ESearchDataType.STRING_CS,
                  IParticipantIdentifier.class,
                  PDQueryManager::getParticipantIDQuery),
  NAME ("name",
        EPDSearchFieldName.NAME,
        ESearchDataType.STRING_CI,
        String.class,
        sQuery -> PDQueryManager.getNameQuery (PDMetaManager.getIndex (), sQuery)),
  COUNTRY ("country",
           EPDSearchFieldName.COUNTRY,
           ESearchDataType.STRING_CI,
           Locale.class,
           PDQueryManager::getCountryCodeQuery),
  GEO_INFO ("geoinfo",
            EPDSearchFieldName.GEO_INFO,
            ESearchDataType.STRING_CI,
            String.class,
            sQuery -> PDQueryManager.getGeoInfoQuery (PDMetaManager.getIndex (), sQuery)),
  IDENTIFIER_SCHEME ("identifierScheme",
                     EPDSearchFieldName.IDENTIFIER,
                     ESearchDataType.STRING_CS,
                     String.class,
                     PDQueryManager::getIdentifierSchemeQuery),
  IDENTIFIER_VALUE ("identifierValue",
                    EPDSearchFieldName.IDENTIFIER,
                    ESearchDataType.STRING_CS,
                    String.class,
                    PDQueryManager::getIdentifierValueQuery),
  WEBSITE ("website",
           EPDSearchFieldName.WEBSITE,
           ESearchDataType.STRING_CI,
           String.class,
           PDQueryManager::getWebsiteQuery),
  CONTACT ("contact",
           EPDSearchFieldName.CONTACT,
           ESearchDataType.STRING_CI,
           String.class,
           PDQueryManager::getContactQuery),
  ADDITIONAL_INFORMATION ("addinfo",
                          EPDSearchFieldName.ADDITIONAL_INFORMATION,
                          ESearchDataType.STRING_CI,
                          String.class,
                          sQuery -> PDQueryManager.getAdditionalInformationQuery (PDMetaManager.getIndex (), sQuery)),
  REGISTRATION_DATE ("regdate",
                     EPDSearchFieldName.REGISTRATION_DATE,
                     ESearchDataType.DATE,
                     LocalDate.class,
                     PDQueryManager::getRegistrationDateQuery),
  DOCUMENT_TYPE ("doctype",
                 EPDSearchFieldName.DOCUMENT_TYPE,
                 ESearchDataType.STRING_CS,
                 IDocumentTypeIdentifier.class,
                 PDQueryManager::getDocumentTypeIDQuery);

  private final String m_sID;
  private final ESearchDataType m_eDataType;
  private final EPDSearchFieldName m_eDisplayText;
  private final Class <?> m_aNativeType;
  private final Function <String, IPDIndexQuery> m_aQueryProvider;

  EPDSearchField (@NonNull @Nonempty final String sID,
                  @NonNull final EPDSearchFieldName eDisplayText,
                  @NonNull final ESearchDataType eDataType,
                  @NonNull final Class <?> aNativeType,
                  @NonNull final Function <String, IPDIndexQuery> aQueryProvider)
  {
    m_sID = sID;
    m_eDataType = eDataType;
    m_eDisplayText = eDisplayText;
    m_aNativeType = aNativeType;
    m_aQueryProvider = aQueryProvider;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @NonNull
  @Nonempty
  public String getFieldName ()
  {
    return getID ();
  }

  @NonNull
  public ESearchDataType getDataType ()
  {
    return m_eDataType;
  }

  @Nullable
  public String getDisplayText (@NonNull final Locale aContentLocale)
  {
    return m_eDisplayText.getDisplayText (aContentLocale);
  }

  @NonNull
  public Class <?> getNativeType ()
  {
    return m_aNativeType;
  }

  @Nullable
  public IPDIndexQuery getQuery (@NonNull final String sQuery)
  {
    return m_aQueryProvider.apply (sQuery);
  }

  /**
   * @return The occurrence to be used, if the query of this search field is combined with the
   *         queries of other search fields. All search fields that perform an exact match only
   *         limit the result set - they must not influence the relevance ordering of the results.
   *         See https://github.com/phax/phoss-directory/issues/49
   */
  @NonNull
  public EPDIndexQueryOccur getCombinationOccurrence ()
  {
    // Deliberately no "default" case, so that adding a new search field is a compile error here
    return switch (this)
    {
      case PARTICIPANT_ID, COUNTRY, IDENTIFIER_SCHEME, IDENTIFIER_VALUE, REGISTRATION_DATE, DOCUMENT_TYPE -> EPDIndexQueryOccur.FILTER;
      case GENERIC, NAME, GEO_INFO, WEBSITE, CONTACT, ADDITIONAL_INFORMATION -> EPDIndexQueryOccur.MUST;
    };
  }

  @Nullable
  public static EPDSearchField getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EPDSearchField.class, sID);
  }
}
