package com.helger.pd.indexer.storage.field;

import javax.annotation.Nonnull;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;

public enum EPDStringFieldTokenize
{
  TOKENIZE
  {
    @Override
    @Nonnull
    public Field createField (@Nonnull final String sFieldName,
                              @Nonnull final String sFieldValue,
                              @Nonnull final Store eStore)
    {
      return new TextField (sFieldName, sFieldValue, eStore);
    }
  },
  NO_TOKENIZE
  {
    @Override
    @Nonnull
    public Field createField (@Nonnull final String sFieldName,
                              @Nonnull final String sFieldValue,
                              @Nonnull final Store eStore)
    {
      return new StringField (sFieldName, sFieldValue, eStore);
    }
  };

  @Nonnull
  public abstract Field createField (@Nonnull String sFieldName,
                                     @Nonnull String sFieldValue,
                                     @Nonnull Field.Store eStore);
}