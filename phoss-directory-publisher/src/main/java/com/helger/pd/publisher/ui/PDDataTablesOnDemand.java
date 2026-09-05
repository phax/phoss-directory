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
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.paging.SortField;
import com.helger.html.hc.html.tabular.HCColGroup;
import com.helger.html.hc.html.tabular.IHCCol;
import com.helger.html.hc.html.tabular.IHCTable;
import com.helger.html.jquery.JQueryAjaxBuilder;
import com.helger.html.jscode.JSAssocArray;
import com.helger.pd.publisher.ajax.CAjax;
import com.helger.photon.ajax.decl.IAjaxFunctionDeclaration;
import com.helger.photon.bootstrap5.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.uicore.page.IWebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.DataTablesOrder;
import com.helger.photon.uictrls.datatables.EDataTablesServerSideMode;
import com.helger.photon.uictrls.datatables.ajax.AjaxExecutorDataTables;
import com.helger.photon.uictrls.datatables.ajax.AjaxExecutorDataTablesOnDemand;
import com.helger.photon.uictrls.datatables.ajax.IDataTablesOnDemandDataProvider;
import com.helger.photon.uictrls.datatables.column.DTCol;

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
// TODO replace with the ph-oton version com.helger.photon.uictrls.datatables.ajax.DataTablesOnDemandHelper (since ph-oton 10.6.0)
@Immutable
public final class PDDataTablesOnDemand
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDDataTablesOnDemand.class);

  private PDDataTablesOnDemand ()
  {}

  private static int _getColumnIndexOfName (@NonNull final IHCTable <?> aTable, @NonNull final String sName)
  {
    final HCColGroup aColGroup = aTable.getColGroup ();
    if (aColGroup != null)
    {
      int nIndex = 0;
      for (final IHCCol <?> aCol : aColGroup.getAllColumns ())
      {
        if (aCol instanceof final DTCol aDTCol && sName.equals (aDTCol.getName ()))
          return nIndex;
        ++nIndex;
      }
    }
    return -1;
  }

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
   * Map the default order declared by the provided columns onto the column indices of the provided
   * table.
   *
   * @param aTable
   *        The table to map onto. May not be <code>null</code>. Its sortable columns must be named
   *        after the ID of the respective {@link IPDTableColumn}.
   * @param aColumns
   *        All available columns. May not be <code>null</code>.
   * @return <code>null</code> if the table contains none of the default order columns.
   * @see IPDTableColumn#getDefaultSortOrder()
   */
  @Nullable
  public static DataTablesOrder createInitialOrder (@NonNull final IHCTable <?> aTable,
                                                    @NonNull final IPDTableColumn <?> [] aColumns)
  {
    ValueEnforcer.notNull (aTable, "Table");
    ValueEnforcer.notNull (aColumns, "Columns");

    final DataTablesOrder ret = new DataTablesOrder ();
    boolean bAny = false;
    for (final SortField aSortField : PDTableColumnHelper.getAllDefaultSortFields (aColumns))
    {
      final int nColumnIndex = _getColumnIndexOfName (aTable, aSortField.getFieldName ());
      if (nColumnIndex < 0)
      {
        LOGGER.warn ("The default sort column '" +
                     aSortField.getFieldName () +
                     "' is not part of the table '" +
                     aTable.getID () +
                     "' - the initial order of the UI and the default order of the backend may differ");
        continue;
      }
      ret.addColumn (nColumnIndex, aSortField.getSortOrder ());
      bAny = true;
    }
    return bAny ? ret : null;
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
                                             @NonNull final IPDTableColumn <?> [] aColumns)
  {
    ValueEnforcer.notNull (aWPEC, "WPEC");
    ValueEnforcer.notNull (aTable, "Table");
    ValueEnforcer.notNull (aAjaxFunction, "AjaxFunction");
    ValueEnforcer.notNull (aColumns, "Columns");

    final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);

    // Overwrite the default AJAX URL of AppCommonUI, that points to the shared executor keeping a
    // copy of the whole table in the session
    aDataTables.setServerSideMode (EDataTablesServerSideMode.ON_DEMAND)
               .setAjaxBuilder (new JQueryAjaxBuilder ().url (aAjaxFunction.getInvocationURL (aWPEC.getRequestScope ()))
                                                        .data (new JSAssocArray ().add (AjaxExecutorDataTables.OBJECT_ID,
                                                                                        aTable.getID ())));

    // The initial order of the table is the default order declared by the columns, so that the
    // order shown in the UI and the order the data provider falls back to cannot drift apart
    final DataTablesOrder aInitialOrder = createInitialOrder (aTable, aColumns);
    if (aInitialOrder != null)
      aDataTables.setInitialOrder (aInitialOrder);

    return aDataTables;
  }
}
