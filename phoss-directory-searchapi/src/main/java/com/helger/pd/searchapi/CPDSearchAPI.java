/*
 * Copyright (C) 2019-2025 Philip Helger (www.helger.com)
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

import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.CodingStyleguideUnaware;
import com.helger.annotation.style.PresentForCodeCoverage;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.io.resource.ClassPathResource;

import jakarta.annotation.Nonnull;

/**
 * Contains all the constants for PD Search API handling.
 *
 * @author Philip Helger
 */
@Immutable
public final class CPDSearchAPI
{
  @Nonnull
  private static ClassLoader _getCL ()
  {
    return CPDSearchAPI.class.getClassLoader ();
  }

  /**
   * XML Schema resources for Result List v1.
   */
  public static final String RESULT_LIST_V1_XSD_PATH = "/schemas/directory-search-result-list-v1.xsd";

  /**
   * XML Schema resources for Result List v1.
   */
  @CodingStyleguideUnaware
  public static final List <ClassPathResource> RESULT_LIST_V1_XSDS = new CommonsArrayList <> (new ClassPathResource (RESULT_LIST_V1_XSD_PATH,
                                                                                                                     _getCL ())).getAsUnmodifiable ();

  @PresentForCodeCoverage
  private static final CPDSearchAPI INSTANCE = new CPDSearchAPI ();

  private CPDSearchAPI ()
  {}
}
