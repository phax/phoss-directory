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
package com.helger.pd.publisher.search;

import java.time.LocalDate;
import java.util.Locale;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.lucene.search.Query;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;
import com.helger.commons.text.display.IHasDisplayText;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.storage.CPDStorage;
import com.helger.pd.indexer.storage.PDQueryManager;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * The Peppol Directory specific search fields. The fields here should be
 * conform to the fields in {@link PDStoredBusinessEntity}.
 *
 * @author Philip Helger
 */
public enum EPDSearchField implements IHasID <String>, IHasDisplayText
{
  GENERIC ("q",
           EPDSearchFieldName.GENERIC,
           ESearchDataType.STRING_CS,
           Object.class,
           sQuery -> PDQueryManager.convertQueryStringToLuceneQuery (PDMetaManager.getLucene (),
                                                                     CPDStorage.FIELD_ALL_FIELDS,
                                                                     sQuery)),
  PARTICIPANT_ID ("participant",
                  EPDSearchFieldName.PARTICIPANT_ID,
                  ESearchDataType.STRING_CS,
                  IParticipantIdentifier.class,
                  PDQueryManager::getParticipantIDLuceneQuery),
  NAME ("name",
        EPDSearchFieldName.NAME,
        ESearchDataType.STRING_CI,
        String.class,
        sQuery -> PDQueryManager.getNameLuceneQuery (PDMetaManager.getLucene (), sQuery)),
  COUNTRY ("country",
           EPDSearchFieldName.COUNTRY,
           ESearchDataType.STRING_CI,
           Locale.class,
           PDQueryManager::getCountryCodeLuceneQuery),
  GEO_INFO ("geoinfo",
            EPDSearchFieldName.GEO_INFO,
            ESearchDataType.STRING_CI,
            String.class,
            sQuery -> PDQueryManager.getGeoInfoLuceneQuery (PDMetaManager.getLucene (), sQuery)),
  IDENTIFIER_SCHEME ("identifierScheme",
                     EPDSearchFieldName.IDENTIFIER,
                     ESearchDataType.STRING_CS,
                     String.class,
                     PDQueryManager::getIdentifierSchemeLuceneQuery),
  IDENTIFIER_VALUE ("identifierValue",
                    EPDSearchFieldName.IDENTIFIER,
                    ESearchDataType.STRING_CS,
                    String.class,
                    PDQueryManager::getIdentifierValueLuceneQuery),
  WEBSITE ("website",
           EPDSearchFieldName.WEBSITE,
           ESearchDataType.STRING_CI,
           String.class,
           PDQueryManager::getWebsiteLuceneQuery),
  CONTACT ("contact",
           EPDSearchFieldName.CONTACT,
           ESearchDataType.STRING_CI,
           String.class,
           PDQueryManager::getContactLuceneQuery),
  ADDITIONAL_INFORMATION ("addinfo",
                          EPDSearchFieldName.ADDITIONAL_INFORMATION,
                          ESearchDataType.STRING_CI,
                          String.class,
                          sQuery -> PDQueryManager.getAdditionalInformationLuceneQuery (PDMetaManager.getLucene (),
                                                                                        sQuery)),
  REGISTRATION_DATE ("regdate",
                     EPDSearchFieldName.REGISTRATION_DATE,
                     ESearchDataType.DATE,
                     LocalDate.class,
                     PDQueryManager::getRegistrationDateLuceneQuery),
  DOCUMENT_TYPE ("doctype",
                 EPDSearchFieldName.DOCUMENT_TYPE,
                 ESearchDataType.STRING_CS,
                 IDocumentTypeIdentifier.class,
                 PDQueryManager::getDocumentTypeIDLuceneQuery);

  private final String m_sID;
  private final ESearchDataType m_eDataType;
  private final EPDSearchFieldName m_eDisplayText;
  private final Class <?> m_aNativeType;
  private final Function <String, Query> m_aQueryProvider;

  EPDSearchField (@Nonnull @Nonempty final String sID,
                  @Nonnull final EPDSearchFieldName eDisplayText,
                  @Nonnull final ESearchDataType eDataType,
                  @Nonnull final Class <?> aNativeType,
                  @Nonnull final Function <String, Query> aQueryProvider)
  {
    m_sID = sID;
    m_eDataType = eDataType;
    m_eDisplayText = eDisplayText;
    m_aNativeType = aNativeType;
    m_aQueryProvider = aQueryProvider;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nonnull
  @Nonempty
  public String getFieldName ()
  {
    return getID ();
  }

  @Nonnull
  public ESearchDataType getDataType ()
  {
    return m_eDataType;
  }

  @Nullable
  public String getDisplayText (@Nonnull final Locale aContentLocale)
  {
    return m_eDisplayText.getDisplayText (aContentLocale);
  }

  @Nonnull
  public Class <?> getNativeType ()
  {
    return m_aNativeType;
  }

  @Nullable
  public Query getQuery (@Nonnull final String sQuery)
  {
    return m_aQueryProvider.apply (sQuery);
  }

  @Nullable
  public static EPDSearchField getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EPDSearchField.class, sID);
  }
}
