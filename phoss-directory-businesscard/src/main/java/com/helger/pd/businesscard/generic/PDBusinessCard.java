/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
import com.helger.commons.string.ToStringGenerator;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

/**
 * Generic business card.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class PDBusinessCard implements Serializable, ICloneable <PDBusinessCard>
{
  private PDIdentifier m_aParticipantIdentifier;
  private ICommonsList <PDBusinessEntity> m_aEntities = new CommonsArrayList <> ();

  public PDBusinessCard ()
  {}

  /**
   * Gets the value of the participantIdentifier property.
   *
   * @return possible object is {@link PDIdentifier }
   */
  @Nullable
  public PDIdentifier getParticipantIdentifier ()
  {
    return m_aParticipantIdentifier;
  }

  /**
   * Sets the value of the participantIdentifier property.
   *
   * @param aParticipantIdentifier
   *        allowed object is {@link PDIdentifier }
   * @return this for chaining
   */
  @Nonnull
  public PDBusinessCard setParticipantIdentifier (@Nullable final PDIdentifier aParticipantIdentifier)
  {
    m_aParticipantIdentifier = aParticipantIdentifier;
    return this;
  }

  /**
   * @return Mutable list of business entities.
   */
  @Nonnull
  @ReturnsMutableObject
  public ICommonsList <PDBusinessEntity> businessEntities ()
  {
    return m_aEntities;
  }

  /**
   * This method clones all values from <code>this</code> to the passed object.
   * All data in the parameter object is overwritten!
   *
   * @param ret
   *        The target object to clone to. May not be <code>null</code>.
   */
  public void cloneTo (@Nonnull final PDBusinessCard ret)
  {
    ret.m_aParticipantIdentifier = m_aParticipantIdentifier;
    ret.m_aEntities = new CommonsArrayList <> (m_aEntities, PDBusinessEntity::getClone);
  }

  @Override
  @Nonnull
  @ReturnsMutableCopy
  public PDBusinessCard getClone ()
  {
    final PDBusinessCard ret = new PDBusinessCard ();
    cloneTo (ret);
    return ret;
  }

  @Nonnull
  public IMicroElement getAsMicroXML (@Nullable final String sNamespaceURI,
                                      @Nonnull @Nonempty final String sElementName)
  {
    final IMicroElement ret = new MicroElement (sNamespaceURI, sElementName);
    ret.appendChild (m_aParticipantIdentifier.getAsMicroXML (sNamespaceURI, "participant"));
    for (final PDBusinessEntity aEntity : m_aEntities)
      ret.appendChild (aEntity.getAsMicroXML (sNamespaceURI, "entity"));
    return ret;
  }

  @Nonnull
  public IJsonObject getAsJson ()
  {
    final IJsonObject ret = new JsonObject ();
    ret.add ("participant", m_aParticipantIdentifier.getAsJson ());
    ret.add ("entity", new JsonArray ().addAllMapped (m_aEntities, PDBusinessEntity::getAsJson));
    return ret;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;

    final PDBusinessCard rhs = ((PDBusinessCard) o);
    return EqualsHelper.equals (m_aParticipantIdentifier, rhs.m_aParticipantIdentifier) &&
           EqualsHelper.equals (m_aEntities, rhs.m_aEntities);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aParticipantIdentifier).append (m_aEntities).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ParticipantIdentifier", m_aParticipantIdentifier)
                                       .append ("Entities", m_aEntities)
                                       .getToString ();
  }
}
