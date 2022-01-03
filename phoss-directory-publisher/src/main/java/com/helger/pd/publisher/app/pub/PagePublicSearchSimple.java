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
package com.helger.pd.publisher.app.pub;

import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.locale.country.CountryCache;
import com.helger.commons.string.StringHelper;
import com.helger.css.property.CCSSProperties;
import com.helger.css.utils.CSSURLHelper;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.ext.HCExtHelper;
import com.helger.html.hc.html.forms.EHCFormMethod;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCForm;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCOL;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.tabular.HCCol;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.storage.CPDStorage;
import com.helger.pd.indexer.storage.EQueryMode;
import com.helger.pd.indexer.storage.PDQueryManager;
import com.helger.pd.indexer.storage.PDStorageManager;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.pd.indexer.storage.PDStoredMLName;
import com.helger.pd.publisher.CPDPublisher;
import com.helger.pd.publisher.app.PDSessionSingleton;
import com.helger.pd.publisher.exportall.ExportAllManager;
import com.helger.pd.publisher.search.EPDSearchField;
import com.helger.pd.publisher.ui.PDCommonUI;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.peppol.pidscheme.IParticipantIdentifierScheme;
import com.helger.peppolid.peppol.pidscheme.ParticipantIdentifierSchemeManager;
import com.helger.photon.ajax.decl.AjaxFunctionDeclaration;
import com.helger.photon.bootstrap4.CBootstrapCSS;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.button.BootstrapSubmitButton;
import com.helger.photon.bootstrap4.button.EBootstrapButtonSize;
import com.helger.photon.bootstrap4.button.EBootstrapButtonType;
import com.helger.photon.bootstrap4.grid.BootstrapRow;
import com.helger.photon.bootstrap4.table.BootstrapTable;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;

public final class PagePublicSearchSimple extends AbstractPagePublicSearch
{
  public static final String FIELD_QUERY = "q";
  public static final String FIELD_PARTICIPANT_ID = EPDSearchField.PARTICIPANT_ID.getFieldName ();
  public static final String PARAM_MAX = "max";
  public static final int DEFAULT_MAX = 50;
  public static final int MAX_MAX = 1000;

  private static final Logger LOGGER = LoggerFactory.getLogger (PagePublicSearchSimple.class);
  private static final String PEPPOL_DEFAULT_SCHEME = PeppolIdentifierFactory.INSTANCE.getDefaultParticipantIdentifierScheme ();
  private static final AjaxFunctionDeclaration s_aExportLast;

