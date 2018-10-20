/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.app.secure.page;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.compare.ESortOrder;
import com.helger.commons.datetime.PDTToString;
import com.helger.commons.url.ISimpleURL;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.tabular.IHCCell;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.index.IIndexerWorkItem;
import com.helger.pd.indexer.reindex.IReIndexWorkItem;
import com.helger.pd.indexer.reindex.IReIndexWorkItemList;
import com.helger.pd.publisher.app.PDPMetaManager;
import com.helger.pd.publisher.ui.AbstractAppWebPageForm;
import com.helger.pd.settings.PDServerConfiguration;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.url.IPeppolURLProvider;
import com.helger.photon.bootstrap3.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap3.alert.BootstrapQuestionBox;
import com.helger.photon.bootstrap3.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap3.button.BootstrapButton;
import com.helger.photon.bootstrap3.button.BootstrapButtonToolbar;
import com.helger.photon.bootstrap3.form.BootstrapForm;
import com.helger.photon.bootstrap3.form.BootstrapFormGroup;
import com.helger.photon.bootstrap3.form.BootstrapViewForm;
import com.helger.photon.bootstrap3.pages.handler.AbstractBootstrapWebPageActionHandlerDelete;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.EWebPageFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.datatables.column.EDTColType;

public abstract class AbstractPageSecureReIndex extends AbstractAppWebPageForm <IReIndexWorkItem>
{
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
      protected void showDeleteQuery (@Nonnull final WebPageExecutionContext aWPEC,
                                      @Nonnull final BootstrapForm aForm,
                                      @Nonnull final IReIndexWorkItem aSelectedObject)
      {
        aForm.addChild (new BootstrapQuestionBox ().addChild ("Are you sure to delete the item " +
                                                              aSelectedObject.getDisplayName () +
                                                              "?"));
      }

      @Override
      protected void performDelete (@Nonnull final WebPageExecutionContext aWPEC,
                                    @Nonnull final IReIndexWorkItem aSelectedObject)
      {
        if (getReIndexWorkItemList ().deleteItem (aSelectedObject.getID ()).isChanged ())
        {
          aWPEC.postRedirectGetInternal (new BootstrapSuccessBox ().addChild ("The item " +
                                                                              aSelectedObject.getDisplayName () +
                                                                              " was successfully deleted!"));
        }
        else
        {
          aWPEC.postRedirectGetInternal (new BootstrapErrorBox ().addChild ("Error deleting the item " +
                                                                            aSelectedObject.getDisplayName () +
                                                                            "!"));
        }
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
    final IPeppolURLProvider aURLProvider = PDServerConfiguration.getURLProvider ();

    final IIndexerWorkItem aWorkItem = aSelectedObject.getWorkItem ();
    final IParticipantIdentifier aParticipantID = aWorkItem.getParticipantID ();

    final BootstrapViewForm aViewForm = aNodeList.addAndReturnChild (new BootstrapViewForm ());
    aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Creation datetime")
                                                     .setCtrl (PDTToString.getAsString (aWorkItem.getCreationDateTime (),
                                                                                        aDisplayLocale)));
    aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Participant ID")
                                                     .setCtrl (aParticipantID.getURIEncoded ()));

    final String sBCSuffix = "/businesscard/" + aParticipantID.getURIPercentEncoded ();
    {
      final HCNodeList aURLs = new HCNodeList ();
      for (final ISMLInfo aSMLInfo : PDPMetaManager.getSMLInfoMgr ().getAllSMLInfos ())
      {
        if (aURLs.hasChildren ())
          aURLs.addChild (new HCDiv ().addChild ("or"));
        try
        {
          aURLs.addChild (new HCDiv ().addChild (HCA.createLinkedWebsite (aURLProvider.getSMPURIOfParticipant (aParticipantID,
                                                                                                               aSMLInfo)
                                                                                      .toString () +
                                                                          sBCSuffix)));
        }
        catch (final IllegalArgumentException ex)
        {
          // Non existing participant!
          aURLs.addChild (new HCDiv ().addChild (aParticipantID.getURIPercentEncoded () +
                                                 " on " +
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
    aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Owner").setCtrl (aWorkItem.getOwnerID ()));
    aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Requesting host")
                                                     .setCtrl (aWorkItem.getRequestingHost ()));
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
    }

    final HCTable aTable = new HCTable (new DTCol ("Reg date").setDisplayType (EDTColType.DATETIME, aDisplayLocale)
                                                              .setInitialSorting (ESortOrder.DESCENDING),
                                        new DTCol ("Participant"),
                                        new DTCol ("Action"),
                                        new DTCol ("Retries").setDisplayType (EDTColType.INT, aDisplayLocale),
                                        m_bDeadIndex ? null
                                                     : new DTCol ("Next retry").setDisplayType (EDTColType.DATETIME,
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
      aActionCell.addChild (createDeleteLink (aWPEC, aItem));
    }

    aNodeList.addChild (aTable);
    aNodeList.addChild (BootstrapDataTables.createDefaultDataTables (aWPEC, aTable));
  }
}
