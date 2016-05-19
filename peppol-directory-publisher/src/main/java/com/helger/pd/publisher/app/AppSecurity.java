/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.app;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.concurrent.Immutable;

import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.role.RoleManager;
import com.helger.photon.security.user.UserManager;
import com.helger.photon.security.usergroup.UserGroupManager;

@Immutable
public final class AppSecurity
{
  // Security roles
  public static final String ROLE_CONFIG_ID = "config";
  public static final String ROLE_CONFIG_NAME = "Config user";
  public static final String ROLE_CONFIG_DESCRIPTION = null;
  public static final Map <String, String> ROLE_CONFIG_CUSTOMATTRS = null;
  public static final String ROLE_VIEW_ID = "view";
  public static final String ROLE_VIEW_NAME = "View user";
  public static final String ROLE_VIEW_DESCRIPTION = null;
  public static final Map <String, String> ROLE_VIEW_CUSTOMATTRS = null;
  public static final String ROLE_SG_OWNER_ID = "sgowner";
  public static final String ROLE_SG_OWNER_NAME = "Service Group owner";
  public static final String ROLE_SG_OWNER_DESCRIPTION = null;
  public static final Map <String, String> ROLE_SG_OWNER_CUSTOMATTRS = null;

  public static final List <String> REQUIRED_ROLE_IDS_CONFIG = new CommonsArrayList <> (ROLE_CONFIG_ID).getAsUnmodifiable ();
  public static final List <String> REQUIRED_ROLE_IDS_VIEW = new CommonsArrayList <> (ROLE_VIEW_ID).getAsUnmodifiable ();

  // User groups
  public static final String USERGROUP_ADMINISTRATORS_ID = CSecurity.USERGROUP_ADMINISTRATORS_ID;
  public static final String USERGROUP_ADMINISTRATORS_NAME = CSecurity.USERGROUP_ADMINISTRATORS_NAME;
  public static final String USERGROUP_ADMINISTRATORS_DESCRIPTION = null;
  public static final Map <String, String> USERGROUP_ADMINISTRATORS_CUSTOMATTRS = null;
  public static final String USERGROUP_CONFIG_ID = "ugconfig";
  public static final String USERGROUP_CONFIG_NAME = "Config user";
  public static final String USERGROUP_CONFIG_DESCRIPTION = null;
  public static final Map <String, String> USERGROUP_CONFIG_CUSTOMATTRS = null;
  public static final String USERGROUP_VIEW_ID = "ugview";
  public static final String USERGROUP_VIEW_NAME = "View user";
  public static final String USERGROUP_VIEW_DESCRIPTION = null;
  public static final Map <String, String> USERGROUP_VIEW_CUSTOMATTRS = null;

  // User ID
  public static final String USER_ADMINISTRATOR_ID = CSecurity.USER_ADMINISTRATOR_ID;
  public static final String USER_ADMINISTRATOR_LOGINNAME = CSecurity.USER_ADMINISTRATOR_EMAIL;
  public static final String USER_ADMINISTRATOR_EMAIL = CSecurity.USER_ADMINISTRATOR_EMAIL;
  public static final String USER_ADMINISTRATOR_PASSWORD = CSecurity.USER_ADMINISTRATOR_PASSWORD;
  public static final String USER_ADMINISTRATOR_FIRSTNAME = null;
  public static final String USER_ADMINISTRATOR_LASTNAME = CSecurity.USER_ADMINISTRATOR_NAME;
  public static final String USER_ADMINISTRATOR_DESCRIPTION = null;
  public static final Locale USER_ADMINISTRATOR_LOCALE = AppCommonUI.DEFAULT_LOCALE;
  public static final Map <String, String> USER_ADMINISTRATOR_CUSTOMATTRS = null;

  private AppSecurity ()
  {}

