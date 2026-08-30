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
package com.helger.pd.publisher.app;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.CheckForSigned;
import com.helger.annotation.style.UsedViaReflection;
import com.helger.pd.indexer.searchindex.query.IPDIndexQuery;
import com.helger.web.scope.singleton.AbstractSessionWebSingleton;

import jakarta.annotation.Nullable;

public final class PDSessionSingleton extends AbstractSessionWebSingleton
{
  private IPDIndexQuery m_aLastQuery;
  private int m_nLastQueryMaxResultCount;

  @Deprecated (forRemoval = false)
  @UsedViaReflection
  public PDSessionSingleton ()
  {}

  @NonNull
  public static PDSessionSingleton getInstance ()
  {
    return getSessionSingleton (PDSessionSingleton.class);
  }

  @Nullable
  public IPDIndexQuery getLastQuery ()
  {
    return m_aLastQuery;
  }

  /**
   * @return The maximum number of results of the last query. Values &le; 0 mean all. Only
   *         meaningful if {@link #getLastQuery()} is not <code>null</code>.
   */
  @CheckForSigned
  public int getLastQueryMaxResultCount ()
  {
    return m_nLastQueryMaxResultCount;
  }

  /**
   * Remember the last executed query together with the maximum number of results it was executed
   * with, so that a follow-up export delivers exactly what was displayed.
   *
   * @param aLastQuery
   *        The last query. May be <code>null</code>.
   * @param nMaxResultCount
   *        The maximum number of results of the last query. Values &le; 0 mean all.
   */
  public void setLastQuery (@Nullable final IPDIndexQuery aLastQuery, @CheckForSigned final int nMaxResultCount)
  {
    m_aLastQuery = aLastQuery;
    m_nLastQueryMaxResultCount = nMaxResultCount;
  }
}
