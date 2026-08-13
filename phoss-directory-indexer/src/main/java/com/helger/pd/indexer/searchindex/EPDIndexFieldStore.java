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
 * Determines if the original value of an index field is stored in the index or not. Only stored
 * values can be read back from a search result document.
 *
 * @author Philip Helger
 */
public enum EPDIndexFieldStore
{
  /** Store the original value in the index */
  YES,
  /** Do not store the original value in the index */
  NO;

  public boolean isStored ()
  {
    return this == YES;
  }
}
