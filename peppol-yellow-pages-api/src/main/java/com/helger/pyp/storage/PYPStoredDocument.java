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
package com.helger.pyp.storage;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.apache.lucene.document.Document;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.string.ToStringGenerator;
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
public class PYPStoredDocument
{
  private String m_sParticipantID;
  private final List <SimpleDocumentTypeIdentifier> m_aDocumentTypeIDs = new ArrayList <> ();
  private String m_sOwnerID;
  private String m_sCountryCode;
  private String m_sName;
  private String m_sGeoInfo;
  private final List <PYPStoredIdentifier> m_aIdentifiers = new ArrayList <> ();
  private String m_sFreeText;
  private boolean m_bDeleted;

  protected PYPStoredDocument ()
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

  public void setOwnerID (@Nonnull @Nonempty final String sOwnerID)
  {
    ValueEnforcer.notEmpty (sOwnerID, "OwnerID");
    m_sOwnerID = sOwnerID;
  }

  @Nonnull
  @Nonempty
  public String getOwnerID ()
  {
    return m_sOwnerID;
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

  public void setName (@Nullable final String sName)
  {
    m_sName = sName;
  }

  @Nullable
  public String getName ()
  {
    return m_sName;
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

  public void addIdentifier (@Nonnull final PYPStoredIdentifier aIdentifier)
  {
    ValueEnforcer.notNull (aIdentifier, "Identifier");
    m_aIdentifiers.add (aIdentifier);
  }

  @Nonnull
  @ReturnsMutableCopy
  public List <PYPStoredIdentifier> getAllIdentifiers ()
  {
    return CollectionHelper.newList (m_aIdentifiers);
  }

  @Nonnegative
  public int getIdentifierCount ()
  {
    return m_aIdentifiers.size ();
  }

  @Nullable
  public PYPStoredIdentifier getIdentifierAtIndex (@Nonnegative final int nIndex)
  {
    return CollectionHelper.getSafe (m_aIdentifiers, nIndex);
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
                                       .append ("OwnerID", m_sOwnerID)
                                       .append ("CountryCode", m_sCountryCode)
                                       .append ("Name", m_sName)
                                       .append ("GeoInfo", m_sGeoInfo)
                                       .append ("Identifiers", m_aIdentifiers)
                                       .append ("FreeText", m_sFreeText)
                                       .append ("Deleted", m_bDeleted)
                                       .toString ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public static PYPStoredDocument create (@Nonnull final Document aDoc)
  {
    final PYPStoredDocument ret = new PYPStoredDocument ();
    ret.setParticipantID (aDoc.get (CPYPStorage.FIELD_PARTICIPANTID));
    for (final String sDocTypeID : aDoc.getValues (CPYPStorage.FIELD_DOCUMENT_TYPE_ID))
      ret.addDocumentTypeID (SimpleDocumentTypeIdentifier.createFromURIPart (sDocTypeID));
    ret.setOwnerID (aDoc.get (CPYPStorage.FIELD_OWNERID));
    ret.setCountryCode (aDoc.get (CPYPStorage.FIELD_COUNTRY_CODE));
    ret.setName (aDoc.get (CPYPStorage.FIELD_NAME));
    ret.setGeoInfo (aDoc.get (CPYPStorage.FIELD_GEOINFO));
    final String [] aIDTypes = aDoc.getValues (CPYPStorage.FIELD_IDENTIFIER_TYPE);
    final String [] aIDValues = aDoc.getValues (CPYPStorage.FIELD_IDENTIFIER);
    if (aIDTypes.length != aIDValues.length)
      throw new IllegalStateException ("Different number of identifier types and values");
    for (int i = 0; i < aIDTypes.length; ++i)
      ret.addIdentifier (new PYPStoredIdentifier (aIDTypes[i], aIDValues[i]));
    ret.setFreeText (aDoc.get (CPYPStorage.FIELD_FREETEXT));
    ret.setDeleted (aDoc.getField (CPYPStorage.FIELD_DELETED) != null);
    return ret;
  }
}
