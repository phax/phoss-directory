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

import javax.annotation.Nonnull;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTWebDateHelper;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.pd.businesscard.generic.PDBusinessCard;
import com.helger.pd.businesscard.generic.PDIdentifier;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.pd.indexer.storage.PDStoredMLName;
import com.helger.pd.publisher.nicename.NiceNameEntry;
import com.helger.pd.publisher.nicename.NiceNameHandler;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.MicroElement;

public final class ExportHelper
{
  public static final String XML_EXPORT_NS_URI = "http://www.peppol.eu/schema/pd/businesscard-generic/201907/";

  private ExportHelper ()
  {}

  @Nonnull
  private static IMicroElement _createMicroElement (@Nonnull final IDocumentTypeIdentifier aDocTypeID)
  {
    final IMicroElement eDocTypeID = new MicroElement (XML_EXPORT_NS_URI, "doctypeid").setAttribute ("scheme",
                                                                                                     aDocTypeID.getScheme ())
                                                                                      .setAttribute ("value",
                                                                                                     aDocTypeID.getValue ());
    final NiceNameEntry aNiceName = NiceNameHandler.getDocTypeNiceName (aDocTypeID.getURIEncoded ());
    if (aNiceName == null)
      eDocTypeID.setAttribute ("non-standard", true);
    else
    {
      eDocTypeID.setAttribute ("displayname", aNiceName.getName ());
      eDocTypeID.setAttribute ("deprecated", aNiceName.isDeprecated ());
    }
    return eDocTypeID;
  }

  @Nonnull
  public static IMicroDocument getAsXML (@Nonnull final ICommonsOrderedMap <IParticipantIdentifier, ICommonsList <PDStoredBusinessEntity>> aMap,
                                         final boolean bIncludeDocTypes)
  {
    // XML root
    final IMicroDocument aDoc = new MicroDocument ();
    final IMicroElement aRoot = aDoc.appendElement (XML_EXPORT_NS_URI, "root");
    aRoot.setAttribute ("version", "2");
    aRoot.setAttribute ("creationdt", PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentZonedDateTimeUTC ()));
    aRoot.setAttribute ("codeListSupported", EPredefinedDocumentTypeIdentifier.CODE_LIST_VERSION);

    // For all BCs
    for (final Map.Entry <IParticipantIdentifier, ICommonsList <PDStoredBusinessEntity>> aEntry : aMap.entrySet ())
    {
      final IParticipantIdentifier aParticipantID = aEntry.getKey ();

      final PDBusinessCard aBC = new PDBusinessCard ();
      aBC.setParticipantIdentifier (new PDIdentifier (aParticipantID.getScheme (), aParticipantID.getValue ()));
      for (final PDStoredBusinessEntity aSBE : aEntry.getValue ())
        aBC.businessEntities ().add (aSBE.getAsBusinessEntity ());
      final IMicroElement eBC = aBC.getAsMicroXML (XML_EXPORT_NS_URI, "businesscard");

      // New in v2 - add all Document types
      if (bIncludeDocTypes && aEntry.getValue ().isNotEmpty ())
        for (final IDocumentTypeIdentifier aDocTypeID : aEntry.getValue ().getFirst ().documentTypeIDs ())
          eBC.appendChild (_createMicroElement (aDocTypeID));

      aRoot.appendChild (eBC);
    }

