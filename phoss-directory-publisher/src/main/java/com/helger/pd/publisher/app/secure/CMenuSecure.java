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
package com.helger.pd.publisher.app.secure;

import com.helger.annotation.concurrent.Immutable;

@Immutable
public final class CMenuSecure
{
  // Menu item IDs
  public static final String MENU_INDEXER = "indexer";
  public static final String MENU_PARTICIPANT_COUNT = "participant_count";
  public static final String MENU_PARTICIPANT_LIST = "participant_list";
  public static final String MENU_PARTICIPANT_ACTIONS = "participant_actions";
  public static final String MENU_INDEX_MANUALLY = "index_manually";
  public static final String MENU_INDEX_IMPORT = "index_import";
  public static final String MENU_DELETE_MANUALLY = "delete_manually";
  public static final String MENU_LIST_INDEX = "list_index";
  public static final String MENU_LIST_RE_INDEX = "list_reindex";
  public static final String MENU_LIST_DEAD_INDEX = "list_deadindex";

  public static final String MENU_ADMIN = "admin";
  public static final String MENU_ADMIN_CHANGE_PASSWORD = "change_password";
  public static final String MENU_ADMIN_SML_CONFIGURATION = "sml_configuration";
  public static final String MENU_ADMIN_LUCENE_INFO = "lucene_info";

  private CMenuSecure ()
  {}
}
