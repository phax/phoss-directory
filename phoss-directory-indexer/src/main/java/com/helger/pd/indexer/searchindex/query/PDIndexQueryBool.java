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

import java.util.List;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;

/**
 * A query that combines an arbitrary number of other queries. Use the nested {@link Builder} class
 * to create instances of this class.
 *
 * @author Philip Helger
 */
@ThreadSafe
public class PDIndexQueryBool extends AbstractPDIndexQuery
{
  /**
   * A single clause of a {@link PDIndexQueryBool}.
   *
   * @author Philip Helger
   */
  @Immutable
  public static class Clause
  {
    private final IPDIndexQuery m_aQuery;
    private final EPDIndexQueryOccur m_eOccur;

    public Clause (@NonNull final IPDIndexQuery aQuery, @NonNull final EPDIndexQueryOccur eOccur)
    {
      m_aQuery = ValueEnforcer.notNull (aQuery, "Query");
      m_eOccur = ValueEnforcer.notNull (eOccur, "Occur");
    }

    @NonNull
    public IPDIndexQuery getQuery ()
    {
      return m_aQuery;
    }

    @NonNull
    public EPDIndexQueryOccur getOccur ()
    {
      return m_eOccur;
    }

    @Override
    public boolean equals (final Object o)
    {
      if (o == this)
        return true;
      if (o == null || !getClass ().equals (o.getClass ()))
        return false;
      final Clause rhs = (Clause) o;
      return m_aQuery.equals (rhs.m_aQuery) && m_eOccur.equals (rhs.m_eOccur);
    }

    @Override
    public int hashCode ()
    {
      return new HashCodeGenerator (this).append (m_aQuery).append (m_eOccur).getHashCode ();
    }

    @Override
    public String toString ()
    {
      return m_eOccur.getPrefix () + m_aQuery.toString ();
    }
  }

  /**
   * The builder for {@link PDIndexQueryBool} objects.
   *
   * @author Philip Helger
   */
  @NotThreadSafe
  public static class Builder
  {
    private final ICommonsList <Clause> m_aClauses = new CommonsArrayList <> ();

    public Builder ()
    {}

    @NonNull
    public Builder add (@NonNull final IPDIndexQuery aQuery, @NonNull final EPDIndexQueryOccur eOccur)
    {
      m_aClauses.add (new Clause (aQuery, eOccur));
      return this;
    }

    @NonNull
    public PDIndexQueryBool build ()
    {
      return new PDIndexQueryBool (m_aClauses);
    }
  }

  private final ICommonsList <Clause> m_aClauses;

  protected PDIndexQueryBool (@NonNull final List <Clause> aClauses)
  {
    ValueEnforcer.notNullNoNullValue (aClauses, "Clauses");
    m_aClauses = new CommonsArrayList <> (aClauses);
  }

  /**
   * @return A copy of all contained clauses in the order they were added. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <Clause> getAllClauses ()
  {
    return m_aClauses.getClone ();
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PDIndexQueryBool rhs = (PDIndexQueryBool) o;
    return m_aClauses.equals (rhs.m_aClauses);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aClauses).getHashCode ();
  }

  @Override
  public String toString ()
  {
    final StringBuilder aSB = new StringBuilder ("(");
    for (final Clause aClause : m_aClauses)
    {
      if (aSB.length () > 1)
        aSB.append (' ');
      aSB.append (aClause.toString ());
    }
    return aSB.append (')').toString ();
  }
}
