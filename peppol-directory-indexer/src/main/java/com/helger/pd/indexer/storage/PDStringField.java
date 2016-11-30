package com.helger.pd.indexer.storage;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;

public class PDStringField <T> extends AbstractPDField <T, String>
{
  public static enum EPDTokenize
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

  private final EPDTokenize m_eTokenize;

  protected PDStringField (@Nonnull @Nonempty final String sFieldName,
                           @Nonnull final Function <? super T, ? extends String> aConverter,
                           @Nonnull final Field.Store eStore,
                           @Nonnull final EPDTokenize eTokenize)
  {
    super (sFieldName, aConverter, eStore);
    m_eTokenize = ValueEnforcer.notNull (eTokenize, "Tokenize");
  }

  @Nonnull
  public String getAsStringValue (@Nonnull final T aValue) throws IllegalStateException
  {
    ValueEnforcer.notNull (aValue, "Value");
    final String sTermValue = getConverter ().apply (aValue);
    if (sTermValue == null)
      throw new IllegalStateException ("The string value of " +
                                       aValue +
                                       " for field " +
                                       getFieldName () +
                                       " should not be null!");
    return sTermValue;
  }

  @Override
  @Nonnull
  public Field getAsField (@Nonnull final T aValue)
  {
    final String sStringValue = getAsStringValue (aValue);
    return m_eTokenize.createField (getFieldName (), sStringValue, getStore ());
  }

  @Override
  @Nonnull
  public Term getQueryTerm (@Nonnull final T aValue)
  {
    return new Term (getFieldName (), getAsStringValue (aValue));
  }

  @Nullable
  public String getDocValue (@Nonnull final Document aDoc)
  {
    final IndexableField aField = getDocField (aDoc);
    if (aField != null)
    {
      final String sValue = aField.stringValue ();
      if (sValue != null)
        return sValue;
    }
    return null;
  }

  @Nullable
  public String [] getDocValues (@Nonnull final Document aDoc)
  {
    return aDoc.getValues (getFieldName ());
  }
}