  static
  {
    s_aExportLast = addAjax ("export", (aRequestScope, aAjaxResponse) -> {
      final Query aLastQuery = PDSessionSingleton.getInstance ().getLastQuery ();
      if (aLastQuery == null)
        aAjaxResponse.createNotFound ();
      else
      {
        final IMicroDocument aDoc = ExportAllManager.queryAllContainedBusinessCardsAsXML (aLastQuery, true);
        aAjaxResponse.xml (aDoc);
        aAjaxResponse.attachment ("last-query-export.xml");
      }
    });
  }

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
    return new HCEdit (new RequestField (FIELD_QUERY)).setPlaceholder ("Search " + CPDPublisher.getApplication ())
                                                      .addClass (CBootstrapCSS.FORM_CONTROL);
  }

  @Nonnull
  private BootstrapRow _createInitialSearchForm (final WebPageExecutionContext aWPEC)
  {
    final HCForm aBigQueryBox = new HCForm ().setAction (aWPEC.getSelfHref ()).setMethod (EHCFormMethod.GET);

    final HCEdit aQueryEdit = _createQueryEdit ();
    aBigQueryBox.addChild (div (aQueryEdit).addClass (CSS_CLASS_BIG_QUERY_BOX));

    {
      final String sHelpText = "Enter the name, address, ID or any other keyword of the entity you are looking for.";
      if (s_eUIMode.isUseHelptext ())
        aBigQueryBox.addChild (div (sHelpText).addClass (CSS_CLASS_BIG_QUERY_HELPTEXT));
      else
        aQueryEdit.setPlaceholder (sHelpText);
    }

    {
      final BootstrapButton aButton = new BootstrapSubmitButton ().addChild ("Search " + CPDPublisher.getApplication ())
                                                                  .setIcon (EDefaultIcon.MAGNIFIER);
      if (s_eUIMode.isUseGreenButton ())
        aButton.setButtonType (EBootstrapButtonType.SUCCESS);
      aBigQueryBox.addChild (new HCDiv ().addClass (CSS_CLASS_BIG_QUERY_BUTTONS).addChild (aButton));
    }

    final BootstrapRow aBodyRow = new BootstrapRow ();
    aBodyRow.createColumn (-1, -1, 1, 2, 3).addClasses (CBootstrapCSS.D_NONE, CBootstrapCSS.D_MD_BLOCK);
    aBodyRow.createColumn (12, 12, 10, 8, 6).addChild (aBigQueryBox);
    aBodyRow.createColumn (-1, -1, 1, 2, 3).addClasses (CBootstrapCSS.D_NONE, CBootstrapCSS.D_MD_BLOCK);
    return aBodyRow;
  }

  private void _showResultList (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nonnull @Nonempty final String sQuery,
                                @Nonnegative final int nMaxResults)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final IRequestWebScopeWithoutResponse aRequestScope = aWPEC.getRequestScope ();
    final PDStorageManager aStorageMgr = PDMetaManager.getStorageMgr ();

    // Search all documents
    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("Searching generically for '" + sQuery + "'");

    // Build Lucene query
    Query aLuceneQuery = PDQueryManager.convertQueryStringToLuceneQuery (PDMetaManager.getLucene (), CPDStorage.FIELD_ALL_FIELDS, sQuery);
    aLuceneQuery = EQueryMode.NON_DELETED_ONLY.getEffectiveQuery (aLuceneQuery);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Created query for '" + sQuery + "' is <" + aLuceneQuery + ">");

    PDSessionSingleton.getInstance ().setLastQuery (aLuceneQuery);

    // Search all documents
    final ICommonsList <PDStoredBusinessEntity> aResultBEs = aStorageMgr.getAllDocuments (aLuceneQuery, nMaxResults);
    // Also get the total hit count for UI display. May be < 0 in case of
    // error
    final int nTotalBEs = aStorageMgr.getCount (aLuceneQuery);
    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("  Result for <" +
                   aLuceneQuery +
                   "> (max=" +
                   nMaxResults +
                   ") " +
                   (aResultBEs.size () == 1 ? "is 1 document" : "are " + aResultBEs.size () + " documents") +
                   "." +
                   (nTotalBEs >= 0 ? " " + nTotalBEs + " total hits are available." : ""));

    // Group by participant ID
    final ICommonsMap <IParticipantIdentifier, ICommonsList <PDStoredBusinessEntity>> aGroupedBEs = PDStorageManager.getGroupedByParticipantID (aResultBEs);

    // Display results
    if (aGroupedBEs.isEmpty ())
    {
      aNodeList.addChild (info ("No search results found for query '" + sQuery + "'"));
    }
    else
    {
      aNodeList.addChild (div (badgeSuccess ("Found " +
                                             (aGroupedBEs.size () == 1 ? "1 entity" : aGroupedBEs.size () + " entities") +
                                             " matching '" +
                                             sQuery +
                                             "'")));
      if (nTotalBEs > nMaxResults)
      {
        aNodeList.addChild (div (badgeWarn ("Found more entities than displayed (" +
                                            nTotalBEs +
                                            " entries exist). Try to be more specific.")));
      }

      // Show basic information
      final HCOL aOL = new HCOL ().setStart (1);
      for (final Map.Entry <IParticipantIdentifier, ICommonsList <PDStoredBusinessEntity>> aEntry : aGroupedBEs.entrySet ())
      {
        final IParticipantIdentifier aDocParticipantID = aEntry.getKey ();
        final ICommonsList <PDStoredBusinessEntity> aDocs = aEntry.getValue ();

        // Start result document
        final HCDiv aResultItem = div ().addClass (CSS_CLASS_RESULT_DOC);
        final HCDiv aHeadRow = aResultItem.addAndReturnChild (new HCDiv ());
        {
          final boolean bIsPeppolDefault = aDocParticipantID.hasScheme (PEPPOL_DEFAULT_SCHEME);
          IHCNode aParticipantNode = null;
          if (bIsPeppolDefault)
          {
            final IParticipantIdentifierScheme aScheme = ParticipantIdentifierSchemeManager.getSchemeOfIdentifier (aDocParticipantID);
            if (aScheme != null)
            {
              aParticipantNode = new HCNodeList ().addChild (aDocParticipantID.getValue ());
              if (StringHelper.hasText (aScheme.getSchemeAgency ()))
                ((HCNodeList) aParticipantNode).addChild (" (" + aScheme.getSchemeAgency () + ")");
            }
          }
          if (aParticipantNode == null)
          {
            // Fallback
            aParticipantNode = code (aDocParticipantID.getURIEncoded ());
          }
          aHeadRow.addChild ("Participant ID: ").addChild (aParticipantNode);
        }
        if (aDocs.size () > 1)
          aHeadRow.addChild (" (" + aDocs.size () + " entities)");

        // Show all entities of the stored document
        final HCUL aUL = aResultItem.addAndReturnChild (new HCUL ());
        for (final PDStoredBusinessEntity aStoredDoc : aEntry.getValue ())
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
                                             .addChild (span (aCountry != null ? aCountry.getDisplayCountry (aDisplayLocale) +
                                                                                 " (" +
                                                                                 sCountryCode +
                                                                                 ")"
                                                                               : sCountryCode).addClass (CSS_CLASS_RESULT_DOC_COUNTRY_CODE)));
          }

          if (aStoredDoc.names ().isNotEmpty ())
          {
            // TODO add locale filter here
            final ICommonsList <PDStoredMLName> aNames = PDCommonUI.getUIFilteredNames (aStoredDoc.names (), aDisplayLocale);

            IHCNode aNameCtrl;
            if (aNames.size () == 1)
              aNameCtrl = PDCommonUI.getMLNameNode (aNames.getFirst (), aDisplayLocale);
            else
            {
              final HCUL aNameUL = new HCUL ();
              aNames.forEach (x -> aNameUL.addItem (PDCommonUI.getMLNameNode (x, aDisplayLocale)));
              aNameCtrl = aNameUL;
            }

            aTable.addBodyRow ().addCell ("Entity Name:").addCell (span (aNameCtrl).addClass (CSS_CLASS_RESULT_DOC_NAME));
          }

          if (aStoredDoc.hasGeoInfo ())
            aTable.addBodyRow ()
                  .addCell ("Geographical information:")
                  .addCell (div (HCExtHelper.nl2divList (aStoredDoc.getGeoInfo ())).addClass (CSS_CLASS_RESULT_DOC_GEOINFO));
          if (aStoredDoc.hasAdditionalInformation ())
            aTable.addBodyRow ()
                  .addCell ("Additional information:")
                  .addCell (div (HCExtHelper.nl2divList (aStoredDoc.getAdditionalInformation ())).addClass (CSS_CLASS_RESULT_DOC_FREETEXT));
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
        aResultItem.addChild (div (aShowDetailsBtn));
        aOL.addItem (aResultItem);

        // Is the max result limit reached?
        if (aOL.getChildCount () >= nMaxResults)
          break;
      }
      aNodeList.addChild (aOL);

      aNodeList.addChild (div (new BootstrapButton ().setOnClick (s_aExportLast.getInvocationURL (aRequestScope))
                                                     .addChild ("Download results as XML")
                                                     .setIcon (EDefaultIcon.SAVE_ALL)));
    }
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();

    final HCDiv aLogoContainer = new HCDiv ().addClass (CSS_CLASS_BIG_QUERY_IMAGE_CONTAINER);
    aLogoContainer.addClass (CBootstrapCSS.MY_2);
    final HCDiv aLogo = new HCDiv ().addClass (CSS_CLASS_BIG_QUERY_IMAGE)
                                    .addStyle (CCSSProperties.BACKGROUND_IMAGE.newValue (CSSURLHelper.getAsCSSURL (CPDPublisher.getLogoImageURL (),
                                                                                                                   true)));
    aLogoContainer.addChild (aLogo);
    aNodeList.addChild (aLogoContainer);

    final String sQuery = aWPEC.params ().getAsStringTrimmed (FIELD_QUERY);
    final String sParticipantID = aWPEC.params ().getAsStringTrimmed (FIELD_PARTICIPANT_ID);
    int nMaxResults = aWPEC.params ().getAsInt (PARAM_MAX, DEFAULT_MAX);
    if (nMaxResults < 1)
    {
      // Avoid "all" results
      nMaxResults = 1;
    }
    else
      if (nMaxResults > MAX_MAX)
      {
        // Avoid too many results
        nMaxResults = MAX_MAX;
      }
    boolean bShowQuery = true;

    if (aWPEC.hasAction (CPageParam.ACTION_VIEW) && StringHelper.hasText (sParticipantID))
    {
      // Show details of a participant
      final IParticipantIdentifier aParticipantID = aIdentifierFactory.parseParticipantIdentifier (sParticipantID);
      if (aParticipantID != null)
      {
        // Show participant details
        final HCNodeList aDetails = createParticipantDetails (aDisplayLocale, sParticipantID, aParticipantID);
        if (aDetails.hasChildren ())
        {
          // Show small query box
          aLogo.addChild (_createInitialSearchForm (aWPEC));

          // Show details afterwards
          aNodeList.addChild (aDetails);
          bShowQuery = false;
        }
      }
      else
      {
        aLogo.addChild (error ("Failed to parse participant identifier '" + sParticipantID + "'"));
      }
    }

    if (bShowQuery)
    {
      if (StringHelper.hasText (sQuery))
      {
        // Show small query box
        aLogo.addChild (_createInitialSearchForm (aWPEC));

        // After Logo
        _showResultList (aWPEC, sQuery, nMaxResults);
      }
      else
      {
        // Show big query box
        aLogo.addChild (_createInitialSearchForm (aWPEC));
      }
    }
  }
}
