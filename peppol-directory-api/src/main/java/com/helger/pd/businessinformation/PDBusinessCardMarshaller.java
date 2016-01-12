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
package com.helger.pd.businessinformation;

import java.util.List;

import javax.annotation.Nonnull;
import javax.xml.bind.JAXBElement;

import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.jaxb.AbstractJAXBMarshaller;
import com.helger.pd.businesscard.ObjectFactory;
import com.helger.pd.businesscard.PDBusinessCardType;

/**
 * This is the reader and writer for {@link PDBusinessCardType} documents. This
 * class may be derived to override protected methods from
 * {@link AbstractJAXBMarshaller}.
 *
 * @author Philip Helger
 */
public class PDBusinessCardMarshaller extends AbstractJAXBMarshaller <PDBusinessCardType>
{
  /** The namespace URI of the BusinessInformation element */
  public static final String BUSINESS_INFORMATION_NS_URI = ObjectFactory._BusinessCard_QNAME.getNamespaceURI ();

  /** XSD resources */
  public static final List <? extends IReadableResource> BUSINESS_INFORMATION_XSDS = CollectionHelper.newUnmodifiableList (new ClassPathResource ("/schemas/peppol-directory-business-card-20160112.xsd"));

  /**
   * Constructor
   */
  public PDBusinessCardMarshaller ()
  {
    super (PDBusinessCardType.class, BUSINESS_INFORMATION_XSDS);
  }

  @Override
  @Nonnull
  protected final JAXBElement <PDBusinessCardType> wrapObject (final PDBusinessCardType aCodeListDocument)
  {
    return new ObjectFactory ().createBusinessCard (aCodeListDocument);
  }
}
