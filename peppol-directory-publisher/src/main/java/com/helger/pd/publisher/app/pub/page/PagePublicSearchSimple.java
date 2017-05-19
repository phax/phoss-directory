/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.app.pub.page;

import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.collection.multimap.IMultiMapListBased;
import com.helger.commons.locale.country.CountryCache;
import com.helger.commons.string.StringHelper;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.ext.HCExtHelper;
import com.helger.html.hc.html.forms.EHCFormMethod;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCForm;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCOL;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.tabular.HCCol;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.html.textlevel.HCSpan;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.storage.PDQueryManager;
import com.helger.pd.indexer.storage.PDStorageManager;
import com.helger.pd.indexer.storage.PDStoredDocument;
import com.helger.pd.publisher.app.AppCommonUI;
import com.helger.pd.publisher.search.EPDSearchField;
import com.helger.pd.publisher.ui.PDCommonUI;
import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.identifier.factory.PeppolIdentifierFactory;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.peppol.issuingagency.IIdentifierIssuingAgency;
import com.helger.photon.bootstrap3.CBootstrapCSS;
import com.helger.photon.bootstrap3.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap3.alert.BootstrapInfoBox;
import com.helger.photon.bootstrap3.button.BootstrapButton;
import com.helger.photon.bootstrap3.button.BootstrapSubmitButton;
import com.helger.photon.bootstrap3.button.EBootstrapButtonSize;
import com.helger.photon.bootstrap3.button.EBootstrapButtonType;
import com.helger.photon.bootstrap3.grid.BootstrapRow;
import com.helger.photon.bootstrap3.inputgroup.BootstrapInputGroup;
import com.helger.photon.bootstrap3.label.BootstrapLabel;
import com.helger.photon.bootstrap3.label.EBootstrapLabelType;
import com.helger.photon.bootstrap3.table.BootstrapTable;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;

public final class PagePublicSearchSimple extends AbstractPagePublicSearch
{
  public static final String FIELD_QUERY = "q";
  public static final String FIELD_PARTICIPANT_ID = EPDSearchField.PARTICIPANT_ID.getFieldName ();
  public static final String PARAM_MAX = "max";
  private static final Logger s_aLogger = LoggerFactory.getLogger (PagePublicSearchSimple.class);

  public PagePublicSearchSimple (@Nonnull @Nonempty final String sID)
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
    return new HCEdit (new RequestField (FIELD_QUERY)).setPlaceholder ("Query PEPPOL Directory");
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

  @Nonnull
  private static BootstrapRow _createInitialSearchForm (final WebPageExecutionContext aWPEC)
  {
    final HCForm aBigQueryBox = new HCForm ().setAction (aWPEC.getSelfHref ()).setMethod (EHCFormMethod.GET);
    aBigQueryBox.addChild (new HCDiv ().addClass (CSS_CLASS_BIG_QUERY_BOX).addChild (_createQueryEdit ()));
    aBigQueryBox.addChild (new HCDiv ().addClass (CSS_CLASS_BIG_QUERY_HELPTEXT)
                                       .addChild ("Enter the name, address, ID or any other keyword of the entity you are looking for."));
    aBigQueryBox.addChild (new HCDiv ().addClass (CSS_CLASS_BIG_QUERY_BUTTONS)
                                       .addChild (new BootstrapSubmitButton ().addChild ("Search PEPPOL Directory")
                                                                              .setIcon (EDefaultIcon.MAGNIFIER)));

    final BootstrapRow aBodyRow = new BootstrapRow ();
    aBodyRow.createColumn (12, 1, 2, 3).addClass (CBootstrapCSS.HIDDEN_XS);
    aBodyRow.createColumn (12, 10, 8, 6).addChild (aBigQueryBox);
    aBodyRow.createColumn (12, 1, 2, 3).addClass (CBootstrapCSS.HIDDEN_XS);
    return aBodyRow;
  }

  private void _showResultList (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nonnull @Nonempty final String sQuery,
                                @Nonnegative final int nMaxResults)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    s_aLogger.info ("Searching for '" + sQuery + "'");

