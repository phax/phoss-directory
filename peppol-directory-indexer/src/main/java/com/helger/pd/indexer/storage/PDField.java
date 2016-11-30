package com.helger.pd.indexer.storage;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.Term;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;

public class PDField <T>
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

  public static final PDField <IParticipantIdentifier> PARTICIPANT_ID = new PDField<> ("participantid",
                                                                                       x -> x.getURIEncoded (),
                                                                                       Field.Store.YES,
                                                                                       EPDTokenize.NO_TOKENIZE);
  public static final PDField <IDocumentTypeIdentifier> DOCTYPE_ID = new PDField<> ("doctypeid",
                                                                                    x -> x.getURIEncoded (),
                                                                                    Field.Store.YES,
                                                                                    EPDTokenize.NO_TOKENIZE);
  public static final PDField <String> REGISTRATION_DATE = new PDField<> ("registrationdate",
                                                                          Function.identity (),
                                                                          Field.Store.YES,
                                                                          EPDTokenize.NO_TOKENIZE);
  public static final PDField <String> NAME = new PDField<> ("name",
                                                             Function.identity (),
                                                             Field.Store.YES,
                                                             EPDTokenize.TOKENIZE);
  public static final PDField <String> COUNTRY_CODE = new PDField<> ("country",
                                                                     Function.identity (),
                                                                     Field.Store.YES,
                                                                     EPDTokenize.NO_TOKENIZE);
  public static final PDField <String> GEO_INFO = new PDField<> ("geoinfo",
                                                                 Function.identity (),
                                                                 Field.Store.YES,
                                                                 EPDTokenize.TOKENIZE);
  public static final PDField <String> IDENTIFIER_SCHEME = new PDField<> ("identifiertype",
                                                                          Function.identity (),
                                                                          Field.Store.YES,
                                                                          EPDTokenize.TOKENIZE);
  public static final PDField <String> IDENTIFIER_VALUE = new PDField<> ("identifier",
                                                                         Function.identity (),
                                                                         Field.Store.YES,
                                                                         EPDTokenize.TOKENIZE);
  public static final PDField <String> WEBSITE_URI = new PDField<> ("website",
                                                                    Function.identity (),
                                                                    Field.Store.YES,
                                                                    EPDTokenize.TOKENIZE);
  public static final PDField <String> CONTACT_TYPE = new PDField<> ("bc-description",
                                                                     Function.identity (),
                                                                     Field.Store.YES,
                                                                     EPDTokenize.TOKENIZE);
  public static final PDField <String> CONTACT_NAME = new PDField<> ("bc-name",
                                                                     Function.identity (),
                                                                     Field.Store.YES,
                                                                     EPDTokenize.TOKENIZE);
  public static final PDField <String> CONTACT_PHONE = new PDField<> ("bc-phone",
                                                                      Function.identity (),
                                                                      Field.Store.YES,
                                                                      EPDTokenize.TOKENIZE);
  public static final PDField <String> CONTACT_EMAIL = new PDField<> ("bc-email",
                                                                      Function.identity (),
                                                                      Field.Store.YES,
                                                                      EPDTokenize.TOKENIZE);
  public static final PDField <String> ADDITIONAL_INFO = new PDField<> ("freetext",
                                                                        Function.identity (),
                                                                        Field.Store.YES,
                                                                        EPDTokenize.TOKENIZE);

  private final String m_sFieldName;
  private final Function <? super T, String> m_aConverter;
  private final Store m_eStore;
  private final EPDTokenize m_eTokenize;

  protected PDField (@Nonnull @Nonempty final String sFieldName,
                     @Nonnull final Function <? super T, String> aConverter,
                     @Nonnull final Field.Store eStore,
                     @Nonnull final EPDTokenize eTokenize)
  {
    m_sFieldName = ValueEnforcer.notEmpty (sFieldName, "FieldName");
    m_aConverter = ValueEnforcer.notNull (aConverter, "Converter");
    m_eStore = ValueEnforcer.notNull (eStore, "Store");
    m_eTokenize = ValueEnforcer.notNull (eTokenize, "Tokenize");
  }

  @Nonnull
  @Nonempty
  public String getFieldName ()
  {
    return m_sFieldName;
  }

  @Nonnull
  public String getAsStringValue (@Nonnull final T aValue) throws IllegalStateException
  {
    ValueEnforcer.notNull (aValue, "Value");
    final String sTermValue = m_aConverter.apply (aValue);
    if (sTermValue == null)
      throw new IllegalStateException ("The string value of " +
                                       aValue +
                                       " for field " +
                                       m_sFieldName +
                                       " should not be null!");
    return sTermValue;
  }

  @Nonnull
  public Field getAsField (@Nonnull final T aValue)
  {
    final String sStringValue = getAsStringValue (aValue);
    return m_eTokenize.createField (m_sFieldName, sStringValue, m_eStore);
  }

  @Nonnull
  public Term getQueryTerm (@Nonnull final T aValue)
  {
    return new Term (m_sFieldName, getAsStringValue (aValue));
  }

  @Nullable
  public String getDocValue (@Nonnull final Document aDoc)
  {
    return aDoc.get (m_sFieldName);
  }

  @Nullable
  public String [] getDocValues (@Nonnull final Document aDoc)
  {
    return aDoc.getValues (m_sFieldName);
  }
}
