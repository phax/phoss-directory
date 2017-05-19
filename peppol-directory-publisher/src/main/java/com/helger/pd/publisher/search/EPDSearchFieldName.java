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
  PARTICIPANT_ID ("Teilnehmer ID", "Participant ID"),
  NAME ("Name", "Name"),
  COUNTRY ("Land", "Country"),
  GEO_INFO ("Geographische Information", "Geographical information"),
  IDENTIFIER ("Andere ID", "Additional identifiers"),
  WEBSITE ("Website", "Web site"),
  CONTACT ("Kontaktperson", "Contact person"),
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
