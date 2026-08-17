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
package com.helger.pd.indexer.searchindex.query;

import org.jspecify.annotations.NonNull;

/**
 * Determines how a single clause of a {@link PDIndexQueryBool} contributes to the overall result.
 *
 * @author Philip Helger
 * @since 0.16.0
 */
public enum EPDIndexQueryOccur
{
  /** The clause must match and it contributes to the score */
  MUST ("+"),
  /**
   * The clause should match. If a boolean query contains neither a MUST nor a FILTER clause, at
   * least one SHOULD clause must match.
   */
  SHOULD (""),
  /** The clause must match but it does not contribute to the score */
  FILTER ("#");

  private final String m_sPrefix;

  EPDIndexQueryOccur (@NonNull final String sPrefix)
  {
    m_sPrefix = sPrefix;
  }

  /**
   * @return The prefix used in the textual representation of a boolean query. Never
   *         <code>null</code> but maybe empty.
   */
  @NonNull
  public String getPrefix ()
  {
    return m_sPrefix;
  }
}
