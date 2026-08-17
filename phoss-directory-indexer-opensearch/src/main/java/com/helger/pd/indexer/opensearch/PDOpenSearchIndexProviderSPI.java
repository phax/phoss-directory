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
package com.helger.pd.indexer.opensearch;

import java.io.IOException;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.IsSPIImplementation;
import com.helger.pd.indexer.searchindex.IPDIndex;
import com.helger.pd.indexer.searchindex.IPDIndexProviderSPI;

/**
 * The {@link IPDIndexProviderSPI} implementation for AWS OpenSearch.
 *
 * @author Philip Helger
 * @since 0.16.0
 */
@IsSPIImplementation
public class PDOpenSearchIndexProviderSPI implements IPDIndexProviderSPI
{
  /** The ID of this search index type - to be used in the configuration */
  public static final String INDEX_TYPE_ID = "opensearch";

  @NonNull
  @Nonempty
  public String getID ()
  {
    return INDEX_TYPE_ID;
  }

  @NonNull
  public IPDIndex createIndex () throws IOException
  {
    return new PDOpenSearchIndex ();
  }
}
