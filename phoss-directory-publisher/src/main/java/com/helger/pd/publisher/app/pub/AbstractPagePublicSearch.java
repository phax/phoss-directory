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

import java.util.Collection;
import java.util.Locale;
import java.util.function.Function;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.debug.GlobalDebug;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.datetime.format.PDTToString;
import com.helger.html.css.DefaultCSSClassProvider;
import com.helger.html.css.ICSSClassProvider;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.grouping.HCHR;
import com.helger.html.hc.html.grouping.HCLI;
import com.helger.html.hc.html.grouping.HCOL;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.masterdata.vat.VATINSyntaxChecker;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.settings.PDServerConfiguration;
import com.helger.pd.indexer.storage.PDStorageManager;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.pd.indexer.storage.PDStoredMLName;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.pd.publisher.ui.PACountryCodeHelper;
import com.helger.pd.publisher.ui.PDCommonUI;
import com.helger.peppol.ui.nicename.NiceNameUI;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.peppol.pidscheme.IPeppolParticipantIdentifierScheme;
import com.helger.peppolid.peppol.pidscheme.PeppolParticipantIdentifierSchemeManager;
import com.helger.photon.bootstrap4.card.BootstrapCard;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.form.BootstrapViewForm;
import com.helger.photon.bootstrap4.nav.BootstrapTabBox;
import com.helger.photon.bootstrap4.pages.BootstrapWebPageUIHandler;
import com.helger.photon.bootstrap4.utils.BootstrapPageHeader;

import jakarta.annotation.Nullable;

public abstract class AbstractPagePublicSearch extends AbstractAppWebPage
{
  protected enum EUIMode implements IHasID <String>
  {
    PEPPOL ("peppol");

    private final String m_sID;

    EUIMode (@NonNull @Nonempty final String sID)
    {
      m_sID = sID;
    }

    @NonNull
    @Nonempty
    public String getID ()
    {
      return m_sID;
    }

    public boolean isUseGreenButton ()
    {
      return this == PEPPOL;
    }

    public boolean isUseHelptext ()
    {
      return this == PEPPOL;
    }

    public boolean isShowPrivacyPolicy ()
    {
      return this == PEPPOL;
    }

    @NonNull
    public static EUIMode getFromIDOrDefault (@Nullable final String sID)
    {
      return EnumHelper.getFromIDOrDefault (EUIMode.class, sID, EUIMode.PEPPOL);
    }
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractPagePublicSearch.class);

  protected static final ICSSClassProvider CSS_CLASS_BIG_QUERY_IMAGE_CONTAINER = DefaultCSSClassProvider.create ("big-query-image-container");
  protected static final ICSSClassProvider CSS_CLASS_BIG_QUERY_IMAGE = DefaultCSSClassProvider.create ("big-query-image");
  protected static final ICSSClassProvider CSS_CLASS_BIG_QUERY_BOX = DefaultCSSClassProvider.create ("big-query-box");
  protected static final ICSSClassProvider CSS_CLASS_BIG_QUERY_HELPTEXT = DefaultCSSClassProvider.create ("big-query-helptext");
  protected static final ICSSClassProvider CSS_CLASS_BIG_QUERY_BUTTONS = DefaultCSSClassProvider.create ("big-query-buttons");

  protected static final ICSSClassProvider CSS_CLASS_RESULT_DOC = DefaultCSSClassProvider.create ("result-doc");
  protected static final ICSSClassProvider CSS_CLASS_RESULT_PANEL = DefaultCSSClassProvider.create ("result-panel");

  protected static final EUIMode UI_MODE;

  static
  {
    // Determined by configuration file!
    UI_MODE = EUIMode.getFromIDOrDefault (PDServerConfiguration.getSearchUIMode ());
  }

  public AbstractPagePublicSearch (@NonNull @Nonempty final String sID, @NonNull final String sName)
  {
    super (sID, sName);
  }

  @NonNull
  protected static <ELEMENTTYPE> String _getImplodedMapped (@NonNull final String sSep,
                                                            @NonNull final String sLastSep,
                                                            @Nullable final Collection <? extends ELEMENTTYPE> aElements,
                                                            @NonNull final Function <? super ELEMENTTYPE, String> aMapper)
  {
    ValueEnforcer.notNull (sSep, "Separator");
    ValueEnforcer.notNull (aMapper, "Mapper");

    final StringBuilder aSB = new StringBuilder ();
    if (aElements != null)
    {
      final int nIndexOfLast = aElements.size () - 1;
      int nIndex = 0;
      for (final ELEMENTTYPE aElement : aElements)
      {
        if (nIndex > 0)
          aSB.append (nIndex == nIndexOfLast ? sLastSep : sSep);
        aSB.append (aMapper.apply (aElement));
        nIndex++;
      }
    }
    return aSB.toString ();
  }

