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
package com.helger.pd.indexer.searchindex;

import java.io.IOException;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.style.IsSPIInterface;
import com.helger.base.id.IHasID;

/**
 * SPI interface to provide an implementation of {@link IPDIndex}. The search index to be used is
 * selected via the configuration property <code>searchindex.type</code> which must match the ID of
 * exactly one registered provider.
 *
 * @author Philip Helger
 * @since 0.16.0
 */
@IsSPIInterface
public interface IPDIndexProviderSPI extends IHasID <String>
{
  /**
   * Create a new search index. This method is only called once per application startup and only for
   * the provider that was selected via the configuration.
   *
   * @return The created search index. May not be <code>null</code>.
   * @throws IOException
   *         On index error
   */
  @NonNull
  IPDIndex createIndex () throws IOException;
}
