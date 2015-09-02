/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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
package com.jelger.pyp.indexer.app;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.photon.basic.app.menu.IMenuItemPage;
import com.helger.photon.basic.app.menu.IMenuTree;
import com.helger.photon.basic.app.menu.filter.MenuObjectFilterUserAssignedToUserGroup;
import com.helger.photon.bootstrap3.pages.BootstrapPagesMenuConfigurator;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uicore.page.system.BasePageShowChildren;

@Immutable
public final class MenuSecure
{
  private MenuSecure ()
  {}

  public static void init (@Nonnull final IMenuTree aMenuTree)
  {
    // We need this additional indirection layer, as the pages are initialized
    // statically!
    final MenuObjectFilterUserAssignedToUserGroup aFilterAdministrators = new MenuObjectFilterUserAssignedToUserGroup (CApp.USERGROUP_ADMINISTRATORS_ID);

    // Administrator
    {
      final IMenuItemPage aAdmin = aMenuTree.createRootItem (new BasePageShowChildren <WebPageExecutionContext> (CMenuSecure.MENU_ADMIN,
                                                                                                                 "Administration",
                                                                                                                 aMenuTree))
                                            .setDisplayFilter (aFilterAdministrators);

      BootstrapPagesMenuConfigurator.addAllItems (aMenuTree, aAdmin, aFilterAdministrators, CApp.DEFAULT_LOCALE);
    }

    // Default menu item
    aMenuTree.setDefaultMenuItemID (CMenuSecure.MENU_ADMIN);
  }
}
