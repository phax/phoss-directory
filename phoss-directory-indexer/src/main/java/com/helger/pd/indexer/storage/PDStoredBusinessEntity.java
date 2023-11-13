/*
 * Copyright (C) 2015-2023 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.storage;

import java.time.LocalDate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.apache.lucene.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.datetime.PDTWebDateHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.json.IJson;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.pd.indexer.storage.field.PDField;
import com.helger.peppol.businesscard.generic.PDBusinessCard;
import com.helger.peppol.businesscard.generic.PDBusinessEntity;
import com.helger.peppol.businesscard.generic.PDIdentifier;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

/**
 * This class represents a document stored in the Lucene index but with a nicer
 * API to not work on a field basis. It contains the data at a certain point of
 * time and this might not necessarily be the most current data. Modifications
 * to this object have no impact on the underlying Lucene document. This is a
 * like a temporary "view" on a Lucene document at a single point of time.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public final class PDStoredBusinessEntity
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDStoredBusinessEntity.class);

  private IParticipantIdentifier m_aParticipantID;
  // Retrieved from SMP
  private final ICommonsList <PDStoredMLName> m_aNames = new CommonsArrayList <> ();
  private String m_sCountryCode;
  private String m_sGeoInfo;
  private final ICommonsList <PDStoredIdentifier> m_aIdentifiers = new CommonsArrayList <> ();
  private final ICommonsList <String> m_aWebsiteURIs = new CommonsArrayList <> ();
  private final ICommonsList <PDStoredContact> m_aContacts = new CommonsArrayList <> ();
  private String m_sAdditionalInformation;
  private LocalDate m_aRegistrationDate;
  private final ICommonsList <IDocumentTypeIdentifier> m_aDocumentTypeIDs = new CommonsArrayList <> ();
  // Status information from PD
  private PDStoredMetaData m_aMetaData;

  protected PDStoredBusinessEntity ()
  {}

  @Nonnull
  public IParticipantIdentifier getParticipantID ()
  {
    return m_aParticipantID;
  }

  void setParticipantID (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    m_aParticipantID = aParticipantID;
  }

  @Nonnull
  @ReturnsMutableObject
  public ICommonsList <PDStoredMLName> names ()
  {
    return m_aNames;
  }

  @Nullable
  public String getCountryCode ()
  {
    return m_sCountryCode;
  }

  public boolean hasCountryCode ()
  {
    return StringHelper.hasText (m_sCountryCode);
  }

  void setCountryCode (@Nullable final String sCountryCode)
  {
    m_sCountryCode = sCountryCode;
  }

  @Nullable
  public String getGeoInfo ()
  {
    return m_sGeoInfo;
  }

  public boolean hasGeoInfo ()
  {
    return StringHelper.hasText (m_sGeoInfo);
  }

  void setGeoInfo (@Nullable final String sGeoInfo)
  {
    m_sGeoInfo = sGeoInfo;
  }

  @Nonnull
  @ReturnsMutableObject
  public ICommonsList <PDStoredIdentifier> identifiers ()
  {
    return m_aIdentifiers;
  }

  @Nonnull
  @ReturnsMutableObject
  public ICommonsList <String> websiteURIs ()
  {
    return m_aWebsiteURIs;
  }

  @Nonnull
  @ReturnsMutableObject
  public ICommonsList <PDStoredContact> contacts ()
  {
    return m_aContacts;
  }

  @Nonnull
  public String getAdditionalInformation ()
  {
    return m_sAdditionalInformation;
  }

  public boolean hasAdditionalInformation ()
  {
    return StringHelper.hasText (m_sAdditionalInformation);
  }

  void setAdditionalInformation (@Nullable final String sAdditionalInformation)
  {
    m_sAdditionalInformation = sAdditionalInformation;
  }

  @Nullable
  public LocalDate getRegistrationDate ()
  {
    return m_aRegistrationDate;
  }

  public boolean hasRegistrationDate ()
  {
    return m_aRegistrationDate != null;
  }

  void setRegistrationDate (@Nullable final LocalDate aRegistrationDate)
  {
    m_aRegistrationDate = aRegistrationDate;
  }

  @Nonnull
  @ReturnsMutableObject
  public ICommonsList <IDocumentTypeIdentifier> documentTypeIDs ()
  {
    return m_aDocumentTypeIDs;
  }

  @Nonnull
  public PDStoredMetaData getMetaData ()
  {
    return m_aMetaData;
  }

  void setMetaData (@Nonnull final PDStoredMetaData aMetaData)
  {
    ValueEnforcer.notNull (aMetaData, "MetaData");
    m_aMetaData = aMetaData;
  }

  /**
   * @return Parts of this {@link PDStoredBusinessEntity} as a
   *         {@link PDBusinessEntity}.
   */
  @Nonnull
  @ReturnsMutableCopy
  public PDBusinessEntity getAsBusinessEntity ()
  {
    // We have a single entity
    final PDBusinessEntity ret = new PDBusinessEntity ();
    ret.names ().setAllMapped (m_aNames, PDStoredMLName::getAsGenericObject);
    ret.setCountryCode (m_sCountryCode);
    ret.setGeoInfo (m_sGeoInfo);
    ret.identifiers ().setAllMapped (m_aIdentifiers, PDStoredIdentifier::getAsGenericObject);
    ret.websiteURIs ().setAll (m_aWebsiteURIs);
    ret.contacts ().setAllMapped (m_aContacts, PDStoredContact::getAsGenericObject);
    ret.setAdditionalInfo (m_sAdditionalInformation);
    ret.setRegistrationDate (m_aRegistrationDate);
    return ret;
  }

  /**
   * @return This {@link PDStoredBusinessEntity} as a {@link PDBusinessCard}.
   */
  @Nonnull
  @ReturnsMutableCopy
  public PDBusinessCard getAsBusinessCard ()
  {
    final PDBusinessCard ret = new PDBusinessCard ();
    ret.setParticipantIdentifier (new PDIdentifier (m_aParticipantID.getScheme (), m_aParticipantID.getValue ()));
    // We have a single entity
    ret.businessEntities ().add (getAsBusinessEntity ());
    return ret;
  }

  /**
   * Create the REST search response XML element.
   *
   * @param aDocs
   *        All the documents that have the same participant ID. May neither be
   *        <code>null</code> nor empty.
   * @return The micro element
   */
  @Nonnull
  public static IMicroElement getAsSearchResultMicroElement (@Nonnull @Nonempty final ICommonsList <PDStoredBusinessEntity> aDocs)
  {
    ValueEnforcer.notEmptyNoNullValue (aDocs, "Docs");

    final PDStoredBusinessEntity aFirst = aDocs.getFirst ();

    final IMicroElement aMatch = new MicroElement ("match");
    aMatch.appendElement ("participantID")
          .setAttribute ("scheme", aFirst.m_aParticipantID.getScheme ())
          .appendText (aFirst.m_aParticipantID.getValue ());

    // Add all document type IDs
    for (final IDocumentTypeIdentifier aDocTypeID : aFirst.m_aDocumentTypeIDs)
    {
      aMatch.appendElement ("docTypeID").setAttribute ("scheme", aDocTypeID.getScheme ()).appendText (aDocTypeID.getValue ());
    }

    // Add all entities
    for (final PDStoredBusinessEntity aDoc : aDocs)
    {
      final IMicroElement aEntity = aMatch.appendElement ("entity");

      for (final PDStoredMLName aName : aDoc.m_aNames)
        aEntity.appendElement ("name").setAttribute ("language", aName.getLanguageCode ()).appendText (aName.getName ());

      aEntity.appendElement ("countryCode").appendText (aDoc.m_sCountryCode);

      if (StringHelper.hasText (aDoc.m_sGeoInfo))
        aEntity.appendElement ("geoInfo").appendText (aDoc.m_sGeoInfo);

      for (final PDStoredIdentifier aID : aDoc.m_aIdentifiers)
        aEntity.appendElement ("identifier").setAttribute ("scheme", aID.getScheme ()).appendText (aID.getValue ());

      for (final String sWebsite : aDoc.m_aWebsiteURIs)
        aEntity.appendElement ("website").appendText (sWebsite);

      for (final PDStoredContact aContact : aDoc.m_aContacts)
        aEntity.appendElement ("contact")
               .setAttribute ("type", aContact.getType ())
               .setAttribute ("name", aContact.getName ())
               .setAttribute ("phone", aContact.getPhone ())
               .setAttribute ("email", aContact.getEmail ());

      if (StringHelper.hasText (aDoc.m_sAdditionalInformation))
        aEntity.appendElement ("additionalInfo").appendText (aDoc.m_sAdditionalInformation);
      if (aDoc.m_aRegistrationDate != null)
        aEntity.appendElement ("regDate").appendText (PDTWebDateHelper.getAsStringXSD (aDoc.m_aRegistrationDate));
    }

    return aMatch;
  }

  @Nonnull
  private static IJson _getIDAsJson (@Nullable final String sScheme, @Nullable final String sValue)
  {
    return new JsonObject ().add ("scheme", sScheme).add ("value", sValue);
  }

  @Nonnull
  private static IJson _getMLNameAsJson (@Nullable final String sName, @Nullable final String sLanguage)
  {
    return new JsonObject ().add ("name", sName).addIfNotNull ("language", sLanguage);
  }

  @Nonnull
  public static IJsonObject getAsSearchResultJsonObject (@Nonnull @Nonempty final ICommonsList <PDStoredBusinessEntity> aDocs)
  {
    ValueEnforcer.notEmptyNoNullValue (aDocs, "Docs");

    final PDStoredBusinessEntity aFirst = aDocs.getFirst ();

    final IJsonObject ret = new JsonObject ();
    ret.addJson ("participantID", _getIDAsJson (aFirst.m_aParticipantID.getScheme (), aFirst.m_aParticipantID.getValue ()));

    // Add the items retrieved from SMP as well
    final IJsonArray aDocTypes = new JsonArray ();
    for (final IDocumentTypeIdentifier aDocTypeID : aFirst.m_aDocumentTypeIDs)
      aDocTypes.add (_getIDAsJson (aDocTypeID.getScheme (), aDocTypeID.getValue ()));
    if (aDocTypes.isNotEmpty ())
      ret.addJson ("docTypes", aDocTypes);

    final IJsonArray aEntities = new JsonArray ();
    for (final PDStoredBusinessEntity aDoc : aDocs)
    {
      final IJsonObject aEntity = new JsonObject ();

      // Multilingual names
      final IJsonArray aMLNames = new JsonArray ();
      for (final PDStoredMLName aName : aDoc.m_aNames)
        aMLNames.add (_getMLNameAsJson (aName.getName (), aName.getLanguageCode ()));
      if (aMLNames.isNotEmpty ())
        aEntity.addJson ("name", aMLNames);

      aEntity.add ("countryCode", aDoc.m_sCountryCode);

      if (StringHelper.hasText (aDoc.m_sGeoInfo))
        aEntity.add ("geoInfo", aDoc.m_sGeoInfo);

      final IJsonArray aIDs = new JsonArray ();
      for (final PDStoredIdentifier aID : aDoc.m_aIdentifiers)
        aIDs.add (_getIDAsJson (aID.getScheme (), aID.getValue ()));
      if (aIDs.isNotEmpty ())
        aEntity.addJson ("identifiers", aIDs);

      final IJsonArray aWebsites = new JsonArray ();
      for (final String sWebsite : aDoc.m_aWebsiteURIs)
        aWebsites.add (sWebsite);
      if (aWebsites.isNotEmpty ())
        aEntity.addJson ("websites", aWebsites);

      final IJsonArray aContacts = new JsonArray ();
      for (final PDStoredContact aContact : aDoc.m_aContacts)
        aContacts.add (new JsonObject ().addIfNotNull ("type", aContact.getType ())
                                        .addIfNotNull ("name", aContact.getName ())
                                        .addIfNotNull ("phone", aContact.getPhone ())
                                        .addIfNotNull ("email", aContact.getEmail ()));
      if (aContacts.isNotEmpty ())
        aEntity.addJson ("contacts", aContacts);

      if (StringHelper.hasText (aDoc.m_sAdditionalInformation))
        aEntity.add ("additionalInfo", aDoc.m_sAdditionalInformation);
      if (aDoc.m_aRegistrationDate != null)
        aEntity.add ("regDate", PDTWebDateHelper.getAsStringXSD (aDoc.m_aRegistrationDate));

      aEntities.add (aEntity);
    }
    ret.addJson ("entities", aEntities);
    return ret;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ParticipantID", m_aParticipantID)
                                       .append ("DocumentTypeIDs", m_aDocumentTypeIDs)
                                       .append ("CountryCode", m_sCountryCode)
                                       .append ("RegistrationDate", m_aRegistrationDate)
                                       .append ("Names", m_aNames)
                                       .append ("GeoInfo", m_sGeoInfo)
                                       .append ("Identifiers", m_aIdentifiers)
                                       .append ("WebsiteURIs", m_aWebsiteURIs)
                                       .append ("Contacts", m_aContacts)
                                       .append ("AdditionalInformation", m_sAdditionalInformation)
                                       .append ("MetaData", m_aMetaData)
                                       .getToString ();
  }

  /**
   * Convert a stored Lucene {@link Document} to a
   * {@link PDStoredBusinessEntity}. This method resolves all Lucene fields to
   * Java fields.
   *
   * @param aDoc
   *        Source Lucene document. May not be <code>null</code>.
   * @return The new {@link PDStoredBusinessEntity}.
   */
  @Nonnull
  @ReturnsMutableCopy
  public static PDStoredBusinessEntity create (@Nonnull final Document aDoc)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Creating PDStoredDocument from " + aDoc);

    final PDStoredBusinessEntity ret = new PDStoredBusinessEntity ();

    ret.setParticipantID (PDField.PARTICIPANT_ID.getDocValue (aDoc));

    for (final IDocumentTypeIdentifier aDocTypeID : PDField.DOCTYPE_ID.getDocValues (aDoc))
      if (aDocTypeID != null)
        ret.documentTypeIDs ().add (aDocTypeID);

    ret.setCountryCode (PDField.COUNTRY_CODE.getDocValue (aDoc));

    ret.setRegistrationDate (PDTWebDateHelper.getLocalDateFromXSD (PDField.REGISTRATION_DATE.getDocValue (aDoc)));

    {
      final String sSingleName = PDField.NAME.getDocValue (aDoc);
      if (sSingleName != null)
      {
        // No language
        ret.names ().add (new PDStoredMLName (sSingleName));
      }
      else
      {
        // Multilingual name
        final ICommonsList <String> aMLNames = PDField.ML_NAME.getDocValues (aDoc);
        final ICommonsList <String> aMLLanguages = PDField.ML_LANGUAGE.getDocValues (aDoc);
        if (aMLNames.size () != aMLLanguages.size ())
          throw new IllegalStateException ("Different number of ML names and languages");
        for (int i = 0; i < aMLNames.size (); ++i)
        {
          String sLang = aMLLanguages.get (i);
          if ("".equals (sLang))
          {
            // Work around internal error
            sLang = null;
          }
          ret.names ().add (new PDStoredMLName (aMLNames.get (i), sLang));
        }
      }
    }

    ret.setGeoInfo (PDField.GEO_INFO.getDocValue (aDoc));

    {
      final ICommonsList <String> aIDTypes = PDField.IDENTIFIER_SCHEME.getDocValues (aDoc);
      final ICommonsList <String> aIDValues = PDField.IDENTIFIER_VALUE.getDocValues (aDoc);
      if (aIDTypes.size () != aIDValues.size ())
        throw new IllegalStateException ("Different number of identifier types and values");
      for (int i = 0; i < aIDTypes.size (); ++i)
        ret.identifiers ().add (new PDStoredIdentifier (aIDTypes.get (i), aIDValues.get (i)));
    }

    for (final String sWebSite : PDField.WEBSITE_URI.getDocValues (aDoc))
      ret.websiteURIs ().add (sWebSite);

    {
      final ICommonsList <String> aBCTypes = PDField.CONTACT_TYPE.getDocValues (aDoc);
      final ICommonsList <String> aBCName = PDField.CONTACT_NAME.getDocValues (aDoc);
      final ICommonsList <String> aBCPhone = PDField.CONTACT_PHONE.getDocValues (aDoc);
      final ICommonsList <String> aBCEmail = PDField.CONTACT_EMAIL.getDocValues (aDoc);
      if (aBCTypes.size () != aBCName.size ())
        throw new IllegalStateException ("Different number of business contact types and names");
      if (aBCTypes.size () != aBCPhone.size ())
        throw new IllegalStateException ("Different number of business contact types and phones");
      if (aBCTypes.size () != aBCEmail.size ())
        throw new IllegalStateException ("Different number of business contact types and emails");
      for (int i = 0; i < aBCTypes.size (); ++i)
        ret.contacts ().add (new PDStoredContact (aBCTypes.get (i), aBCName.get (i), aBCPhone.get (i), aBCEmail.get (i)));
    }

    {
      final PDStoredMetaData aMetaData = new PDStoredMetaData (PDField.METADATA_CREATIONDT.getDocValue (aDoc),
                                                               PDField.METADATA_OWNERID.getDocValue (aDoc),
                                                               PDField.METADATA_REQUESTING_HOST.getDocValue (aDoc));
      ret.setMetaData (aMetaData);
    }
    ret.setAdditionalInformation (PDField.ADDITIONAL_INFO.getDocValue (aDoc));
    return ret;
  }
}
