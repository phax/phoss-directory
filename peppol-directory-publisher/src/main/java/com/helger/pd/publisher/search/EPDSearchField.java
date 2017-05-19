package com.helger.pd.publisher.search;

import java.time.LocalDate;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;
import com.helger.commons.text.display.IHasDisplayText;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;

/**
 * The PEPPOL Directory specific search fields.
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
  REGISTRATION_DATE ("regdate", EPDSearchFieldName.REGISTRATION_DATE, ESearchDataType.DATE, LocalDate.class),
  DOCUMENT_TYPE ("doctype", EPDSearchFieldName.DOCUMENT_TYPE, ESearchDataType.STRING_CS, IDocumentTypeIdentifier.class);

  private final String m_sID;
  private final ESearchDataType m_eDataType;
  private final EPDSearchFieldName m_eDisplayText;

  private EPDSearchField (@Nonnull @Nonempty final String sID,
                          @Nonnull final EPDSearchFieldName eDisplayText,
                          @Nonnull final ESearchDataType eDataType,
                          @Nonnull final Class <?> aNativeType)
  {
    m_sID = sID;
    m_eDataType = eDataType;
    m_eDisplayText = eDisplayText;
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

  @Nullable
  public static EPDSearchField getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EPDSearchField.class, sID);
  }
}
