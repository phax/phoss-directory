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
package com.helger.pd.businesscard.v1;

import java.util.List;

import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.jaxb.AbstractJAXBMarshaller;
import com.helger.pd.businesscard.v1.ObjectFactory;
import com.helger.pd.businesscard.v1.PD1BusinessCardType;

/**
 * This is the reader and writer for {@link PD1BusinessCardType} documents. This
 * class may be derived to override protected methods from
 * {@link AbstractJAXBMarshaller}.
 *
 * @author Philip Helger
 */
public class PD1BusinessCardMarshaller extends AbstractJAXBMarshaller <PD1BusinessCardType>
{
  /** The namespace URI of the BusinessInformation element */
  public static final String BUSINESS_INFORMATION_NS_URI = ObjectFactory._BusinessCard_QNAME.getNamespaceURI ();

  /** XSD resources */
  public static final List <? extends IReadableResource> BUSINESS_CARD_XSDS = new CommonsArrayList <> (new ClassPathResource ("/schemas/peppol-directory-business-card-20160112.xsd")).getAsUnmodifiable ();

  /**
   * Constructor
   */
  public PD1BusinessCardMarshaller ()
  {
    super (PD1BusinessCardType.class, BUSINESS_CARD_XSDS, x -> new ObjectFactory ().createBusinessCard (x));
  }
}
