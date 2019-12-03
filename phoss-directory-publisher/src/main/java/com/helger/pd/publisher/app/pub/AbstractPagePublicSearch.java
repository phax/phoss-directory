/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.collection.multimap.IMultiMapListBased;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;
import com.helger.commons.string.StringHelper;
import com.helger.html.css.DefaultCSSClassProvider;
import com.helger.html.css.ICSSClassProvider;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.grouping.HCLI;
import com.helger.html.hc.html.grouping.HCOL;
import com.helger.html.hc.html.sections.HCH1;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.html.textlevel.HCSmall;
import com.helger.html.hc.html.textlevel.HCSpan;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.settings.PDServerConfiguration;
import com.helger.pd.indexer.storage.PDStorageManager;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.pd.indexer.storage.PDStoredMLName;
import com.helger.pd.publisher.nicename.NiceNameUI;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.pd.publisher.ui.PDCommonUI;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.peppol.pidscheme.IParticipantIdentifierScheme;
import com.helger.peppolid.peppol.pidscheme.ParticipantIdentifierSchemeManager;
import com.helger.photon.bootstrap4.alert.BootstrapInfoBox;
import com.helger.photon.bootstrap4.alert.BootstrapWarnBox;
import com.helger.photon.bootstrap4.badge.BootstrapBadge;
import com.helger.photon.bootstrap4.badge.EBootstrapBadgeType;
import com.helger.photon.bootstrap4.card.BootstrapCard;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.form.BootstrapViewForm;
import com.helger.photon.bootstrap4.nav.BootstrapTabBox;
import com.helger.photon.bootstrap4.pages.BootstrapWebPageUIHandler;
import com.helger.photon.bootstrap4.utils.BootstrapPageHeader;

public abstract class AbstractPagePublicSearch extends AbstractAppWebPage
{
  protected static enum EUIMode implements IHasID <String>
  {
    DEFAULT ("default"),
    TOOP ("toop");

    private final String m_sID;

    private EUIMode (@Nonnull @Nonempty final String sID)
    {
      m_sID = sID;
    }

    @Nonnull
    @Nonempty
    public String getID ()
    {
      return m_sID;
    }

    public boolean isUseGreenButton ()
    {
      return this == DEFAULT;
    }

    public boolean isUseHelptext ()
    {
      return this == DEFAULT;
    }

    @Nonnull
    public static EUIMode getFromIDOrDefault (@Nullable final String sID)
    {
      return EnumHelper.getFromIDOrDefault (EUIMode.class, sID, EUIMode.DEFAULT);
    }
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractPagePublicSearch.class);

  protected static final ICSSClassProvider CSS_CLASS_BIG_QUERY_IMAGE_CONTAINER = DefaultCSSClassProvider.create ("big-query-image-container");
  protected static final ICSSClassProvider CSS_CLASS_BIG_QUERY_IMAGE = DefaultCSSClassProvider.create ("big-query-image");
  protected static final ICSSClassProvider CSS_CLASS_BIG_QUERY_BOX = DefaultCSSClassProvider.create ("big-query-box");
  protected static final ICSSClassProvider CSS_CLASS_BIG_QUERY_HELPTEXT = DefaultCSSClassProvider.create ("big-query-helptext");
  protected static final ICSSClassProvider CSS_CLASS_BIG_QUERY_BUTTONS = DefaultCSSClassProvider.create ("big-query-buttons");

  protected static final ICSSClassProvider CSS_CLASS_RESULT_DOC = DefaultCSSClassProvider.create ("result-doc");
  protected static final ICSSClassProvider CSS_CLASS_RESULT_DOC_HEADER = DefaultCSSClassProvider.create ("result-doc-header");
  protected static final ICSSClassProvider CSS_CLASS_RESULT_DOC_COUNTRY_CODE = DefaultCSSClassProvider.create ("result-doc-country-code");
  protected static final ICSSClassProvider CSS_CLASS_RESULT_DOC_NAME = DefaultCSSClassProvider.create ("result-doc-name");
  protected static final ICSSClassProvider CSS_CLASS_RESULT_DOC_GEOINFO = DefaultCSSClassProvider.create ("result-doc-geoinfo");
  protected static final ICSSClassProvider CSS_CLASS_RESULT_DOC_FREETEXT = DefaultCSSClassProvider.create ("result-doc-freetext");
  protected static final ICSSClassProvider CSS_CLASS_RESULT_DOC_SDBUTTON = DefaultCSSClassProvider.create ("result-doc-sdbutton");
  protected static final ICSSClassProvider CSS_CLASS_RESULT_PANEL = DefaultCSSClassProvider.create ("result-panel");

  protected static final EUIMode s_eUIMode;

  static
  {
    // Determined by configuration file!
    s_eUIMode = EUIMode.getFromIDOrDefault (PDServerConfiguration.getSearchUIMode ());
  }

