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
package com.helger.pd.publisher.ui;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.html.hc.html.tabular.IHCTable;
import com.helger.pd.publisher.ajax.CAjax;
import com.helger.photon.ajax.decl.IAjaxFunctionDeclaration;
import com.helger.photon.bootstrap5.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.paging.ITableColumn;
import com.helger.photon.uicore.page.IWebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.EDataTablesServerSideMode;
import com.helger.photon.uictrls.datatables.ajax.AjaxExecutorDataTables;
import com.helger.photon.uictrls.datatables.ajax.AjaxExecutorDataTablesOnDemand;
import com.helger.photon.uictrls.datatables.ajax.DataTablesOnDemandHelper;
import com.helger.photon.uictrls.datatables.ajax.IDataTablesOnDemandDataProvider;

/**
 * Helper to run a DataTables in the server side mode {@link EDataTablesServerSideMode#ON_DEMAND}:
 * the table is never rendered as a whole, instead every AJAX request queries only the rows of the
 * requested page.<br>
 * The default configuration of {@link com.helger.pd.publisher.app.AppCommonUI} points all
 * DataTables to the shared {@link AjaxExecutorDataTables}, which keeps a rendered copy of the whole
 * table in the session. This class overrides that per table.
 *
 * @author Philip Helger
 * @since 0.17.3
 */
@Immutable
public final class PDDataTablesOnDemand
{
  private PDDataTablesOnDemand ()
  {}

  /**
   * Register the AJAX function that provides the rows of a single page of a secure page. Call this
   * once per page instance, e.g. from a field initializer.
   *
   * @param aDataProvider
   *        The provider that queries and renders the rows. May not be <code>null</code>.
   * @return The created function declaration. Never <code>null</code>.
   */
  @NonNull
  public static IAjaxFunctionDeclaration registerSecure (@NonNull final IDataTablesOnDemandDataProvider aDataProvider)
  {
    ValueEnforcer.notNull (aDataProvider, "DataProvider");
    return CAjax.addAjaxWithLogin (new AjaxExecutorDataTablesOnDemand (aDataProvider));
  }

  /**
   * Create the DataTables for the provided table and switch it to the "on demand" server side mode,
   * so that the AJAX requests are answered by the provided function instead of by the shared
   * {@link AjaxExecutorDataTables}.
   *
   * @param aWPEC
   *        The current web page execution context. May not be <code>null</code>.
   * @param aTable
   *        The table to be turned into a DataTables. May not be <code>null</code>. It must have an
   *        ID.
   * @param aAjaxFunction
   *        The AJAX function providing the rows, as created by
   *        {@link #registerSecure(IDataTablesOnDemandDataProvider)}. May not be <code>null</code>.
   * @param aColumns
   *        All available columns, to determine the initial order. May not be <code>null</code>.
   * @return The created DataTables. Never <code>null</code>.
   */
  @NonNull
  public static DataTables createDataTables (@NonNull final IWebPageExecutionContext aWPEC,
                                             @NonNull final IHCTable <?> aTable,
                                             @NonNull final IAjaxFunctionDeclaration aAjaxFunction,
                                             @NonNull final ITableColumn <?> [] aColumns)
  {
    ValueEnforcer.notNull (aWPEC, "WPEC");
    ValueEnforcer.notNull (aTable, "Table");
    ValueEnforcer.notNull (aAjaxFunction, "AjaxFunction");
    ValueEnforcer.notNull (aColumns, "Columns");

    final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);

    return DataTablesOnDemandHelper.applyOnDemandMode (aDataTables,
                                                       aTable,
                                                       aAjaxFunction,
                                                       aWPEC.getRequestScope (),
                                                       aColumns);
  }
}
