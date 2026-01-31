/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.businesscard;

import java.io.Serializable;
import java.util.function.Predicate;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.json.IHasJson;
import com.helger.json.IJson;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.peppol.businesscard.generic.PDBusinessCard;
import com.helger.peppol.businesscard.generic.PDIdentifier;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.simple.doctype.SimpleDocumentTypeIdentifier;

import jakarta.annotation.Nullable;

/**
 * This class encapsulates all the data to be added to the Lucene index. It consists of the main
 * {@link PDBusinessCard} object as retrieved from the SMP plus a list of all document types
 * supported by the respective service group.
 *
 * @author Philip Helger
 */
@Immutable
public class PDExtendedBusinessCard implements IHasJson, Serializable
{
  private final PDBusinessCard m_aBusinessCard;
  private final ICommonsList <IDocumentTypeIdentifier> m_aDocumentTypeIDs = new CommonsArrayList <> ();

  /**
   * Constructor with Business Card.
   *
   * @param aBusinessCard
   *        Business Card to use. May not be <code>null</code>.
   * @param aDocumentTypeIDs
   *        Document types supported. May be <code>null</code>.
   */
  public PDExtendedBusinessCard (@NonNull final PDBusinessCard aBusinessCard,
                                 @Nullable final Iterable <? extends IDocumentTypeIdentifier> aDocumentTypeIDs)
  {
    m_aBusinessCard = ValueEnforcer.notNull (aBusinessCard, "BusinessCard");
    if (aDocumentTypeIDs != null)
      for (final IDocumentTypeIdentifier aDocTypeID : aDocumentTypeIDs)
        if (aDocTypeID != null)
        {
          // Just enforce the same type, but no need to use the
          // IdentifierFactory
          m_aDocumentTypeIDs.add (new SimpleDocumentTypeIdentifier (aDocTypeID));
        }
  }

  /**
   * @return The mutable {@link PDBusinessCard} object as provided in the constructor (or the
   *         converted object). Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableObject
  public PDBusinessCard getBusinessCard ()
  {
    return m_aBusinessCard;
  }

  /**
   * @return A copy of the list of all contained document type IDs. Never <code>null</code> but
   *         maybe empty.
   */
  @NonNull
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

  @NonNull
  public IJsonObject getAsJson ()
  {
    final IJsonObject ret = new JsonObject ();
    ret.add ("businesscard", m_aBusinessCard.getAsJson ());
    ret.add ("doctypes",
             new JsonArray ().addAllMapped (m_aDocumentTypeIDs,
                                            x -> PDIdentifier.getAsJson (x.getScheme (), x.getValue ())));
    return ret;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("BusinessCard", m_aBusinessCard)
                                       .append ("DocTypeIDs", m_aDocumentTypeIDs)
                                       .getToString ();
  }

  @NonNull
  public static PDExtendedBusinessCard of (@NonNull final IJsonObject aJson)
  {
    final PDBusinessCard aBC = PDBusinessCard.of (aJson.getAsObject ("businesscard"));
    final ICommonsList <IDocumentTypeIdentifier> aDocTypes = CommonsArrayList.createFiltered (aJson.getAsArray ("doctypes"),
                                                                                              (Predicate <IJson>) IJson::isObject,
                                                                                              x -> new SimpleDocumentTypeIdentifier (x.getAsObject ()
                                                                                                                                      .getAsString ("scheme"),
                                                                                                                                     x.getAsObject ()
                                                                                                                                      .getAsString ("value")));
    return new PDExtendedBusinessCard (aBC, aDocTypes);
  }
}
