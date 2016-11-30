package com.helger.pd.indexer.storage;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;

public abstract class AbstractPDField <SRC, DST>
{
  private final String m_sFieldName;
  private final Function <? super SRC, ? extends DST> m_aConverter;
  private final Field.Store m_eStore;

  protected AbstractPDField (@Nonnull @Nonempty final String sFieldName,
                             @Nonnull final Function <? super SRC, ? extends DST> aConverter,
                             @Nonnull final Field.Store eStore)
  {
    m_sFieldName = ValueEnforcer.notEmpty (sFieldName, "FieldName");
    m_aConverter = ValueEnforcer.notNull (aConverter, "Converter");
    m_eStore = ValueEnforcer.notNull (eStore, "Store");
  }

  @Nonnull
  @Nonempty
  public final String getFieldName ()
  {
    return m_sFieldName;
  }

  @Nonnull
  protected final Function <? super SRC, ? extends DST> getConverter ()
  {
    return m_aConverter;
  }

  @Nonnull
  protected final Field.Store getStore ()
  {
    return m_eStore;
  }

  @Nonnull
  public abstract Field getAsField (@Nonnull final SRC aValue);

  @Nonnull
  public abstract Term getQueryTerm (@Nonnull final SRC aValue);

  @Nullable
  public final IndexableField getDocField (@Nonnull final Document aDoc)
  {
    return aDoc.getField (m_sFieldName);
  }

  @Nonnull
  public final IndexableField [] getDocFields (@Nonnull final Document aDoc)
  {
    return aDoc.getFields (m_sFieldName);
  }
}
