/*
 * Copyright (C) 2015-2022 Philip Helger (www.helger.com)
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

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.text.IMultilingualText;
import com.helger.commons.text.display.IHasDisplayText;
import com.helger.commons.text.resolve.DefaultTextResolver;
import com.helger.commons.text.util.TextHelper;

/**
 * Multilingual names for {@link EPDSearchField}.
 *
 * @author Philip Helger
 */
public enum EPDSearchFieldName implements IHasDisplayText
{
  GENERIC ("Allgemeiner Suchtext", "Generic search text"),
  PARTICIPANT_ID ("Teilnehmer ID", "Participant ID"),
  NAME ("Name", "Name"),
  COUNTRY ("Land", "Country"),
  GEO_INFO ("Geographische Information", "Geographical information"),
  IDENTIFIER ("Andere ID", "Additional identifiers"),
  WEBSITE ("Website", "Web site"),
  CONTACT ("Kontaktperson", "Contact person"),
  ADDITIONAL_INFORMATION ("Freitext", "Additional information"),
  REGISTRATION_DATE ("Registrierungsdatum", "Registration date"),
  DOCUMENT_TYPE ("Dokumentenart", "Document type");

  private final IMultilingualText m_aTP;

  private EPDSearchFieldName (@Nonnull final String sDE, @Nonnull final String sEN)
  {
    m_aTP = TextHelper.create_DE_EN (sDE, sEN);
  }

  @Nullable
  public String getDisplayText (@Nonnull final Locale aContentLocale)
  {
    return DefaultTextResolver.getTextStatic (this, m_aTP, aContentLocale);
  }
}
