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
package com.helger.pd.publisher.exportall;

import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.WillNotClose;
import com.helger.base.io.stream.NonClosingOutputStream;
import com.helger.base.io.stream.StreamHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.web.PDTWebDateHelper;
import com.helger.mime.CMimeType;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.pd.indexer.storage.PDStoredContact;
import com.helger.pd.indexer.storage.PDStoredIdentifier;
import com.helger.pd.indexer.storage.PDStoredMLName;
import com.helger.peppol.ui.types.nicename.NiceNameEntry;
import com.helger.peppol.ui.types.nicename.NiceNameManager;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;

import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;

/**
 * Export all Business Cards as JSON.
 *
 * @author Philip Helger
 */
public class ExportAllHandlerBusinessCardJSON extends AbstractExportAllHandler
{
  private static final boolean INCLUDE_DOC_TYPES = true;

  private Writer m_aWriter;
  private JsonGenerator m_aJsonGen;

  public ExportAllHandlerBusinessCardJSON (@NonNull @Nonempty final String sDisplayName,
                                           @NonNull @Nonempty final String sS3Filename)
  {
    super (sDisplayName, sS3Filename, CMimeType.APPLICATION_JSON);
  }

  public boolean isBusinessEntityDataNeeded ()
  {
    return true;
  }

  public void onStart (@NonNull @WillNotClose final OutputStream aOS,
                       @Nonnegative final int nParticipantCount) throws Exception
  {
    m_aWriter = StreamHelper.createWriter (new NonClosingOutputStream (aOS), StandardCharsets.UTF_8);
    m_aJsonGen = Json.createGenerator (m_aWriter);

    // JSON root
    m_aJsonGen.writeStartObject ()
              .write ("version", 2)
              .write ("creationdt", PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentZonedDateTimeUTC ()))
              .write ("participantCount", nParticipantCount)
              .write ("codeListSupported", EPredefinedDocumentTypeIdentifier.CODE_LIST_VERSION)
              .writeStartArray ("bc");
  }

  public void onParticipant (@NonNull @Nonempty final String sParticipantID,
                             @NonNull final IParticipantIdentifier aParticipantID,
                             @NonNull final ICommonsList <PDStoredBusinessEntity> aEntities) throws Exception
  {
    // If the participant was deleted in the meantime, it is not exported
    if (aEntities.isEmpty ())
      return;

    m_aJsonGen.writeStartObject ().write ("pid", sParticipantID).writeStartArray ("entities");

    for (final PDStoredBusinessEntity aSBE : aEntities)
    {
      m_aJsonGen.writeStartObject ();
      {
        m_aJsonGen.writeStartArray ("names");
        for (final PDStoredMLName aName : aSBE.names ())
        {
          m_aJsonGen.writeStartObject ().write ("name", aName.getName ());
          if (aName.hasLanguageCode ())
            m_aJsonGen.write ("lang", aName.getLanguageCode ());
          m_aJsonGen.writeEnd ();
        }
        m_aJsonGen.writeEnd ();
      }
      if (aSBE.hasCountryCode ())
        m_aJsonGen.write ("countryCode", aSBE.getCountryCode ());
      if (aSBE.hasGeoInfo ())
        m_aJsonGen.write ("geoinfo", aSBE.getGeoInfo ());
      if (aSBE.identifiers ().isNotEmpty ())
      {
        m_aJsonGen.writeStartArray ("identifiers");
        for (final PDStoredIdentifier aID : aSBE.identifiers ())
        {
          m_aJsonGen.writeStartObject ().write ("scheme", aID.getScheme ()).write ("value", aID.getValue ()).writeEnd ();
        }
        m_aJsonGen.writeEnd ();
      }
      if (aSBE.websiteURIs ().isNotEmpty ())
      {
        m_aJsonGen.writeStartArray ("websiteURIs");
        for (final String sWebsite : aSBE.websiteURIs ())
          m_aJsonGen.write (sWebsite);
        m_aJsonGen.writeEnd ();
      }
      if (aSBE.contacts ().isNotEmpty ())
      {
        m_aJsonGen.writeStartArray ("contacts");
        for (final PDStoredContact aContact : aSBE.contacts ())
        {
          m_aJsonGen.writeStartObject ();
          if (aContact.hasType ())
            m_aJsonGen.write ("type", aContact.getType ());
          if (aContact.hasName ())
            m_aJsonGen.write ("name", aContact.getName ());
          if (aContact.hasPhone ())
            m_aJsonGen.write ("phone", aContact.getPhone ());
          if (aContact.hasEmail ())
            m_aJsonGen.write ("email", aContact.getEmail ());
          m_aJsonGen.writeEnd ();
        }
        m_aJsonGen.writeEnd ();
      }
      if (aSBE.hasAdditionalInformation ())
        m_aJsonGen.write ("additionalInfo", aSBE.getAdditionalInformation ());
      if (aSBE.hasRegistrationDate ())
        m_aJsonGen.write ("regdate", PDTWebDateHelper.getAsStringXSD (aSBE.getRegistrationDate ()));
      m_aJsonGen.writeEnd ();
    }
    m_aJsonGen.writeEnd ();

    // Add all Document types (if wanted)
    if (INCLUDE_DOC_TYPES)
    {
      m_aJsonGen.writeStartArray ("docTypes");
      for (final IDocumentTypeIdentifier aDocTypeID : aEntities.getFirstOrNull ().documentTypeIDs ())
      {
        m_aJsonGen.writeStartObject ().write ("scheme", aDocTypeID.getScheme ()).write ("value", aDocTypeID.getValue ());
        final NiceNameEntry aNiceName = NiceNameManager.getDocTypeNiceName (aDocTypeID.getURIEncoded ());
        if (aNiceName == null)
          m_aJsonGen.write ("nonStandard", true);
        else
        {
          m_aJsonGen.write ("displayName", aNiceName.getName ());
          // New in JSON v2: use "state" instead of "deprecated"
          m_aJsonGen.write ("state", aNiceName.getState ().getID ());
        }
        m_aJsonGen.writeEnd ();
      }
      m_aJsonGen.writeEnd ();
    }

    m_aJsonGen.writeEnd ();
  }

  public void onEnd () throws Exception
  {
    m_aJsonGen.writeEnd ().writeEnd ();
    m_aJsonGen.close ();
    m_aWriter.close ();
  }
}
