/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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

import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

/**
 * Toeknize or not tokenize?
 * 
 * @author Philip Helger
 */
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
