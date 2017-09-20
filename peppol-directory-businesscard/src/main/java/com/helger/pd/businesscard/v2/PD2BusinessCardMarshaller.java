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
package com.helger.pd.businesscard.v2;

import java.util.List;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.CodingStyleguideUnaware;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.jaxb.GenericJAXBMarshaller;
import com.helger.pd.businesscard.v1.PD1APIHelper;
import com.helger.pd.businesscard.v1.PD1BusinessCardType;
import com.helger.pd.businesscard.v1.PD1BusinessEntityType;
import com.helger.pd.businesscard.v1.PD1IdentifierType;

/**
 * This is the reader and writer for {@link PD2BusinessCardType} documents. This
 * class may be derived to override protected methods from
 * {@link GenericJAXBMarshaller}.
 *
 * @author Philip Helger
 */
public class PD2BusinessCardMarshaller extends GenericJAXBMarshaller <PD2BusinessCardType>
{
  @Nonnull
  private static ClassLoader _getCL ()
  {
    return PD2BusinessCardMarshaller.class.getClassLoader ();
  }

  /** The namespace URI of the BusinessInformation element */
  public static final String BUSINESS_INFORMATION_NS_URI = ObjectFactory._BusinessCard_QNAME.getNamespaceURI ();

  /** XSD resources */
  @CodingStyleguideUnaware
  public static final List <ClassPathResource> BUSINESS_CARD_XSDS = new CommonsArrayList <> (new ClassPathResource ("/schemas/peppol-directory-business-card-20161123.xsd",
                                                                                                                    _getCL ())).getAsUnmodifiable ();

  /**
   * Constructor
   */
  public PD2BusinessCardMarshaller ()
  {
    super (PD2BusinessCardType.class, BUSINESS_CARD_XSDS, x -> new ObjectFactory ().createBusinessCard (x));
  }

  @Nonnull
  private static PD1IdentifierType _getAsID1 (@Nonnull final PD2IdentifierType aID2)
  {
    return PD1APIHelper.createIdentifier (aID2.getScheme (), aID2.getValue ());
  }

  /**
   * Convert a V2 BusinessCard to a V1 Business Card. This is straight forward,
   * as the V2 BC format is a total subset of the V1 BC format.
   *
   * @param aBusinessCard
   *        V2 (smaller) business card. May not be <code>null</code>.
   * @return The V1 (bigger) business card. Will never be <code>null</code>.
   */
  @Nonnull
  public static PD1BusinessCardType getAsV1 (@Nonnull final PD2BusinessCardType aBusinessCard)
  {
    ValueEnforcer.notNull (aBusinessCard, "BusinessCard");
    final PD1BusinessCardType ret = new PD1BusinessCardType ();
    ret.setParticipantIdentifier (_getAsID1 (aBusinessCard.getParticipantIdentifier ()));
    for (final PD2BusinessEntityType aEntity2 : aBusinessCard.getBusinessEntity ())
    {
      final PD1BusinessEntityType aEntity1 = new PD1BusinessEntityType ();
      aEntity1.setName (aEntity2.getName ());
      aEntity1.setCountryCode (aEntity2.getCountryCode ());
      aEntity1.setGeographicalInformation (aEntity2.getGeographicalInformation ());
      for (final PD2IdentifierType aID2 : aEntity2.getIdentifier ())
        aEntity1.addIdentifier (_getAsID1 (aID2));
      aEntity1.setRegistrationDate (aEntity2.getRegistrationDate ());
      ret.addBusinessEntity (aEntity1);
    }
    return ret;
  }
}
