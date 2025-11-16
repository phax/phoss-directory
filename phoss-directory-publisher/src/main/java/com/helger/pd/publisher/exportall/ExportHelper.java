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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jspecify.annotations.NonNull;

import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.web.PDTWebDateHelper;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.pd.indexer.storage.PDStoredContact;
import com.helger.pd.indexer.storage.PDStoredIdentifier;
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

final class ExportHelper
{
  // XML_EXPORT_NS_URI_V2 = "http://www.peppol.eu/schema/pd/businesscard-generic/201907/";
  public static final String XML_EXPORT_NS_URI_V3 = "urn:peppol:schema:pd:businesscard-generic:2025:03";

  private ExportHelper ()
  {}

  @NonNull
  static IMicroDocument getAsXML (@NonNull final ICommonsOrderedMap <IParticipantIdentifier, ICommonsList <PDStoredBusinessEntity>> aMap,
                                  final boolean bIncludeDocTypes)
  {
    // XML root
    final IMicroDocument aDoc = new MicroDocument ();
    final IMicroElement aRoot = aDoc.addElementNS (XML_EXPORT_NS_URI_V3, "root");
    aRoot.setAttribute ("version", "3");
    aRoot.setAttribute ("creationdt", PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentZonedDateTimeUTC ()));
    aRoot.setAttribute ("codeListSupported", EPredefinedDocumentTypeIdentifier.CODE_LIST_VERSION);

    // For all BCs
    for (final Map.Entry <IParticipantIdentifier, ICommonsList <PDStoredBusinessEntity>> aEntry : aMap.entrySet ())
    {
      final IParticipantIdentifier aParticipantID = aEntry.getKey ();
      final ICommonsList <PDStoredBusinessEntity> aStoredBEs = aEntry.getValue ();

      final PDBusinessCard aBC = new PDBusinessCard ();
      aBC.setParticipantIdentifier (new PDIdentifier (aParticipantID.getScheme (), aParticipantID.getValue ()));
      for (final PDStoredBusinessEntity aSBE : aStoredBEs)
        aBC.businessEntities ().add (aSBE.getAsBusinessEntity ());
      final IMicroElement eBC = aBC.getAsMicroXML (XML_EXPORT_NS_URI_V3, "businesscard");

      // New in XML v2 - add all Document types
      if (bIncludeDocTypes && aStoredBEs.isNotEmpty ())
        for (final IDocumentTypeIdentifier aDocTypeID : aStoredBEs.getFirstOrNull ().documentTypeIDs ())
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
          eBC.addChild (eDocTypeID);
        }
      aRoot.addChild (eBC);
    }

    return aDoc;
  }

  static void writeElement (@NonNull final IParticipantIdentifier aParticipantID,
                            @NonNull final ICommonsList <PDStoredBusinessEntity> aBEs,
                            final boolean bIncludeDocTypes,
                            @NonNull final XMLStreamWriter aXSW) throws XMLStreamException
  {
    aXSW.writeStartElement (XML_EXPORT_NS_URI_V3, "businesscard");

    aXSW.writeEmptyElement (XML_EXPORT_NS_URI_V3, "participant");
    aXSW.writeAttribute ("scheme", aParticipantID.getScheme ());
    aXSW.writeAttribute ("value", aParticipantID.getValue ());

    for (final PDStoredBusinessEntity aSBE : aBEs)
    {
      aXSW.writeStartElement (XML_EXPORT_NS_URI_V3, "entity");
      aXSW.writeAttribute ("countrycode", aSBE.getCountryCode ());

      for (final var aName : aSBE.names ())
      {
        aXSW.writeEmptyElement (XML_EXPORT_NS_URI_V3, "name");
        aXSW.writeAttribute ("name", aName.getName ());
        if (aName.hasLanguageCode ())
          aXSW.writeAttribute ("language", aName.getLanguageCode ());
      }
      if (aSBE.hasGeoInfo ())
      {
        aXSW.writeStartElement (XML_EXPORT_NS_URI_V3, "geoinfo");
        aXSW.writeCharacters (aSBE.getGeoInfo ());
        aXSW.writeEndElement ();
      }
      for (final PDStoredIdentifier aID : aSBE.identifiers ())
      {
        aXSW.writeEmptyElement (XML_EXPORT_NS_URI_V3, "id");
        aXSW.writeAttribute ("scheme", aID.getScheme ());
        aXSW.writeAttribute ("value", aID.getValue ());
      }
      for (final String sWebsiteURI : aSBE.websiteURIs ())
      {
        aXSW.writeStartElement (XML_EXPORT_NS_URI_V3, "website");
        aXSW.writeCharacters (sWebsiteURI);
        aXSW.writeEndElement ();
      }
      for (final PDStoredContact aContact : aSBE.contacts ())
      {
        aXSW.writeEmptyElement (XML_EXPORT_NS_URI_V3, "contact");
        if (aContact.hasType ())
          aXSW.writeAttribute ("type", aContact.getType ());
        if (aContact.hasName ())
          aXSW.writeAttribute ("name", aContact.getName ());
        if (aContact.hasPhone ())
          aXSW.writeAttribute ("phone", aContact.getPhone ());
        if (aContact.hasEmail ())
          aXSW.writeAttribute ("email", aContact.getEmail ());
      }
      if (aSBE.hasAdditionalInformation ())
      {
        aXSW.writeStartElement (XML_EXPORT_NS_URI_V3, "additionalinfo");
        aXSW.writeCharacters (aSBE.getAdditionalInformation ());
        aXSW.writeEndElement ();
      }
      if (aSBE.hasRegistrationDate ())
      {
        aXSW.writeStartElement (XML_EXPORT_NS_URI_V3, "regdate");
        aXSW.writeCharacters (PDTWebDateHelper.getAsStringXSD (aSBE.getRegistrationDate ()));
        aXSW.writeEndElement ();
      }

      // entity
      aXSW.writeEndElement ();
    }

    // New in XML v2 - add all Document types
    if (bIncludeDocTypes && aBEs.isNotEmpty ())
      for (final IDocumentTypeIdentifier aDocTypeID : aBEs.getFirstOrNull ().documentTypeIDs ())
      {
        aXSW.writeEmptyElement (XML_EXPORT_NS_URI_V3, "doctypeid");
        aXSW.writeAttribute ("scheme", aDocTypeID.getScheme ());
        aXSW.writeAttribute ("value", aDocTypeID.getValue ());
        final NiceNameEntry aNiceName = NiceNameManager.getDocTypeNiceName (aDocTypeID.getURIEncoded ());
        if (aNiceName == null)
          aXSW.writeAttribute ("non-standard", "true");
        else
        {
          aXSW.writeAttribute ("displayname", aNiceName.getName ());
          // New in XML v3: use "state" instead of "deprecated"
          aXSW.writeAttribute ("state", aNiceName.getState ().getID ());
        }
      }

    // businesscard
    aXSW.writeEndElement ();
  }
}
