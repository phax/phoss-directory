/*
 * Copyright (C) 2019-2024 Philip Helger (www.helger.com)
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

import com.helger.jaxb.builder.JAXBWriterBuilder;
import com.helger.pd.searchapi.v1.ResultListType;
import com.helger.xml.namespace.MapBasedNamespaceContext;

/**
 * A writer builder for BDE documents.
 *
 * @author Philip Helger
 * @param <JAXBTYPE>
 *        The BDE implementation class to be written
 * @deprecated Use {@link PDResultListMarshaller} instead
 */
@NotThreadSafe
@Deprecated (forRemoval = true, since = "0.12.1")
public class PDSearchAPIWriter <JAXBTYPE> extends JAXBWriterBuilder <JAXBTYPE, PDSearchAPIWriter <JAXBTYPE>>
{
  public PDSearchAPIWriter (@Nonnull final EPDSearchAPIDocumentType eDocType)
  {
    super (eDocType);

    // Create a special namespace context for the passed document type
    final MapBasedNamespaceContext aNSContext = new MapBasedNamespaceContext ();
    aNSContext.addDefaultNamespaceURI (m_aDocType.getNamespaceURI ());
    setNamespaceContext (aNSContext);
  }

  /**
   * Create a writer builder for ResultListType.
   *
   * @return The builder and never <code>null</code>
   */
  @Nonnull
  public static PDSearchAPIWriter <ResultListType> resultListV1 ()
  {
    return new PDSearchAPIWriter <> (EPDSearchAPIDocumentType.RESULT_LIST_V1);
  }
}
