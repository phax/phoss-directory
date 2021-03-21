/**
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
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
import com.helger.pd.businesscard.generic.PDBusinessCard;
import com.helger.pd.businesscard.generic.PDIdentifier;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.pd.publisher.nicename.NiceNameEntry;
import com.helger.pd.publisher.nicename.NiceNameHandler;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
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
    final IMicroElement eDocTypeID = new MicroElement (XML_EXPORT_NS_URI,
                                                       "doctypeid").setAttribute ("scheme", aDocTypeID.getScheme ())
                                                                   .setAttribute ("value", aDocTypeID.getValue ());
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
}
