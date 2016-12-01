package com.helger.pd.indexer.storage.field;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

import com.helger.commons.annotation.Nonempty;

public class PDLongField <NATIVE_TYPE> extends AbstractPDField <NATIVE_TYPE, Number>
{
  public PDLongField (@Nonnull @Nonempty final String sFieldName,
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
  @Nullable
  protected NATIVE_TYPE getFieldNativeValue (@Nonnull final IndexableField aField)
  {
    return getAsNativeValue (aField.numericValue ());
  }
}
