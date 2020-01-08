/**
 * Copyright (C) 2019-2020 Philip Helger (www.helger.com)
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
package com.helger.pd.searchapi;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.jaxb.builder.JAXBValidationBuilder;
import com.helger.pd.searchapi.v1.ResultListType;

/**
 * A validator builder for BDE documents.
 *
 * @author Philip Helger
 * @param <JAXBTYPE>
 *        The BDE implementation class to be validated
 */
@NotThreadSafe
public class PDSearchAPIValidator <JAXBTYPE> extends JAXBValidationBuilder <JAXBTYPE, PDSearchAPIValidator <JAXBTYPE>>
{
  public PDSearchAPIValidator (@Nonnull final EPDSearchAPIDocumentType eDocType)
  {
    super (eDocType);
  }

  /**
   * Create a validator builder for ResultListType.
   *
   * @return The builder and never <code>null</code>
   */
  @Nonnull
  public static PDSearchAPIValidator <ResultListType> resultListV1 ()
  {
    return new PDSearchAPIValidator <> (EPDSearchAPIDocumentType.RESULT_LIST_V1);
  }
}
