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
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.lucene.search.Query;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.SimpleURL;
import com.helger.html.css.DefaultCSSClassProvider;
import com.helger.html.css.ICSSClassProvider;
import com.helger.html.hc.html.forms.EHCFormMethod;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCForm;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCOL;
import com.helger.html.hc.html.sections.HCH2;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.photon.bootstrap3.CBootstrapCSS;
import com.helger.photon.bootstrap3.alert.BootstrapInfoBox;
import com.helger.photon.bootstrap3.button.BootstrapSubmitButton;
import com.helger.photon.bootstrap3.grid.BootstrapRow;
import com.helger.photon.bootstrap3.inputgroup.BootstrapInputGroup;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.pyp.indexer.mgr.PYPMetaManager;
import com.helger.pyp.publisher.ui.AbstractAppWebPage;
import com.helger.pyp.publisher.ui.HCExtImg;
import com.helger.pyp.storage.PYPQueryManager;
import com.helger.pyp.storage.PYPStorageManager;
import com.helger.pyp.storage.PYPStoredDocument;

public final class PagePublicSearch extends AbstractAppWebPage
{
  private static final String FIELD_QUERY = "q";

  private static final ICSSClassProvider CSS_CLASS_BIG_QUERY_BOX = DefaultCSSClassProvider.create ("big-querybox");
  private static final ICSSClassProvider CSS_CLASS_BIG_QUERY_BUTTONS = DefaultCSSClassProvider.create ("big-querybuttons");
  private static final ICSSClassProvider CSS_CLASS_SMALL_QUERY_BOX = DefaultCSSClassProvider.create ("small-querybox");

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

  @Override
  protected void fillContent (final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();

    {
      final BootstrapRow aHeaderRow = aNodeList.addAndReturnChild (new BootstrapRow ());
      // A PYP logo would be nice
      aHeaderRow.createColumn (12, 12, 1, 2).addClass (CBootstrapCSS.HIDDEN_SM);
      aHeaderRow.createColumn (12, 6, 5, 4).addChild (new HCH2 ().addChild ("PYP logo goes here"));
      aHeaderRow.createColumn (12, 6, 5, 4)
                .addChild (new HCExtImg (new SimpleURL ("/imgs/peppol.png")).addClass (CBootstrapCSS.PULL_RIGHT));
      aHeaderRow.createColumn (12, 12, 1, 2).addClass (CBootstrapCSS.HIDDEN_SM);
    }

    final String sQuery = aWPEC.getAttributeAsString (FIELD_QUERY);
    if (StringHelper.hasText (sQuery))
    {
      // Show small query box
      // Show big query box
      final HCForm aSmallQueryBox = new HCForm ().setAction (aWPEC.getSelfHref ()).setMethod (EHCFormMethod.GET);
      aSmallQueryBox.addChild (new BootstrapInputGroup (_createQueryEdit ()).addSuffix (new BootstrapSubmitButton ().setIcon (EDefaultIcon.MAGNIFIER))
                                                                            .addClass (CSS_CLASS_SMALL_QUERY_BOX));

      final BootstrapRow aBodyRow = aNodeList.addAndReturnChild (new BootstrapRow ());
      aBodyRow.createColumn (12, 6, 6, 6).addChild (aSmallQueryBox);

      // Build Lucene query
      final Query aLuceneQuery = PYPQueryManager.convertQueryStringToLuceneQuery (PYPMetaManager.getLucene (), sQuery);
      // Search all documents
      final List <PYPStoredDocument> aResultDocs = PYPMetaManager.getStorageMgr ().getAllDocuments (aLuceneQuery);
      // Group by participant ID
      final Map <String, List <PYPStoredDocument>> aGroupedDocs = PYPStorageManager.getGroupedByParticipantID (aResultDocs);

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
          final String sParticipantID = aEntry.getKey ();
          final List <PYPStoredDocument> aDocs = aEntry.getValue ();

          final HCDiv aResultItem = new HCDiv ();
          aResultItem.addChild (sParticipantID);
          if (aDocs.size () > 1)
            aResultItem.addChild (" (" + aDocs.size () + " entities)");

          aOL.addItem (aResultItem);
          if (aOL.getChildCount () >= 10)
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