  @NonNull
  protected HCNodeList createParticipantDetails (@NonNull final Locale aDisplayLocale,
                                                 @NonNull final String sParticipantID,
                                                 @NonNull final IParticipantIdentifier aParticipantID,
                                                 final boolean bIsLoggedInUserAdministrator)
  {
    final HCNodeList aDetails = new HCNodeList ();

    // Search document matching participant ID
    final ICommonsList <PDStoredBusinessEntity> aResultDocs = PDMetaManager.getStorageMgr ()
                                                                           .getAllDocumentsOfParticipant (aParticipantID);
    // Group by participant ID
    final ICommonsMap <IParticipantIdentifier, ICommonsList <PDStoredBusinessEntity>> aGroupedDocs = PDStorageManager.getGroupedByParticipantID (aResultDocs);
    if (aGroupedDocs.isEmpty ())
      LOGGER.error ("No stored document matches participant identifier '" + sParticipantID + "' - cannot show details");
    else
    {
      if (aGroupedDocs.size () > 1)
        LOGGER.warn ("Found " +
                     aGroupedDocs.size () +
                     " entries for participant identifier '" +
                     sParticipantID +
                     "' - weird");
      // Get the first one
      final ICommonsList <PDStoredBusinessEntity> aStoredEntities = aGroupedDocs.getFirstValue ();

      final IPeppolParticipantIdentifierScheme aPIScheme = PeppolParticipantIdentifierSchemeManager.getSchemeOfIdentifier (aParticipantID);

      // Details header
      {
        final BootstrapPageHeader aDetailsHeader;
        if (aPIScheme != null)
        {
          // Known scheme
          aDetailsHeader = new BootstrapPageHeader ();
          aDetailsHeader.addChild (h1 ("Details for: " + aParticipantID.getValue ()));
          if (StringHelper.isNotEmpty (aPIScheme.getSchemeName ()))
            aDetailsHeader.addChild (div (strong ("(" + aPIScheme.getSchemeName () + ")")));
        }
        else
        {
          // Fallback
          aDetailsHeader = BootstrapWebPageUIHandler.INSTANCE.createPageHeader ("Details for " + sParticipantID);
        }
        aDetails.addChild (aDetailsHeader);
      }

      // Country specifics
      {
        if (aPIScheme != null)
        {
          final String sParticipantIDValue = aParticipantID.getValue ().toUpperCase (Locale.ROOT);
          final String sCountryCode = PACountryCodeHelper.getCountryCode (sParticipantIDValue);
          if (PACountryCodeHelper.BE.equals (sCountryCode))
          {
            // Belgium specifics
            String sCBENumber = null;
            if (sParticipantIDValue.startsWith ("0208:"))
              sCBENumber = sParticipantIDValue.substring (5);
            else
              if (sParticipantIDValue.startsWith ("9925:BE"))
                sCBENumber = sParticipantIDValue.substring (7);

            if (sCBENumber != null && !VATINSyntaxChecker.isValidVATIN_BE (sCBENumber))
              aDetails.addChild (warn ("The CBE number '" +
                                       sCBENumber +
                                       "' does not seem to match the syntax requirements (length 10, start with 0 or 1, mod97 check digit)"));
          }
        }
      }

      final BootstrapTabBox aTabBox = new BootstrapTabBox ();

      // Business information
      {
        final HCNodeList aOL = new HCNodeList ();
        int nIndex = 1;
        for (final PDStoredBusinessEntity aStoredEntity : aStoredEntities)
        {
          final BootstrapCard aCard = aOL.addAndReturnChild (new BootstrapCard ());
          aCard.addClass (CSS_CLASS_RESULT_PANEL);
          if (aStoredEntities.size () > 1)
            aCard.createAndAddHeader ().addChild ("Business information entity " + nIndex);
          final BootstrapViewForm aViewForm = PDCommonUI.showBusinessInfoDetails (aStoredEntity, aDisplayLocale);
          aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Full Peppol participant ID")
                                                           .setCtrl (code (sParticipantID)));

          if (GlobalDebug.isDebugMode () || bIsLoggedInUserAdministrator)
          {
            aViewForm.addChild (new HCHR ());
            aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("[Debug] Creation DT")
                                                             .setCtrl (PDTToString.getAsString (aStoredEntity.getMetaData ()
                                                                                                             .getCreationDT (),
                                                                                                aDisplayLocale)));
            aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("[Debug] Owner ID")
                                                             .setCtrl (code (aStoredEntity.getMetaData ()
                                                                                          .getOwnerID ())));
            aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("[Debug] Requesting Host")
                                                             .setCtrl (code (aStoredEntity.getMetaData ()
                                                                                          .getRequestingHost ())));
          }

          aCard.createAndAddBody ().addChild (aViewForm);
          ++nIndex;
        }
        // Add whole list or just the first item?
        final IHCNode aTabLabel = span ("Business information ").addChild (badgePrimary (aStoredEntities.size ()));
        aTabBox.addTab ("businessinfo", aTabLabel, aOL, true);
      }

      // Document types
      {
        final HCNodeList aDocTypeCtrl = new HCNodeList ();
        final ICommonsList <String> aNames = new CommonsArrayList <> ();
        for (final PDStoredBusinessEntity aStoredEntity : aStoredEntities)
          aNames.addAllMapped (aStoredEntity.names (), PDStoredMLName::getName);
        aDocTypeCtrl.addChild (info ("The following document types are supported by " +
                                     _getImplodedMapped (", ", " and ", aNames, x -> "'" + x + "'") +
                                     " at the time of the last indexation:"));

        HCOL aDocTypeOL = null;
        final ICommonsList <IDocumentTypeIdentifier> aDocTypeIDs = aResultDocs.getFirstOrNull ()
                                                                              .documentTypeIDs ()
                                                                              .getSorted (IDocumentTypeIdentifier.comparator ());
        for (final IDocumentTypeIdentifier aDocTypeID : aDocTypeIDs)
        {
          if (aDocTypeOL == null)
            aDocTypeOL = aDocTypeCtrl.addAndReturnChild (new HCOL ());

          final HCLI aLI = aDocTypeOL.addItem ();
          aLI.addChild (NiceNameUI.createDocTypeID (aDocTypeID, true));
        }

        if (aDocTypeOL == null)
          aDocTypeCtrl.addChild (warn ("This participant does not support any document types!"));

        aTabBox.addTab ("doctypes",
                        span ("Supported document types ").addChild (badgePrimary (aDocTypeIDs.size ())),
                        aDocTypeCtrl,
                        false);
      }
      aDetails.addChild (aTabBox);
    }
    return aDetails;
  }
}
