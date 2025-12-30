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
package com.helger.pd.publisher.app.pub;

import java.util.Locale;
import java.util.Map;

import org.apache.lucene.search.Query;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.css.property.CCSSProperties;
import com.helger.css.utils.CSSURLHelper;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.ext.HCA_MailTo;
import com.helger.html.hc.ext.HCExtHelper;
import com.helger.html.hc.html.forms.EHCFormMethod;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCForm;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCHR;
import com.helger.html.hc.html.grouping.HCLI;
import com.helger.html.hc.html.grouping.HCOL;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.storage.CPDStorage;
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
import com.helger.peppolid.peppol.pidscheme.IPeppolParticipantIdentifierScheme;
import com.helger.peppolid.peppol.pidscheme.PeppolParticipantIdentifierSchemeManager;
import com.helger.photon.ajax.decl.AjaxFunctionDeclaration;
import com.helger.photon.bootstrap4.CBootstrapCSS;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.button.BootstrapSubmitButton;
import com.helger.photon.bootstrap4.button.EBootstrapButtonType;
import com.helger.photon.bootstrap4.grid.BootstrapCol;
import com.helger.photon.bootstrap4.grid.BootstrapGridSpec;
import com.helger.photon.bootstrap4.grid.BootstrapRow;
import com.helger.photon.bootstrap4.nav.BootstrapTabBox;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.text.locale.country.CountryCache;
import com.helger.url.SimpleURL;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;

import jakarta.annotation.Nullable;

public final class PagePublicSearchSimple extends AbstractPagePublicSearch
{
  public static final String FIELD_QUERY = "q";
  public static final String FIELD_PARTICIPANT_ID = EPDSearchField.PARTICIPANT_ID.getFieldName ();
  public static final String PARAM_MAX = "max";
  public static final int DEFAULT_MAX = 50;
  public static final int MAX_MAX = 1000;

  private static final Logger LOGGER = LoggerFactory.getLogger (PagePublicSearchSimple.class);
  private static final AjaxFunctionDeclaration AJAX_EXPORT_LAST;

