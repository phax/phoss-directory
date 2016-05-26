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
package com.helger.pd.publisher.app.secure.page;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.compare.ESortOrder;
import com.helger.commons.errorlist.FormErrors;
import com.helger.commons.url.ISimpleURL;
import com.helger.datetime.format.PDTToString;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.domain.IReIndexWorkItem;
import com.helger.pd.indexer.domain.IndexerWorkItem;
import com.helger.pd.indexer.mgr.IReIndexWorkItemList;
import com.helger.pd.publisher.ui.AbstractAppWebPageForm;
import com.helger.photon.bootstrap3.button.BootstrapButton;
import com.helger.photon.bootstrap3.button.BootstrapButtonToolbar;
import com.helger.photon.bootstrap3.form.BootstrapForm;
import com.helger.photon.bootstrap3.form.BootstrapFormGroup;
import com.helger.photon.bootstrap3.form.BootstrapViewForm;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.EWebPageFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.datatables.column.EDTColType;

public abstract class AbstractPageSecureReIndex extends AbstractAppWebPageForm <IReIndexWorkItem>
{
  public AbstractPageSecureReIndex (@Nonnull @Nonempty final String sID, @Nonnull final String sName)
  {
    super (sID, sName);
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

    final IndexerWorkItem aWorkItem = aSelectedObject.getWorkItem ();

    final BootstrapViewForm aViewForm = aNodeList.addAndReturnChild (new BootstrapViewForm ());
    aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Creation datetime")
                                                     .setCtrl (PDTToString.getAsString (aWorkItem.getCreationDT (),
                                                                                        aDisplayLocale)));
    aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Participant ID")
                                                     .setCtrl (aWorkItem.getParticipantID ().getURIEncoded ()));
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
                                                 @Nonnull final FormErrors aFormErrors,
                                                 @Nonnull final EWebPageFormAction eFormAction)
  {
    throw new UnsupportedOperationException ();
  }

  @Override
  protected void showInputForm (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nullable final IReIndexWorkItem aSelectedObject,
                                @Nonnull final BootstrapForm aForm,
                                @Nonnull final EWebPageFormAction eFormAction,
                                @Nonnull final FormErrors aFormErrors)
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
                                        new DTCol ("Next retry").setDisplayType (EDTColType.DATETIME, aDisplayLocale),
                                        new DTCol ("Last retry").setDisplayType (EDTColType.DATETIME,
                                                                                 aDisplayLocale)).setID (getID ());

    for (final IReIndexWorkItem aItem : getReIndexWorkItemList ().getAllItems ())
    {
      final ISimpleURL aViewLink = createViewURL (aWPEC, aItem);
      final IndexerWorkItem aWorkItem = aItem.getWorkItem ();

      final HCRow aRow = aTable.addBodyRow ();
      aRow.addCell (new HCA (aViewLink).addChild (PDTToString.getAsString (aWorkItem.getCreationDT (),
                                                                           aDisplayLocale)));
      aRow.addCell (aWorkItem.getParticipantID ().getURIEncoded ());
      aRow.addCell (aWorkItem.getType ().getDisplayName ());
      aRow.addCell (Integer.toString (aItem.getRetryCount ()));
      aRow.addCell (PDTToString.getAsString (aItem.getNextRetryDT (), aDisplayLocale));
      aRow.addCell (PDTToString.getAsString (aItem.getMaxRetryDT (), aDisplayLocale));
    }

    aNodeList.addChild (aTable);
    aNodeList.addChild (BootstrapDataTables.createDefaultDataTables (aWPEC, aTable));
  }
}
