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

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.hashcode.HashCodeGenerator;

/**
 * A query that matches all documents of the index.
 *
 * @author Philip Helger
 */
@Immutable
public final class PDIndexQueryMatchAll implements IPDIndexQuery
{
  /** The single instance of this query */
  public static final PDIndexQueryMatchAll INSTANCE = new PDIndexQueryMatchAll ();

  private PDIndexQueryMatchAll ()
  {}

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    return o != null && getClass ().equals (o.getClass ());
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return "*:*";
  }
}
