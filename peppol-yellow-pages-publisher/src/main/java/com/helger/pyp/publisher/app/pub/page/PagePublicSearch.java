/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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
package com.helger.pyp.publisher.app.pub.page;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.SimpleURL;
import com.helger.html.css.DefaultCSSClassProvider;
import com.helger.html.css.ICSSClassProvider;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.ext.HCExtHelper;
import com.helger.html.hc.html.forms.EHCFormMethod;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCForm;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCLI;
import com.helger.html.hc.html.grouping.HCOL;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.grouping.IHCLI;
import com.helger.html.hc.html.sections.HCH1;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCSpan;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppol.identifier.doctype.ComparatorDocumentTypeIdentifier;
import com.helger.peppol.identifier.doctype.IPeppolDocumentTypeIdentifier;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.photon.bootstrap3.CBootstrapCSS;
import com.helger.photon.bootstrap3.alert.BootstrapInfoBox;
import com.helger.photon.bootstrap3.alert.BootstrapWarnBox;
import com.helger.photon.bootstrap3.badge.BootstrapBadge;
import com.helger.photon.bootstrap3.button.BootstrapSubmitButton;
import com.helger.photon.bootstrap3.grid.BootstrapRow;
import com.helger.photon.bootstrap3.inputgroup.BootstrapInputGroup;
import com.helger.photon.bootstrap3.nav.BootstrapTabBox;
import com.helger.photon.bootstrap3.panel.BootstrapPanel;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.pyp.indexer.mgr.PYPMetaManager;
import com.helger.pyp.indexer.storage.PYPQueryManager;
import com.helger.pyp.indexer.storage.PYPStorageManager;
import com.helger.pyp.indexer.storage.PYPStoredDocument;
import com.helger.pyp.publisher.ui.AbstractAppWebPage;
import com.helger.pyp.publisher.ui.HCExtImg;
import com.helger.pyp.publisher.ui.PYPCommonUI;

