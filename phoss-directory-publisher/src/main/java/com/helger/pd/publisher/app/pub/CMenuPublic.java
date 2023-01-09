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
package com.helger.pd.publisher.app.pub;

import javax.annotation.concurrent.Immutable;

@Immutable
public final class CMenuPublic
{
  public static final String MENU_SEARCH_SIMPLE = "search";
  public static final String MENU_SEARCH_EXTENDED = "search-ext";
  public static final String MENU_DOCS_INTRODUCTION = "docs-introduction";
  public static final String MENU_DOCS_HOW_TO = "docs-how-to";
  public static final String MENU_DOCS_REST_API = "docs-rest-api";
  public static final String MENU_DOCS_EXPORT_ALL = "docs-export-all";
  public static final String MENU_SUPPORT_CONTACT_US = "contact-us";
  public static final String MENU_ABOUT = "about";

  // flags
  public static final String FLAG_FOOTER = "footer";

  private CMenuPublic ()
  {}
}
