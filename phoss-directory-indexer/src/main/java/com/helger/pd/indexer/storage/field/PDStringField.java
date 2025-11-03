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
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * A Lucene field that can be mapped to a {@link String} and back.
 *
 * @author Philip Helger
 * @param <NATIVE_TYPE>
 *        The native type.
 */
public class PDStringField <NATIVE_TYPE> extends AbstractPDField <NATIVE_TYPE, String>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDStringField.class);

  private final EPDStringFieldTokenize m_eTokenize;

  private PDStringField (@Nonnull @Nonempty final String sFieldName,
                         @Nonnull final Function <? super NATIVE_TYPE, ? extends String> aConverterToStorage,
                         @Nonnull final Function <? super String, ? extends NATIVE_TYPE> aConverterFromStorage,
                         @Nonnull final Field.Store eStore,
                         @Nonnull final EPDStringFieldTokenize eTokenize)
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

  private String _getSafeStorageValue (@Nonnull final NATIVE_TYPE aValue)
  {
    final String sStorageValue = getAsStorageValue (aValue);
    // No masking needed
    return sStorageValue;
  }

  @Nonnull
  public Term getExactMatchTerm (@Nonnull final NATIVE_TYPE aValue)
  {
    return new Term (getFieldName (), _getSafeStorageValue (aValue));
  }

  @Nonnull
  public Term getContainsTerm (@Nonnull final NATIVE_TYPE aValue)
  {
    return new Term (getFieldName (), "*" + _getSafeStorageValue (aValue) + "*");
  }

  @Override
  @Nullable
  protected NATIVE_TYPE getFieldNativeValue (@Nonnull final IndexableField aField)
  {
    final String sValue = aField.stringValue ();
    if (sValue != null)
      try
      {
        return getAsNativeValue (sValue);
      }
      catch (final PDFieldSerializeException e)
      {
        LOGGER.error ("Failed to convert value '" + sValue + "' to native value");
        // Fall through
      }
    return null;
  }

  @Nonnull
  public static PDStringField <String> createString (@Nonnull @Nonempty final String sFieldName,
                                                     @Nonnull final Field.Store eStore,
                                                     @Nonnull final EPDStringFieldTokenize eTokenize)
  {
    return new PDStringField <> (sFieldName, Function.identity (), Function.identity (), eStore, eTokenize);
  }

  @Nonnull
  public static PDStringField <IParticipantIdentifier> createParticipantIdentifier (@Nonnull @Nonempty final String sFieldName,
                                                                                    @Nonnull final Field.Store eStore,
                                                                                    @Nonnull final EPDStringFieldTokenize eTokenize)
  {
    return new PDStringField <> (sFieldName,
                                 IParticipantIdentifier::getURIEncoded,
                                 x -> PDMetaManager.getIdentifierFactory ().parseParticipantIdentifier (x),
                                 eStore,
                                 eTokenize);
  }

  @Nonnull
  public static PDStringField <IDocumentTypeIdentifier> createDocumentTypeIdentifier (@Nonnull @Nonempty final String sFieldName,
                                                                                      @Nonnull final Field.Store eStore,
                                                                                      @Nonnull final EPDStringFieldTokenize eTokenize)
  {
    return new PDStringField <> (sFieldName,
                                 IDocumentTypeIdentifier::getURIEncoded,
                                 x -> PDMetaManager.getIdentifierFactory ().parseDocumentTypeIdentifier (x),
                                 eStore,
                                 eTokenize);
  }
}
