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
package com.helger.pd.publisher.app.pub;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.debug.GlobalDebug;
import com.helger.io.resource.ClassPathResource;
import com.helger.pd.publisher.CPDPublisher;
import com.helger.pd.publisher.ui.AppPageViewExternal;
import com.helger.photon.core.menu.IMenuTree;

@Immutable
public final class MenuPublic
{
  private MenuPublic ()
  {}

  public static void init (@NonNull final IMenuTree aMenuTree)
  {
    // Not logged in
    aMenuTree.createRootItem (new PagePublicSearchSimple (CMenuPublic.MENU_SEARCH_SIMPLE));
    if (GlobalDebug.isDebugMode ())
      aMenuTree.createRootItem (new PagePublicSearchExtended (CMenuPublic.MENU_SEARCH_EXTENDED));
    aMenuTree.createRootItem (new AppPageViewExternal (CMenuPublic.MENU_DOCS_INTRODUCTION,
                                                       "Introduction",
                                                       new ClassPathResource ("viewpages/en/docs_introduction.xml")));
    aMenuTree.createRootItem (new AppPageViewExternal (CMenuPublic.MENU_DOCS_HOW_TO,
                                                       "How to use it",
                                                       new ClassPathResource ("viewpages/en/docs_how_to.xml")));
    aMenuTree.createRootItem (new AppPageViewExternal (CMenuPublic.MENU_DOCS_REST_API,
                                                       "REST API documentation",
                                                       new ClassPathResource ("viewpages/en/docs_rest_api.xml")));
    aMenuTree.createRootItem (new AppPageViewExternal (CMenuPublic.MENU_DOCS_EXPORT_ALL,
                                                       "Export data",
                                                       new ClassPathResource ("viewpages/en/docs_export_all.xml")));
    aMenuTree.createRootItem (new AppPageViewExternal (CMenuPublic.MENU_ABOUT,
                                                       "About " + CPDPublisher.getApplication (),
                                                       new ClassPathResource ("viewpages/en/about.xml")));
    aMenuTree.createRootItem (new AppPageViewExternal (CMenuPublic.MENU_CHANGE_LOG,
                                                       "Change Log",
                                                       new ClassPathResource ("viewpages/en/changelog.xml")));

    // Set default
    aMenuTree.setDefaultMenuItemID (CMenuPublic.MENU_SEARCH_SIMPLE);
  }
}
