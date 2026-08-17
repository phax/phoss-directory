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

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;

import jakarta.annotation.Nullable;

/**
 * A single document of the search index. A document is an ordered list of {@link PDIndexField}
 * objects. The same field name may occur more than once - the order in which the fields of a
 * certain name were added is retained.
 *
 * @author Philip Helger
 * @since 0.16.0
 */
@NotThreadSafe
public class PDIndexDocument
{
  private final ICommonsList <PDIndexField> m_aFields;

  public PDIndexDocument ()
  {
    m_aFields = new CommonsArrayList <> ();
  }

  /**
   * Constructor for the case, that the number of fields to be added is already known.
   *
   * @param nFieldCount
   *        The number of fields this document will contain. Must be &ge; 0.
   */
  public PDIndexDocument (@Nonnegative final int nFieldCount)
  {
    m_aFields = new CommonsArrayList <> (nFieldCount);
  }

  /**
   * Add a new field at the end of this document.
   *
   * @param aField
   *        The field to be added. May not be <code>null</code>.
   * @return this for chaining
   */
  @NonNull
  public PDIndexDocument add (@NonNull final PDIndexField aField)
  {
    ValueEnforcer.notNull (aField, "Field");

    m_aFields.add (aField);
    return this;
  }

  /**
   * @return The mutable list of all contained fields, in the order they were added. Never
   *         <code>null</code>.
   */
  @NonNull
  @ReturnsMutableObject
  public ICommonsList <@NonNull PDIndexField> fields ()
  {
    return m_aFields;
  }

  /**
   * Get the first field with the provided name.
   *
   * @param sName
   *        The field name to search. May neither be <code>null</code> nor empty.
   * @return <code>null</code> if no such field is contained.
   */
  @Nullable
  public PDIndexField getFieldOfName (@NonNull @Nonempty final String sName)
  {
    ValueEnforcer.notEmpty (sName, "Name");

    return m_aFields.findFirst (x -> x.getName ().equals (sName));
  }

  /**
   * Get all fields with the provided name, in the order they were added.
   *
   * @param sName
   *        The field name to search. May neither be <code>null</code> nor empty.
   * @return Never <code>null</code> but maybe empty list.
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <PDIndexField> getAllFieldsOfName (@NonNull @Nonempty final String sName)
  {
    ValueEnforcer.notEmpty (sName, "Name");

    return m_aFields.getAll (x -> x.getName ().equals (sName));
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Fields", m_aFields).getToString ();
  }
}
