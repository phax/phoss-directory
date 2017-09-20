/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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

import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.functional.IFunction;

public class PDNumericField <NATIVE_TYPE> extends AbstractPDField <NATIVE_TYPE, Number>
{
  public PDNumericField (@Nonnull @Nonempty final String sFieldName,
                         @Nonnull final IFunction <? super NATIVE_TYPE, ? extends Number> aConverterToStorage,
                         @Nonnull final IFunction <? super Number, ? extends NATIVE_TYPE> aConverterFromStorage,
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
  @Nullable
  protected NATIVE_TYPE getFieldNativeValue (@Nonnull final IndexableField aField)
  {
    return getAsNativeValue (aField.numericValue ());
  }
}