  static
  {
    AJAX_EXPORT_LAST = addAjax ("export", (aRequestScope, aAjaxResponse) -> {
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

  public PagePublicSearchSimple (@NonNull @Nonempty final String sID)
  {
    super (sID, "Search");
  }

  @Override
  @Nullable
  public String getHeaderText (@NonNull final WebPageExecutionContext aWPEC)
  {
    return null;
  }

  @NonNull
  private static HCEdit _createQueryEdit ()
  {
    return new HCEdit (new RequestField (FIELD_QUERY)).setPlaceholder ("Search " + CPDPublisher.getApplication ())
                                                      .addClass (CBootstrapCSS.FORM_CONTROL);
  }

  @NonNull
  private BootstrapRow _createInitialSearchForm (@NonNull final WebPageExecutionContext aWPEC)
  {
    final HCForm aBigQueryBox = new HCForm ().setAction (aWPEC.getSelfHref ()).setMethod (EHCFormMethod.GET);

    final HCEdit aQueryEdit = _createQueryEdit ();
    aBigQueryBox.addChild (div (aQueryEdit).addClass (CSS_CLASS_BIG_QUERY_BOX));

    {
      final String sHelpText = "Enter the name, address, ID or any other keyword of the entity you are looking for.";
      if (UI_MODE.isUseHelptext ())
        aBigQueryBox.addChild (div (sHelpText).addClass (CSS_CLASS_BIG_QUERY_HELPTEXT));
      else
        aQueryEdit.setPlaceholder (sHelpText);
    }

    {
      final BootstrapButton aButton = new BootstrapSubmitButton ().addChild ("Search " + CPDPublisher.getApplication ())
                                                                  .setIcon (EDefaultIcon.MAGNIFIER);
      if (UI_MODE.isUseGreenButton ())
        aButton.setButtonType (EBootstrapButtonType.SUCCESS);
      aBigQueryBox.addChild (new HCDiv ().addClass (CSS_CLASS_BIG_QUERY_BUTTONS).addChild (aButton));
    }

    final BootstrapRow aBodyRow = new BootstrapRow ();
    aBodyRow.createColumn (-1, -1, 1, 2, 3).addClasses (CBootstrapCSS.D_NONE, CBootstrapCSS.D_MD_BLOCK);
    aBodyRow.createColumn (12, 12, 10, 8, 6).addChild (aBigQueryBox);
    aBodyRow.createColumn (-1, -1, 1, 2, 3).addClasses (CBootstrapCSS.D_NONE, CBootstrapCSS.D_MD_BLOCK);
    return aBodyRow;
  }

  private void _showResultList (@NonNull final WebPageExecutionContext aWPEC,
                                @NonNull @Nonempty final String sQuery,
                                @Nonnegative final int nMaxResults)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final IRequestWebScopeWithoutResponse aRequestScope = aWPEC.getRequestScope ();
    final PDStorageManager aStorageMgr = PDMetaManager.getStorageMgr ();

    // Search all documents
    LOGGER.info ("Searching generically for '" + sQuery + "'");

    // Build Lucene query
    final Query aLuceneQuery = PDQueryManager.convertQueryStringToLuceneQuery (PDMetaManager.getLucene (),
                                                                               CPDStorage.FIELD_ALL_FIELDS,
                                                                               sQuery);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Created query for '" + sQuery + "' is <" + aLuceneQuery + ">");

    PDSessionSingleton.getInstance ().setLastQuery (aLuceneQuery);

    // Search all documents
    final ICommonsList <PDStoredBusinessEntity> aResultBEs = aStorageMgr.getAllDocuments (aLuceneQuery, nMaxResults);
    // Also get the total hit count for UI display. May be < 0 in case of
    // error
    final int nTotalBEs = aStorageMgr.getCount (aLuceneQuery);
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
                                             (aGroupedBEs.size () == 1 ? "1 entity" : aGroupedBEs.size () +
                                                                                      " entities") +
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
          final IHCNode aParticipantNode;
          final IPeppolParticipantIdentifierScheme aScheme = PeppolParticipantIdentifierSchemeManager.getSchemeOfIdentifier (aDocParticipantID);
          if (aScheme != null)
          {
            aParticipantNode = new HCNodeList ().addChild (strong (aDocParticipantID.getValue ()));
            if (StringHelper.isNotEmpty (aScheme.getSchemeName ()))
              ((HCNodeList) aParticipantNode).addChild (" (" + aScheme.getSchemeName () + ")");
          }
          else
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

        // Must be 12 in total
        final BootstrapGridSpec aLeft = BootstrapGridSpec.create (3, 3, 3, 2, 2);
        final BootstrapGridSpec aRight = BootstrapGridSpec.create (9, 9, 9, 10, 10);

        boolean bFirstEntity = true;
        for (final PDStoredBusinessEntity aStoredDoc : aEntry.getValue ())
        {
          final HCLI aLI = aUL.addItem ();

          if (!bFirstEntity)
            aLI.addChild (new HCHR ().addClass (CBootstrapCSS.P_0));

          if (aStoredDoc.hasCountryCode ())
          {
            // Add country flag (if available)
            final String sCountryCode = aStoredDoc.getCountryCode ();
            final Locale aCountry = CountryCache.getInstance ().getCountry (sCountryCode);
            final BootstrapRow aRow = aLI.addAndReturnChild (new BootstrapRow ());
            aRow.createColumn (aLeft).addChild ("Country:");
            final BootstrapCol aColRight = aRow.createColumn (aRight).addChild (PDCommonUI.getFlagNode (sCountryCode));
            if (aCountry != null)
              aColRight.addChild (" ")
                       .addChild (aCountry.getDisplayCountry (aDisplayLocale) + " (" + sCountryCode + ")");
            else
              aColRight.addChild (sCountryCode);
          }

          if (aStoredDoc.names ().isNotEmpty ())
          {
            // TODO add locale filter here
            final ICommonsList <PDStoredMLName> aNames = PDCommonUI.getUIFilteredNames (aStoredDoc.names (),
                                                                                        aDisplayLocale);

            final IHCNode aNameCtrl;
            if (aNames.size () == 1)
              aNameCtrl = PDCommonUI.getMLNameNode (aNames.getFirstOrNull (), aDisplayLocale);
            else
            {
              final HCUL aNameUL = new HCUL ();
              aNames.forEach (x -> aNameUL.addItem (PDCommonUI.getMLNameNode (x, aDisplayLocale)));
              aNameCtrl = aNameUL;
            }

            final BootstrapRow aRow = aLI.addAndReturnChild (new BootstrapRow ());
            aRow.createColumn (aLeft).addChild ("Entity Name:");
            aRow.createColumn (aRight).addChild (aNameCtrl);
          }

          if (aStoredDoc.hasGeoInfo ())
          {
            final BootstrapRow aRow = aLI.addAndReturnChild (new BootstrapRow ());
            aRow.createColumn (aLeft).addChild ("Geographical information:");
            aRow.createColumn (aRight).addChildren (HCExtHelper.nl2divList (aStoredDoc.getGeoInfo ()));
          }
          if (aStoredDoc.hasAdditionalInformation ())
          {
            final BootstrapRow aRow = aLI.addAndReturnChild (new BootstrapRow ());
            aRow.createColumn (aLeft).addChild ("Additional information:");
            aRow.createColumn (aRight).addChildren (HCExtHelper.nl2divList (aStoredDoc.getAdditionalInformation ()));
          }

          bFirstEntity = false;
        }

        final BootstrapButton aShowDetailsBtn = new BootstrapButton (EBootstrapButtonType.SUCCESS).addChild ("Show details")
                                                                                                  .setIcon (EDefaultIcon.MAGNIFIER)
                                                                                                  .addClasses (CBootstrapCSS.MT_1,
                                                                                                               CBootstrapCSS.ML_1)
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

      aNodeList.addChild (div (new BootstrapButton ().setOnClick (AJAX_EXPORT_LAST.getInvocationURL (aRequestScope))
                                                     .addChild ("Download results as XML")
                                                     .setIcon (EDefaultIcon.SAVE_ALL)));
    }
  }

  @Override
  protected void fillContent (@NonNull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();

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

    final HCDiv aLogoContainer = div ().addClass (CSS_CLASS_BIG_QUERY_IMAGE_CONTAINER).addClass (CBootstrapCSS.MY_2);
    final HCDiv aLogo = div ().addClass (CSS_CLASS_BIG_QUERY_IMAGE)
                              .addStyle (CCSSProperties.BACKGROUND_IMAGE.newValue (CSSURLHelper.getAsCSSURL (CPDPublisher.getLogoImageURL ()
                                                                                                                         .getAsString (),
                                                                                                             true)));
    aLogoContainer.addChild (aLogo);
    if (UI_MODE.isShowPrivacyPolicy ())
    {
      final BootstrapTabBox aTabBox = new BootstrapTabBox ();
      aTabBox.addTab ("search", "Search", aLogoContainer);
      aTabBox.addTab ("privacy",
                      "Privacy Statement",
                      new HCNodeList ().addChild (strong ("Privacy Policy for the OpenPeppol Directory"))
                                       .addChild (div ().addChild ("This OpenPeppol Directory, and the related services under which data in the Directory are made available to certain third parties, are operated and provided jointly by OpenPeppol AISBL and the Service Providers of the Peppol Network (the ‘Operators’)." +
                                                                   " Data in the OpenPeppol Directory can occasionally, and at a limited scale, contain data that would qualify as personal data under European data protection law." +
                                                                   " Specifically, categories of personal data can include identity information and contact information of persons that act on behalf of end users of the Peppol Network, as communicated to the Operators by the Service Providers." +
                                                                   " When this is the case, the Operators act as the joint data controllers in the sense of European data protection law, and the Directory is provided on the basis of the legitimate interest of the Operators in ensuring the proper functioning of the Peppol Network, as requested by the end users."))
                                       .addChild (div ().addChild ("Any personal data in the Directory may only be used insofar as this is necessary to ensure the correct, effective and secure operation of the Peppol Network." +
                                                                   " Under this policy, the Operators only permit access to personal data in the Directory by persons who are contractually authorised by at least one of the Operators to use the Peppol Network." +
                                                                   " These are the only permitted recipients of personal data under this policy." +
                                                                   " Personal data in the Directory will be retained as long as the persons concerned remain registered as persons acting on behalf of end users of the Peppol Network."))
                                       .addChild (div ().addChild ("In case of any questions in relation to the Directory, or to exercise their rights to access, correct or delete their personal data, or to restrict or object to future processing, data subjects may contact the Operators via ")
                                                        .addChild (HCA_MailTo.createLinkedEmail ("info@peppol.eu"))
                                                        .addChild ("." +
                                                                   " If the provided answer is not satisfactory, data subjects may choose to lodge a complaint with ")
                                                        .addChild (a (new SimpleURL ("https://edpb.europa.eu/about-edpb/about-edpb/members_en")).addChild ("their local data protection authority."))));
      aNodeList.addChild (div (aTabBox).addClass (CBootstrapCSS.MY_3));
    }
    else
    {
      // No tab box needed
      aNodeList.addChild (aLogoContainer);
    }

    if (aWPEC.hasAction (CPageParam.ACTION_VIEW) && StringHelper.isNotEmpty (sParticipantID))
    {
      // Show details of a participant
      final IParticipantIdentifier aParticipantID = aIdentifierFactory.parseParticipantIdentifier (sParticipantID);
      if (aParticipantID != null)
      {
        // Show participant details
        final HCNodeList aDetails = createParticipantDetails (aDisplayLocale,
                                                              sParticipantID,
                                                              aParticipantID,
                                                              aWPEC.isLoggedInUserAdministrator ());
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
      if (StringHelper.isNotEmpty (sQuery))
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
