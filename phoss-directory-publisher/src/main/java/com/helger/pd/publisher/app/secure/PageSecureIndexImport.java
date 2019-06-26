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
package com.helger.pd.publisher.app.secure;

import java.util.Locale;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.error.IError;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.businesscard.IPDBusinessCardProvider;
import com.helger.pd.indexer.businesscard.SMPBusinessCardProvider;
import com.helger.pd.indexer.index.EIndexerWorkItemType;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.publisher.app.AppCommonUI;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.photon.bootstrap4.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap4.alert.BootstrapInfoBox;
import com.helger.photon.bootstrap4.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap4.alert.BootstrapWarnBox;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.form.BootstrapForm;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.uictrls.ext.BootstrapFileUpload;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.web.fileupload.FileItemResource;
import com.helger.web.fileupload.IFileItem;
import com.helger.xml.EXMLParserFeature;
import com.helger.xml.sax.CollectingSAXErrorHandler;
import com.helger.xml.serialize.read.SAXReader;
import com.helger.xml.serialize.read.SAXReaderSettings;

public final class PageSecureIndexImport extends AbstractAppWebPage
{
  public static final String FIELD_FILE = "file";
  private static final Logger LOGGER = LoggerFactory.getLogger (PageSecureIndexImport.class);

  public PageSecureIndexImport (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Import participants");
  }

  @Override
  protected void fillContent (final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();
    final FormErrorList aFormErrors = new FormErrorList ();

    {
      final IPDBusinessCardProvider aBCProv = PDMetaManager.getBusinessCardProvider ();
      if (aBCProv instanceof SMPBusinessCardProvider)
      {
        final SMPBusinessCardProvider aSMPBCProv = (SMPBusinessCardProvider) aBCProv;
        if (aSMPBCProv.isFixedSMP ())
        {
          aNodeList.addChild (new BootstrapInfoBox ().addChild ("Fixed SMP URI " +
                                                                aSMPBCProv.getFixedSMPURI () +
                                                                " is used."));
        }
        else
        {
          aNodeList.addChild (new BootstrapInfoBox ().addChild ("The following SMLs are crawled for entries: " +
                                                                StringHelper.getImplodedMapped (", ",
                                                                                                aSMPBCProv.getAllSMLsToUse (),
                                                                                                ISMLInfo::getDisplayName)));
        }
      }
    }

    final boolean bIsFormSubmitted = aWPEC.hasAction (CPageParam.ACTION_PERFORM);
    if (bIsFormSubmitted)
    {
      final IFileItem aFile = aWPEC.params ().getAsFileItem (FIELD_FILE);
      if (aFile == null || StringHelper.hasNoText (aFile.getName ()))
        aFormErrors.addFieldError (FIELD_FILE, "No file was selected");

      if (aFormErrors.isEmpty ())
      {
        final HCNodeList aResultNL = new HCNodeList ();
        final SAXReaderSettings aSettings = new SAXReaderSettings ();
        aSettings.setFeatureValues (EXMLParserFeature.AVOID_DOS_SETTINGS);

        final CollectingSAXErrorHandler aErrorHandler = new CollectingSAXErrorHandler ();
        aSettings.setErrorHandler (aErrorHandler);

        final ICommonsList <IParticipantIdentifier> aQueued = new CommonsArrayList <> ();
        final ICommonsList <IParticipantIdentifier> aNotQueued = new CommonsArrayList <> ();
        aSettings.setContentHandler (new DefaultHandler ()
        {
          @Override
          public void startElement (final String sURI,
                                    final String sLocalName,
                                    final String sQName,
                                    final Attributes aAttributes) throws SAXException
          {
            if (sQName.equals ("participant"))
            {
              final String sScheme = aAttributes.getValue ("scheme");
              final String sValue = aAttributes.getValue ("value");
              final IParticipantIdentifier aParticipantID = aIdentifierFactory.createParticipantIdentifier (sScheme,
                                                                                                            sValue);
              if (aParticipantID != null)
              {
                if (PDMetaManager.getIndexerMgr ()
                                 .queueWorkItem (aParticipantID,
                                                 EIndexerWorkItemType.CREATE_UPDATE,
                                                 "import-triggered",
                                                 "localhost")
                                 .isChanged ())
                {
                  aQueued.add (aParticipantID);
                }
                else
                {
                  aNotQueued.add (aParticipantID);
                }
              }
              else
                LOGGER.error ("Failed to convert '" + sScheme + "' and '" + sValue + "' to a participant identifier");
            }
          }
        });

        LOGGER.info ("Importing participant IDs from '" + aFile.getNameSecure () + "'");

        final ESuccess eSuccess = SAXReader.readXMLSAX (new FileItemResource (aFile), aSettings);

        LOGGER.info ("Finished reading XML file. Queued " +
                     aQueued.size () +
                     "; not queued: " +
                     aNotQueued.size () +
                     "; errors: " +
                     aErrorHandler.getErrorList ().size ());

        // Some things may have been queued even in case of error
        if (aQueued.isNotEmpty ())
        {
          final HCUL aUL = new HCUL ();
          for (final IParticipantIdentifier aPI : aQueued)
            aUL.addItem (aPI.getURIEncoded ());
          aResultNL.addChild (new BootstrapSuccessBox ().addChild (new HCDiv ().addChild ("The following identifiers were successfully queued for indexing:"))
                                                        .addChild (aUL));
        }
        if (aNotQueued.isNotEmpty ())
        {
          final HCUL aUL = new HCUL ();
          for (final IParticipantIdentifier aPI : aNotQueued)
            aUL.addItem (aPI.getURIEncoded ());
          aResultNL.addChild (new BootstrapWarnBox ().addChild (new HCDiv ().addChild ("The following identifiers could not be queued (because they are already in the queue):"))
                                                     .addChild (aUL));
        }

        if (eSuccess.isFailure ())
        {
          final HCUL aUL = new HCUL ();
          for (final IError aError : aErrorHandler.getErrorList ())
          {
            final String sMsg = aError.getAsString (AppCommonUI.DEFAULT_LOCALE);
            LOGGER.error ("  " + sMsg);
            aUL.addItem (sMsg);
          }
          aResultNL.addChild (new BootstrapErrorBox ().addChild (new HCDiv ().addChild ("Error parsing provided XML:"))
                                                      .addChild (aUL));
        }
        aWPEC.postRedirectGetInternal (aResultNL);
      }
    }

    final BootstrapForm aForm = aNodeList.addAndReturnChild (getUIHandler ().createFormFileUploadSelf (aWPEC,
                                                                                                       bIsFormSubmitted));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Import file")
                                                 .setCtrl (new BootstrapFileUpload (FIELD_FILE, aDisplayLocale))
                                                 .setHelpText ("Select a file that was created from a full XML export to index of all them manually.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_FILE)));

    final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (new BootstrapButtonToolbar (aWPEC));
    aToolbar.addHiddenField (CPageParam.PARAM_ACTION, CPageParam.ACTION_PERFORM);
    aToolbar.addSubmitButton ("Import all", EDefaultIcon.YES);
  }
}
