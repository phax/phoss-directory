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

import java.util.BitSet;
import java.util.function.Function;

import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;

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
  private final EPDStringFieldTokenize m_eTokenize;

  public PDStringField (@Nonnull @Nonempty final String sFieldName,
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

  private static final BitSet MASK_CHARS = new BitSet (256);
  static
  {
    for (final char c : "+-&|!(){}[]^\"~*?:\\/".toCharArray ())
      MASK_CHARS.set (c);
  }

  private String _getMaskedStorageValue (@Nonnull final NATIVE_TYPE aValue)
  {
    final String sStorageValue = getAsStorageValue (aValue);
    if (true)
    {
      // No masking needed
      return sStorageValue;
    }
    // Masking is only needed, when the QueryParser is used
    final StringBuilder aSB = new StringBuilder (sStorageValue.length () * 2);
    for (final char c : sStorageValue.toCharArray ())
    {
      if (c <= 255 && MASK_CHARS.get (c))
        aSB.append ('\\');
      aSB.append (c);
    }
    return aSB.toString ();
  }

  @Nonnull
  public Term getExactMatchTerm (@Nonnull final NATIVE_TYPE aValue)
  {
    return new Term (getFieldName (), _getMaskedStorageValue (aValue));
  }

  @Nonnull
  public Term getContainsTerm (@Nonnull final NATIVE_TYPE aValue)
  {
    return new Term (getFieldName (), "*" + _getMaskedStorageValue (aValue) + "*");
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
                                                     @Nonnull final EPDStringFieldTokenize eTokenize)
  {
    return new PDStringField <> (sFieldName, x -> x, x -> x, eStore, eTokenize);
  }

  @Nonnull
  public static PDStringField <IParticipantIdentifier> createParticipantIdentifier (@Nonnull @Nonempty final String sFieldName,
                                                                                    @Nonnull final Field.Store eStore,
                                                                                    @Nonnull final EPDStringFieldTokenize eTokenize)
  {
    return new PDStringField <> (sFieldName,
                                 x -> x.getURIEncoded (),
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
                                 x -> x.getURIEncoded (),
                                 x -> PDMetaManager.getIdentifierFactory ().parseDocumentTypeIdentifier (x),
                                 eStore,
                                 eTokenize);
  }
}
