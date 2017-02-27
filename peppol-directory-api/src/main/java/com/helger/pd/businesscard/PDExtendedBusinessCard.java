/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.pd.businesscard;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.string.ToStringGenerator;
import com.helger.pd.businesscard.v1.PD1BusinessCardType;
import com.helger.pd.businesscard.v2.PD2BusinessCardMarshaller;
import com.helger.pd.businesscard.v2.PD2BusinessCardType;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.doctype.SimpleDocumentTypeIdentifier;

/**
 * This class encapsulates all the data to be added to the Lucene index. It
 * consists of the main {@link PD1BusinessCardType} object as retrieved from the
 * SMP plus a list of all document types supported by the respective service
 * group.
 *
 * @author Philip Helger
 */
@Immutable
public class PDExtendedBusinessCard
{
  private final PD1BusinessCardType m_aBusinessCard;
  private final ICommonsList <IDocumentTypeIdentifier> m_aDocumentTypeIDs = new CommonsArrayList<> ();

  public PDExtendedBusinessCard (@Nonnull final PD1BusinessCardType aBusinessCard,
                                 @Nullable final Iterable <? extends IDocumentTypeIdentifier> aDocumentTypeIDs)
  {
    m_aBusinessCard = ValueEnforcer.notNull (aBusinessCard, "BusinessCard");
    if (aDocumentTypeIDs != null)
      for (final IDocumentTypeIdentifier aDocTypeID : aDocumentTypeIDs)
        if (aDocTypeID != null)
          m_aDocumentTypeIDs.add (new SimpleDocumentTypeIdentifier (aDocTypeID));
  }

  public PDExtendedBusinessCard (@Nonnull final PD2BusinessCardType aBusinessCard,
                                 @Nullable final Iterable <? extends IDocumentTypeIdentifier> aDocumentTypeIDs)
  {
    this (PD2BusinessCardMarshaller.getAsV1 (aBusinessCard), aDocumentTypeIDs);
  }

  /**
   * @return The mutable {@link PD1BusinessCardType} object as provided in the
   *         constructor. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableObject ("design")
  public PD1BusinessCardType getBusinessCard ()
  {
    return m_aBusinessCard;
  }

  /**
   * @return A copy of the list of all contained document type IDs. Never
   *         <code>null</code> but maybe empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IDocumentTypeIdentifier> getAllDocumentTypeIDs ()
  {
    return m_aDocumentTypeIDs.getClone ();
  }

  /**
   * @return The number of contained document types. Always &ge; 0.
   */
  @Nonnegative
  public int getDocumentTypeCount ()
  {
    return m_aDocumentTypeIDs.size ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("BusinessCard", m_aBusinessCard)
                                       .append ("DocTypeIDs", m_aDocumentTypeIDs)
                                       .getToString ();
  }
}
