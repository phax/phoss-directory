/*
 * Copyright (C) 2015-2022 Philip Helger (www.helger.com)
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.CommonsTreeSet;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.collection.impl.ICommonsSortedSet;
import com.helger.commons.compare.IComparator;
import com.helger.commons.csv.CSVWriter;
import com.helger.commons.datetime.PDTToString;
import com.helger.commons.datetime.PDTWebDateHelper;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mutable.MutableInt;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCOL;
import com.helger.html.hc.html.grouping.IHCLI;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.index.EIndexerWorkItemType;
import com.helger.pd.indexer.mgr.PDIndexerManager;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.storage.CPDStorage;
import com.helger.pd.indexer.storage.EQueryMode;
import com.helger.pd.indexer.storage.PDStoredMetaData;
import com.helger.pd.indexer.storage.field.PDField;
import com.helger.pd.publisher.CPDPublisher;
import com.helger.pd.publisher.exportall.ExportAllDataJob;
import com.helger.pd.publisher.exportall.ExportAllManager;
import com.helger.pd.publisher.servlet.ExportDeliveryHttpHandler;
import com.helger.pd.publisher.servlet.ExportServlet;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.pd.publisher.updater.SyncAllBusinessCardsJob;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.SimpleIdentifierFactory;
import com.helger.photon.ajax.decl.AjaxFunctionDeclaration;
import com.helger.photon.app.url.LinkHelper;
import com.helger.photon.bootstrap4.CBootstrapCSS;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.button.EBootstrapButtonType;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.card.BootstrapCard;
import com.helger.photon.bootstrap4.card.BootstrapCardBody;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.poi.excel.EExcelVersion;
import com.helger.poi.excel.WorkbookCreationHelper;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;

public final class PageSecureParticipantActions extends AbstractAppWebPage
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PageSecureParticipantActions.class);
  private static final String ACTION_UPDATE_EXPORTED_BCS = "update-exported-bcs";
  private static final String ACTION_SYNC_BCS_UNFORCED = "sync-bcs-unforced";
  private static final String ACTION_SYNC_BCS_FORCED = "sync-bcs-forced";
  private static final String ACTION_SHOW_DUPLICATES = "show-duplicates";
  private static final String ACTION_DELETE_DUPLICATES = "delete-duplicates";

  private static final AjaxFunctionDeclaration AJAX_DOWNLOAD_ALL_IDS_XML;
  private static final AjaxFunctionDeclaration AJAX_DOWNLOAD_ALL_IDS_AND_METADATA_XML;
  private static final AjaxFunctionDeclaration AJAX_DOWNLOAD_ALL_BCS_XML_FULL;
  private static final AjaxFunctionDeclaration AJAX_DOWNLOAD_ALL_BCS_XML_NO_DOCTYPES;
  private static final AjaxFunctionDeclaration AJAX_DOWNLOAD_ALL_BCS_EXCEL;
  private static final AjaxFunctionDeclaration AJAX_DOWNLOAD_ALL_BCS_CSV;

  static
  {
    AJAX_DOWNLOAD_ALL_IDS_XML = addAjax ( (req, res) -> {
      LOGGER.info ("Starting AJAX_DOWNLOAD_ALL_IDS_XML");
      final IMicroDocument aDoc = new MicroDocument ();
      final IMicroElement aRoot = aDoc.appendElement ("root");
      final Set <IParticipantIdentifier> aAllIDs = PDMetaManager.getStorageMgr ()
                                                                .getAllContainedParticipantIDs (EQueryMode.NON_DELETED_ONLY)
                                                                .keySet ();
      for (final IParticipantIdentifier aParticipantID : aAllIDs)
      {
        // Use the same layout as for the "full export", so that it can be used
        // by the import
        aRoot.appendElement ("participant")
             .setAttribute ("scheme", aParticipantID.getScheme ())
             .setAttribute ("value", aParticipantID.getValue ());
      }
      res.xml (aDoc);
      res.attachment ("directory-participant-list.xml");
      LOGGER.info ("Finished AJAX_DOWNLOAD_ALL_IDS_XML");
    });
    AJAX_DOWNLOAD_ALL_IDS_AND_METADATA_XML = addAjax ( (req, res) -> {
      LOGGER.info ("Starting AJAX_DOWNLOAD_ALL_IDS_AND_METADATA_XML");
      final IMicroDocument aDoc = new MicroDocument ();
      final IMicroElement aRoot = aDoc.appendElement ("root");
      final MutableInt aCount = new MutableInt (0);
      final ICommonsSet <IParticipantIdentifier> aUniquePIDs = new CommonsHashSet <> ();
      PDMetaManager.getStorageMgr ().searchAll (EQueryMode.NON_DELETED_ONLY.getEffectiveQuery (new MatchAllDocsQuery ()), -1, doc -> {
        final int n = aCount.inc ();
        if ((n % 1000) == 0)
          LOGGER.info ("Exporting #" + n);

        final IParticipantIdentifier aPID = PDField.PARTICIPANT_ID.getDocValue (doc);
        if (aUniquePIDs.add (aPID))
        {
          final IMicroElement eP = aRoot.appendElement ("participant")
                                        .setAttribute ("scheme", aPID.getScheme ())
                                        .setAttribute ("value", aPID.getValue ());

          final String sOwnerID = PDField.METADATA_OWNERID.getDocValue (doc);
          eP.appendElement ("metadata")
            .setAttribute ("creationDT", PDTWebDateHelper.getAsStringXSD (PDField.METADATA_CREATIONDT.getDocValue (doc)))
            .setAttribute ("ownerID", sOwnerID)
            .setAttribute ("ownerSeatNum", PDStoredMetaData.getOwnerIDSeatNumber (sOwnerID))
            .setAttribute ("requestingHost", PDField.METADATA_REQUESTING_HOST.getDocValue (doc));
        }
      });
      res.xml (aDoc);
      res.attachment ("directory-participant-list-with-metadata.xml");
      LOGGER.info ("Finished AJAX_DOWNLOAD_ALL_IDS_AND_METADATA_XML");
    });
    AJAX_DOWNLOAD_ALL_BCS_XML_FULL = addAjax ( (req, res) -> {
      LOGGER.info ("Starting AJAX_DOWNLOAD_ALL_BCS_XML_FULL");
      final IMicroDocument aDoc = ExportAllManager.queryAllContainedBusinessCardsAsXML (EQueryMode.NON_DELETED_ONLY, true);
      res.xml (aDoc);
      res.attachment (ExportAllManager.EXTERNAL_EXPORT_ALL_BUSINESSCARDS_XML_FULL);
      LOGGER.info ("Finished AJAX_DOWNLOAD_ALL_BCS_XML_FULL");
    });
    AJAX_DOWNLOAD_ALL_BCS_XML_NO_DOCTYPES = addAjax ( (req, res) -> {
      LOGGER.info ("Starting AJAX_DOWNLOAD_ALL_BCS_XML_NO_DOCTYPES");
      final IMicroDocument aDoc = ExportAllManager.queryAllContainedBusinessCardsAsXML (EQueryMode.NON_DELETED_ONLY, false);
      res.xml (aDoc);
      res.attachment (ExportAllManager.EXTERNAL_EXPORT_ALL_BUSINESSCARDS_XML_NO_DOC_TYPES);
      LOGGER.info ("Finished AJAX_DOWNLOAD_ALL_BCS_XML_NO_DOCTYPES");
    });
    AJAX_DOWNLOAD_ALL_BCS_EXCEL = addAjax ( (req, res) -> {
      LOGGER.info ("Starting AJAX_DOWNLOAD_ALL_BCS_EXCEL");
      try (final WorkbookCreationHelper aWBCH = ExportAllManager.queryAllContainedBusinessCardsAsExcel (EQueryMode.NON_DELETED_ONLY, true);
           final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
      {
        aWBCH.writeTo (aBAOS);
        res.binary (aBAOS, EExcelVersion.XLSX.getMimeType (), ExportAllManager.EXTERNAL_EXPORT_ALL_BUSINESSCARDS_XLSX);
      }
      LOGGER.info ("Finished AJAX_DOWNLOAD_ALL_BCS_EXCEL");
    });
    AJAX_DOWNLOAD_ALL_BCS_CSV = addAjax ( (req, res) -> {
      LOGGER.info ("Starting AJAX_DOWNLOAD_ALL_BCS_CSV");
      try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ();
           final CSVWriter aCSVWriter = new CSVWriter (StreamHelper.createWriter (aBAOS, StandardCharsets.ISO_8859_1)))
      {
        ExportAllManager.queryAllContainedBusinessCardsAsCSV (EQueryMode.NON_DELETED_ONLY, aCSVWriter);
        res.binary (aBAOS, CMimeType.TEXT_CSV, ExportAllManager.EXTERNAL_EXPORT_ALL_BUSINESSCARDS_CSV);
      }
      LOGGER.info ("Finished AJAX_DOWNLOAD_ALL_BCS_CSV");
    });
  }

  public PageSecureParticipantActions (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Participant actions");
  }

  @Nonnull
  private static ICommonsMap <IParticipantIdentifier, ICommonsSortedSet <String>> _getDuplicateSourceMap ()
  {
    LOGGER.info ("_getDuplicateSourceMap () start");
    final ICommonsMap <IParticipantIdentifier, ICommonsSortedSet <String>> aMap = new CommonsHashMap <> ();
    final Query aQuery = EQueryMode.NON_DELETED_ONLY.getEffectiveQuery (new MatchAllDocsQuery ());
    try
    {
      final Consumer <Document> aConsumer = aDoc -> {
        final IParticipantIdentifier aResolvedParticipantID = PDField.PARTICIPANT_ID.getDocValue (aDoc);
        // Get the unparsed value
        final String sParticipantID = PDField.PARTICIPANT_ID.getDocField (aDoc).stringValue ();
        aMap.computeIfAbsent (aResolvedParticipantID, k -> new CommonsTreeSet <> ()).add (sParticipantID);
      };
      PDMetaManager.getStorageMgr ().searchAll (aQuery, -1, aConsumer);
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Error searching for documents with query " + aQuery, ex);
    }

    // Take only the duplicate ones
    final ICommonsMap <IParticipantIdentifier, ICommonsSortedSet <String>> ret = new CommonsHashMap <> ();
    for (final Map.Entry <IParticipantIdentifier, ICommonsSortedSet <String>> aEntry : aMap.entrySet ())
      if (aEntry.getValue ().size () > 1)
      {
        ret.put (aEntry);
        LOGGER.info ("  Potential duplicate in: " + aEntry.getKey ().getURIEncoded ());
      }

    LOGGER.info ("_getDuplicateSourceMap () done");
    return ret;
  }

  private void _showDuplicateIDs (@Nonnull final WebPageExecutionContext aWPEC)
  {
    // This method can take a couple of minutes
    LOGGER.info ("Showing all duplicate participant identifiers");
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final ICommonsMap <IParticipantIdentifier, ICommonsSortedSet <String>> aDupMap = _getDuplicateSourceMap ();

    final HCNodeList aNL = new HCNodeList ();
    for (final Map.Entry <IParticipantIdentifier, ICommonsSortedSet <String>> aEntry : aDupMap.entrySet ())
    {
      final ICommonsSortedSet <String> aSet = aEntry.getValue ();
      final IParticipantIdentifier aPI = aEntry.getKey ();
      final String sDesiredVersion = aPI.getURIEncoded ();
      final HCDiv aDiv = div ("Found " + aSet.size () + " duplicate IDs for ").addChild (code (sDesiredVersion)).addChild (":");
      final HCOL aOL = aDiv.addAndReturnChild (new HCOL ());
      for (final String sVersion : aSet.getSorted (IComparator.getComparatorCollating (aDisplayLocale)))
      {
        final boolean bIsDesired = sDesiredVersion.equals (sVersion);
        final IHCLI <?> aLI = aOL.addAndReturnItem (code (sVersion));
        if (bIsDesired)
          aLI.addChild (" ").addChild (badgeSuccess ("desired version"));
      }
      aNL.addChild (aDiv);
    }
    if (aNL.hasChildren ())
    {
      final String sMsg = "Found duplicate entries for " + aDupMap.size () + " " + (aDupMap.size () == 1 ? "participant" : "participants");
      LOGGER.info (sMsg);
      aNL.addChildAt (0, h2 (sMsg));
      aWPEC.postRedirectGetInternal (aNL);
    }
    else
      aWPEC.postRedirectGetInternal (success ("Found no duplicate entries"));
  }

  private void _deleteDuplicateIDs (@Nonnull final WebPageExecutionContext aWPEC)
  {
    LOGGER.info ("Deleting all duplicate participant identifiers");
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final ICommonsMap <IParticipantIdentifier, ICommonsSortedSet <String>> aDupMap = _getDuplicateSourceMap ();

    final ICommonsSortedSet <String> aPIsToDelete = new CommonsTreeSet <> ();
    final ICommonsSortedSet <String> aPIsToAdd = new CommonsTreeSet <> ();
    for (final Map.Entry <IParticipantIdentifier, ICommonsSortedSet <String>> aEntry : aDupMap.entrySet ())
    {
      final ICommonsSortedSet <String> aSet = aEntry.getValue ();
      final IParticipantIdentifier aPI = aEntry.getKey ();
      final String sDesiredVersion = aPI.getURIEncoded ();

      if (aSet.contains (sDesiredVersion))
      {
        // Simple kill the other ones
        aPIsToDelete.addAll (aSet, x -> !x.equals (sDesiredVersion));
      }
      else
      {
        // Remove all and index the correct version
        aPIsToDelete.addAll (aSet);
        aPIsToAdd.add (sDesiredVersion);
      }
    }

    if (aPIsToDelete.isNotEmpty ())
    {
      final HCNodeList aNL = new HCNodeList ();
      // Important to use this identifier factory so that the correct key is
      // created
      final IIdentifierFactory aIF = SimpleIdentifierFactory.INSTANCE;
      final PDIndexerManager aIndexerMgr = PDMetaManager.getIndexerMgr ();

      String sMsg = "Deleting " + aPIsToDelete.size () + " participant ID(s):";
      LOGGER.info (sMsg);
      aNL.addChild (h2 (sMsg));
      HCOL aOL = aNL.addAndReturnChild (new HCOL ());
      for (final String s : aPIsToDelete.getSorted (IComparator.getComparatorCollating (aDisplayLocale)))
      {
        aOL.addItem (s);
        aIndexerMgr.queueWorkItem (aIF.parseParticipantIdentifier (s),
                                   EIndexerWorkItemType.DELETE,
                                   CPDStorage.OWNER_DUPLICATE_ELIMINATION,
                                   PDIndexerManager.HOST_LOCALHOST);
      }

      if (aPIsToAdd.isNotEmpty ())
      {
        sMsg = "Adding " + aPIsToAdd.size () + " participant ID(s) instead:";
        LOGGER.info (sMsg);
        aNL.addChild (h2 (sMsg));
        aOL = aNL.addAndReturnChild (new HCOL ());
        for (final String s : aPIsToAdd.getSorted (IComparator.getComparatorCollating (aDisplayLocale)))
        {
          aOL.addItem (s);
          aIndexerMgr.queueWorkItem (aIF.parseParticipantIdentifier (s),
                                     EIndexerWorkItemType.CREATE_UPDATE,
                                     CPDStorage.OWNER_DUPLICATE_ELIMINATION,
                                     PDIndexerManager.HOST_LOCALHOST);
        }
      }

      aWPEC.postRedirectGetInternal (aNL);
    }
    else
    {
      final String sMsg = "Found no duplicate entries to remove";
      LOGGER.info (sMsg);
      aWPEC.postRedirectGetInternal (success (sMsg));
    }
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final IRequestWebScopeWithoutResponse aRequestScope = aWPEC.getRequestScope ();

    if (aWPEC.hasAction (ACTION_UPDATE_EXPORTED_BCS))
    {
      LOGGER.info ("Manually exporting all Business Cards now");
      // run in the background
      ExportAllDataJob.exportAllBusinessCardsInBackground ();
      aWPEC.postRedirectGetInternal (success ("The new exported data is (hopefully) available in a few minutes. Check the 'is running' state below."));
    }
    else
      if (aWPEC.hasAction (ACTION_SYNC_BCS_UNFORCED))
      {
        LOGGER.info ("Manually synchronizing all Business Cards now (unforced)");
        if (SyncAllBusinessCardsJob.syncAllBusinessCards (false).isChanged ())
          aWPEC.postRedirectGetInternal (success ("The unforced synchronization was started successfully and is now running in the background."));
        else
          aWPEC.postRedirectGetInternal (warn ("The synchronization was not started because the last sync was at " +
                                               PDTToString.getAsString (SyncAllBusinessCardsJob.getLastSync (), aDisplayLocale)));
      }
      else
        if (aWPEC.hasAction (ACTION_SYNC_BCS_FORCED))
        {
          LOGGER.info ("Manually synchronizing all Business Cards now (FORCED)");
          if (SyncAllBusinessCardsJob.syncAllBusinessCards (true).isChanged ())
            aWPEC.postRedirectGetInternal (success ("The forced synchronization was started successfully and is now running in the background."));
          else
            aWPEC.postRedirectGetInternal (error ("Force synchronization should always work"));
        }
        else
          if (aWPEC.hasAction (ACTION_SHOW_DUPLICATES))
          {
            _showDuplicateIDs (aWPEC);
          }
          else
            if (aWPEC.hasAction (ACTION_DELETE_DUPLICATES))
            {
              _deleteDuplicateIDs (aWPEC);
            }

    {
      final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
      aToolbar.addButton ("Refresh", aWPEC.getSelfHref (), EDefaultIcon.MAGNIFIER);
      aNodeList.addChild (aToolbar);
    }

    final BootstrapCard aCard = aNodeList.addAndReturnChild (new BootstrapCard ());
    aCard.createAndAddHeader ()
         .addChild ("Live data downloads - Danger zone")
         .addClasses (CBootstrapCSS.BG_DANGER, CBootstrapCSS.TEXT_WHITE);
    BootstrapCardBody aBody = aCard.createAndAddBody ();
    aBody.addChild (new BootstrapButton (EBootstrapButtonType.DANGER).addChild ("Download all IDs (XML, live)")
                                                                     .setOnClick (AJAX_DOWNLOAD_ALL_IDS_XML.getInvocationURL (aRequestScope))
                                                                     .setIcon (EDefaultIcon.SAVE_ALL));
    aBody.addChild (new BootstrapButton (EBootstrapButtonType.DANGER).addChild ("Download all IDs and Metadata (XML, live)")
                                                                     .setOnClick (AJAX_DOWNLOAD_ALL_IDS_AND_METADATA_XML.getInvocationURL (aRequestScope))
                                                                     .setIcon (EDefaultIcon.SAVE_ALL));
    aBody.addChild (new BootstrapButton (EBootstrapButtonType.DANGER).addChild ("Download all Business Cards (XML, full, live) (may take long)")
                                                                     .setOnClick (AJAX_DOWNLOAD_ALL_BCS_XML_FULL.getInvocationURL (aRequestScope))
                                                                     .setIcon (EDefaultIcon.SAVE_ALL));
    aBody.addChild (new BootstrapButton (EBootstrapButtonType.DANGER).addChild ("Download all Business Cards (XML, no document types, live) (may take long)")
                                                                     .setOnClick (AJAX_DOWNLOAD_ALL_BCS_XML_NO_DOCTYPES.getInvocationURL (aRequestScope))
                                                                     .setIcon (EDefaultIcon.SAVE_ALL));
    aBody.addChild (new BootstrapButton (EBootstrapButtonType.DANGER).addChild ("Download all Business Cards (Excel, live) (may take long)")
                                                                     .setOnClick (AJAX_DOWNLOAD_ALL_BCS_EXCEL.getInvocationURL (aRequestScope))
                                                                     .setIcon (EDefaultIcon.SAVE_ALL));
    aBody.addChild (new BootstrapButton (EBootstrapButtonType.DANGER).addChild ("Download all Business Cards (CSV, live) (may take long)")
                                                                     .setOnClick (AJAX_DOWNLOAD_ALL_BCS_CSV.getInvocationURL (aRequestScope))
                                                                     .setIcon (EDefaultIcon.SAVE_ALL));

    aCard.createAndAddHeader ().addChild ("Cached data downloads");
    aBody = aCard.createAndAddBody ();
    aBody.addChild (new BootstrapButton ().addChild ("Download all Business Cards (XML, full, cached)")
                                          .setOnClick (LinkHelper.getURLWithContext (aRequestScope,
                                                                                     ExportServlet.SERVLET_DEFAULT_PATH +
                                                                                                    ExportDeliveryHttpHandler.SPECIAL_BUSINESS_CARDS_XML_FULL))
                                          .setIcon (EDefaultIcon.SAVE_ALL));
    aBody.addChild (new BootstrapButton ().addChild ("Download all Business Cards (XML, no document types, cached)")
                                          .setOnClick (LinkHelper.getURLWithContext (aRequestScope,
                                                                                     ExportServlet.SERVLET_DEFAULT_PATH +
                                                                                                    ExportDeliveryHttpHandler.SPECIAL_BUSINESS_CARDS_XML_NO_DOC_TYPES))
                                          .setIcon (EDefaultIcon.SAVE_ALL));
    if (CPDPublisher.EXPORT_BUSINESS_CARDS_EXCEL)
    {
      aBody.addChild (new BootstrapButton ().addChild ("Download all Business Cards (Excel, cached)")
                                            .setOnClick (LinkHelper.getURLWithContext (aRequestScope,
                                                                                       ExportServlet.SERVLET_DEFAULT_PATH +
                                                                                                      ExportDeliveryHttpHandler.SPECIAL_BUSINESS_CARDS_EXCEL))
                                            .setIcon (EDefaultIcon.SAVE_ALL));
    }
    if (CPDPublisher.EXPORT_BUSINESS_CARDS_CSV)
    {
      aBody.addChild (new BootstrapButton ().addChild ("Download all Business Cards (CSV, cached)")
                                            .setOnClick (LinkHelper.getURLWithContext (aRequestScope,
                                                                                       ExportServlet.SERVLET_DEFAULT_PATH +
                                                                                                      ExportDeliveryHttpHandler.SPECIAL_BUSINESS_CARDS_CSV))
                                            .setIcon (EDefaultIcon.SAVE_ALL));
    }
    if (CPDPublisher.EXPORT_PARTICIPANTS_XML)
    {
      aBody.addChild (new BootstrapButton ().addChild ("Download all Participants (XML, cached)")
                                            .setOnClick (LinkHelper.getURLWithContext (aRequestScope,
                                                                                       ExportServlet.SERVLET_DEFAULT_PATH +
                                                                                                      ExportDeliveryHttpHandler.SPECIAL_PARTICIPANTS_XML))
                                            .setIcon (EDefaultIcon.SAVE_ALL));
    }
    if (CPDPublisher.EXPORT_PARTICIPANTS_JSON)
    {
      aBody.addChild (new BootstrapButton ().addChild ("Download all Participants (JSON, cached)")
                                            .setOnClick (LinkHelper.getURLWithContext (aRequestScope,
                                                                                       ExportServlet.SERVLET_DEFAULT_PATH +
                                                                                                      ExportDeliveryHttpHandler.SPECIAL_PARTICIPANTS_JSON))
                                            .setIcon (EDefaultIcon.SAVE_ALL));
    }
    if (CPDPublisher.EXPORT_PARTICIPANTS_CSV)
    {
      aBody.addChild (new BootstrapButton ().addChild ("Download all Participants (CSV, cached)")
                                            .setOnClick (LinkHelper.getURLWithContext (aRequestScope,
                                                                                       ExportServlet.SERVLET_DEFAULT_PATH +
                                                                                                      ExportDeliveryHttpHandler.SPECIAL_PARTICIPANTS_CSV))
                                            .setIcon (EDefaultIcon.SAVE_ALL));
    }

    aCard.createAndAddHeader ().addChild ("Cache management");
    aBody = aCard.createAndAddBody ();
    final boolean bIsRunning = ExportAllDataJob.isExportCurrentlyRunning ();
    if (bIsRunning)
    {
      final LocalDateTime aStartDT = ExportAllDataJob.getExportAllBusinessCardsStartDT ();
      aBody.addChild (info ("Export of Business Card cache is currently running. Started at " +
                            PDTToString.getAsString (aStartDT, aDisplayLocale)));
    }
    aBody.addChild (new BootstrapButton ().addChild ("Update Business Card export cache (in background; takes too long)")
                                          .setOnClick (aWPEC.getSelfHref ().add (CPageParam.PARAM_ACTION, ACTION_UPDATE_EXPORTED_BCS))
                                          .setIcon (EDefaultIcon.INFO)
                                          .setDisabled (bIsRunning));

    aCard.createAndAddHeader ().addChild ("Data Synchronization");
    aBody = aCard.createAndAddBody ();
    aBody.addChild (new BootstrapButton ().addChild ("Synchronize all Business Cards (re-query from SMP - unforced)")
                                          .setOnClick (aWPEC.getSelfHref ().add (CPageParam.PARAM_ACTION, ACTION_SYNC_BCS_UNFORCED))
                                          .setIcon (EDefaultIcon.REFRESH));
    aBody.addChild (new BootstrapButton (EBootstrapButtonType.DANGER).addChild ("Synchronize all Business Cards (re-query from SMP - forced)")
                                                                     .setOnClick (aWPEC.getSelfHref ()
                                                                                       .add (CPageParam.PARAM_ACTION,
                                                                                             ACTION_SYNC_BCS_FORCED))
                                                                     .setIcon (EDefaultIcon.REFRESH));

    aCard.createAndAddHeader ().addChild ("Duplication handling");
    aBody = aCard.createAndAddBody ();
    if (PDMetaManager.getIdentifierFactory () instanceof SimpleIdentifierFactory)
    {
      aBody.addChild (info ("Since the simple identifier factory is used, duplicates cannot be determined"));
    }
    else
    {
      aBody.addChild (new BootstrapButton ().addChild ("Show all duplicate entries")
                                            .setOnClick (aWPEC.getSelfHref ().add (CPageParam.PARAM_ACTION, ACTION_SHOW_DUPLICATES))
                                            .setIcon (EDefaultIcon.MAGNIFIER));
      aBody.addChild (new BootstrapButton (EBootstrapButtonType.DANGER).addChild ("Delete all duplicate entries")
                                                                       .setOnClick (aWPEC.getSelfHref ()
                                                                                         .add (CPageParam.PARAM_ACTION,
                                                                                               ACTION_DELETE_DUPLICATES))
                                                                       .setIcon (EDefaultIcon.DELETE));
    }
  }
}
