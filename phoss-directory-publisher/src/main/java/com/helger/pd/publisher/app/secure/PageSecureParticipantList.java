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

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.numeric.mutable.MutableInt;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSortedMap;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.tabular.IHCCell;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.publisher.app.pub.CMenuPublic;
import com.helger.pd.publisher.app.pub.PagePublicSearchSimple;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.pd.publisher.ui.PDDataTablesOnDemand;
import com.helger.pd.publisher.ui.PDTableColumnHelper;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.photon.ajax.decl.IAjaxFunctionDeclaration;
import com.helger.photon.bootstrap5.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.core.appid.CApplicationID;
import com.helger.photon.core.execcontext.LayoutExecutionContext;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.ajax.DataTablesOnDemandRequest;
import com.helger.photon.uictrls.datatables.ajax.DataTablesOnDemandResult;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.datatables.column.EDTColType;
import com.helger.url.ISimpleURL;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

public final class PageSecureParticipantList extends AbstractAppWebPage
{
  private static final String FIELD_PARTICIPANT_ID = "partid";
  private static final EContainedParticipantColumn [] COLUMNS = EContainedParticipantColumn.values ();

  /** Provides the rows of a single page - see {@link #_getOnDemandData(DataTablesOnDemandRequest, IRequestWebScopeWithoutResponse)} */
  private final IAjaxFunctionDeclaration m_aAjaxOnDemand = PDDataTablesOnDemand.registerSecure (this::_getOnDemandData);

  public PageSecureParticipantList (@NonNull @Nonempty final String sID)
  {
    super (sID, "Participant list");
  }

  /**
   * Get all participants contained in the search index, together with their business entity count.
   *
   * @return Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  private static ICommonsList <ContainedParticipant> _getAllContainedParticipants ()
  {
    final ICommonsSortedMap <IParticipantIdentifier, MutableInt> aAllIDs = PDMetaManager.getStorageMgr ()
                                                                                        .getAllContainedParticipantIDs ();
    final ICommonsList <ContainedParticipant> ret = new CommonsArrayList <> ();
    for (final Map.Entry <IParticipantIdentifier, MutableInt> aEntry : aAllIDs.entrySet ())
      ret.add (new ContainedParticipant (aEntry.getKey (), aEntry.getValue ().intValue ()));
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
  private HCTable _createTable (@NonNull final WebPageExecutionContext aWPEC)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    // The column names are the IDs of EContainedParticipantColumn, so that the sort order requested
    // by the client can be resolved onto the respective comparator
    return new HCTable (new DTCol ("ID").setName (EContainedParticipantColumn.PARTICIPANT.getID ()),
                        new DTCol ("Entities").setDisplayType (EDTColType.INT, aDisplayLocale)
                                              .setName (EContainedParticipantColumn.ENTITIES.getID ()),
                        new BootstrapDTColAction (aDisplayLocale).setOrderable (false)).setID (getID ());
  }

  private static void _addRow (@NonNull final WebPageExecutionContext aWPEC,
                               @NonNull final HCRow aRow,
                               @NonNull final ContainedParticipant aObj)
  {
    final String sParticipantID = aObj.getParticipantID ().getURIEncoded ();

    aRow.addCell (sParticipantID);
    aRow.addCell (Integer.toString (aObj.getEntityCount ()));

    final IHCCell <?> aActionCell = aRow.addCell ();
    final ISimpleURL aShowDetails = aWPEC.getLinkToMenuItem (CApplicationID.APP_ID_PUBLIC,
                                                             CMenuPublic.MENU_SEARCH_SIMPLE)
                                         .add (PagePublicSearchSimple.FIELD_QUERY, sParticipantID)
                                         .add (CPageParam.PARAM_ACTION, CPageParam.ACTION_VIEW)
                                         .add (PagePublicSearchSimple.FIELD_PARTICIPANT_ID, sParticipantID);
    aActionCell.addChild (new HCA (aShowDetails).addChild ("Search"));
    aActionCell.addChild (" ");
    final ISimpleURL aReIndex = aWPEC.getLinkToMenuItem (CMenuSecure.MENU_INDEX_MANUALLY)
                                     .add (PageSecureIndexManually.FIELD_PARTICIPANT_ID, sParticipantID)
                                     .add (CPageParam.PARAM_ACTION, CPageParam.ACTION_PERFORM);
    aActionCell.addChild (new HCA (aReIndex).addChild ("Reindex"));
    aActionCell.addChild (" ");
    final ISimpleURL aDelete = aWPEC.getSelfHref ()
                                    .add (FIELD_PARTICIPANT_ID, sParticipantID)
                                    .add (CPageParam.PARAM_ACTION, CPageParam.ACTION_DELETE);
    aActionCell.addChild (new HCA (aDelete).addChild ("Delete"));
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
    // The list is a copy already, so it may be filtered and sorted in place
    final ICommonsList <ContainedParticipant> aAllItems = _getAllContainedParticipants ();
    final String [] aSearchTexts = aRequest.getSearchTexts ();
    final long nTotalCount = aAllItems.size ();
    final long nFilteredCount = PDTableColumnHelper.getCount (COLUMNS, aAllItems, aSearchTexts);

    final ICommonsList <HCRow> aRows = new CommonsArrayList <> ();
    for (final ContainedParticipant aObj : PDTableColumnHelper.getPage (COLUMNS,
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
    final IRequestWebScopeWithoutResponse aRequestScope = aWPEC.getRequestScope ();

    if (aWPEC.hasAction (CPageParam.ACTION_DELETE))
    {
      final String sParticipantID = aRequestScope.params ().getAsString (FIELD_PARTICIPANT_ID);

      final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aParticipantID = aIdentifierFactory.parseParticipantIdentifier (sParticipantID);

      if (aParticipantID != null)
      {
        boolean bSuccess = false;
        try
        {
          bSuccess = PDMetaManager.getStorageMgr ().deleteEntry (aParticipantID, null, false) > 0;
        }
        catch (final IOException ex)
        {
          // ignore
        }
        if (bSuccess)
          aNodeList.addChild (info ("The participant ID '" + aParticipantID.getURIEncoded () + "' was deleted"));
        else
          aNodeList.addChild (error ("Error deleting participant ID '" + aParticipantID.getURIEncoded () + "'"));
      }
    }

    // The rows are filled by the AJAX function only
    final HCTable aTable = _createTable (aWPEC);
    aNodeList.addChild (aTable);
    aNodeList.addChild (PDDataTablesOnDemand.createDataTables (aWPEC, aTable, m_aAjaxOnDemand, COLUMNS));
  }
}
