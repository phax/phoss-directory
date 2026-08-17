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
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.spi.ServiceLoaderHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;

/**
 * Factory to resolve the {@link IPDIndex} implementation to be used, based on the registered
 * {@link IPDIndexProviderSPI} implementations.
 *
 * @author Philip Helger
 * @since 0.16.0
 */
@Immutable
public final class PDIndexFactory
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDIndexFactory.class);

  private PDIndexFactory ()
  {}

  /**
   * @return The IDs of all registered search index providers, in the order they were resolved.
   *         Never <code>null</code> but maybe empty.
   */
  @NonNull
  public static ICommonsList <String> getAllIndexTypeIDs ()
  {
    final List <IPDIndexProviderSPI> aProviders = ServiceLoaderHelper.getAllSPIImplementations (IPDIndexProviderSPI.class);
    return new CommonsArrayList <> (aProviders, IPDIndexProviderSPI::getID);
  }

  /**
   * Create the search index of the provided type.
   *
   * @param sIndexType
   *        The ID of the {@link IPDIndexProviderSPI} to be used. May neither be <code>null</code>
   *        nor empty. Case insensitive matching is used.
   * @return The created search index. Never <code>null</code>.
   * @throws IOException
   *         On index error
   * @throws IllegalArgumentException
   *         If no provider with the provided ID is registered
   */
  @NonNull
  public static IPDIndex createIndex (@NonNull @Nonempty final String sIndexType) throws IOException
  {
    ValueEnforcer.notEmpty (sIndexType, "IndexType");

    final List <IPDIndexProviderSPI> aProviders = ServiceLoaderHelper.getAllSPIImplementations (IPDIndexProviderSPI.class);
    for (final IPDIndexProviderSPI aProvider : aProviders)
      if (aProvider.getID ().equalsIgnoreCase (sIndexType))
      {
        LOGGER.info ("Creating the search index of type '" +
                     aProvider.getID () +
                     "' using " +
                     aProvider.getClass ().getName ());
        return aProvider.createIndex ();
      }

    throw new IllegalArgumentException ("Failed to resolve the search index of type '" +
                                        sIndexType +
                                        "'. Available types are: " +
                                        getAllIndexTypeIDs ());
  }
}
