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
package com.helger.pyp.businessinformation;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import com.helger.commons.annotation.PresentForCodeCoverage;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.IReadableResource;

/**
 * Constants for the handling of Business Information XML documents
 *
 * @author Philip Helger
 */
@Immutable
public final class CPYPBusinessInformation
{
  /** XSD resources */
  public static final List <? extends IReadableResource> BUSINESS_CARD_XSDS = CollectionHelper.newUnmodifiableList (new ClassPathResource ("/schemas/pyp-business-information-201505.xsd"));

  @PresentForCodeCoverage
  private static final CPYPBusinessInformation s_aInstance = new CPYPBusinessInformation ();

  private CPYPBusinessInformation ()
  {}
}
