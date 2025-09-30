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
package com.helger.pd.publisher.app.secure;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.pd.publisher.app.AppCommonUI;
import com.helger.pd.publisher.app.AppSecurity;
import com.helger.photon.bootstrap4.pages.BootstrapPagesMenuConfigurator;
import com.helger.photon.bootstrap4.pages.security.BasePageSecurityChangePassword;
import com.helger.photon.core.menu.IMenuItemPage;
import com.helger.photon.core.menu.IMenuTree;
import com.helger.photon.core.menu.filter.MenuObjectFilterUserAssignedToUserGroup;
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
    final MenuObjectFilterUserAssignedToUserGroup aFilterAdministrators = new MenuObjectFilterUserAssignedToUserGroup (AppSecurity.USERGROUP_ADMINISTRATORS_ID);

    // Indexer
    {
      final IMenuItemPage aIndexer = aMenuTree.createRootItem (new BasePageShowChildren <> (CMenuSecure.MENU_INDEXER,
                                                                                            "Indexer",
                                                                                            aMenuTree));
      aMenuTree.createItem (aIndexer, new PageSecureParticipantCount (CMenuSecure.MENU_PARTICIPANT_COUNT));
      aMenuTree.createItem (aIndexer, new PageSecureParticipantList (CMenuSecure.MENU_PARTICIPANT_LIST));
      aMenuTree.createItem (aIndexer, new PageSecureParticipantActions (CMenuSecure.MENU_PARTICIPANT_ACTIONS));
      aMenuTree.createItem (aIndexer, new PageSecureIndexManually (CMenuSecure.MENU_INDEX_MANUALLY));
      aMenuTree.createItem (aIndexer, new PageSecureIndexImport (CMenuSecure.MENU_INDEX_IMPORT));
      aMenuTree.createItem (aIndexer, new PageSecureDeleteManually (CMenuSecure.MENU_DELETE_MANUALLY));
      aMenuTree.createSeparator (aIndexer);
      aMenuTree.createItem (aIndexer, new PageSecureListIndex (CMenuSecure.MENU_LIST_INDEX));
      aMenuTree.createItem (aIndexer, new PageSecureListReIndex (CMenuSecure.MENU_LIST_RE_INDEX));
      aMenuTree.createItem (aIndexer, new PageSecureListDeadIndex (CMenuSecure.MENU_LIST_DEAD_INDEX));
    }

    // Administrator
    {
      final IMenuItemPage aAdmin = aMenuTree.createRootItem (new BasePageShowChildren <> (CMenuSecure.MENU_ADMIN,
                                                                                          "Administration",
                                                                                          aMenuTree));

      aMenuTree.createItem (aAdmin, new BasePageSecurityChangePassword <> (CMenuSecure.MENU_ADMIN_CHANGE_PASSWORD));
      aMenuTree.createItem (aAdmin, new PageSecureAdminSMLConfiguration (CMenuSecure.MENU_ADMIN_SML_CONFIGURATION));
      aMenuTree.createItem (aAdmin, new PageSecureAdminLuceneInformation (CMenuSecure.MENU_ADMIN_LUCENE_INFO));
      BootstrapPagesMenuConfigurator.addAllItems (aMenuTree, aAdmin, aFilterAdministrators, AppCommonUI.DEFAULT_LOCALE);
    }

    // Default menu item
    aMenuTree.setDefaultMenuItemID (CMenuSecure.MENU_INDEXER);
  }
}
