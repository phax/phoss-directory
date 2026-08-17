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
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.equals.EqualsHelper;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.tostring.ToStringGenerator;

import jakarta.annotation.Nullable;

/**
 * A single named value of a {@link PDIndexDocument}. A field either has a String value or a numeric
 * value. Numeric values are always stored and never tokenized.<br>
 * Fields that are read back from the index are always stored fields - the tokenization that was
 * used when the value was written is not available anymore in that case.
 *
 * @author Philip Helger
 * @since 0.16.0
 */
@Immutable
public final class PDIndexField
{
  private final String m_sName;
  private final String m_sStringValue;
  private final Number m_aNumericValue;
  private final EPDIndexFieldStore m_eStore;
  private final EPDIndexFieldTokenize m_eTokenize;

  private PDIndexField (@NonNull @Nonempty final String sName,
                        @Nullable final String sStringValue,
                        @Nullable final Number aNumericValue,
                        @NonNull final EPDIndexFieldStore eStore,
                        @NonNull final EPDIndexFieldTokenize eTokenize)
  {
    m_sName = ValueEnforcer.notEmpty (sName, "Name");
    m_sStringValue = sStringValue;
    m_aNumericValue = aNumericValue;
    m_eStore = ValueEnforcer.notNull (eStore, "Store");
    m_eTokenize = ValueEnforcer.notNull (eTokenize, "Tokenize");
  }

  @NonNull
  @Nonempty
  public String getName ()
  {
    return m_sName;
  }

  /**
   * @return <code>true</code> if this field has a numeric value, <code>false</code> if it has a
   *         String value.
   */
  public boolean isNumeric ()
  {
    return m_aNumericValue != null;
  }

  /**
   * @return The String value of this field. <code>null</code> if this is a numeric field.
   */
  @Nullable
  public String getStringValue ()
  {
    return m_sStringValue;
  }

  /**
   * @return The numeric value of this field. <code>null</code> if this is a String field.
   */
  @Nullable
  public Number getNumericValue ()
  {
    return m_aNumericValue;
  }

  /**
   * @return The value of this field as a String, independent of the value type. May be
   *         <code>null</code>.
   */
  @Nullable
  public String getValueAsString ()
  {
    return m_aNumericValue != null ? m_aNumericValue.toString () : m_sStringValue;
  }

  @NonNull
  public EPDIndexFieldStore getStore ()
  {
    return m_eStore;
  }

  @NonNull
  public EPDIndexFieldTokenize getTokenize ()
  {
    return m_eTokenize;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PDIndexField rhs = (PDIndexField) o;
    return m_sName.equals (rhs.m_sName) &&
           EqualsHelper.equals (m_sStringValue, rhs.m_sStringValue) &&
           EqualsHelper.equals (m_aNumericValue, rhs.m_aNumericValue) &&
           m_eStore.equals (rhs.m_eStore) &&
           m_eTokenize.equals (rhs.m_eTokenize);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sName)
                                       .append (m_sStringValue)
                                       .append (m_aNumericValue)
                                       .append (m_eStore)
                                       .append (m_eTokenize)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Name", m_sName)
                                       .appendIfNotNull ("StringValue", m_sStringValue)
                                       .appendIfNotNull ("NumericValue", m_aNumericValue)
                                       .append ("Store", m_eStore)
                                       .append ("Tokenize", m_eTokenize)
                                       .getToString ();
  }

  /**
   * Create a new String based field.
   *
   * @param sName
   *        Field name. May neither be <code>null</code> nor empty.
   * @param sValue
   *        Field value. May not be <code>null</code>.
   * @param eStore
   *        Store the value in the index? May not be <code>null</code>.
   * @param eTokenize
   *        Tokenize the value before indexing? May not be <code>null</code>.
   * @return The created field. Never <code>null</code>.
   */
  @NonNull
  public static PDIndexField createString (@NonNull @Nonempty final String sName,
                                           @NonNull final String sValue,
                                           @NonNull final EPDIndexFieldStore eStore,
                                           @NonNull final EPDIndexFieldTokenize eTokenize)
  {
    ValueEnforcer.notNull (sValue, "Value");

    return new PDIndexField (sName, sValue, null, eStore, eTokenize);
  }

  /**
   * Create a new String based field as it was read back from the index. Such a field is always
   * stored and the original tokenization is unknown.
   *
   * @param sName
   *        Field name. May neither be <code>null</code> nor empty.
   * @param sValue
   *        Field value. May not be <code>null</code>.
   * @return The created field. Never <code>null</code>.
   */
  @NonNull
  public static PDIndexField createStoredString (@NonNull @Nonempty final String sName, @NonNull final String sValue)
  {
    return createString (sName, sValue, EPDIndexFieldStore.YES, EPDIndexFieldTokenize.NO_TOKENIZE);
  }

  /**
   * Create a new numeric field. Numeric fields are always stored and are not indexed.
   *
   * @param sName
   *        Field name. May neither be <code>null</code> nor empty.
   * @param aValue
   *        Field value. May not be <code>null</code>.
   * @return The created field. Never <code>null</code>.
   */
  @NonNull
  public static PDIndexField createNumeric (@NonNull @Nonempty final String sName, @NonNull final Number aValue)
  {
    ValueEnforcer.notNull (aValue, "Value");

    return new PDIndexField (sName, null, aValue, EPDIndexFieldStore.YES, EPDIndexFieldTokenize.NO_TOKENIZE);
  }
}
