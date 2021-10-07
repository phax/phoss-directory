/*
 * Copyright (C) 2019-2021 Philip Helger (www.helger.com)
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

import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.validation.Schema;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.jaxb.builder.IJAXBDocumentType;
import com.helger.jaxb.builder.JAXBDocumentType;
import com.helger.pd.searchapi.v1.ResultListType;

/**
 * Enumeration with all available document types.
 *
 * @author Philip Helger
 */
public enum EPDSearchAPIDocumentType implements IJAXBDocumentType
{
  RESULT_LIST_V1 (ResultListType.class, CPDSearchAPI.RESULT_LIST_V1_XSDS, x -> "resultlist");

  private final JAXBDocumentType m_aDocType;

  private EPDSearchAPIDocumentType (@Nonnull final Class <?> aClass,
                                    @Nonnull final List <ClassPathResource> aXSDs,
                                    @Nullable final Function <? super String, ? extends String> aTypeToElementNameMapper)
  {
    m_aDocType = new JAXBDocumentType (aClass, aXSDs, aTypeToElementNameMapper);
  }

  @Nonnull
  public Class <?> getImplementationClass ()
  {
    return m_aDocType.getImplementationClass ();
  }

  @Nonnull
  @Nonempty
  @ReturnsMutableCopy
  public ICommonsList <ClassPathResource> getAllXSDResources ()
  {
    return m_aDocType.getAllXSDResources ();
  }

  @Nonnull
  public String getNamespaceURI ()
  {
    return m_aDocType.getNamespaceURI ();
  }

  @Nonnull
  @Nonempty
  public String getLocalName ()
  {
    return m_aDocType.getLocalName ();
  }

  @Nonnull
  public Schema getSchema ()
  {
    return m_aDocType.getSchema ();
  }
}
