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
package com.helger.pd.searchapi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;

/**
 * The query fields of the PD search REST API version 1.0. The ID of each enum entry is the query
 * parameter name to be used in the search URL.
 *
 * @author Philip Helger
 * @since 0.19.1
 */
public enum EPDSearchAPIField implements IHasID <String>
{
  /** Generic full text search over all indexed fields */
  GENERIC ("q"),
  /** Participant identifier, in the URI encoded form "scheme::value" */
  PARTICIPANT_ID ("participant"),
  /** Business entity name */
  NAME ("name"),
  /** Business entity country code */
  COUNTRY ("country"),
  /** Business entity geographical information */
  GEO_INFO ("geoinfo"),
  /** Scheme of an additional identifier */
  IDENTIFIER_SCHEME ("identifierScheme"),
  /** Value of an additional identifier */
  IDENTIFIER_VALUE ("identifierValue"),
  /** Business entity website URI */
  WEBSITE ("website"),
  /** Business entity contact information */
  CONTACT ("contact"),
  /** Business entity additional information */
  ADDITIONAL_INFORMATION ("addinfo"),
  /** Business entity registration date */
  REGISTRATION_DATE ("regdate"),
  /** Document type identifier, in the URI encoded form "scheme::value" */
  DOCUMENT_TYPE ("doctype");

  private final String m_sID;

  EPDSearchAPIField (@NonNull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  /**
   * @return The name of the query parameter to be used in the search URL. Never <code>null</code>
   *         nor empty. This is identical to {@link #getID()}.
   */
  @NonNull
  @Nonempty
  public String getFieldName ()
  {
    return m_sID;
  }

  @Nullable
  public static EPDSearchAPIField getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EPDSearchAPIField.class, sID);
  }
}
