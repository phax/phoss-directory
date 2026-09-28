/*
 * Copyright (C) 2019-2026 Philip Helger (www.helger.com)
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

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.CodingStyleguideUnaware;
import com.helger.annotation.style.PresentForCodeCoverage;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.io.resource.ClassPathResource;

/**
 * Contains all the constants for PD Search API handling.
 *
 * @author Philip Helger
 */
@Immutable
public final class CPDSearchAPI
{
  @NonNull
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

  /**
   * The fixed part of the URL to the PD search API version 1.0, relative to the PD host URI. Always
   * ends with a trailing slash.
   *
   * @since 0.19.1
   */
  public static final String PATH_SEARCH_10 = "search/1.0/";

  /**
   * The output format requesting XML. This is the default of the server, and the only format for
   * which an XML Schema and a JAXB binding exist.
   *
   * @since 0.19.1
   */
  public static final String OUTPUT_FORMAT_XML = "xml";

  /**
   * The output format requesting JSON.
   *
   * @since 0.19.1
   */
  public static final String OUTPUT_FORMAT_JSON = "json";

  /**
   * Name of the query parameter holding the 0-based index of the result page to be returned.
   *
   * @since 0.19.1
   */
  public static final String QUERY_PARAM_RESULT_PAGE_INDEX = "resultPageIndex";

  /**
   * Name of the query parameter holding the number of results per page.
   *
   * @since 0.19.1
   */
  public static final String QUERY_PARAM_RESULT_PAGE_COUNT = "resultPageCount";

  /**
   * Name of the query parameter requesting a human readable, indented response.
   *
   * @since 0.19.1
   */
  public static final String QUERY_PARAM_BEAUTIFY = "beautify";

  /**
   * The default result page index used by the server if none is provided.
   *
   * @since 0.19.1
   */
  public static final int DEFAULT_RESULT_PAGE_INDEX = 0;

  /**
   * The default number of results per page used by the server if none is provided.
   *
   * @since 0.19.1
   */
  public static final int DEFAULT_RESULT_PAGE_COUNT = 20;

  /**
   * The maximum result index the server accepts. A query for a first or a last result index beyond
   * this value is rejected with HTTP 400, so no more than this many results can be retrieved for a
   * single query, no matter how many matches exist.
   *
   * @since 0.19.1
   */
  public static final int MAX_RESULTS = 1_000;

  @PresentForCodeCoverage
  private static final CPDSearchAPI INSTANCE = new CPDSearchAPI ();

  private CPDSearchAPI ()
  {}
}
