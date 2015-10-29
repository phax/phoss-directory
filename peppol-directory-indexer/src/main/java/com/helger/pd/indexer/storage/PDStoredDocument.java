/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.datetime.PDTFactory;
import com.helger.peppol.identifier.doctype.IPeppolDocumentTypeIdentifier;
import com.helger.peppol.identifier.doctype.SimpleDocumentTypeIdentifier;

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
  private final List <SimpleDocumentTypeIdentifier> m_aDocumentTypeIDs = new ArrayList <> ();
  private String m_sCountryCode;
  private String m_sName;
  private String m_sGeoInfo;
  private final List <PDStoredIdentifier> m_aIdentifiers = new ArrayList <> ();
  private String m_sFreeText;
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

  public void setFreeText (@Nullable final String sFreeText)
  {
    m_sFreeText = sFreeText;
  }

  @Nonnull
  public String getFreeText ()
  {
    return m_sFreeText;
  }

  public boolean hasFreeText ()
  {
    return StringHelper.hasText (m_sFreeText);
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
                                       .append ("Name", m_sName)
                                       .append ("GeoInfo", m_sGeoInfo)
                                       .append ("Identifiers", m_aIdentifiers)
                                       .append ("FreeText", m_sFreeText)
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
    ret.setName (aDoc.get (CPDStorage.FIELD_NAME));
    ret.setGeoInfo (aDoc.get (CPDStorage.FIELD_GEOINFO));
    final String [] aIDTypes = aDoc.getValues (CPDStorage.FIELD_IDENTIFIER_TYPE);
    final String [] aIDValues = aDoc.getValues (CPDStorage.FIELD_IDENTIFIER);
    if (aIDTypes.length != aIDValues.length)
      throw new IllegalStateException ("Different number of identifier types and values");
    for (int i = 0; i < aIDTypes.length; ++i)
      ret.addIdentifier (new PDStoredIdentifier (aIDTypes[i], aIDValues[i]));
    {
      final IndexableField aFieldMetadata = aDoc.getField (CPDStorage.FIELD_METADATA_CREATIONDT);
      final PDDocumentMetaData aMetaData = new PDDocumentMetaData (PDTFactory.createDateTimeFromMillis (aFieldMetadata.numericValue ()
                                                                                                                        .longValue ())
                                                                               .toLocalDateTime (),
                                                                     aDoc.get (CPDStorage.FIELD_METADATA_OWNERID),
                                                                     aDoc.get (CPDStorage.FIELD_METADATA_REQUESTING_HOST));
      ret.setMetaData (aMetaData);
    }
    ret.setFreeText (aDoc.get (CPDStorage.FIELD_FREETEXT));
    ret.setDeleted (aDoc.getField (CPDStorage.FIELD_DELETED) != null);
    return ret;
  }
}
