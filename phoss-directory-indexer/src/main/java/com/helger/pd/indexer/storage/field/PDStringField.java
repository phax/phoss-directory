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

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.searchindex.EPDIndexFieldStore;
import com.helger.pd.indexer.searchindex.EPDIndexFieldTokenize;
import com.helger.pd.indexer.searchindex.PDIndexField;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryContains;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryPrefix;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryTerm;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;

import jakarta.annotation.Nullable;

/**
 * An index field that can be mapped to a {@link String} and back.
 *
 * @author Philip Helger
 * @param <NATIVE_TYPE>
 *        The native type.
 */
public class PDStringField <NATIVE_TYPE> extends AbstractPDField <NATIVE_TYPE, String>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDStringField.class);

  private final EPDIndexFieldTokenize m_eTokenize;

  private PDStringField (@NonNull @Nonempty final String sFieldName,
                         @NonNull final Function <? super NATIVE_TYPE, ? extends String> aConverterToStorage,
                         @NonNull final Function <? super String, ? extends NATIVE_TYPE> aConverterFromStorage,
                         @NonNull final EPDIndexFieldStore eStore,
                         @NonNull final EPDIndexFieldTokenize eTokenize)
  {
    super (sFieldName, aConverterToStorage, aConverterFromStorage, eStore);
    m_eTokenize = ValueEnforcer.notNull (eTokenize, "Tokenize");
  }

  @Override
  @NonNull
  public PDIndexField getAsField (@NonNull final NATIVE_TYPE aValue)
  {
    final String sStringValue = getAsStorageValue (aValue);
    return PDIndexField.createString (getFieldName (), sStringValue, getStore (), m_eTokenize);
  }

  private String _getSafeStorageValue (@NonNull final NATIVE_TYPE aValue)
  {
    final String sStorageValue = getAsStorageValue (aValue);
    // No masking needed
    return sStorageValue;
  }

  @NonNull
  public PDIndexQueryTerm getExactMatchQuery (@NonNull final NATIVE_TYPE aValue)
  {
    return new PDIndexQueryTerm (getFieldName (), _getSafeStorageValue (aValue));
  }

  @NonNull
  public PDIndexQueryPrefix getPrefixQuery (@NonNull final NATIVE_TYPE aValue)
  {
    return new PDIndexQueryPrefix (getFieldName (), _getSafeStorageValue (aValue));
  }

  @NonNull
  public PDIndexQueryContains getContainsQuery (@NonNull final NATIVE_TYPE aValue)
  {
    return new PDIndexQueryContains (getFieldName (), _getSafeStorageValue (aValue));
  }

  @Override
  @Nullable
  protected NATIVE_TYPE getFieldNativeValue (@NonNull final PDIndexField aField)
  {
    final String sValue = aField.getStringValue ();
    if (sValue != null)
      try
      {
        return getAsNativeValue (sValue);
      }
      catch (final PDFieldSerializeException e)
      {
        // Less logging in production
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Failed to convert value '" + sValue + "' to native value");
        // Fall through
      }
    return null;
  }

  @NonNull
  public static PDStringField <String> createString (@NonNull @Nonempty final String sFieldName,
                                                     @NonNull final EPDIndexFieldStore eStore,
                                                     @NonNull final EPDIndexFieldTokenize eTokenize)
  {
    return new PDStringField <> (sFieldName, Function.identity (), Function.identity (), eStore, eTokenize);
  }

  @NonNull
  public static PDStringField <IParticipantIdentifier> createParticipantIdentifier (@NonNull @Nonempty final String sFieldName,
                                                                                    @NonNull final EPDIndexFieldStore eStore,
                                                                                    @NonNull final EPDIndexFieldTokenize eTokenize)
  {
    return new PDStringField <> (sFieldName,
                                 IParticipantIdentifier::getURIEncoded,
                                 x -> PDMetaManager.getIdentifierFactory ().parseParticipantIdentifier (x),
                                 eStore,
                                 eTokenize);
  }

  @NonNull
  public static PDStringField <IDocumentTypeIdentifier> createDocumentTypeIdentifier (@NonNull @Nonempty final String sFieldName,
                                                                                      @NonNull final EPDIndexFieldStore eStore,
                                                                                      @NonNull final EPDIndexFieldTokenize eTokenize)
  {
    return new PDStringField <> (sFieldName,
                                 IDocumentTypeIdentifier::getURIEncoded,
                                 x -> PDMetaManager.getIdentifierFactory ().parseDocumentTypeIdentifier (x),
                                 eStore,
                                 eTokenize);
  }
}
