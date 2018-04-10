/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.apache.lucene.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
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
import com.helger.pd.businesscard.generic.PDBusinessCard;
import com.helger.pd.businesscard.generic.PDBusinessEntity;
import com.helger.pd.businesscard.generic.PDIdentifier;
import com.helger.pd.indexer.storage.field.PDField;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
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
public class PDStoredDocument
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PDStoredDocument.class);

  private IParticipantIdentifier m_aParticipantID;
  // Retrieved from SMP
  private String m_sName;
  private String m_sCountryCode;
  private String m_sGeoInfo;
  private final ICommonsList <PDStoredIdentifier> m_aIdentifiers = new CommonsArrayList <> ();
  private final ICommonsList <String> m_aWebsiteURIs = new CommonsArrayList <> ();
  private final ICommonsList <PDStoredContact> m_aContacts = new CommonsArrayList <> ();
  private String m_sAdditionalInformation;
  private LocalDate m_aRegistrationDate;
  private final ICommonsList <IDocumentTypeIdentifier> m_aDocumentTypeIDs = new CommonsArrayList <> ();
  // Status information from PD
  private PDDocumentMetaData m_aMetaData;
  private boolean m_bDeleted;

  protected PDStoredDocument ()
  {}

  public void setParticipantID (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    m_aParticipantID = aParticipantID;
  }

  @Nonnull
  public IParticipantIdentifier getParticipantID ()
  {
    return m_aParticipantID;
  }

  public void setName (@Nullable final String sName)
  {
    m_sName = sName;
  }

  @Nullable
  public String getName ()
  {
    return m_sName;
  }

  public boolean hasName ()
  {
    return StringHelper.hasText (m_sName);
  }

  public void setCountryCode (@Nullable final String sCountryCode)
  {
    m_sCountryCode = sCountryCode;
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

  public void setGeoInfo (@Nullable final String sGeoInfo)
  {
    m_sGeoInfo = sGeoInfo;
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

  public void addIdentifier (@Nonnull final PDStoredIdentifier aIdentifier)
  {
    ValueEnforcer.notNull (aIdentifier, "Identifier");
    m_aIdentifiers.add (aIdentifier);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <PDStoredIdentifier> getAllIdentifiers ()
  {
    return m_aIdentifiers.getClone ();
  }

  @Nonnegative
  public int getIdentifierCount ()
  {
    return m_aIdentifiers.size ();
  }

  @Nullable
  public PDStoredIdentifier getIdentifierAtIndex (@Nonnegative final int nIndex)
  {
    return m_aIdentifiers.getAtIndex (nIndex);
  }

  public boolean hasAnyIdentifier ()
  {
    return m_aIdentifiers.isNotEmpty ();
  }

  public void addWebsiteURI (@Nonnull @Nonempty final String sWebsiteURI)
  {
    ValueEnforcer.notEmpty (sWebsiteURI, "WebSite");
    m_aWebsiteURIs.add (sWebsiteURI);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <String> getAllWebsiteURIs ()
  {
    return m_aWebsiteURIs.getClone ();
  }

  @Nonnegative
  public int getWebsiteURICount ()
  {
    return m_aWebsiteURIs.size ();
  }

  @Nullable
  public String getWebsiteURIAtIndex (@Nonnegative final int nIndex)
  {
    return m_aWebsiteURIs.getAtIndex (nIndex);
  }

  public boolean hasAnyWebsiteURIs ()
  {
    return m_aWebsiteURIs.isNotEmpty ();
  }

  public void addContact (@Nonnull final PDStoredContact aContact)
  {
    ValueEnforcer.notNull (aContact, "Contact");
    m_aContacts.add (aContact);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <PDStoredContact> getAllContacts ()
  {
    return m_aContacts.getClone ();
  }

  @Nonnegative
  public int getContactCount ()
  {
    return m_aContacts.size ();
  }

  @Nullable
  public PDStoredContact getContactAtIndex (@Nonnegative final int nIndex)
  {
    return m_aContacts.getAtIndex (nIndex);
  }

  public boolean hasAnyContact ()
  {
    return m_aContacts.isNotEmpty ();
  }

  public void setAdditionalInformation (@Nullable final String sAdditionalInformation)
  {
    m_sAdditionalInformation = sAdditionalInformation;
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

  public void setRegistrationDate (@Nullable final LocalDate aRegistrationDate)
  {
    m_aRegistrationDate = aRegistrationDate;
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

  public void addDocumentTypeID (@Nonnull final IDocumentTypeIdentifier aDocumentTypeID)
  {
    ValueEnforcer.notNull (aDocumentTypeID, "DocumentTypeID");
    m_aDocumentTypeIDs.add (aDocumentTypeID);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <? extends IDocumentTypeIdentifier> getAllDocumentTypeIDs ()
  {
    return m_aDocumentTypeIDs.getClone ();
  }

  @Nonnegative
  public int getDocumentTypeIDCount ()
  {
    return m_aDocumentTypeIDs.size ();
  }

  @Nullable
  public IDocumentTypeIdentifier getDocumentTypeIDAtIndex (@Nonnegative final int nIndex)
  {
    return m_aDocumentTypeIDs.getAtIndex (nIndex);
  }

  @Nonnull
  public PDDocumentMetaData getMetaData ()
  {
    return m_aMetaData;
  }

  public void setMetaData (@Nonnull final PDDocumentMetaData aMetaData)
  {
    ValueEnforcer.notNull (aMetaData, "MetaData");
    m_aMetaData = aMetaData;
  }

  public void setDeleted (final boolean bDeleted)
  {
    m_bDeleted = bDeleted;
  }

  public boolean isDeleted ()
  {
    return m_bDeleted;
  }

  /**
   * @return Parts of this {@link PDStoredDocument} as a
   *         {@link PDBusinessEntity}.
   */
  @Nonnull
  public PDBusinessEntity getAsBusinessEntity ()
  {
    // We have a single entity
    final PDBusinessEntity ret = new PDBusinessEntity ();
    ret.setName (m_sName);
    ret.setCountryCode (m_sCountryCode);
    ret.setGeoInfo (m_sGeoInfo);
    for (final PDStoredIdentifier aID : m_aIdentifiers)
      ret.identifiers ().add (aID.getAsGenericObject ());
    ret.websiteURIs ().addAll (m_aWebsiteURIs);
    for (final PDStoredContact aContact : m_aContacts)
      ret.contacts ().add (aContact.getAsGenericObject ());
    ret.setAdditionalInfo (m_sAdditionalInformation);
    ret.setRegistrationDate (m_aRegistrationDate);
    return ret;
  }

  /**
   * @return This {@link PDStoredDocument} as a {@link PDBusinessCard}.
   */
  @Nonnull
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
  public static IMicroElement getAsSearchResultMicroElement (@Nonnull @Nonempty final ICommonsList <PDStoredDocument> aDocs)
  {
    ValueEnforcer.notEmptyNoNullValue (aDocs, "Docs");

    final PDStoredDocument aFirst = aDocs.getFirst ();

    final IMicroElement aMatch = new MicroElement ("match");
    aMatch.appendElement ("participantID")
          .setAttribute ("scheme", aFirst.m_aParticipantID.getScheme ())
          .appendText (aFirst.m_aParticipantID.getValue ());

    // Add all document type IDs
    for (final IDocumentTypeIdentifier aDocTypeID : aFirst.m_aDocumentTypeIDs)
    {
      aMatch.appendElement ("docTypeID")
            .setAttribute ("scheme", aDocTypeID.getScheme ())
            .appendText (aDocTypeID.getValue ());
    }

    // Add all entities
    for (final PDStoredDocument aDoc : aDocs)
    {
      final IMicroElement aEntity = aMatch.appendElement ("entity");
      aEntity.appendElement ("name").appendText (aDoc.m_sName);
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

      // Usually only non-deleted elements are returned, so don't give an
      // indicator to the outside
      if (aDoc.m_bDeleted)
        aEntity.setAttribute ("deleted", true);
    }

    return aMatch;
  }

  @Nonnull
  private static IJson _getIDAsJson (@Nullable final String sScheme, @Nullable final String sValue)
  {
    return new JsonObject ().add ("scheme", sScheme).add ("value", sValue);
  }

  @Nonnull
  public static IJsonObject getAsSearchResultJsonObject (@Nonnull @Nonempty final ICommonsList <PDStoredDocument> aDocs)
  {
    ValueEnforcer.notEmptyNoNullValue (aDocs, "Docs");

    final PDStoredDocument aFirst = aDocs.getFirst ();

    final IJsonObject ret = new JsonObject ();
    ret.add ("participantID", _getIDAsJson (aFirst.m_aParticipantID.getScheme (), aFirst.m_aParticipantID.getValue ()));

    // Add the items retrieved from SMP as well
    final JsonArray aDocTypes = new JsonArray ();
    for (final IDocumentTypeIdentifier aDocTypeID : aFirst.m_aDocumentTypeIDs)
      aDocTypes.add (_getIDAsJson (aDocTypeID.getScheme (), aDocTypeID.getValue ()));
    if (aDocTypes.isNotEmpty ())
      ret.add ("docTypes", aDocTypes);

    final IJsonArray aEntities = new JsonArray ();
    for (final PDStoredDocument aDoc : aDocs)
    {
      final IJsonObject aEntity = new JsonObject ();
      aEntity.add ("name", aDoc.m_sName);
      aEntity.add ("countryCode", aDoc.m_sCountryCode);
      if (StringHelper.hasText (aDoc.m_sGeoInfo))
        aEntity.add ("geoInfo", aDoc.m_sGeoInfo);

      final JsonArray aIDs = new JsonArray ();
      for (final PDStoredIdentifier aID : aDoc.m_aIdentifiers)
        aIDs.add (_getIDAsJson (aID.getScheme (), aID.getValue ()));
      if (aIDs.isNotEmpty ())
        aEntity.add ("identifiers", aIDs);

      final JsonArray aWebsites = new JsonArray ();
      for (final String sWebsite : aDoc.m_aWebsiteURIs)
        aWebsites.add (sWebsite);
      if (aWebsites.isNotEmpty ())
        aEntity.add ("websites", aWebsites);

      final JsonArray aContacts = new JsonArray ();
      for (final PDStoredContact aContact : aDoc.m_aContacts)
        aContacts.add (new JsonObject ().addIfNotNull ("type", aContact.getType ())
                                        .addIfNotNull ("name", aContact.getName ())
                                        .addIfNotNull ("phone", aContact.getPhone ())
                                        .addIfNotNull ("email", aContact.getEmail ()));
      if (aContacts.isNotEmpty ())
        aEntity.add ("contacts", aContacts);

      if (StringHelper.hasText (aDoc.m_sAdditionalInformation))
        aEntity.add ("additionalInfo", aDoc.m_sAdditionalInformation);
      if (aDoc.m_aRegistrationDate != null)
        aEntity.add ("regDate", PDTWebDateHelper.getAsStringXSD (aDoc.m_aRegistrationDate));

      // Usually only non-deleted elements are returned, so don't give an
      // indicator to the outside
      if (aDoc.m_bDeleted)
        aEntity.add ("deleted", true);
      aEntities.add (aEntity);
    }
    ret.add ("entities", aEntities);
    return ret;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ParticipantID", m_aParticipantID)
                                       .append ("DocumentTypeIDs", m_aDocumentTypeIDs)
                                       .append ("CountryCode", m_sCountryCode)
                                       .append ("RegistrationDate", m_aRegistrationDate)
                                       .append ("Name", m_sName)
                                       .append ("GeoInfo", m_sGeoInfo)
                                       .append ("Identifiers", m_aIdentifiers)
                                       .append ("WebsiteURIs", m_aWebsiteURIs)
                                       .append ("Contacts", m_aContacts)
                                       .append ("AdditionalInformation", m_sAdditionalInformation)
                                       .append ("MetaData", m_aMetaData)
                                       .append ("Deleted", m_bDeleted)
                                       .getToString ();
  }

  /**
   * Convert a stored Lucene {@link Document} to a {@link PDStoredDocument}.
   * This method resolves all Lucene fields to Java fields.
   *
   * @param aDoc
   *        Source Lucene document. May not be <code>null</code>.
   * @return The new {@link PDStoredDocument}.
   */
  @Nonnull
  @ReturnsMutableCopy
  public static PDStoredDocument create (@Nonnull final Document aDoc)
  {
    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Creating PDStoredDocument from " + aDoc);

    final PDStoredDocument ret = new PDStoredDocument ();

    ret.setParticipantID (PDField.PARTICIPANT_ID.getDocValue (aDoc));

    for (final IDocumentTypeIdentifier aDocTypeID : PDField.DOCTYPE_ID.getDocValues (aDoc))
      ret.addDocumentTypeID (aDocTypeID);

    ret.setCountryCode (PDField.COUNTRY_CODE.getDocValue (aDoc));

    ret.setRegistrationDate (PDTWebDateHelper.getLocalDateFromXSD (PDField.REGISTRATION_DATE.getDocValue (aDoc)));

    ret.setName (PDField.NAME.getDocValue (aDoc));

    ret.setGeoInfo (PDField.GEO_INFO.getDocValue (aDoc));

    final ICommonsList <String> aIDTypes = PDField.IDENTIFIER_SCHEME.getDocValues (aDoc);
    final ICommonsList <String> aIDValues = PDField.IDENTIFIER_VALUE.getDocValues (aDoc);
    if (aIDTypes.size () != aIDValues.size ())
      throw new IllegalStateException ("Different number of identifier types and values");
    for (int i = 0; i < aIDTypes.size (); ++i)
      ret.addIdentifier (new PDStoredIdentifier (aIDTypes.get (i), aIDValues.get (i)));

    for (final String sWebSite : PDField.WEBSITE_URI.getDocValues (aDoc))
      ret.addWebsiteURI (sWebSite);

    final ICommonsList <String> aBCDescription = PDField.CONTACT_TYPE.getDocValues (aDoc);
    final ICommonsList <String> aBCName = PDField.CONTACT_NAME.getDocValues (aDoc);
    final ICommonsList <String> aBCPhone = PDField.CONTACT_PHONE.getDocValues (aDoc);
    final ICommonsList <String> aBCEmail = PDField.CONTACT_EMAIL.getDocValues (aDoc);
    if (aBCDescription.size () != aBCName.size ())
      throw new IllegalStateException ("Different number of business contact descriptions and names");
    if (aBCDescription.size () != aBCPhone.size ())
      throw new IllegalStateException ("Different number of business contact descriptions and phones");
    if (aBCDescription.size () != aBCEmail.size ())
      throw new IllegalStateException ("Different number of business contact descriptions and emails");
    for (int i = 0; i < aBCDescription.size (); ++i)
      ret.addContact (new PDStoredContact (aBCDescription.get (i),
                                           aBCName.get (i),
                                           aBCPhone.get (i),
                                           aBCEmail.get (i)));

    {
      final PDDocumentMetaData aMetaData = new PDDocumentMetaData (PDField.METADATA_CREATIONDT.getDocValue (aDoc),
                                                                   PDField.METADATA_OWNERID.getDocValue (aDoc),
                                                                   PDField.METADATA_REQUESTING_HOST.getDocValue (aDoc));
      ret.setMetaData (aMetaData);
    }
    ret.setAdditionalInformation (PDField.ADDITIONAL_INFO.getDocValue (aDoc));
    ret.setDeleted (aDoc.getField (CPDStorage.FIELD_DELETED) != null);
    return ret;
  }
}
