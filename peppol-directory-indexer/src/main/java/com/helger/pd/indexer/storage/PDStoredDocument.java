/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.joda.time.LocalDate;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.datetime.PDTFactory;
import com.helger.peppol.identifier.doctype.IPeppolDocumentTypeIdentifier;
import com.helger.peppol.identifier.doctype.SimpleDocumentTypeIdentifier;
import com.helger.web.datetime.PDTWebDateHelper;

/**
 * This class represents a document stored in the Lucene index but with a nicer
 * API to not work on a field basis. It contains the data at a certain point of
 * time and this might not necessarily be the most current data. Modifications
 * to this object have no impact on the underlying Lucene document!
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class PDStoredDocument
{
  private String m_sParticipantID;
  private final List <SimpleDocumentTypeIdentifier> m_aDocumentTypeIDs = new ArrayList<> ();
  private String m_sName;
  private String m_sCountryCode;
  private String m_sGeoInfo;
  private final List <PDStoredIdentifier> m_aIdentifiers = new ArrayList<> ();
  private final List <String> m_aWebsiteURIs = new ArrayList<> ();
  private final List <PDStoredContact> m_aContacts = new ArrayList<> ();
  private String m_sAdditionalInformation;
  private LocalDate m_aRegistrationDate;
  private PDDocumentMetaData m_aMetaData;
  private boolean m_bDeleted;

  protected PDStoredDocument ()
  {}

  public void setParticipantID (@Nonnull @Nonempty final String sParticipantID)
  {
    ValueEnforcer.notEmpty (sParticipantID, "ParticipantID");
    m_sParticipantID = sParticipantID;
  }

  @Nonnull
  @Nonempty
  public String getParticipantID ()
  {
    return m_sParticipantID;
  }

  public void addDocumentTypeID (@Nonnull final SimpleDocumentTypeIdentifier aDocumentTypeID)
  {
    ValueEnforcer.notNull (aDocumentTypeID, "DocumentTypeID");
    m_aDocumentTypeIDs.add (aDocumentTypeID);
  }

  @Nonnull
  @ReturnsMutableCopy
  public List <IPeppolDocumentTypeIdentifier> getAllDocumentTypeIDs ()
  {
    return CollectionHelper.newList (m_aDocumentTypeIDs);
  }

  @Nonnegative
  public int getDocumentTypeIDCount ()
  {
    return m_aDocumentTypeIDs.size ();
  }

  @Nullable
  public IPeppolDocumentTypeIdentifier getDocumentTypeIDAtIndex (@Nonnegative final int nIndex)
  {
    return CollectionHelper.getSafe (m_aDocumentTypeIDs, nIndex);
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
  public List <PDStoredIdentifier> getAllIdentifiers ()
  {
    return CollectionHelper.newList (m_aIdentifiers);
  }

  @Nonnegative
  public int getIdentifierCount ()
  {
    return m_aIdentifiers.size ();
  }

  @Nullable
  public PDStoredIdentifier getIdentifierAtIndex (@Nonnegative final int nIndex)
  {
    return CollectionHelper.getSafe (m_aIdentifiers, nIndex);
  }

  public boolean hasAnyIdentifier ()
  {
    return CollectionHelper.isNotEmpty (m_aIdentifiers);
  }

  public void addWebsiteURI (@Nonnull @Nonempty final String sWebsiteURI)
  {
    ValueEnforcer.notEmpty (sWebsiteURI, "WebSite");
    m_aWebsiteURIs.add (sWebsiteURI);
  }

  @Nonnull
  @ReturnsMutableCopy
  public List <String> getAllWebsiteURIs ()
  {
    return CollectionHelper.newList (m_aWebsiteURIs);
  }

  @Nonnegative
  public int getWebsiteURICount ()
  {
    return m_aWebsiteURIs.size ();
  }

  @Nullable
  public String getWebsiteURIAtIndex (@Nonnegative final int nIndex)
  {
    return CollectionHelper.getSafe (m_aWebsiteURIs, nIndex);
  }

  public boolean hasAnyWebsiteURIs ()
  {
    return CollectionHelper.isNotEmpty (m_aWebsiteURIs);
  }

  public void addContact (@Nonnull final PDStoredContact aContact)
  {
    ValueEnforcer.notNull (aContact, "Contact");
    m_aContacts.add (aContact);
  }

  @Nonnull
  @ReturnsMutableCopy
  public List <PDStoredContact> getAllContacts ()
  {
    return CollectionHelper.newList (m_aContacts);
  }

  @Nonnegative
  public int getContactCount ()
  {
    return m_aContacts.size ();
  }

  @Nullable
  public PDStoredContact getContactAtIndex (@Nonnegative final int nIndex)
  {
    return CollectionHelper.getSafe (m_aContacts, nIndex);
  }

  public boolean hasAnyContact ()
  {
    return CollectionHelper.isNotEmpty (m_aContacts);
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

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ParticipantID", m_sParticipantID)
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
                                       .toString ();
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
    final PDStoredDocument ret = new PDStoredDocument ();

    ret.setParticipantID (aDoc.get (CPDStorage.FIELD_PARTICIPANTID));

    for (final String sDocTypeID : aDoc.getValues (CPDStorage.FIELD_DOCUMENT_TYPE_ID))
      ret.addDocumentTypeID (SimpleDocumentTypeIdentifier.createFromURIPart (sDocTypeID));

    ret.setCountryCode (aDoc.get (CPDStorage.FIELD_COUNTRY_CODE));

    ret.setRegistrationDate (PDTWebDateHelper.getLocalDateFromXSD (aDoc.get (CPDStorage.FIELD_REGISTRATION_DATE)));

    ret.setName (aDoc.get (CPDStorage.FIELD_NAME));

    ret.setGeoInfo (aDoc.get (CPDStorage.FIELD_GEOGRAPHICAL_INFORMATION));

    final String [] aIDTypes = aDoc.getValues (CPDStorage.FIELD_IDENTIFIER_SCHEME);
    final String [] aIDValues = aDoc.getValues (CPDStorage.FIELD_IDENTIFIER);
    if (aIDTypes.length != aIDValues.length)
      throw new IllegalStateException ("Different number of identifier types and values");
    for (int i = 0; i < aIDTypes.length; ++i)
      ret.addIdentifier (new PDStoredIdentifier (aIDTypes[i], aIDValues[i]));

    final String [] aWebSites = aDoc.getValues (CPDStorage.FIELD_WEBSITEURI);
    for (final String sWebSite : aWebSites)
      ret.addWebsiteURI (sWebSite);

    final String [] aBCDescription = aDoc.getValues (CPDStorage.FIELD_CONTACT_TYPE);
    final String [] aBCName = aDoc.getValues (CPDStorage.FIELD_CONTACT_NAME);
    final String [] aBCPhone = aDoc.getValues (CPDStorage.FIELD_CONTACT_PHONE);
    final String [] aBCEmail = aDoc.getValues (CPDStorage.FIELD_CONTACT_EMAIL);
    if (aBCDescription.length != aBCName.length)
      throw new IllegalStateException ("Different number of business contact descriptions and names");
    if (aBCDescription.length != aBCPhone.length)
      throw new IllegalStateException ("Different number of business contact descriptions and phones");
    if (aBCDescription.length != aBCEmail.length)
      throw new IllegalStateException ("Different number of business contact descriptions and emails");
    for (int i = 0; i < aBCDescription.length; ++i)
      ret.addContact (new PDStoredContact (aBCDescription[i], aBCName[i], aBCPhone[i], aBCEmail[i]));

    {
      final IndexableField aFieldMetadata = aDoc.getField (CPDStorage.FIELD_METADATA_CREATIONDT);
      final PDDocumentMetaData aMetaData = new PDDocumentMetaData (PDTFactory.createDateTimeFromMillis (aFieldMetadata.numericValue ()
                                                                                                                      .longValue ())
                                                                             .toLocalDateTime (),
                                                                   aDoc.get (CPDStorage.FIELD_METADATA_OWNERID),
                                                                   aDoc.get (CPDStorage.FIELD_METADATA_REQUESTING_HOST));
      ret.setMetaData (aMetaData);
    }
    ret.setAdditionalInformation (aDoc.get (CPDStorage.FIELD_ADDITIONAL_INFORMATION));
    ret.setDeleted (aDoc.getField (CPDStorage.FIELD_DELETED) != null);
    return ret;
  }
}