  public static void init ()
  {
    final UserManager aUserMgr = PhotonSecurityManager.getUserMgr ();
    final UserGroupManager aUserGroupMgr = PhotonSecurityManager.getUserGroupMgr ();
    final RoleManager aRoleMgr = PhotonSecurityManager.getRoleMgr ();

    // Standard users
    if (!aUserMgr.containsUserWithID (USER_ADMINISTRATOR_ID))
    {
      final boolean bDisabled = false;
      aUserMgr.createPredefinedUser (USER_ADMINISTRATOR_ID,
                                     USER_ADMINISTRATOR_LOGINNAME,
                                     USER_ADMINISTRATOR_EMAIL,
                                     USER_ADMINISTRATOR_PASSWORD,
                                     USER_ADMINISTRATOR_FIRSTNAME,
                                     USER_ADMINISTRATOR_LASTNAME,
                                     USER_ADMINISTRATOR_DESCRIPTION,
                                     USER_ADMINISTRATOR_LOCALE,
                                     USER_ADMINISTRATOR_CUSTOMATTRS,
                                     bDisabled);
    }

    // Create all roles
    if (!aRoleMgr.containsRoleWithID (ROLE_CONFIG_ID))
      aRoleMgr.createPredefinedRole (ROLE_CONFIG_ID,
                                     ROLE_CONFIG_NAME,
                                     ROLE_CONFIG_DESCRIPTION,
                                     ROLE_CONFIG_CUSTOMATTRS);
    if (!aRoleMgr.containsRoleWithID (ROLE_VIEW_ID))
      aRoleMgr.createPredefinedRole (ROLE_VIEW_ID, ROLE_VIEW_NAME, ROLE_VIEW_DESCRIPTION, ROLE_VIEW_CUSTOMATTRS);
    if (!aRoleMgr.containsRoleWithID (ROLE_SG_OWNER_ID))
      aRoleMgr.createPredefinedRole (ROLE_SG_OWNER_ID,
                                     ROLE_SG_OWNER_NAME,
                                     ROLE_SG_OWNER_DESCRIPTION,
                                     ROLE_SG_OWNER_CUSTOMATTRS);

    // User group Administrators
    if (!aUserGroupMgr.containsUserGroupWithID (USERGROUP_ADMINISTRATORS_ID))
    {
      aUserGroupMgr.createPredefinedUserGroup (USERGROUP_ADMINISTRATORS_ID,
                                               USERGROUP_ADMINISTRATORS_NAME,
                                               USERGROUP_ADMINISTRATORS_DESCRIPTION,
                                               USERGROUP_ADMINISTRATORS_CUSTOMATTRS);
      // Assign administrator user to administrators user group
      aUserGroupMgr.assignUserToUserGroup (USERGROUP_ADMINISTRATORS_ID, USER_ADMINISTRATOR_ID);
    }
    aUserGroupMgr.assignRoleToUserGroup (USERGROUP_ADMINISTRATORS_ID, ROLE_CONFIG_ID);
    aUserGroupMgr.assignRoleToUserGroup (USERGROUP_ADMINISTRATORS_ID, ROLE_VIEW_ID);
    aUserGroupMgr.assignRoleToUserGroup (USERGROUP_ADMINISTRATORS_ID, ROLE_SG_OWNER_ID);

    // User group for Config users
    if (!aUserGroupMgr.containsUserGroupWithID (USERGROUP_CONFIG_ID))
      aUserGroupMgr.createPredefinedUserGroup (USERGROUP_CONFIG_ID,
                                               USERGROUP_CONFIG_NAME,
                                               USERGROUP_CONFIG_DESCRIPTION,
                                               USERGROUP_CONFIG_CUSTOMATTRS);
    aUserGroupMgr.assignRoleToUserGroup (USERGROUP_CONFIG_ID, ROLE_CONFIG_ID);

    // User group for View users
    if (!aUserGroupMgr.containsUserGroupWithID (USERGROUP_VIEW_ID))
      aUserGroupMgr.createPredefinedUserGroup (USERGROUP_VIEW_ID,
                                               USERGROUP_VIEW_NAME,
                                               USERGROUP_VIEW_DESCRIPTION,
                                               USERGROUP_VIEW_CUSTOMATTRS);
    aUserGroupMgr.assignRoleToUserGroup (USERGROUP_VIEW_ID, ROLE_VIEW_ID);
  }
}