    // Build Lucene query
    final Query aLuceneQuery = PDQueryManager.convertQueryStringToLuceneQuery (PDMetaManager.getLucene (), sQuery);

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Created query for '" + sQuery + "' is <" + aLuceneQuery + ">");

    // Search all documents
    final ICommonsList <PDStoredDocument> aResultDocs = PDMetaManager.getStorageMgr ().getAllDocuments (aLuceneQuery);

    s_aLogger.info ("  Result for <" +
                    aLuceneQuery +
                    "> " +
                    (aResultDocs.size () == 1 ? "is 1 document" : "are " + aResultDocs.size () + " documents"));

    // Group by participant ID
    final IMultiMapListBased <IParticipantIdentifier, PDStoredDocument> aGroupedDocs = PDStorageManager.getGroupedByParticipantID (aResultDocs);

    // Display results
    if (aGroupedDocs.isEmpty ())
    {
      aNodeList.addChild (new BootstrapInfoBox ().addChild ("No search results found for query '" + sQuery + "'"));
    }
    else
    {
      aNodeList.addChild (new HCDiv ().addChild (new BootstrapLabel (EBootstrapLabelType.SUCCESS).addChild ("Found " +
                                                                                                            aGroupedDocs.size () +
                                                                                                            " entities matching '" +
                                                                                                            sQuery +
                                                                                                            "'")));
      if (aGroupedDocs.size () > nMaxResults)
        aNodeList.addChild (new HCDiv ().addChild (new BootstrapLabel (EBootstrapLabelType.WARNING).addChild ("Found many matches. Try to be nmore specific.")));

      // Show basic information
      final HCOL aOL = new HCOL ().setStart (1);
      for (final Map.Entry <IParticipantIdentifier, ICommonsList <PDStoredDocument>> aEntry : aGroupedDocs.entrySet ())
      {
        final IParticipantIdentifier aDocParticipantID = aEntry.getKey ();
        final ICommonsList <PDStoredDocument> aDocs = aEntry.getValue ();

        // Start result document
        final HCDiv aResultItem = new HCDiv ().addClass (CSS_CLASS_RESULT_DOC);
        final HCDiv aHeadRow = aResultItem.addAndReturnChild (new HCDiv ());
        {
          final boolean bIsPeppolDefault = aDocParticipantID.hasScheme (PeppolIdentifierFactory.INSTANCE.getDefaultParticipantIdentifierScheme ());
          IHCNode aParticipant = null;
          if (bIsPeppolDefault)
          {
            final IIdentifierIssuingAgency aIIA = AppCommonUI.getAgencyOfIdentifier (aDocParticipantID);
            if (aIIA != null)
              aParticipant = new HCNodeList ().addChild (aDocParticipantID.getValue () +
                                                         " (" +
                                                         aIIA.getSchemeAgency () +
                                                         ")");
          }
          if (aParticipant == null)
          {
            // Fallback
            aParticipant = new HCCode ().addChild (aDocParticipantID.getURIEncoded ());
          }
          aHeadRow.addChild ("Participant ID: ").addChild (aParticipant);
        }
        if (aDocs.size () > 1)
          aHeadRow.addChild (" (" + aDocs.size () + " entities)");

        // Show all entities of the stored document
        final HCUL aUL = aResultItem.addAndReturnChild (new HCUL ());
        for (final PDStoredDocument aStoredDoc : aEntry.getValue ())
        {
          final BootstrapTable aTable = new BootstrapTable (HCCol.perc (20), HCCol.star ());
          aTable.setCondensed (true);
          if (aStoredDoc.hasCountryCode ())
          {
            // Add country flag (if available)
            final String sCountryCode = aStoredDoc.getCountryCode ();
            final Locale aCountry = CountryCache.getInstance ().getCountry (sCountryCode);
            aTable.addBodyRow ()
                  .addCell ("Country:")
                  .addCell (new HCNodeList ().addChild (PDCommonUI.getFlagNode (sCountryCode))
                                             .addChild (" ")
                                             .addChild (new HCSpan ().addChild (aCountry != null ? aCountry.getDisplayCountry (aDisplayLocale) +
                                                                                                   " (" +
                                                                                                   sCountryCode +
                                                                                                   ")"
                                                                                                 : sCountryCode)
                                                                     .addClass (CSS_CLASS_RESULT_DOC_COUNTRY_CODE)));
          }
          if (aStoredDoc.hasName ())
            aTable.addBodyRow ()
                  .addCell ("Name:")
                  .addCell (new HCSpan ().addChild (aStoredDoc.getName ()).addClass (CSS_CLASS_RESULT_DOC_NAME));

          if (aStoredDoc.hasGeoInfo ())
            aTable.addBodyRow ()
                  .addCell ("Geographical information:")
                  .addCell (new HCDiv ().addChildren (HCExtHelper.nl2divList (aStoredDoc.getGeoInfo ()))
                                        .addClass (CSS_CLASS_RESULT_DOC_GEOINFO));
          if (aStoredDoc.hasAdditionalInformation ())
            aTable.addBodyRow ()
                  .addCell ("Additional information:")
                  .addCell (new HCDiv ().addChildren (HCExtHelper.nl2divList (aStoredDoc.getAdditionalInformation ()))
                                        .addClass (CSS_CLASS_RESULT_DOC_FREETEXT));
          aUL.addAndReturnItem (aTable).addClass (CSS_CLASS_RESULT_DOC_HEADER);
        }

        final BootstrapButton aShowDetailsBtn = new BootstrapButton (EBootstrapButtonType.SUCCESS,
                                                                     EBootstrapButtonSize.DEFAULT).addChild ("Show details")
                                                                                                  .setIcon (EDefaultIcon.MAGNIFIER)
                                                                                                  .addClass (CSS_CLASS_RESULT_DOC_SDBUTTON)
                                                                                                  .setOnClick (aWPEC.getSelfHref ()
                                                                                                                    .add (FIELD_QUERY,
                                                                                                                          sQuery)
                                                                                                                    .add (CPageParam.PARAM_ACTION,
                                                                                                                          CPageParam.ACTION_VIEW)
                                                                                                                    .add (FIELD_PARTICIPANT_ID,
                                                                                                                          aDocParticipantID.getURIEncoded ()));
        aResultItem.addChild (new HCDiv ().addChild (aShowDetailsBtn));
        aOL.addItem (aResultItem);

        // Is the max result limit reached?
        if (aOL.getChildCount () >= nMaxResults)
          break;
      }
      aNodeList.addChild (aOL);
    }
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();

