package com.helger.pd.indexer.storage;

import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.json.JsonpSerializable;

import com.helger.base.string.StringHelper;
import com.helger.datetime.web.PDTWebDateHelper;
import com.helger.pd.indexer.businesscard.PDExtendedBusinessCard;
import com.helger.pd.indexer.storage.model.PDStoredMetaData;
import com.helger.peppol.businesscard.generic.PDBusinessEntity;
import com.helger.peppol.businesscard.generic.PDContact;
import com.helger.peppol.businesscard.generic.PDIdentifier;
import com.helger.peppol.businesscard.generic.PDName;
import com.helger.peppolid.CIdentifier;
import com.helger.peppolid.IDocumentTypeIdentifier;

import jakarta.annotation.Nonnull;
import jakarta.json.stream.JsonGenerator;

public final class OpenSearchIndexData implements JsonpSerializable
{
  private final PDExtendedBusinessCard m_aExtBI;
  final PDStoredMetaData m_aMetaData;

  OpenSearchIndexData (@Nonnull final PDExtendedBusinessCard aExtBI, @Nonnull final PDStoredMetaData aMetaData)
  {
    m_aExtBI = aExtBI;
    m_aMetaData = aMetaData;
  }

  @Nonnull
  public String getID ()
  {
    final PDIdentifier aPI = m_aExtBI.getBusinessCard ().getParticipantIdentifier ();
    return CIdentifier.getURIEncoded (aPI.getScheme (), aPI.getValue ());
  }

  public void serialize (final JsonGenerator generator, final JsonpMapper mapper)
  {
    final PDIdentifier aPI = m_aExtBI.getBusinessCard ().getParticipantIdentifier ();
    generator.writeStartObject ()
             .writeStartObject ("participant")
             .write ("scheme", aPI.getScheme ())
             .write ("value", aPI.getValue ())
             .writeEnd ()
             .writeStartArray ("entity");
    for (final PDBusinessEntity aEntity : m_aExtBI.getBusinessCard ().businessEntities ())
    {
      generator.writeStartObject ();
      generator.writeStartArray ("name");
      for (final PDName aName : aEntity.names ())
      {
        generator.writeStartObject ().write ("name", aName.getName ());
        if (!aName.hasNoLanguageCode ())
          generator.write ("language", aName.getLanguageCode ());
        generator.writeEnd ();
      }
      generator.writeEnd ().write ("countrycode", aEntity.getCountryCode ());
      if (aEntity.hasGeoInfo ())
        generator.write ("geoinfo", aEntity.getGeoInfo ());
      if (aEntity.identifiers ().isNotEmpty ())
      {
        generator.writeStartArray ("id");
        for (final PDIdentifier aID : aEntity.identifiers ())
        {
          generator.writeStartObject ()
                   .write ("scheme", aID.getScheme ())
                   .write ("value", aID.getValue ())
                   .writeEnd ();
        }
        generator.writeEnd ();
      }
      if (aEntity.websiteURIs ().isNotEmpty ())
      {
        generator.writeStartArray ("website");
        for (final String s : aEntity.websiteURIs ())
          generator.write (s);
        generator.writeEnd ();
      }
      if (aEntity.contacts ().isNotEmpty ())
      {
        generator.writeStartArray ("contact");
        for (final PDContact aContact : aEntity.contacts ())
        {
          generator.writeStartObject ();
          if (StringHelper.isNotEmpty (aContact.getType ()))
            generator.write ("type", aContact.getType ());
          if (StringHelper.isNotEmpty (aContact.getName ()))
            generator.write ("name", aContact.getName ());
          if (StringHelper.isNotEmpty (aContact.getPhoneNumber ()))
            generator.write ("phonenumber", aContact.getPhoneNumber ());
          if (StringHelper.isNotEmpty (aContact.getEmail ()))
            generator.write ("email", aContact.getEmail ());
          generator.writeEnd ();
        }
        generator.writeEnd ();
      }
      if (aEntity.hasAdditionalInfo ())
        generator.write ("additionalinfo", aEntity.getAdditionalInfo ());
      if (aEntity.hasRegistrationDate ())
        generator.write ("regdate", PDTWebDateHelper.getAsStringXSD (aEntity.getRegistrationDate ()));
      generator.writeEnd ();
    }
    generator.writeEnd ();
    generator.writeStartArray ("doctypes");
    for (final IDocumentTypeIdentifier aID : m_aExtBI.getAllDocumentTypeIDs ())
    {
      generator.writeStartObject ().write ("scheme", aID.getScheme ()).write ("value", aID.getValue ()).writeEnd ();
    }
    generator.writeEnd ();
    generator.writeStartObject ("metadata");
    generator.write ("creationdt", PDTWebDateHelper.getAsStringXSD (m_aMetaData.getCreationDT ()));
    generator.write ("ownerid", m_aMetaData.getOwnerID ());
    generator.write ("requestingHost", m_aMetaData.getRequestingHost ());
    generator.writeEnd ();
    generator.writeEnd ();
  }
}