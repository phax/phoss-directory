package com.helger.pd.indexer.storage.field;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;

public class PDStringField <NATIVE_TYPE> extends AbstractPDField <NATIVE_TYPE, String>
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

  public PDStringField (@Nonnull @Nonempty final String sFieldName,
                        @Nonnull final Function <? super NATIVE_TYPE, ? extends String> aConverterToStorage,
                        @Nonnull final Function <? super String, ? extends NATIVE_TYPE> aConverterFromStorage,
                        @Nonnull final Field.Store eStore,
                        @Nonnull final EPDTokenize eTokenize)
  {
    super (sFieldName, aConverterToStorage, aConverterFromStorage, eStore);
    m_eTokenize = ValueEnforcer.notNull (eTokenize, "Tokenize");
  }

  @Override
  @Nonnull
  public Field getAsField (@Nonnull final NATIVE_TYPE aValue)
  {
    final String sStringValue = getAsStorageValue (aValue);
    return m_eTokenize.createField (getFieldName (), sStringValue, getStore ());
  }

  @Nonnull
  public Term getTerm (@Nonnull final NATIVE_TYPE aValue)
  {
    return new Term (getFieldName (), getAsStorageValue (aValue));
  }

  @Override
  @Nullable
  protected NATIVE_TYPE getFieldNativeValue (@Nonnull final IndexableField aField)
  {
    final String sValue = aField.stringValue ();
    if (sValue != null)
      return getAsNativeValue (sValue);
    return null;
  }

  @Nonnull
  public static PDStringField <String> createString (@Nonnull @Nonempty final String sFieldName,
                                                     @Nonnull final Field.Store eStore,
                                                     @Nonnull final EPDTokenize eTokenize)
  {
    return new PDStringField<> (sFieldName, Function.identity (), Function.identity (), eStore, eTokenize);
  }

  @Nonnull
  public static PDStringField <IParticipantIdentifier> createParticipantIdentifier (@Nonnull @Nonempty final String sFieldName,
                                                                                    @Nonnull final Field.Store eStore,
                                                                                    @Nonnull final EPDTokenize eTokenize)
  {
    return new PDStringField<> (sFieldName,
                                x -> x.getURIEncoded (),
                                x -> PDMetaManager.getIdentifierFactory ().parseParticipantIdentifier (x),
                                eStore,
                                eTokenize);
  }

  @Nonnull
  public static PDStringField <IDocumentTypeIdentifier> createDocumentTypeIdentifier (@Nonnull @Nonempty final String sFieldName,
                                                                                      @Nonnull final Field.Store eStore,
                                                                                      @Nonnull final EPDTokenize eTokenize)
  {
    return new PDStringField<> (sFieldName,
                                x -> x.getURIEncoded (),
                                x -> PDMetaManager.getIdentifierFactory ().parseDocumentTypeIdentifier (x),
                                eStore,
                                eTokenize);
  }
}