  public AbstractPagePublicSearch (@Nonnull @Nonempty final String sID, @Nonnull final String sName)
  {
    super (sID, sName);
  }

  @Nonnull
  protected static <ELEMENTTYPE> String _getImplodedMapped (@Nonnull final String sSep,
                                                            @Nonnull final String sLastSep,
                                                            @Nullable final Collection <? extends ELEMENTTYPE> aElements,
                                                            @Nonnull final Function <? super ELEMENTTYPE, String> aMapper)
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

  @Nonnull
  protected static HCNodeList createParticipantDetails (@Nonnull final Locale aDisplayLocale,
                                                        @Nonnull final String sParticipantID,
                                                        @Nonnull final IParticipantIdentifier aParticipantID)
  {
    final HCNodeList aDetails = new HCNodeList ();

    // Search document matching participant ID
    final ICommonsList <PDStoredBusinessEntity> aResultDocs = PDMetaManager.getStorageMgr ()
                                                                           .getAllDocumentsOfParticipant (aParticipantID);
    // Group by participant ID
    final IMultiMapListBased <IParticipantIdentifier, PDStoredBusinessEntity> aGroupedDocs = PDStorageManager.getGroupedByParticipantID (aResultDocs);
    if (aGroupedDocs.isEmpty ())
      LOGGER.error ("No stored document matches participant identifier '" + sParticipantID + "'");
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

      // Details header
      {
        IHCNode aDetailsHeader = null;
        final boolean bIsPeppolDefault = aParticipantID.hasScheme (PeppolIdentifierFactory.INSTANCE.getDefaultParticipantIdentifierScheme ());
        if (bIsPeppolDefault)
        {
          final IParticipantIdentifierScheme aIIA = ParticipantIdentifierSchemeManager.getSchemeOfIdentifier (aParticipantID);
          if (aIIA != null)
          {
            final HCH1 aH1 = new HCH1 ().addChild ("Details for: " + aParticipantID.getValue ());
            if (StringHelper.hasText (aIIA.getSchemeAgency ()))
              aH1.addChild (new HCSmall ().addChild (" (" + aIIA.getSchemeAgency () + ")"));
            aDetailsHeader = new BootstrapPageHeader ().addChild (aH1);
          }
        }
        if (aDetailsHeader == null)
        {
          // Fallback
          aDetailsHeader = BootstrapWebPageUIHandler.INSTANCE.createPageHeader ("Details for " + sParticipantID);
        }
        aDetails.addChild (aDetailsHeader);
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
          aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Full PEPPOL participant ID")
                                                           .setCtrl (new HCCode ().addChild (sParticipantID)));
          aCard.createAndAddBody ().addChild (aViewForm);
          ++nIndex;
        }
        // Add whole list or just the first item?
        final IHCNode aTabLabel = new HCSpan ().addChild ("Business information ")
                                               .addChild (BootstrapBadge.createNumeric (aStoredEntities.size ())
                                                                        .setBadgeType (EBootstrapBadgeType.PRIMARY));
        aTabBox.addTab ("businessinfo", aTabLabel, aOL, true);
      }

      // Document types
      {
        final HCNodeList aDocTypeCtrl = new HCNodeList ();
        final ICommonsList <String> aNames = new CommonsArrayList <> ();
        for (final PDStoredBusinessEntity aStoredEntity : aStoredEntities)
          aNames.addAllMapped (aStoredEntity.names (), PDStoredMLName::getName);
        aDocTypeCtrl.addChild (new BootstrapInfoBox ().addChild ("The following document types are supported by " +
                                                                 _getImplodedMapped (", ",
                                                                                     " and ",
                                                                                     aNames,
                                                                                     x -> "'" + x + "'") +
                                                                 ":"));

        HCOL aDocTypeOL = null;
        final ICommonsList <IDocumentTypeIdentifier> aDocTypeIDs = aResultDocs.getFirst ()
                                                                              .documentTypeIDs ()
                                                                              .getSorted (IDocumentTypeIdentifier.comparator ());
        for (final IDocumentTypeIdentifier aDocTypeID : aDocTypeIDs)
        {
          if (aDocTypeOL == null)
            aDocTypeOL = aDocTypeCtrl.addAndReturnChild (new HCOL ());

          final HCLI aLI = aDocTypeOL.addItem ();
          aLI.addChild (NiceNameUI.getDocumentTypeID (aDocTypeID));
        }

        if (aDocTypeOL == null)
          aDocTypeCtrl.addChild (new BootstrapWarnBox ().addChild ("This participant does not support any document types!"));

        aTabBox.addTab ("doctypes",
                        new HCSpan ().addChild ("Supported document types ")
                                     .addChild (BootstrapBadge.createNumeric (aDocTypeIDs.size ())
                                                              .setBadgeType (EBootstrapBadgeType.PRIMARY)),
                        aDocTypeCtrl,
                        false);
      }
      aDetails.addChild (aTabBox);
    }
    return aDetails;
  }
}
