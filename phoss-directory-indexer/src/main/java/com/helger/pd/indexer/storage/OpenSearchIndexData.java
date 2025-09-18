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
  public static final String FIELD_PARTICIPANT = "participant";
  public static final String FIELD_ENTITY = "entity";
  public static final String FIELD_LANGUAGE = "language";
  public static final String FIELD_COUNTRYCODE = "countrycode";
  public static final String FIELD_GEOINFO = "geoinfo";
  public static final String FIELD_ID = "id";
  public static final String FIELD_WEBSITE = "website";
  public static final String FIELD_CONTACT = "contact";
  public static final String FIELD_TYPE = "type";
  public static final String FIELD_NAME = "name";
  public static final String FIELD_PHONENUMBER = "phonenumber";
  public static final String FIELD_EMAIL = "email";
  public static final String FIELD_ADDITIONALINFO = "additionalinfo";
  public static final String FIELD_REGDATE = "regdate";
  public static final String FIELD_DOCTYPES = "doctypes";
  public static final String FIELD_SCHEME = "scheme";
  public static final String FIELD_VALUE = "value";
  public static final String FIELD_METADATA = "metadata";
  public static final String FIELD_CREATIONDT = "creationdt";
  public static final String FIELD_OWNERID = "ownerid";
  public static final String FIELD_REQUESTING_HOST = "requestingHost";

  private final PDExtendedBusinessCard m_aExtBI;
  private final PDStoredMetaData m_aMetaData;

  OpenSearchIndexData (@Nonnull final PDExtendedBusinessCard aExtBI, @Nonnull final PDStoredMetaData aMetaData)
  {
    m_aExtBI = aExtBI;
    m_aMetaData = aMetaData;
  }

  @Nonnull
  PDStoredMetaData getMetaData ()
  {
    return m_aMetaData;
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
             .writeStartObject (FIELD_PARTICIPANT)
             .write (FIELD_SCHEME, aPI.getScheme ())
             .write (FIELD_VALUE, aPI.getValue ())
             .writeEnd ()
             .writeStartArray (FIELD_ENTITY);
    for (final PDBusinessEntity aEntity : m_aExtBI.getBusinessCard ().businessEntities ())
    {
      generator.writeStartObject ();
      generator.writeStartArray (FIELD_NAME);
      for (final PDName aName : aEntity.names ())
      {
        generator.writeStartObject ().write (FIELD_NAME, aName.getName ());
        if (!aName.hasNoLanguageCode ())
          generator.write (FIELD_LANGUAGE, aName.getLanguageCode ());
        generator.writeEnd ();
      }
      generator.writeEnd ().write (FIELD_COUNTRYCODE, aEntity.getCountryCode ());
      if (aEntity.hasGeoInfo ())
        generator.write (FIELD_GEOINFO, aEntity.getGeoInfo ());
      if (aEntity.identifiers ().isNotEmpty ())
      {
        generator.writeStartArray (FIELD_ID);
        for (final PDIdentifier aID : aEntity.identifiers ())
        {
          generator.writeStartObject ()
                   .write (FIELD_SCHEME, aID.getScheme ())
                   .write (FIELD_VALUE, aID.getValue ())
                   .writeEnd ();
        }
        generator.writeEnd ();
      }
      if (aEntity.websiteURIs ().isNotEmpty ())
      {
        generator.writeStartArray (FIELD_WEBSITE);
        for (final String s : aEntity.websiteURIs ())
          generator.write (s);
        generator.writeEnd ();
      }
      if (aEntity.contacts ().isNotEmpty ())
      {
        generator.writeStartArray (FIELD_CONTACT);
        for (final PDContact aContact : aEntity.contacts ())
        {
          generator.writeStartObject ();
          if (StringHelper.isNotEmpty (aContact.getType ()))
            generator.write (FIELD_TYPE, aContact.getType ());
          if (StringHelper.isNotEmpty (aContact.getName ()))
            generator.write (FIELD_NAME, aContact.getName ());
          if (StringHelper.isNotEmpty (aContact.getPhoneNumber ()))
            generator.write (FIELD_PHONENUMBER, aContact.getPhoneNumber ());
          if (StringHelper.isNotEmpty (aContact.getEmail ()))
            generator.write (FIELD_EMAIL, aContact.getEmail ());
          generator.writeEnd ();
        }
        generator.writeEnd ();
      }
      if (aEntity.hasAdditionalInfo ())
        generator.write (FIELD_ADDITIONALINFO, aEntity.getAdditionalInfo ());
      if (aEntity.hasRegistrationDate ())
        generator.write (FIELD_REGDATE, PDTWebDateHelper.getAsStringXSD (aEntity.getRegistrationDate ()));
      generator.writeEnd ();
    }
    generator.writeEnd ();
    generator.writeStartArray (FIELD_DOCTYPES);
    for (final IDocumentTypeIdentifier aID : m_aExtBI.getAllDocumentTypeIDs ())
    {
      generator.writeStartObject ()
               .write (FIELD_SCHEME, aID.getScheme ())
               .write (FIELD_VALUE, aID.getValue ())
               .writeEnd ();
    }
    generator.writeEnd ();
    generator.writeStartObject (FIELD_METADATA);
    generator.write (FIELD_CREATIONDT, PDTWebDateHelper.getAsStringXSD (m_aMetaData.getCreationDT ()));
    generator.write (FIELD_OWNERID, m_aMetaData.getOwnerID ());
    generator.write (FIELD_REQUESTING_HOST, m_aMetaData.getRequestingHost ());
    generator.writeEnd ();
    generator.writeEnd ();
  }
}
