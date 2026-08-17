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

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.hashcode.HashCodeGenerator;

/**
 * Abstract base class for all queries that match a single value in a single field.
 *
 * @author Philip Helger
 * @since 0.16.0
 */
@ThreadSafe
public abstract class AbstractPDIndexQueryField extends AbstractPDIndexQuery
{
  private final String m_sFieldName;
  private final String m_sValue;

  protected AbstractPDIndexQueryField (@NonNull @Nonempty final String sFieldName, @NonNull final String sValue)
  {
    m_sFieldName = ValueEnforcer.notEmpty (sFieldName, "FieldName");
    m_sValue = ValueEnforcer.notNull (sValue, "Value");
  }

  /**
   * @return The name of the field to be queried. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public final String getFieldName ()
  {
    return m_sFieldName;
  }

  /**
   * @return The value to be searched for. Never <code>null</code> but maybe empty.
   */
  @NonNull
  public final String getValue ()
  {
    return m_sValue;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final AbstractPDIndexQueryField rhs = (AbstractPDIndexQueryField) o;
    return m_sFieldName.equals (rhs.m_sFieldName) && m_sValue.equals (rhs.m_sValue);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sFieldName).append (m_sValue).getHashCode ();
  }
}
