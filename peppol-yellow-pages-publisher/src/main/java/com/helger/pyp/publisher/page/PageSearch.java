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
package com.helger.pyp.publisher.page;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.url.SimpleURL;
import com.helger.html.css.DefaultCSSClassProvider;
import com.helger.html.css.ICSSClassProvider;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCForm;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.sections.HCH2;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.photon.bootstrap3.CBootstrapCSS;
import com.helger.photon.bootstrap3.button.BootstrapSubmitButton;
import com.helger.photon.bootstrap3.grid.BootstrapRow;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.pyp.publisher.ui.HCExtImg;

public final class PageSearch extends AbstractAppWebPage
{
  private static final String FIELD_QUERY = "q";

  private static final ICSSClassProvider CSS_CLASS_QUERYBOX = DefaultCSSClassProvider.create ("querybox");
  private static final ICSSClassProvider CSS_CLASS_QUERYBUTTONS = DefaultCSSClassProvider.create ("querybuttons");

  public PageSearch (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Search");
  }

  @Override
  @Nullable
  public String getHeaderText (@Nonnull final WebPageExecutionContext aWPEC)
  {
    return null;
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

    {
      final HCForm aQueryBox = new HCForm ().setAction (aWPEC.getSelfHref ());
      aQueryBox.addChild (new HCDiv ().addClass (CSS_CLASS_QUERYBOX)
                                      .addChild (new HCEdit (new RequestField (FIELD_QUERY))));
      aQueryBox.addChild (new HCDiv ().addClass (CSS_CLASS_QUERYBUTTONS)
                                      .addChild (new BootstrapSubmitButton ().addChild ("Search PYP")
                                                                             .setIcon (EDefaultIcon.MAGNIFIER)));

      final BootstrapRow aBodyRow = aNodeList.addAndReturnChild (new BootstrapRow ());
      aBodyRow.createColumn (12, 1, 2, 3).addClass (CBootstrapCSS.HIDDEN_XS);
      aBodyRow.createColumn (12, 10, 8, 6).addChild (aQueryBox);
      aBodyRow.createColumn (12, 1, 2, 3).addClass (CBootstrapCSS.HIDDEN_XS);
    }
  }
}
