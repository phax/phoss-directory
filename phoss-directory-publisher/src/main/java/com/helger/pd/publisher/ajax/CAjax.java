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
package com.helger.pd.publisher.ajax;

import java.util.function.Predicate;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.pd.publisher.app.AppCommonUI;
import com.helger.photon.ajax.GlobalAjaxInvoker;
import com.helger.photon.ajax.IAjaxRegistry;
import com.helger.photon.ajax.decl.AjaxFunctionDeclaration;
import com.helger.photon.ajax.decl.IAjaxFunctionDeclaration;
import com.helger.photon.ajax.executor.IAjaxExecutor;
import com.helger.photon.security.login.LoggedInUserManager;
import com.helger.photon.uictrls.datatables.ajax.AjaxExecutorDataTables;
import com.helger.photon.uictrls.datatables.ajax.AjaxExecutorDataTablesI18N;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * This class defines the available ajax functions for the application.
 *
 * @author Philip Helger
 */
@Immutable
public final class CAjax
{
  public static final Predicate <? super IRequestWebScopeWithoutResponse> FILTER_IS_USER_LOGGED_IN = x -> LoggedInUserManager.getInstance ()
                                                                                                                             .isUserLoggedInInCurrentSession ();

  public static final IAjaxFunctionDeclaration DATATABLES = AjaxFunctionDeclaration.builder ("dataTables")
                                                                                   .executor (AjaxExecutorDataTables.class)
                                                                                   .build ();
  public static final IAjaxFunctionDeclaration DATATABLES_I18N = AjaxFunctionDeclaration.builder ("datatables-i18n")
                                                                                        .executor (new AjaxExecutorDataTablesI18N (AppCommonUI.DEFAULT_LOCALE))
                                                                                        .build ();

  private CAjax ()
  {}

  public static void initAjax (@NonNull final IAjaxRegistry aAjaxRegistry)
  {
    aAjaxRegistry.registerFunction (DATATABLES);
    aAjaxRegistry.registerFunction (DATATABLES_I18N);
  }

  /**
   * Register an additional AJAX function that may only be invoked by a logged in user. It is used
   * for the AJAX functions that are created per page instance, and therefore cannot be declared as
   * a constant of this class.
   *
   * @param aExecutor
   *        The executor to be invoked. May not be <code>null</code>.
   * @return The created function declaration with a random name. Never <code>null</code>.
   */
  @NonNull
  public static AjaxFunctionDeclaration addAjaxWithLogin (@NonNull final IAjaxExecutor aExecutor)
  {
    // Random name
    final AjaxFunctionDeclaration aFunction = AjaxFunctionDeclaration.builder ()
                                                                     .executor (aExecutor)
                                                                     .filter (FILTER_IS_USER_LOGGED_IN)
                                                                     .build ();
    GlobalAjaxInvoker.getInstance ().getRegistry ().registerFunction (aFunction);
    return aFunction;
  }
}
