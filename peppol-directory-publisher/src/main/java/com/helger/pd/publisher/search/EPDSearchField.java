/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;
import com.helger.commons.text.display.IHasDisplayText;
import com.helger.pd.indexer.storage.PDStoredDocument;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;

/**
 * The PEPPOL Directory specific search fields. The fields here should be
 * conform to the fields in {@link PDStoredDocument}.
 *
 * @author Philip Helger
 */
public enum EPDSearchField implements IHasID <String>, IHasDisplayText
{
  PARTICIPANT_ID ("participant",
                  EPDSearchFieldName.PARTICIPANT_ID,
                  ESearchDataType.STRING_CI,
                  IParticipantIdentifier.class),
  NAME ("name", EPDSearchFieldName.NAME, ESearchDataType.STRING_CI, String.class),
  COUNTRY ("country", EPDSearchFieldName.COUNTRY, ESearchDataType.STRING_CI, Locale.class),
  GEO_INFO ("geoinfo", EPDSearchFieldName.GEO_INFO, ESearchDataType.STRING_CI, String.class),
  IDENTIFIER ("identifier", EPDSearchFieldName.IDENTIFIER, ESearchDataType.STRING_CS, String.class),
  WEBSITE ("website", EPDSearchFieldName.WEBSITE, ESearchDataType.STRING_CI, String.class),
  CONTACT ("contact", EPDSearchFieldName.CONTACT, ESearchDataType.STRING_CI, String.class),
  ADDITIONAL_INFORMATION ("addinfo",
                          EPDSearchFieldName.ADDITIONAL_INFORMATION,
                          ESearchDataType.STRING_CI,
                          String.class),
  REGISTRATION_DATE ("regdate", EPDSearchFieldName.REGISTRATION_DATE, ESearchDataType.DATE, LocalDate.class),
  DOCUMENT_TYPE ("doctype", EPDSearchFieldName.DOCUMENT_TYPE, ESearchDataType.STRING_CS, IDocumentTypeIdentifier.class);

  private final String m_sID;
  private final ESearchDataType m_eDataType;
  private final EPDSearchFieldName m_eDisplayText;
  private final Class <?> m_aNativeType;

  private EPDSearchField (@Nonnull @Nonempty final String sID,
                          @Nonnull final EPDSearchFieldName eDisplayText,
                          @Nonnull final ESearchDataType eDataType,
                          @Nonnull final Class <?> aNativeType)
  {
    m_sID = sID;
    m_eDataType = eDataType;
    m_eDisplayText = eDisplayText;
    m_aNativeType = aNativeType;
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
  public static EPDSearchField getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EPDSearchField.class, sID);
  }
}
