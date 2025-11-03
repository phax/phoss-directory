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

import java.util.Locale;

import com.helger.annotation.Nonempty;
import com.helger.base.compare.ESortOrder;
import com.helger.datetime.format.PDTToString;
import com.helger.datetime.helper.PDTFactory;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.tabular.IHCCell;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.index.EIndexerWorkItemType;
import com.helger.pd.indexer.index.IIndexerWorkItem;
import com.helger.pd.indexer.mgr.PDIndexerManager;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.reindex.IReIndexWorkItem;
import com.helger.pd.indexer.reindex.IReIndexWorkItemList;
import com.helger.pd.indexer.settings.PDServerConfiguration;
import com.helger.pd.indexer.storage.CPDStorage;
import com.helger.pd.publisher.app.PDPMetaManager;
import com.helger.pd.publisher.ui.AbstractAppWebPageForm;
import com.helger.pd.publisher.ui.PDCommonUI;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.form.BootstrapForm;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.form.BootstrapViewForm;
import com.helger.photon.bootstrap4.pages.handler.AbstractBootstrapWebPageActionHandler;
import com.helger.photon.bootstrap4.pages.handler.AbstractBootstrapWebPageActionHandlerDelete;
import com.helger.photon.bootstrap4.pages.handler.AbstractBootstrapWebPageActionHandlerWithQuery;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.EShowList;
import com.helger.photon.uicore.page.EWebPageFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.datatables.column.EDTColType;
import com.helger.smpclient.url.ISMPURLProvider;
import com.helger.smpclient.url.SMPDNSResolutionException;
import com.helger.url.ISimpleURL;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class AbstractPageSecureReIndex extends AbstractAppWebPageForm <IReIndexWorkItem>
{
  private static final String ACTION_DELETE_ALL = "deleteall";
  private static final String ACTION_REINDEX_NOW = "reindexnow";

  private final boolean m_bDeadIndex;

  public AbstractPageSecureReIndex (@Nonnull @Nonempty final String sID,
                                    @Nonnull final String sName,
                                    final boolean bDeadIndex)
  {
    super (sID, sName);
    m_bDeadIndex = bDeadIndex;
    setDeleteHandler (new AbstractBootstrapWebPageActionHandlerDelete <IReIndexWorkItem, WebPageExecutionContext> ()
    {
      @Override
      protected void showQuery (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nonnull final BootstrapForm aForm,
                                @Nullable final IReIndexWorkItem aSelectedObject)
      {
        aForm.addChild (question ("Are you sure to delete the item " + aSelectedObject.getDisplayName () + "?"));
      }

      @Override
      protected void performAction (@Nonnull final WebPageExecutionContext aWPEC,
                                    @Nullable final IReIndexWorkItem aSelectedObject)
      {
        if (getReIndexWorkItemList ().deleteItem (aSelectedObject.getID ()).isChanged ())
        {
          aWPEC.postRedirectGetInternal (success ("The item " +
                                                  aSelectedObject.getDisplayName () +
                                                  " was successfully deleted!"));
        }
        else
        {
          aWPEC.postRedirectGetInternal (error ("Error deleting the item " + aSelectedObject.getDisplayName () + "!"));
        }
      }
    });
    addCustomHandler (ACTION_DELETE_ALL,
                      new AbstractBootstrapWebPageActionHandlerWithQuery <IReIndexWorkItem, WebPageExecutionContext> (false,
                                                                                                                      ACTION_DELETE_ALL,
                                                                                                                      "deleteall")
                      {
                        @Override
                        protected void showQuery (@Nonnull final WebPageExecutionContext aWPEC,
                                                  @Nonnull final BootstrapForm aForm,
                                                  @Nullable final IReIndexWorkItem aSelectedObject)
                        {
                          aForm.addChild (question ("Are you sure to delete all items?"));
                        }

                        @Override
                        protected void performAction (final WebPageExecutionContext aWPEC,
                                                      final IReIndexWorkItem aSelectedObject)
                        {
                          if (getReIndexWorkItemList ().deleteAllItems ().isChanged ())
                            aWPEC.postRedirectGetInternal (success ("Successfully deleted all items."));
                          else
                            aWPEC.postRedirectGetInternal (warn ("Seems like there is no item to be deleted."));
                        }

                      });
    addCustomHandler (ACTION_REINDEX_NOW,
                      new AbstractBootstrapWebPageActionHandler <IReIndexWorkItem, WebPageExecutionContext> (true)
                      {
                        @Nonnull
                        public EShowList handleAction (@Nonnull final WebPageExecutionContext aWPEC,
                                                       @Nonnull final IReIndexWorkItem aSelectedObject)
                        {
                          final IParticipantIdentifier aParticipantID = aSelectedObject.getWorkItem ()
                                                                                       .getParticipantID ();
                          if (PDMetaManager.getIndexerMgr ()
                                           .queueWorkItem (aParticipantID,
                                                           EIndexerWorkItemType.CREATE_UPDATE,
                                                           CPDStorage.OWNER_MANUALLY_TRIGGERED,
                                                           PDIndexerManager.HOST_LOCALHOST)
                                           .isChanged ())
                          {
                            aWPEC.postRedirectGetInternal (success ("The re-indexing of participant ID '" +
                                                                    aParticipantID.getURIEncoded () +
                                                                    "' was successfully triggered!"));
                          }
                          else
                          {
                            aWPEC.postRedirectGetInternal (warn ("Participant ID '" +
                                                                 aParticipantID.getURIEncoded () +
                                                                 "' is already in the indexing queue!"));
                          }
                          return EShowList.SHOW_LIST;
                        }
                      });
  }

  @Nonnull
  protected abstract IReIndexWorkItemList getReIndexWorkItemList ();

  @Override
  protected IReIndexWorkItem getSelectedObject (@Nonnull final WebPageExecutionContext aWPEC, final String sID)
  {
    return getReIndexWorkItemList ().getItemOfID (sID);
  }

  @Override
  protected boolean isActionAllowed (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final EWebPageFormAction eFormAction,
                                     @Nullable final IReIndexWorkItem aSelectedObject)
  {
    if (eFormAction.isDelete ())
      return true;
    if (eFormAction.isWriting ())
      return false;
    return super.isActionAllowed (aWPEC, eFormAction, aSelectedObject);
  }

  @Override
  protected void showSelectedObject (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final IReIndexWorkItem aSelectedObject)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final ISMPURLProvider aURLProvider = PDServerConfiguration.getURLProvider ();

    final IIndexerWorkItem aWorkItem = aSelectedObject.getWorkItem ();
    final IParticipantIdentifier aParticipantID = aWorkItem.getParticipantID ();

    final BootstrapViewForm aViewForm = aNodeList.addAndReturnChild (new BootstrapViewForm ());
    aViewForm.setLeft (2);
    aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Creation datetime")
                                                     .setCtrl (PDTToString.getAsString (aWorkItem.getCreationDateTime (),
                                                                                        aDisplayLocale)));
    aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Participant ID")
                                                     .setCtrl (code (aParticipantID.getURIEncoded ())));

    final String sBCSuffix = "/businesscard/" + aParticipantID.getURIPercentEncoded ();
    {
      final HCNodeList aURLs = new HCNodeList ();
      for (final ISMLInfo aSMLInfo : PDPMetaManager.getSMLInfoMgr ().getAll ())
      {
        if (aURLs.hasChildren ())
          aURLs.addChild (div ("or"));

        try
        {
          aURLs.addChild (div (HCA.createLinkedWebsite (aURLProvider.getSMPURIOfParticipant (aParticipantID, aSMLInfo)
                                                                    .toString () + sBCSuffix)));
        }
        catch (final SMPDNSResolutionException ex)
        {
          // Non existing participant or cache issue
          aURLs.addChild (div ("Error to get SMP URI on " +
                               aSMLInfo.getDisplayName () +
                               " @ " +
                               sBCSuffix +
                               " [" +
                               ex.getMessage () +
                               "]"));
        }
      }
      aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Business Card URL").setCtrl (aURLs));
    }
    aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Action type")
                                                     .setCtrl (aWorkItem.getType ().getDisplayName ()));
    aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Owner").setCtrl (code (aWorkItem.getOwnerID ())));
    aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Requesting host")
                                                     .setCtrl (code (aWorkItem.getRequestingHost ())));
    aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Retries so far")
                                                     .setCtrl (Integer.toString (aSelectedObject.getRetryCount ())));
    if (aSelectedObject.hasPreviousRetryDT ())
      aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Previous retry")
                                                       .setCtrl (PDTToString.getAsString (aSelectedObject.getPreviousRetryDT (),
                                                                                          aDisplayLocale)));
    if (!m_bDeadIndex)
      aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Next retry")
                                                       .setCtrl (PDTToString.getAsString (aSelectedObject.getNextRetryDT (),
                                                                                          aDisplayLocale)));
    aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Last retry")
                                                     .setCtrl (PDTToString.getAsString (aSelectedObject.getMaxRetryDT (),
                                                                                        aDisplayLocale)));
  }

  @Override
  protected void validateAndSaveInputParameters (@Nonnull final WebPageExecutionContext aWPEC,
                                                 @Nullable final IReIndexWorkItem aSelectedObject,
                                                 @Nonnull final FormErrorList aFormErrors,
                                                 @Nonnull final EWebPageFormAction eFormAction)
  {
    throw new UnsupportedOperationException ();
  }

  @Override
  protected void showInputForm (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nullable final IReIndexWorkItem aSelectedObject,
                                @Nonnull final BootstrapForm aForm,
                                final boolean bFormSubmitted,
                                @Nonnull final EWebPageFormAction eFormAction,
                                @Nonnull final FormErrorList aFormErrors)
  {
    throw new UnsupportedOperationException ();
  }

  @Override
  protected void showListOfExistingObjects (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    // Add toolbar
    {
      final BootstrapButtonToolbar aToolbar = aNodeList.addAndReturnChild (new BootstrapButtonToolbar (aWPEC));
      aToolbar.addChild (new BootstrapButton ().addChild ("Refresh")
                                               .setIcon (EDefaultIcon.REFRESH)
                                               .setOnClick (aWPEC.getSelfHref ()));
      aToolbar.addChild (new BootstrapButton ().addChild ("Delete all entries")
                                               .setIcon (EDefaultIcon.DELETE)
                                               .setOnClick (aWPEC.getSelfHref ()
                                                                 .add (CPageParam.PARAM_ACTION, ACTION_DELETE_ALL)));
      aToolbar.addChild (span ("Current server time: " +
                               PDTToString.getAsString (PDTFactory.getCurrentLocalTime (), aDisplayLocale)).addClass (
                                                                                                                      PDCommonUI.CSS_CLASS_VERTICAL_PADDED_TEXT));
    }

    final HCTable aTable = new HCTable (new DTCol ("Reg date").setDisplayType (EDTColType.DATETIME, aDisplayLocale)
                                                              .setInitialSorting (ESortOrder.DESCENDING),
                                        new DTCol ("Participant"),
                                        new DTCol ("Action"),
                                        new DTCol ("Retries").setDisplayType (EDTColType.INT, aDisplayLocale),
                                        m_bDeadIndex ? null : new DTCol ("Next retry").setDisplayType (
                                                                                                       EDTColType.DATETIME,
                                                                                                       aDisplayLocale),
                                        new DTCol ("Last retry").setDisplayType (EDTColType.DATETIME, aDisplayLocale),
                                        new BootstrapDTColAction (aDisplayLocale)).setID (getID ());

    for (final IReIndexWorkItem aItem : getReIndexWorkItemList ().getAllItems ())
    {
      final ISimpleURL aViewLink = createViewURL (aWPEC, aItem);
      final IIndexerWorkItem aWorkItem = aItem.getWorkItem ();

      final HCRow aRow = aTable.addBodyRow ();
      aRow.addCell (new HCA (aViewLink).addChild (PDTToString.getAsString (aWorkItem.getCreationDateTime (),
                                                                           aDisplayLocale)));
      aRow.addCell (aWorkItem.getParticipantID ().getURIEncoded ());
      aRow.addCell (aWorkItem.getType ().getDisplayName ());
      aRow.addCell (Integer.toString (aItem.getRetryCount ()));
      if (!m_bDeadIndex)
        aRow.addCell (PDTToString.getAsString (aItem.getNextRetryDT (), aDisplayLocale));
      aRow.addCell (PDTToString.getAsString (aItem.getMaxRetryDT (), aDisplayLocale));

      final IHCCell <?> aActionCell = aRow.addCell ();
      if (m_bDeadIndex)
      {
        aActionCell.addChild (new HCA (aWPEC.getSelfHref ()
                                            .add (CPageParam.PARAM_ACTION, ACTION_REINDEX_NOW)
                                            .add (CPageParam.PARAM_OBJECT, aItem.getID ())).setTitle (
                                                                                                      "Re-index the entry now")
                                                                                           .addChild (EDefaultIcon.NEXT.getAsNode ()));
        aActionCell.addChild (" ");
      }
      aActionCell.addChild (createDeleteLink (aWPEC, aItem));
    }

    aNodeList.addChild (aTable);
    aNodeList.addChild (BootstrapDataTables.createDefaultDataTables (aWPEC, aTable));
  }
}
