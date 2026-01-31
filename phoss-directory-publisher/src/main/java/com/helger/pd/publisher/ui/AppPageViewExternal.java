/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.ui;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.html.EHTMLVersion;
import com.helger.html.hc.IHCNode;
import com.helger.io.resource.IReadableResource;
import com.helger.photon.bootstrap4.pages.BootstrapWebPageUIHandler;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uicore.page.external.BasePageViewExternal;
import com.helger.photon.uicore.page.external.PageViewExternalHTMLCleanser;
import com.helger.xml.microdom.IMicroContainer;
import com.helger.xml.microdom.util.MicroVisitor;

import jakarta.annotation.Nullable;

public class AppPageViewExternal extends BasePageViewExternal <WebPageExecutionContext>
{
  private static void _cleanCode (@NonNull final IMicroContainer aCont)
  {
    // Do not clean texts, because this destroys "pre" formatting!
    final PageViewExternalHTMLCleanser aCleanser = new PageViewExternalHTMLCleanser (EHTMLVersion.HTML5).setCleanTexts (false);
    MicroVisitor.visit (aCont, aCleanser);
  }

  public AppPageViewExternal (@NonNull @Nonempty final String sID,
                              @NonNull final String sName,
                              @NonNull final IReadableResource aResource)
  {
    // Special content cleaner
    super (sID, sName, aResource, AppPageViewExternal::_cleanCode);
  }

  @Override
  @Nullable
  public IHCNode getHeaderNode (@NonNull final WebPageExecutionContext aWPEC)
  {
    final String sHeaderText = getHeaderText (aWPEC);
    return BootstrapWebPageUIHandler.INSTANCE.createPageHeader (sHeaderText);
  }

  @Override
  protected void fillContent (@NonNull final WebPageExecutionContext aWPEC)
  {
    super.fillContent (aWPEC);
  }
}
