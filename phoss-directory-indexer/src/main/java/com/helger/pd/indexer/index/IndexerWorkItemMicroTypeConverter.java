/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.index;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.string.StringHelper;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.SimpleIdentifierFactory;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.IMicroTypeConverter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * {@link IMicroTypeConverter} implementation for {@link IndexerWorkItem}
 *
 * @author Philip Helger
 */
public final class IndexerWorkItemMicroTypeConverter implements IMicroTypeConverter <IndexerWorkItem>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (IndexerWorkItemMicroTypeConverter.class);

  private static final String ATTR_ID = "id";
  private static final String ATTR_CREATION_DATE_TIME = "creationdt";
  private static final String ATTR_PARTICIPANT_ID = "participantid";
  private static final String ATTR_TYPE = "type";
  private static final String ATTR_OWNER_ID = "ownerid";
  private static final String ATTR_HOST = "host";

  @Nullable
  public IMicroElement convertToMicroElement (@Nonnull final IndexerWorkItem aValue,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.setAttribute (ATTR_ID, aValue.getID ());
    aElement.setAttributeWithConversion (ATTR_CREATION_DATE_TIME, aValue.getCreationDateTime ());
    aElement.setAttribute (ATTR_PARTICIPANT_ID, aValue.getParticipantID ().getURIEncoded ());
    aElement.setAttribute (ATTR_TYPE, aValue.getType ().getID ());
    aElement.setAttribute (ATTR_OWNER_ID, aValue.getOwnerID ());
    aElement.setAttribute (ATTR_HOST, aValue.getRequestingHost ());
    return aElement;
  }

  @Nullable
  public IndexerWorkItem convertToNative (@Nonnull final IMicroElement aElement)
  {
    final String sID = aElement.getAttributeValue (ATTR_ID);

    final LocalDateTime aCreationDT = aElement.getAttributeValueWithConversion (ATTR_CREATION_DATE_TIME,
                                                                                LocalDateTime.class);

    final String sParticipantID = StringHelper.trim (aElement.getAttributeValue (ATTR_PARTICIPANT_ID));
    IParticipantIdentifier aParticipantID = PDMetaManager.getIdentifierFactory ()
                                                         .parseParticipantIdentifier (sParticipantID);
    if (aParticipantID == null)
    {
      LOGGER.warn ("Failed to parse '" +
                   sParticipantID +
                   "' with the configured IdentifierFactory: " +
                   PDMetaManager.getIdentifierFactory ().toString ());

      // For read bogus entries - we need to stick with the simple one for now.
      // TODO hard coded use of SimpleIdentifierFactory
      aParticipantID = SimpleIdentifierFactory.INSTANCE.parseParticipantIdentifier (sParticipantID);
    }
    if (aParticipantID == null)
      throw new IllegalStateException ("Failed to parse participant identifier '" + sParticipantID + "'");

    final String sTypeID = aElement.getAttributeValue (ATTR_TYPE);
    final EIndexerWorkItemType eType = EIndexerWorkItemType.getFromIDOrNull (sTypeID);
    if (eType == null)
      throw new IllegalStateException ("Failed to parse type ID '" + sTypeID + "'");

    final String sOwnerID = aElement.getAttributeValue (ATTR_OWNER_ID);

    final String sRequestingHost = aElement.getAttributeValue (ATTR_HOST);

    return new IndexerWorkItem (sID, aCreationDT, aParticipantID, eType, sOwnerID, sRequestingHost);
  }
}
