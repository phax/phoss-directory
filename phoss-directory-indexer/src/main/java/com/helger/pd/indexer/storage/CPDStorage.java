/*
 * Copyright (C) 2015-2023 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.storage;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Constants Lucene field names
 *
 * @author Philip Helger
 */
@Immutable
public final class CPDStorage
{
  public static final String FIELD_ALL_FIELDS = "allfields";

  public static final String OWNER_MANUALLY_TRIGGERED = "manually-triggered";
  public static final String OWNER_IMPORT_TRIGGERED = "import-triggered";
  public static final String OWNER_DUPLICATE_ELIMINATION = "duplicate-elimination";
  public static final String OWNER_SYNC_JOB = "sync-job";

  private CPDStorage ()
  {}

  public static boolean isSpecialOwnerID (@Nullable final String s)
  {
    return OWNER_MANUALLY_TRIGGERED.equals (s) ||
           OWNER_IMPORT_TRIGGERED.equals (s) ||
           OWNER_DUPLICATE_ELIMINATION.equals (s) ||
           OWNER_SYNC_JOB.equals (s);
  }
}