    return aDoc;
  }

  @Nonnull
  private static IJsonObject _createJsonObject (@Nonnull final PDStoredBusinessEntity aSBE)
  {
    final IJsonObject ret = new JsonObject ();
    {
      final IJsonArray aNames = new JsonArray ();
      for (final PDStoredMLName aName : aSBE.names ())
        aNames.add (new JsonObject ().add ("name", aName.getName ()).addIfNotNull ("lang", aName.getLanguageCode ()));
      ret.add ("names", aNames);
    }
    if (aSBE.hasCountryCode ())
      ret.add ("countryCode", aSBE.getCountryCode ());
    if (aSBE.hasGeoInfo ())
      ret.add ("geoinfo", aSBE.getGeoInfo ());
    if (aSBE.identifiers ().isNotEmpty ())
      ret.add ("identifiers",
               new JsonArray ().addAllMapped (aSBE.identifiers (),
                                              x -> new JsonObject ().add ("scheme", x.getScheme ())
                                                                    .add ("value", x.getValue ())));
    if (aSBE.websiteURIs ().isNotEmpty ())
      ret.add ("websiteURIs", new JsonArray ().addAll (aSBE.websiteURIs ()));
    if (aSBE.contacts ().isNotEmpty ())
      ret.add ("contacts",
               new JsonArray ().addAllMapped (aSBE.contacts (),
                                              x -> new JsonObject ().addIfNotNull ("type", x.getType ())
                                                                    .addIfNotNull ("name", x.getName ())
                                                                    .addIfNotNull ("phone", x.getPhone ())
                                                                    .addIfNotNull ("email", x.getEmail ())));
    if (aSBE.hasAdditionalInformation ())
      ret.add ("additionalInfo", aSBE.getAdditionalInformation ());
    if (aSBE.hasRegistrationDate ())
      ret.add ("regdate", PDTWebDateHelper.getAsStringXSD (aSBE.getRegistrationDate ()));

    return ret;
  }

  @Nonnull
  private static IJsonObject _createJsonObject (@Nonnull final IDocumentTypeIdentifier aDocTypeID)
  {
    final IJsonObject ret = new JsonObject ();
    ret.add ("scheme", aDocTypeID.getScheme ());
    ret.add ("value", aDocTypeID.getValue ());
    final NiceNameEntry aNiceName = NiceNameHandler.getDocTypeNiceName (aDocTypeID.getURIEncoded ());
    if (aNiceName == null)
      ret.add ("nonStandard", true);
    else
    {
      ret.add ("displayName", aNiceName.getName ());
      ret.add ("deprecated", aNiceName.isDeprecated ());
    }
    return ret;
  }

  @Nonnull
  public static IJsonObject getAsJSON (@Nonnull final ICommonsOrderedMap <IParticipantIdentifier, ICommonsList <PDStoredBusinessEntity>> aMap,
                                       final boolean bIncludeDocTypes)
  {
    // XML root
    final IJsonObject aObj = new JsonObject ();
    aObj.add ("version", 1);
    aObj.add ("creationdt", PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentZonedDateTimeUTC ()));
    aObj.add ("participantCount", aMap.size ());
    aObj.add ("codeListSupported", EPredefinedDocumentTypeIdentifier.CODE_LIST_VERSION);

    final IJsonArray aBCs = new JsonArray ();
    // For all BCs
    for (final Map.Entry <IParticipantIdentifier, ICommonsList <PDStoredBusinessEntity>> aEntry : aMap.entrySet ())
    {
      final IParticipantIdentifier aParticipantID = aEntry.getKey ();

      final IJsonObject aBC = new JsonObject ();
      aBC.add ("pid", aParticipantID.getURIEncoded ());

      final IJsonArray aBEs = new JsonArray ();
      for (final PDStoredBusinessEntity aSBE : aEntry.getValue ())
        aBEs.add (_createJsonObject (aSBE));
      aBC.add ("entities", aBEs);

      // Add all Document types (if wanted)
      if (bIncludeDocTypes)
      {
        final IJsonArray aDocTypes = new JsonArray ();
        if (aEntry.getValue ().isNotEmpty ())
          for (final IDocumentTypeIdentifier aDocTypeID : aEntry.getValue ().getFirst ().documentTypeIDs ())
            aDocTypes.add (_createJsonObject (aDocTypeID));
        aBC.add ("docTypes", aDocTypes);
      }

      aBCs.add (aBC);
    }
    aObj.add ("bc", aBCs);
    return aObj;
  }
}
