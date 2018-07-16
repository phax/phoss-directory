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
package com.helger.pd.businesscard.generic;

import java.io.Serializable;
import java.time.LocalDate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.lang.ICloneable;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

/**
 * Generic business entity.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class PDBusinessEntity implements Serializable, ICloneable <PDBusinessEntity>
{
  private final ICommonsList <PDName> m_aNames = new CommonsArrayList <> ();
  private String m_sCountryCode;
  private String m_sGeoInfo;
  private final ICommonsList <PDIdentifier> m_aIDs = new CommonsArrayList <> ();
  private final ICommonsList <String> m_aWebsiteURIs = new CommonsArrayList <> ();
  private final ICommonsList <PDContact> m_aContacts = new CommonsArrayList <> ();
  private String m_sAdditionalInfo;
  private LocalDate m_aRegistrationDate;

  public PDBusinessEntity ()
  {}

  public PDBusinessEntity (@Nullable final ICommonsList <PDName> aNames,
                           @Nullable final String sCountryCode,
                           @Nullable final String sGeoInfo,
                           @Nullable final ICommonsList <PDIdentifier> aIDs,
                           @Nullable final ICommonsList <String> aWebsiteURIs,
                           @Nullable final ICommonsList <PDContact> aContacts,
                           @Nullable final String sAdditionalInfo,
                           @Nullable final LocalDate aRegDate)
  {
    names ().setAll (aNames);
    setCountryCode (sCountryCode);
    setGeoInfo (sGeoInfo);
    identifiers ().setAll (aIDs);
    websiteURIs ().setAll (aWebsiteURIs);
    contacts ().setAllMapped (aContacts, PDContact::getClone);
    setAdditionalInfo (sAdditionalInfo);
    setRegistrationDate (aRegDate);
  }

  /**
   * @return All names of the entity. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableObject
  public final ICommonsList <PDName> names ()
  {
    return m_aNames;
  }

  /**
   * @return The country code. Should not be <code>null</code>.
   */
  @Nullable
  public final String getCountryCode ()
  {
    return m_sCountryCode;
  }

  public final boolean hasCountryCode ()
  {
    return StringHelper.hasText (m_sCountryCode);
  }

  /**
   * Sets the value of the countryCode property.
   *
   * @param sCountryCode
   *        The country code to use. Should not be <code>null</code>.
   */
  public final void setCountryCode (@Nullable final String sCountryCode)
  {
    m_sCountryCode = sCountryCode;
  }

  /**
   * @return The geographical information. May be <code>null</code>.
   */
  @Nullable
  public final String getGeoInfo ()
  {
    return m_sGeoInfo;
  }

  public final boolean hasGeoInfo ()
  {
    return StringHelper.hasText (m_sGeoInfo);
  }

  /**
   * @param sGeoInfo
   *        Geographical information. May be <code>null</code>.
   */
  public final void setGeoInfo (@Nullable final String sGeoInfo)
  {
    m_sGeoInfo = sGeoInfo;
  }

  /**
   * @return Identifier lists. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableObject
  public final ICommonsList <PDIdentifier> identifiers ()
  {
    return m_aIDs;
  }

  /**
   * @return All Website URIs of the entity. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableObject
  public final ICommonsList <String> websiteURIs ()
  {
    return m_aWebsiteURIs;
  }

  /**
   * @return Mutable list of all contacts. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableObject
  public final ICommonsList <PDContact> contacts ()
  {
    return m_aContacts;
  }

  /**
   * @return The optional additional information. May be <code>null</code>.
   */
  @Nullable
  public final String getAdditionalInfo ()
  {
    return m_sAdditionalInfo;
  }

  public final boolean hasAdditionalInfo ()
  {
    return StringHelper.hasText (m_sAdditionalInfo);
  }

  /**
   * Set the additional information / free text.
   *
   * @param sAdditionalInfo
   *        Additional information to be used (free text). May be
   *        <code>null</code>.
   */
  public final void setAdditionalInfo (@Nullable final String sAdditionalInfo)
  {
    m_sAdditionalInfo = sAdditionalInfo;
  }

  /**
   * @return The optional registration date. May be <code>null</code>.
   */
  @Nullable
  public final LocalDate getRegistrationDate ()
  {
    return m_aRegistrationDate;
  }

  public final boolean hasRegistrationDate ()
  {
    return m_aRegistrationDate != null;
  }

  /**
   * Sets the value of the registration date property.
   *
   * @param aRegDate
   *        The registration date. May be <code>null</code>.
   */
  public final void setRegistrationDate (@Nullable final LocalDate aRegDate)
  {
    m_aRegistrationDate = aRegDate;
  }

  /**
   * This method clones all values from <code>this</code> to the passed object.
   * All data in the parameter object is overwritten!
   *
   * @param ret
   *        The target object to clone to. May not be <code>null</code>.
   */
  public void cloneTo (@Nonnull final PDBusinessEntity ret)
  {
    ret.m_aNames.setAll (m_aNames);
    ret.m_sCountryCode = m_sCountryCode;
    ret.m_sGeoInfo = m_sGeoInfo;
    ret.m_aIDs.setAll (m_aIDs);
    ret.m_aWebsiteURIs.setAll (m_aWebsiteURIs);
    ret.m_aContacts.setAllMapped (m_aContacts, PDContact::getClone);
    ret.m_sAdditionalInfo = m_sAdditionalInfo;
    // Identifier are immutable
    ret.m_aRegistrationDate = m_aRegistrationDate;
  }

  @Nonnull
  @ReturnsMutableCopy
  public PDBusinessEntity getClone ()
  {
    final PDBusinessEntity ret = new PDBusinessEntity ();
    cloneTo (ret);
    return ret;
  }

  @Nonnull
  public IMicroElement getAsMicroXML (@Nullable final String sNamespaceURI,
                                      @Nonnull @Nonempty final String sElementName)
  {
    final IMicroElement ret = new MicroElement (sNamespaceURI, sElementName);
    for (final PDName aName : m_aNames)
      ret.appendChild (aName.getAsMicroXML (sNamespaceURI, "name"));
    ret.setAttribute ("countrycode", m_sCountryCode);
    if (hasGeoInfo ())
      ret.appendElement (sNamespaceURI, "geoinfo").appendText (m_sGeoInfo);
    for (final PDIdentifier aID : m_aIDs)
      ret.appendChild (aID.getAsMicroXML (sNamespaceURI, "id"));
    for (final String sWebsiteURI : m_aWebsiteURIs)
      ret.appendElement (sNamespaceURI, "website").appendText (sWebsiteURI);
    for (final PDContact aContact : m_aContacts)
      ret.appendChild (aContact.getAsMicroXML (sNamespaceURI, "contact"));
    if (hasAdditionalInfo ())
      ret.appendElement (sNamespaceURI, "additionalinfo").appendText (m_sAdditionalInfo);
    return ret;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;

    final PDBusinessEntity rhs = (PDBusinessEntity) o;
    return EqualsHelper.equals (m_aNames, rhs.m_aNames) &&
           EqualsHelper.equals (m_sCountryCode, rhs.m_sCountryCode) &&
           EqualsHelper.equals (m_sGeoInfo, rhs.m_sGeoInfo) &&
           EqualsHelper.equals (m_aIDs, rhs.m_aIDs) &&
           EqualsHelper.equals (m_aWebsiteURIs, rhs.m_aWebsiteURIs) &&
           EqualsHelper.equals (m_aContacts, rhs.m_aContacts) &&
           EqualsHelper.equals (m_sAdditionalInfo, rhs.m_sAdditionalInfo) &&
           EqualsHelper.equals (m_aRegistrationDate, rhs.m_aRegistrationDate);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aNames)
                                       .append (m_sCountryCode)
                                       .append (m_sGeoInfo)
                                       .append (m_aIDs)
                                       .append (m_aWebsiteURIs)
                                       .append (m_aContacts)
                                       .append (m_sAdditionalInfo)
                                       .append (m_aRegistrationDate)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("names", m_aNames)
                                       .append ("countryCode", m_sCountryCode)
                                       .append ("geographicalInformation", m_sGeoInfo)
                                       .append ("identifier", m_aIDs)
                                       .append ("websiteURI", m_aWebsiteURIs)
                                       .append ("contact", m_aContacts)
                                       .append ("additionalInformation", m_sAdditionalInfo)
                                       .append ("registrationDate", m_aRegistrationDate)
                                       .getToString ();
  }
}
