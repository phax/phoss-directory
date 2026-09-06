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

import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.format.PDTToString;
import com.helger.datetime.helper.PDTFactory;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.index.IIndexerWorkItem;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.pd.publisher.ui.PDCommonUI;
import com.helger.pd.publisher.ui.PDDataTablesOnDemand;
import com.helger.photon.ajax.decl.IAjaxFunctionDeclaration;
import com.helger.photon.bootstrap5.button.BootstrapButton;
import com.helger.photon.bootstrap5.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap5.table.BootstrapTable;
import com.helger.photon.core.execcontext.LayoutExecutionContext;
import com.helger.photon.core.paging.TableColumnHelper;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.ajax.DataTablesOnDemandRequest;
import com.helger.photon.uictrls.datatables.ajax.DataTablesOnDemandResult;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.datatables.column.EDTColType;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

public final class PageSecureListIndex extends AbstractAppWebPage
{
  private static final String TABLE_ID = "indexqueue";
  private static final EIndexerWorkItemColumn [] COLUMNS = EIndexerWorkItemColumn.values ();

  /**
   * Provides the rows of a single page - see
   * {@link #_getOnDemandData(DataTablesOnDemandRequest, IRequestWebScopeWithoutResponse)}
   */
  private final IAjaxFunctionDeclaration m_aAjaxOnDemand = PDDataTablesOnDemand.registerSecure (this::_getOnDemandData);

  public PageSecureListIndex (@NonNull @Nonempty final String sID)
  {
    super (sID, "Index Queue");
  }

  /**
   * Get a snapshot of all work items currently in the indexer work queue. The queue also contains
   * the internal "stop" object, so everything else is filtered out.
   *
   * @return Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  private static ICommonsList <IIndexerWorkItem> _getAllQueuedWorkItems ()
  {
    final LinkedBlockingQueue <Object> aQueue = PDMetaManager.getIndexerMgr ()
                                                             .getIndexerWorkQueue ()
                                                             .internalGetQueue ();
    final ICommonsList <IIndexerWorkItem> ret = new CommonsArrayList <> ();
    for (final Object o : aQueue)
      if (o instanceof final IIndexerWorkItem aObj)
        ret.add (aObj);
    return ret;
  }

  /**
   * Create the table with the columns only. The rows are added by the "on demand" AJAX function,
   * because only the rows of the currently displayed page are ever queried and rendered.
   *
   * @param aWPEC
   *        The current context. May not be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @NonNull
  private static BootstrapTable _createTable (@NonNull final WebPageExecutionContext aWPEC)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    // The column names are the IDs of EIndexerWorkItemColumn, so that the sort order requested by
    // the client can be resolved onto the respective comparator
    final BootstrapTable ret = new BootstrapTable (new DTCol ("Queue date time").setDisplayType (EDTColType.DATETIME,
                                                                                                 aDisplayLocale)
                                                                                .setName (EIndexerWorkItemColumn.QUEUE_DATE.getID ()),
                                                   new DTCol ("Participant ID").setName (EIndexerWorkItemColumn.PARTICIPANT.getID ()),
                                                   new DTCol ("Action").setName (EIndexerWorkItemColumn.ACTION.getID ()),
                                                   new DTCol ("Owner").setName (EIndexerWorkItemColumn.OWNER.getID ()),
                                                   new DTCol ("Requestor").setName (EIndexerWorkItemColumn.REQUESTOR.getID ()));
    return ret.setID (TABLE_ID);
  }

  private static void _addRow (@NonNull final WebPageExecutionContext aWPEC,
                               @NonNull final HCRow aRow,
                               @NonNull final IIndexerWorkItem aObj)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    aRow.addCell (PDTToString.getAsString (aObj.getCreationDateTime (), aDisplayLocale));
    aRow.addCell (aObj.getParticipantID ().getURIEncoded ());
    aRow.addCell (aObj.getType ().getDisplayName ());
    aRow.addCell (aObj.getOwnerID ());
    aRow.addCell (aObj.getRequestingHost ());
  }

  /**
   * Provide the rows of a single page. Only the entries of the requested page are rendered -
   * nothing is kept in the session.
   *
   * @param aRequest
   *        The DataTables request. May not be <code>null</code>.
   * @param aRequestScope
   *        The current request scope. May not be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @NonNull
  private DataTablesOnDemandResult _getOnDemandData (@NonNull final DataTablesOnDemandRequest aRequest,
                                                     @NonNull final IRequestWebScopeWithoutResponse aRequestScope)
  {
    final WebPageExecutionContext aWPEC = new WebPageExecutionContext (LayoutExecutionContext.createForAjaxOrAction (aRequestScope),
                                                                       this);
    // The list is a snapshot already, so it may be filtered and sorted in place
    final ICommonsList <IIndexerWorkItem> aAllItems = _getAllQueuedWorkItems ();
    final String [] aSearchTexts = aRequest.getSearchTexts ();
    final long nTotalCount = aAllItems.size ();
    final long nFilteredCount = TableColumnHelper.getCount (COLUMNS, aAllItems, aSearchTexts);

    final ICommonsList <HCRow> aRows = new CommonsArrayList <> ();
    for (final IIndexerWorkItem aObj : TableColumnHelper.getPage (COLUMNS,
                                                                  aAllItems,
                                                                  aRequest.getPagingSpec (),
                                                                  aSearchTexts))
    {
      final HCRow aRow = new HCRow ();
      _addRow (aWPEC, aRow, aObj);
      aRows.add (aRow);
    }
    return new DataTablesOnDemandResult (nTotalCount, nFilteredCount, aRows);
  }

  @Override
  protected void fillContent (final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    // Add toolbar
    {
      final BootstrapButtonToolbar aToolbar = aNodeList.addAndReturnChild (new BootstrapButtonToolbar (aWPEC));
      aToolbar.addChild (new BootstrapButton ().addChild ("Refresh")
                                               .setIcon (EDefaultIcon.REFRESH)
                                               .setOnClick (aWPEC.getSelfHref ()));
      aToolbar.addChild (span ("Current server time: " +
                               PDTToString.getAsString (PDTFactory.getCurrentLocalTime (), aDisplayLocale)).addClass (
                                                                                                                      PDCommonUI.CSS_CLASS_VERTICAL_PADDED_TEXT));
    }

    // Count only, to avoid creating a copy of the whole queue just for the message
    int nLength = 0;
    for (final Object o : PDMetaManager.getIndexerMgr ().getIndexerWorkQueue ().internalGetQueue ())
      if (o instanceof IIndexerWorkItem)
        ++nLength;
    if (nLength == 0)
    {
      aNodeList.addChild (success ("The Index Queue is currently empty"));
    }
    else
    {
      aNodeList.addChild (info ("The Index Queue contains " + nLength + " entries"));

      // The rows are filled by the AJAX function only
      final BootstrapTable aTable = _createTable (aWPEC);
      aNodeList.addChild (aTable);
      aNodeList.addChild (PDDataTablesOnDemand.createDataTables (aWPEC, aTable, m_aAjaxOnDemand, COLUMNS));
    }
  }
}
