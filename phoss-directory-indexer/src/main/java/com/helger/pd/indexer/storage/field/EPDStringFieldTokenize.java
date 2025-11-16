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

import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.jspecify.annotations.NonNull;

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
    @NonNull
    public Field createField (@NonNull final String sFieldName,
                              @NonNull final String sFieldValue,
                              @NonNull final Store eStore)
    {
      return new TextField (sFieldName, sFieldValue, eStore);
    }
  },
  NO_TOKENIZE
  {
    @Override
    @NonNull
    public Field createField (@NonNull final String sFieldName,
                              @NonNull final String sFieldValue,
                              @NonNull final Store eStore)
    {
      return new StringField (sFieldName, sFieldValue, eStore);
    }
  };

  @NonNull
  public abstract Field createField (@NonNull String sFieldName,
                                     @NonNull String sFieldValue,
                                     Field.@NonNull Store eStore);
}
