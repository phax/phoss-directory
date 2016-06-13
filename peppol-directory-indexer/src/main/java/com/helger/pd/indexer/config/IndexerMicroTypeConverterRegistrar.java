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
package com.helger.pd.indexer.config;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.pd.indexer.index.IndexerWorkItem;
import com.helger.pd.indexer.index.IndexerWorkItemMicroTypeConverter;
import com.helger.pd.indexer.reindex.ReIndexWorkItem;
import com.helger.pd.indexer.reindex.ReIndexWorkItemMicroTypeConverter;
import com.helger.xml.microdom.convert.IMicroTypeConverterRegistrarSPI;
import com.helger.xml.microdom.convert.IMicroTypeConverterRegistry;

/**
 * Implementation of {@link IMicroTypeConverterRegistrarSPI} for indexer types
 *
 * @author Philip Helger
 */
@Immutable
@IsSPIImplementation
public final class IndexerMicroTypeConverterRegistrar implements IMicroTypeConverterRegistrarSPI
{
  public void registerMicroTypeConverter (@Nonnull final IMicroTypeConverterRegistry aRegistry)
  {
    aRegistry.registerMicroElementTypeConverter (IndexerWorkItem.class, new IndexerWorkItemMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (ReIndexWorkItem.class, new ReIndexWorkItemMicroTypeConverter ());
  }
}