    aNodeList.addAndReturnChild (createLogoRow ());

    final String sQuery = aWPEC.getAttributeAsString (FIELD_QUERY);
    final String sParticipantID = aWPEC.getAttributeAsString (FIELD_PARTICIPANT_ID);
    final int nMaxResults = Math.max (aWPEC.getAttributeAsInt (PARAM_MAX, 50), 1);
    boolean bShowQuery = true;

    if (aWPEC.hasAction (CPageParam.ACTION_VIEW) && StringHelper.hasText (sParticipantID))
    {
      final IParticipantIdentifier aParticipantID = aIdentifierFactory.parseParticipantIdentifier (sParticipantID);
      if (aParticipantID != null)
      {
        // Show participant details
        final HCNodeList aDetails = createParticipantDetails (aDisplayLocale, sParticipantID, aParticipantID);
        if (aDetails.hasChildren ())
        {
          // Show small query box
          aNodeList.addChild (_createSmallQueryBox (aWPEC));

          // Show details afterwards
          aNodeList.addChild (aDetails);
          bShowQuery = false;
        }
      }
      else
      {
        aNodeList.addChild (new BootstrapErrorBox ().addChild ("Failed to parse participant identifier '" +
                                                               sParticipantID +
                                                               "'"));
      }
    }

    if (bShowQuery)
    {
      if (StringHelper.hasText (sQuery))
      {
        // Show small query box
        aNodeList.addChild (_createSmallQueryBox (aWPEC));
        _showResultList (aWPEC, sQuery, nMaxResults);
      }
      else
      {
        // Show big query box
        final BootstrapRow aBodyRow = _createInitialSearchForm (aWPEC);
        aNodeList.addChild (aBodyRow);
      }
    }
  }
}
