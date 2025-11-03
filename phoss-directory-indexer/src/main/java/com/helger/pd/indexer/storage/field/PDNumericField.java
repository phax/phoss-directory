/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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

import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

import com.helger.annotation.Nonempty;

import jakarta.annotation.Nonnull;

/**
 * A Lucene field that can be mapped to a {@link Number} and back.
 *
 * @author Philip Helger
 * @param <NATIVE_TYPE>
 *        The native type.
 */
public class PDNumericField <NATIVE_TYPE> extends AbstractPDField <NATIVE_TYPE, Number>
{
  public PDNumericField (@Nonnull @Nonempty final String sFieldName,
                         @Nonnull final Function <? super NATIVE_TYPE, ? extends Number> aConverterToStorage,
                         @Nonnull final Function <? super Number, ? extends NATIVE_TYPE> aConverterFromStorage,
                         @Nonnull final Field.Store eStore)
  {
    super (sFieldName, aConverterToStorage, aConverterFromStorage, eStore);
  }

  @Override
  @Nonnull
  public Field getAsField (@Nonnull final NATIVE_TYPE aValue)
  {
    final Number aLongValue = getAsStorageValue (aValue);
    return new StoredField (getFieldName (), aLongValue.longValue ());
  }

  @Override
  @Nonnull
  protected NATIVE_TYPE getFieldNativeValue (@Nonnull final IndexableField aField)
  {
    try
    {
      return getAsNativeValue (aField.numericValue ());
    }
    catch (final PDFieldSerializeException ex)
    {
      // Parsing a numerical value should never fail
      throw new IllegalStateException ("Failed to parse numerical value - weird", ex);
    }
  }
}
