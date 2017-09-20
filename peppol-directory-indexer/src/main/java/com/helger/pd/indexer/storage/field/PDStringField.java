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
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.functional.IFunction;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;

public class PDStringField <NATIVE_TYPE> extends AbstractPDField <NATIVE_TYPE, String>
{
  private final EPDStringFieldTokenize m_eTokenize;

  public PDStringField (@Nonnull @Nonempty final String sFieldName,
                        @Nonnull final IFunction <? super NATIVE_TYPE, ? extends String> aConverterToStorage,
                        @Nonnull final IFunction <? super String, ? extends NATIVE_TYPE> aConverterFromStorage,
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
