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

/**
 * Determines if the value of an index field is split into single terms before it is indexed, or if
 * it is indexed as a single term.
 *
 * @author Philip Helger
 */
public enum EPDIndexFieldTokenize
{
  /** Split the value into terms before indexing */
  TOKENIZE,
  /** Index the value as a single term */
  NO_TOKENIZE;

  public boolean isTokenize ()
  {
    return this == TOKENIZE;
  }
}
