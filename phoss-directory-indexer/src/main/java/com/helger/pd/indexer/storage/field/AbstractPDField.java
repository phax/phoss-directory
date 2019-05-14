/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.storage.field;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexableField;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.functional.IFunction;

/**
 * Abstract Directory Lucene field.
 *
 * @author Philip Helger
 * @param <NATIVE_TYPE>
 *        Native type. Must be convertible to storage type and back.
 * @param <STORAGE_TYPE>
 *        Storage type. Must be convertible to native type and back.
 */
public abstract class AbstractPDField <NATIVE_TYPE, STORAGE_TYPE>
{
  private final String m_sFieldName;
  private final IFunction <? super NATIVE_TYPE, ? extends STORAGE_TYPE> m_aConverterToStorage;
  private final IFunction <? super STORAGE_TYPE, ? extends NATIVE_TYPE> m_aConverterFromStorage;
  private final Field.Store m_eStore;

  protected AbstractPDField (@Nonnull @Nonempty final String sFieldName,
                             @Nonnull final IFunction <? super NATIVE_TYPE, ? extends STORAGE_TYPE> aConverterToStorage,
                             @Nonnull final IFunction <? super STORAGE_TYPE, ? extends NATIVE_TYPE> aConverterFromStorage,
                             @Nonnull final Field.Store eStore)
  {
    m_sFieldName = ValueEnforcer.notEmpty (sFieldName, "FieldName");
    m_aConverterToStorage = ValueEnforcer.notNull (aConverterToStorage, "ConverterToStorage");
    m_aConverterFromStorage = ValueEnforcer.notNull (aConverterFromStorage, "ConverterFromStorage");
    m_eStore = ValueEnforcer.notNull (eStore, "Store");
  }

  @Nonnull
  @Nonempty
  public final String getFieldName ()
  {
    return m_sFieldName;
  }

  @Nonnull
  protected final IFunction <? super NATIVE_TYPE, ? extends STORAGE_TYPE> getConverterToStorage ()
  {
    return m_aConverterToStorage;
  }

  @Nonnull
  protected final IFunction <? super STORAGE_TYPE, ? extends NATIVE_TYPE> getConverterFromStorage ()
  {
    return m_aConverterFromStorage;
  }

  @Nonnull
  protected final Field.Store getStore ()
  {
    return m_eStore;
  }

  @Nonnull
  public abstract Field getAsField (@Nonnull NATIVE_TYPE aValue);

  @Nonnull
  public STORAGE_TYPE getAsStorageValue (@Nonnull final NATIVE_TYPE aValue) throws IllegalStateException
  {
    ValueEnforcer.notNull (aValue, "Value");
    final STORAGE_TYPE sStorageValue = getConverterToStorage ().apply (aValue);
    if (sStorageValue == null)
      throw new IllegalStateException ("The storage value of " +
                                       aValue +
                                       " for field " +
                                       getFieldName () +
                                       " should not be null!");
    return sStorageValue;
  }

  @Nonnull
  public NATIVE_TYPE getAsNativeValue (@Nonnull final STORAGE_TYPE aValue) throws IllegalStateException
  {
    ValueEnforcer.notNull (aValue, "Value");
    final NATIVE_TYPE aNativeValue = getConverterFromStorage ().apply (aValue);
    if (aNativeValue == null)
      throw new IllegalStateException ("The native value of " +
                                       aValue +
                                       " for field " +
                                       getFieldName () +
                                       " should not be null!");
    return aNativeValue;
  }

  @Nullable
  public final IndexableField getDocField (@Nonnull final Document aDoc)
  {
    return aDoc.getField (m_sFieldName);
  }

  @Nonnull
  public final ICommonsList <IndexableField> getDocFields (@Nonnull final Document aDoc)
  {
    final ICommonsList <IndexableField> ret = new CommonsArrayList <> ();
    for (final IndexableField aField : aDoc)
      if (aField.name ().equals (m_sFieldName))
        ret.add (aField);
    return ret;
  }

  @Nullable
  protected abstract NATIVE_TYPE getFieldNativeValue (@Nonnull IndexableField aField);

  /**
   * Get the value of this field in the provided document
   *
   * @param aDoc
   *        The Lucene result document
   * @return <code>null</code> if no such field is present, the stored value
   *         otherwise.
   */
  @Nullable
  public final NATIVE_TYPE getDocValue (@Nonnull final Document aDoc)
  {
    final IndexableField aField = getDocField (aDoc);
    if (aField != null)
      return getFieldNativeValue (aField);
    return null;
  }

  @Nonnull
  public final ICommonsList <NATIVE_TYPE> getDocValues (@Nonnull final Document aDoc)
  {
    final ICommonsList <NATIVE_TYPE> ret = new CommonsArrayList <> ();
    for (final IndexableField aField : getDocFields (aDoc))
      ret.add (getFieldNativeValue (aField));
    return ret;
  }
}