public final class PagePublicSearch extends AbstractAppWebPage
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PagePublicSearch.class);
  private static final String FIELD_QUERY = "q";
  private static final String FIELD_PARTICIPANT_ID = "partid";

  private static final ICSSClassProvider CSS_CLASS_BIG_QUERY_BOX = DefaultCSSClassProvider.create ("big-querybox");
  private static final ICSSClassProvider CSS_CLASS_BIG_QUERY_BUTTONS = DefaultCSSClassProvider.create ("big-querybuttons");
  private static final ICSSClassProvider CSS_CLASS_SMALL_QUERY_BOX = DefaultCSSClassProvider.create ("small-querybox");
  private static final ICSSClassProvider CSS_CLASS_RESULT_DOC = DefaultCSSClassProvider.create ("result-doc");
  private static final ICSSClassProvider CSS_CLASS_RESULT_DOC_HEADER = DefaultCSSClassProvider.create ("result-doc-header");
  private static final ICSSClassProvider CSS_CLASS_RESULT_DOC_COUNTRY_CODE = DefaultCSSClassProvider.create ("result-doc-country-code");
  private static final ICSSClassProvider CSS_CLASS_RESULT_DOC_NAME = DefaultCSSClassProvider.create ("result-doc-name");
  private static final ICSSClassProvider CSS_CLASS_RESULT_DOC_GEOINFO = DefaultCSSClassProvider.create ("result-doc-geoinfo");
  private static final ICSSClassProvider CSS_CLASS_RESULT_DOC_FREETEXT = DefaultCSSClassProvider.create ("result-doc-freetext");

  public PagePublicSearch (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Search");
  }

  @Override
  @Nullable
  public String getHeaderText (@Nonnull final WebPageExecutionContext aWPEC)
  {
    return null;
  }

  @Nonnull
  private static HCEdit _createQueryEdit ()
  {
    return new HCEdit (new RequestField (FIELD_QUERY)).setPlaceholder ("Your query goes here");
  }

  @Nonnull
  private BootstrapRow _createSmallQueryBox (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCForm aSmallQueryBox = new HCForm ().setAction (aWPEC.getSelfHref ()).setMethod (EHCFormMethod.GET);
    aSmallQueryBox.addChild (new BootstrapInputGroup (_createQueryEdit ()).addSuffix (new BootstrapSubmitButton ().setIcon (EDefaultIcon.MAGNIFIER))
                                                                          .addClass (CSS_CLASS_SMALL_QUERY_BOX));

    final BootstrapRow aBodyRow = new BootstrapRow ();
    aBodyRow.createColumn (12, 6, 6, 6).addChild (aSmallQueryBox);
    return aBodyRow;
  }

  @Override
  protected void fillContent (final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    {
      final BootstrapRow aHeaderRow = aNodeList.addAndReturnChild (new BootstrapRow ());
      // A PYP logo would be nice
      aHeaderRow.createColumn (12, 12, 1, 2).addClass (CBootstrapCSS.HIDDEN_SM);
      aHeaderRow.createColumn (12, 6, 5, 4)
                .addChild (new HCExtImg (new SimpleURL ("/imgs/pyplogo.png")).addClass (CBootstrapCSS.PULL_LEFT));
      aHeaderRow.createColumn (12, 6, 5, 4)
                .addChild (new HCExtImg (new SimpleURL ("/imgs/peppol.png")).addClass (CBootstrapCSS.PULL_RIGHT));
      aHeaderRow.createColumn (12, 12, 1, 2).addClass (CBootstrapCSS.HIDDEN_SM);
    }

    final String sQuery = aWPEC.getAttributeAsString (FIELD_QUERY);
    final String sParticipantID = aWPEC.getAttributeAsString (FIELD_PARTICIPANT_ID);
    boolean bShowQuery = true;

    if (aWPEC.hasAction (CPageParam.ACTION_VIEW) && StringHelper.hasText (sParticipantID))
    {
      final SimpleParticipantIdentifier aParticipantID = SimpleParticipantIdentifier.createFromURIPartOrNull (sParticipantID);
      if (aParticipantID != null)
      {
        // Show small query box
        aNodeList.addChild (_createSmallQueryBox (aWPEC));

        // Search document matching participant ID
        final List <PYPStoredDocument> aResultDocs = PYPMetaManager.getStorageMgr ()
                                                                   .getAllDocumentsOfParticipant (aParticipantID);
        // Group by participant ID
        final Map <String, List <PYPStoredDocument>> aGroupedDocs = PYPStorageManager.getGroupedByParticipantID (aResultDocs);
        if (aGroupedDocs.isEmpty ())
          s_aLogger.warn ("No stored document matches participant identifier '" + sParticipantID + "'");
        else
        {
          if (aGroupedDocs.size () > 1)
            s_aLogger.warn ("Found " +
                            aGroupedDocs.size () +
                            " entries for participant identifier '" +
                            sParticipantID +
                            "' - weird");
          // Get the first one
          final List <PYPStoredDocument> aDocuments = CollectionHelper.getFirstElement (aGroupedDocs.values ());
          bShowQuery = false;

          aNodeList.addChild (new HCH1 ().addChild ("Details for " + sParticipantID));

          final BootstrapTabBox aTabBox = aNodeList.addAndReturnChild (new BootstrapTabBox ());

          // Buisness information
          {
            final HCNodeList aOL = new HCNodeList ();
            int nIndex = 1;
            for (final PYPStoredDocument aStoredDoc : aDocuments)
            {
              final BootstrapPanel aPanel = aOL.addAndReturnChild (new BootstrapPanel ());
              if (aDocuments.size () > 1)
                aPanel.getOrCreateHeader ().addChild ("Business information entity " + nIndex);
              aPanel.getBody ().addChild (PYPCommonUI.showBusinessInfoDetails (aStoredDoc, aDisplayLocale));
              ++nIndex;
            }
            // Add whole list or just the first item?
            final IHCNode aTabLabel = new HCSpan ().addChild ("Business information ")
                                                   .addChild (new BootstrapBadge ().addChild (Integer.toString (aDocuments.size ())));
            aTabBox.addTab (aTabLabel, aOL);
          }

          // Document types
          {
            final HCOL aDocTypeCtrl = new HCOL ();
            final List <IPeppolDocumentTypeIdentifier> aDocTypeIDs = CollectionHelper.getSorted (aResultDocs.get (0)
                                                                                                            .getAllDocumentTypeIDs (),
                                                                                                 new ComparatorDocumentTypeIdentifier ());
            for (final IPeppolDocumentTypeIdentifier aDocTypeID : aDocTypeIDs)
            {
              final IHCLI <?> aLI = aDocTypeCtrl.addItem ();
              aLI.addChild (PYPCommonUI.getDocumentTypeID (aDocTypeID));
              aLI.addChild (PYPCommonUI.getDocumentTypeIDDetails (aDocTypeID.getParts ()));
            }
            aTabBox.addTab (new HCSpan ().addChild ("Document types ")
                                         .addChild (new BootstrapBadge ().addChild (Integer.toString (aDocTypeIDs.size ()))),
                            aDocTypeCtrl.hasChildren () ? aDocTypeCtrl
                                                        : new BootstrapWarnBox ().addChild ("No document types available for this participant"));
          }
        }
      }
      else
        s_aLogger.warn ("Failed to parse participant identifier '" + sParticipantID + "'");
    }

    if (bShowQuery)
    {
      if (StringHelper.hasText (sQuery))
      {
        // Show small query box
        aNodeList.addChild (_createSmallQueryBox (aWPEC));

        s_aLogger.info ("Searching for '" + sQuery + "'");

        // Build Lucene query
        final Query aLuceneQuery = PYPQueryManager.convertQueryStringToLuceneQuery (PYPMetaManager.getLucene (),
                                                                                    sQuery);
        // Search all documents
        final List <PYPStoredDocument> aResultDocs = PYPMetaManager.getStorageMgr ().getAllDocuments (aLuceneQuery);
        // Group by participant ID
        final Map <String, List <PYPStoredDocument>> aGroupedDocs = PYPStorageManager.getGroupedByParticipantID (aResultDocs);

        final int nMaxResults = 10;

        // Display results
        if (aGroupedDocs.isEmpty ())
        {
          aNodeList.addChild (new BootstrapInfoBox ().addChild ("No search results found for query '" + sQuery + "'"));
        }
        else
        {
          final HCOL aOL = new HCOL ().setStart (1);
          for (final Map.Entry <String, List <PYPStoredDocument>> aEntry : aGroupedDocs.entrySet ())
          {
            final String sDocParticipantID = aEntry.getKey ();
            final List <PYPStoredDocument> aDocs = aEntry.getValue ();

            // Start result document
            final HCDiv aResultItem = new HCDiv ().addClass (CSS_CLASS_RESULT_DOC);
            final HCDiv aHeadRow = aResultItem.addAndReturnChild (new HCDiv ());
            aHeadRow.addChild (sDocParticipantID);
            if (aDocs.size () > 1)
              aHeadRow.addChild (" (" + aDocs.size () + " entities)");
            aHeadRow.addChild (new HCA (aWPEC.getSelfHref ()
                                             .add (FIELD_QUERY, sQuery)
                                             .add (CPageParam.PARAM_ACTION, CPageParam.ACTION_VIEW)
                                             .add (FIELD_PARTICIPANT_ID,
                                                   sDocParticipantID)).addChild (EDefaultIcon.MAGNIFIER.getAsNode ()));

            // Show all entities of the stored document
            final HCUL aUL = aResultItem.addAndReturnChild (new HCUL ());
            for (final PYPStoredDocument aStoredDoc : aEntry.getValue ())
            {
              final IHCLI <?> aLI = aUL.addAndReturnItem (new HCLI ().addClass (CSS_CLASS_RESULT_DOC_HEADER));
              final HCDiv aDocHeadRow = new HCDiv ();
              if (aStoredDoc.hasCountryCode ())
              {
                // Add country flag (if available)
                aDocHeadRow.addChild (PYPCommonUI.getFlagNode (aStoredDoc.getCountryCode ()));
                aDocHeadRow.addChild (new HCSpan ().addChild (aStoredDoc.getCountryCode ())
                                                   .addClass (CSS_CLASS_RESULT_DOC_COUNTRY_CODE));
              }
              if (aStoredDoc.hasName ())
                aDocHeadRow.addChild (new HCSpan ().addChild (aStoredDoc.getName ())
                                                   .addClass (CSS_CLASS_RESULT_DOC_NAME));
              if (aDocHeadRow.hasChildren ())
                aLI.addChild (aDocHeadRow);

              if (aStoredDoc.hasGeoInfo ())
                aLI.addChild (new HCDiv ().addChildren (HCExtHelper.nl2divList (aStoredDoc.getGeoInfo ()))
                                          .addClass (CSS_CLASS_RESULT_DOC_GEOINFO));
              if (aStoredDoc.hasFreeText ())
                aLI.addChild (new HCDiv ().addChildren (HCExtHelper.nl2divList (aStoredDoc.getFreeText ()))
                                          .addClass (CSS_CLASS_RESULT_DOC_FREETEXT));
            }

            aOL.addItem (aResultItem);

            // Break at 10 results
            if (aOL.getChildCount () >= nMaxResults)
              break;
          }
          aNodeList.addChild (aOL);
        }
      }
      else
      {
        // Show big query box
        final HCForm aBigQueryBox = new HCForm ().setAction (aWPEC.getSelfHref ()).setMethod (EHCFormMethod.GET);
        aBigQueryBox.addChild (new HCDiv ().addClass (CSS_CLASS_BIG_QUERY_BOX).addChild (_createQueryEdit ()));
        aBigQueryBox.addChild (new HCDiv ().addClass (CSS_CLASS_BIG_QUERY_BUTTONS)
                                           .addChild (new BootstrapSubmitButton ().addChild ("Search PYP")
                                                                                  .setIcon (EDefaultIcon.MAGNIFIER)));

        final BootstrapRow aBodyRow = aNodeList.addAndReturnChild (new BootstrapRow ());
        aBodyRow.createColumn (12, 1, 2, 3).addClass (CBootstrapCSS.HIDDEN_XS);
        aBodyRow.createColumn (12, 10, 8, 6).addChild (aBigQueryBox);
        aBodyRow.createColumn (12, 1, 2, 3).addClass (CBootstrapCSS.HIDDEN_XS);
      }
    }
  }
}
