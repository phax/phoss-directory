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
package com.helger.pd.publisher.exportall;

import java.util.Map;

import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.web.PDTWebDateHelper;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.peppol.businesscard.generic.PDBusinessCard;
import com.helger.peppol.businesscard.generic.PDIdentifier;
import com.helger.peppol.ui.types.nicename.NiceNameEntry;
import com.helger.peppol.ui.types.nicename.NiceNameManager;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.MicroElement;

import jakarta.annotation.Nonnull;

final class ExportHelper
{
  // XML_EXPORT_NS_URI_V2 = "http://www.peppol.eu/schema/pd/businesscard-generic/201907/";
  public static final String XML_EXPORT_NS_URI_V3 = "urn:peppol:schema:pd:businesscard-generic:2025:03";

  private ExportHelper ()
  {}

  @Nonnull
  static IMicroElement createMicroElement (@Nonnull final IDocumentTypeIdentifier aDocTypeID)
  {
    final IMicroElement eDocTypeID = new MicroElement (XML_EXPORT_NS_URI_V3, "doctypeid").setAttribute ("scheme",
                                                                                                        aDocTypeID.getScheme ())
                                                                                         .setAttribute ("value",
                                                                                                        aDocTypeID.getValue ());
    final NiceNameEntry aNiceName = NiceNameManager.getDocTypeNiceName (aDocTypeID.getURIEncoded ());
    if (aNiceName == null)
      eDocTypeID.setAttribute ("non-standard", true);
    else
    {
      eDocTypeID.setAttribute ("displayname", aNiceName.getName ());
      // New in XML v3: use "state" instead of "deprecated"
      eDocTypeID.setAttribute ("state", aNiceName.getState ().getID ());
    }
    return eDocTypeID;
  }

  @Nonnull
  static IMicroDocument createXML ()
  {
    // XML root
    final IMicroDocument aDoc = new MicroDocument ();
    final IMicroElement aRoot = aDoc.addElementNS (XML_EXPORT_NS_URI_V3, "root");
    aRoot.setAttribute ("version", "3");
    aRoot.setAttribute ("creationdt", PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentZonedDateTimeUTC ()));
    aRoot.setAttribute ("codeListSupported", EPredefinedDocumentTypeIdentifier.CODE_LIST_VERSION);
    return aDoc;
  }

  static IMicroElement createMicroElement (@Nonnull final IParticipantIdentifier aParticipantID,
                                           @Nonnull final ICommonsList <PDStoredBusinessEntity> aBEs,
                                           final boolean bIncludeDocTypes)
  {
    final PDBusinessCard aBC = new PDBusinessCard ();
    aBC.setParticipantIdentifier (new PDIdentifier (aParticipantID.getScheme (), aParticipantID.getValue ()));
    for (final PDStoredBusinessEntity aSBE : aBEs)
      aBC.businessEntities ().add (aSBE.getAsBusinessEntity ());
    final IMicroElement eBC = aBC.getAsMicroXML (XML_EXPORT_NS_URI_V3, "businesscard");

    // New in XML v2 - add all Document types
    if (bIncludeDocTypes && aBEs.isNotEmpty ())
      for (final IDocumentTypeIdentifier aDocTypeID : aBEs.getFirstOrNull ().documentTypeIDs ())
        eBC.addChild (createMicroElement (aDocTypeID));

    return eBC;
  }

  @Nonnull
  static IMicroDocument getAsXML (@Nonnull final ICommonsOrderedMap <IParticipantIdentifier, ICommonsList <PDStoredBusinessEntity>> aMap,
                                  final boolean bIncludeDocTypes)
  {
    // XML root
    final IMicroDocument aDoc = createXML ();
    final IMicroElement aRoot = aDoc.getDocumentElement ();

    // For all BCs
    for (final Map.Entry <IParticipantIdentifier, ICommonsList <PDStoredBusinessEntity>> aEntry : aMap.entrySet ())
    {
      aRoot.addChild (createMicroElement (aEntry.getKey (), aEntry.getValue (), bIncludeDocTypes));
    }

    return aDoc;
  }
}
