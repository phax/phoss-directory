package com.helger.pyp.indexer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joda.time.LocalDateTime;

import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.MicroElement;
import com.helger.commons.microdom.convert.IMicroTypeConverter;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.identifier.IdentifierHelper;

public final class IndexerWorkItemMicroTypeConverter implements IMicroTypeConverter
{
  private static final String ATTR_CREATION_DATE_TIME = "creationdt";
  private static final String ATTR_PARTICIPANT_ID = "participantid";
  private static final String ATTR_TYPE = "type";

  @Nullable
  public IMicroElement convertToMicroElement (@Nonnull final Object aObject,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final IndexerWorkItem aValue = (IndexerWorkItem) aObject;
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.setAttributeWithConversion (ATTR_CREATION_DATE_TIME, aValue.getCreationDT ());
    aElement.setAttribute (ATTR_PARTICIPANT_ID, aValue.getParticipantID ().getURIEncoded ());
    aElement.setAttribute (ATTR_TYPE, aValue.getType ().getID ());
    return aElement;
  }

  @Nullable
  public IndexerWorkItem convertToNative (@Nonnull final IMicroElement aElement)
  {
    final LocalDateTime aCreationDT = aElement.getAttributeValueWithConversion (ATTR_CREATION_DATE_TIME,
                                                                                LocalDateTime.class);
    final String sParticipantID = aElement.getAttributeValue (ATTR_PARTICIPANT_ID);
    final IParticipantIdentifier aParticipantID = IdentifierHelper.createParticipantIdentifierFromURIPart (sParticipantID);
    if (aParticipantID == null)
      throw new IllegalStateException ("Failed to parse participant identifier '" + sParticipantID + "'");

    final String sTypeID = aElement.getAttributeValue (ATTR_TYPE);
    final EIndexerWorkItemType eType = EIndexerWorkItemType.getFromIDOrNull (sTypeID);
    if (eType == null)
      throw new IllegalStateException ("Failed to parse type ID '" + sTypeID + "'");

    return new IndexerWorkItem (aCreationDT, aParticipantID, eType);
  }

}
