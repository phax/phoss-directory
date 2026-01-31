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
package com.helger.pd.indexer.storage.field;

import java.util.function.Function;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexableField;
import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;

import jakarta.annotation.Nullable;

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
  private final Function <? super NATIVE_TYPE, ? extends STORAGE_TYPE> m_aConverterToStorage;
  private final Function <? super STORAGE_TYPE, ? extends NATIVE_TYPE> m_aConverterFromStorage;
  private final Field.Store m_eStore;

  protected AbstractPDField (@NonNull @Nonempty final String sFieldName,
                             @NonNull final Function <? super NATIVE_TYPE, ? extends STORAGE_TYPE> aConverterToStorage,
                             @NonNull final Function <? super STORAGE_TYPE, ? extends NATIVE_TYPE> aConverterFromStorage,
                             final Field.@NonNull Store eStore)
  {
    m_sFieldName = ValueEnforcer.notEmpty (sFieldName, "FieldName");
    m_aConverterToStorage = ValueEnforcer.notNull (aConverterToStorage, "ConverterToStorage");
    m_aConverterFromStorage = ValueEnforcer.notNull (aConverterFromStorage, "ConverterFromStorage");
    m_eStore = ValueEnforcer.notNull (eStore, "Store");
  }

  @NonNull
  @Nonempty
  public final String getFieldName ()
  {
    return m_sFieldName;
  }

  @NonNull
  protected final Function <? super NATIVE_TYPE, ? extends STORAGE_TYPE> getConverterToStorage ()
  {
    return m_aConverterToStorage;
  }

  @NonNull
  protected final Function <? super STORAGE_TYPE, ? extends NATIVE_TYPE> getConverterFromStorage ()
  {
    return m_aConverterFromStorage;
  }

  protected final Field.@NonNull Store getStore ()
  {
    return m_eStore;
  }

  @NonNull
  public abstract Field getAsField (@NonNull NATIVE_TYPE aValue);

  @NonNull
  public STORAGE_TYPE getAsStorageValue (@NonNull final NATIVE_TYPE aValue) throws IllegalStateException
  {
    ValueEnforcer.notNull (aValue, "Value");

    // In the current setup, all "to storage" converters are guaranteed to be non-null
    final STORAGE_TYPE sStorageValue = getConverterToStorage ().apply (aValue);
    if (sStorageValue == null)
      throw new IllegalStateException ("The storage value of " +
                                       aValue +
                                       " for field " +
                                       getFieldName () +
                                       " should not be null!");
    return sStorageValue;
  }

  @NonNull
  public NATIVE_TYPE getAsNativeValue (@NonNull final STORAGE_TYPE aValue) throws PDFieldSerializeException
  {
    ValueEnforcer.notNull (aValue, "Value");
    final NATIVE_TYPE aNativeValue = getConverterFromStorage ().apply (aValue);
    if (aNativeValue == null)
      throw new PDFieldSerializeException ("The native value of " +
                                           aValue +
                                           " for field " +
                                           getFieldName () +
                                           " should not be null!");
    return aNativeValue;
  }

  @Nullable
  public final IndexableField getDocField (@NonNull final Document aDoc)
  {
    return aDoc.getField (m_sFieldName);
  }

  @NonNull
  public final ICommonsList <IndexableField> getDocFields (@NonNull final Document aDoc)
  {
    final ICommonsList <IndexableField> ret = new CommonsArrayList <> ();
    for (final IndexableField aField : aDoc)
      if (aField.name ().equals (m_sFieldName))
        ret.add (aField);
    return ret;
  }

  @Nullable
  protected abstract NATIVE_TYPE getFieldNativeValue (@NonNull IndexableField aField);

  /**
   * Get the value of this field in the provided document
   *
   * @param aDoc
   *        The Lucene result document
   * @return <code>null</code> if no such field is present, the stored value otherwise.
   */
  @Nullable
  public final NATIVE_TYPE getDocValue (@NonNull final Document aDoc)
  {
    final IndexableField aField = getDocField (aDoc);
    if (aField != null)
      return getFieldNativeValue (aField);
    return null;
  }

  @NonNull
  public final ICommonsList <NATIVE_TYPE> getDocValues (@NonNull final Document aDoc)
  {
    final ICommonsList <NATIVE_TYPE> ret = new CommonsArrayList <> ();
    for (final IndexableField aField : getDocFields (aDoc))
    {
      // List may contain null values!
      ret.add (getFieldNativeValue (aField));
    }
    return ret;
  }
}
