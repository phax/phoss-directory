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
package com.helger.pd.publisher.app.secure;

import java.util.Locale;

import org.jspecify.annotations.NonNull;

import com.helger.base.string.StringHelper;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.IHCElement;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.metadata.HCHead;
import com.helger.html.hc.html.root.HCHtml;
import com.helger.html.hc.html.sections.HCBody;
import com.helger.html.hc.html.textlevel.HCSmall;
import com.helger.html.hc.html.textlevel.HCSpan;
import com.helger.html.hc.html.textlevel.HCStrong;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.CDirectoryVersion;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.publisher.CPDPublisher;
import com.helger.pd.publisher.app.AppCommonUI;
import com.helger.photon.app.url.LinkHelper;
import com.helger.photon.bootstrap4.CBootstrapCSS;
import com.helger.photon.bootstrap4.breadcrumb.BootstrapBreadcrumb;
import com.helger.photon.bootstrap4.breadcrumb.BootstrapBreadcrumbProvider;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.layout.BootstrapContainer;
import com.helger.photon.bootstrap4.navbar.BootstrapNavbar;
import com.helger.photon.bootstrap4.navbar.BootstrapNavbarToggleable;
import com.helger.photon.bootstrap4.uictrls.ext.BootstrapMenuItemRenderer;
import com.helger.photon.bootstrap4.uictrls.ext.BootstrapPageRenderer;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.appid.RequestSettings;
import com.helger.photon.core.execcontext.ISimpleWebExecutionContext;
import com.helger.photon.core.execcontext.LayoutExecutionContext;
import com.helger.photon.core.execcontext.SimpleWebExecutionContext;
import com.helger.photon.core.html.AbstractSWECHTMLProvider;
import com.helger.photon.core.html.CLayout;
import com.helger.photon.core.menu.IMenuItemPage;
import com.helger.photon.core.servlet.AbstractPublicApplicationServlet;
import com.helger.photon.core.servlet.LogoutServlet;
import com.helger.photon.security.login.LoggedInUserManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.util.SecurityHelper;
import com.helger.url.ISimpleURL;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.forcedredirect.ForcedRedirectException;

/**
 * Main class for creating HTML output
 *
 * @author Philip Helger
 */
public class SecureHTMLProvider extends AbstractSWECHTMLProvider
{
  @NonNull
  private static IHCNode _getNavbar (@NonNull final SimpleWebExecutionContext aSWEC)
  {
    final Locale aDisplayLocale = aSWEC.getDisplayLocale ();
    final IRequestWebScopeWithoutResponse aRequestScope = aSWEC.getRequestScope ();

    final ISimpleURL aLinkToStartPage = aSWEC.getLinkToMenuItem (aSWEC.getMenuTree ().getDefaultMenuItemID ());

    final BootstrapNavbar aNavbar = new BootstrapNavbar ();
    aNavbar.addBrand (new HCNodeList ().addChild (new HCSpan ().addClass (AppCommonUI.CSS_CLASS_LOGO1)
                                                               .addChild (CPDPublisher.getApplicationTitle ()))
                                       .addChild (new HCSpan ().addClass (AppCommonUI.CSS_CLASS_LOGO2)
                                                               .addChild (" Administration")), aLinkToStartPage);

    // Index queue length
    {
      final int nQueueLen = PDMetaManager.getIndexerMgr ().getIndexerWorkQueue ().getQueueLength ();
      aNavbar.addAndReturnText ().addChild ("[Queue length: " + nQueueLen + "]");
    }

    final BootstrapNavbarToggleable aToggleable = aNavbar.addAndReturnToggleable ();

    {
      final IUser aUser = LoggedInUserManager.getInstance ().getCurrentUser ();
      aToggleable.addAndReturnText ()
                 .addClass (CBootstrapCSS.ML_AUTO)
                 .addClass (CBootstrapCSS.MX_2)
                 .addChild ("Welcome ")
                 .addChild (new HCStrong ().addChild (SecurityHelper.getUserDisplayName (aUser, aDisplayLocale)));
      aToggleable.addChild (new BootstrapButton ().addClass (CBootstrapCSS.MX_2)
                                                  .addChild ("Goto public area")
                                                  .setOnClick (LinkHelper.getURLWithContext (AbstractPublicApplicationServlet.SERVLET_DEFAULT_PATH +
                                                                                             "/")));
      aToggleable.addChild (new BootstrapButton ().addClass (CBootstrapCSS.MX_2)
                                                  .setOnClick (LinkHelper.getURLWithContext (aRequestScope,
                                                                                             LogoutServlet.SERVLET_DEFAULT_PATH))
                                                  .addChild (EPhotonCoreText.LOGIN_LOGOUT.getDisplayText (aDisplayLocale)));
    }
    return aNavbar;
  }

  @NonNull
  public static IHCNode getMenuContent (@NonNull final LayoutExecutionContext aLEC)
  {
    final IHCElement <?> ret = BootstrapMenuItemRenderer.createSideBarMenu (aLEC);

    return new HCNodeList ().addChild (ret);
  }

  @NonNull
  public static IHCNode getContent (@NonNull final LayoutExecutionContext aLEC)
  {
    final HCNodeList ret = new HCNodeList ();

    // Header
    ret.addChild (_getNavbar (aLEC));

    final BootstrapContainer aOuterContainer = ret.addAndReturnChild (new BootstrapContainer ().setFluid (true));

    // Breadcrumbs
    {
      final BootstrapBreadcrumb aBreadcrumbs = BootstrapBreadcrumbProvider.createBreadcrumb (aLEC);
      aBreadcrumbs.addClasses (CBootstrapCSS.D_NONE, CBootstrapCSS.D_SM_BLOCK);
      aOuterContainer.addChild (aBreadcrumbs);
    }

    // Content
    {
      final HCDiv aRow = aOuterContainer.addAndReturnChild (new HCDiv ().addClass (CBootstrapCSS.D_MD_FLEX));
      final HCDiv aCol1 = aRow.addAndReturnChild (new HCDiv ().addClass (CBootstrapCSS.D_MD_FLEX)
                                                              .addClass (CBootstrapCSS.MR_2));
      final HCDiv aCol2 = aRow.addAndReturnChild (new HCDiv ().addClass (CBootstrapCSS.FLEX_FILL));

      // left

      // We need a wrapper span for easy AJAX content replacement
      aCol1.addClass (CBootstrapCSS.D_PRINT_NONE)
           .addChild (new HCSpan ().setID (CLayout.LAYOUT_AREAID_MENU)
                                   .addChild (getMenuContent (aLEC))
                                   .addChild (new HCDiv ().addChild (new HCSmall ().addChild (CDirectoryVersion.BUILD_VERSION)))
                                   .addChild (new HCDiv ().addChild (new HCSmall ().addChild (CDirectoryVersion.BUILD_TIMESTAMP))));
      aCol1.addChild (new HCDiv ().setID (CLayout.LAYOUT_AREAID_SPECIAL));

      // content - determine is exactly same as for view
      aCol2.addChild (BootstrapPageRenderer.getPageContent (aLEC));
    }

    return ret;
  }

  @Override
  protected void fillBody (@NonNull final ISimpleWebExecutionContext aSWEC, @NonNull final HCHtml aHtml)
                                                                                                         throws ForcedRedirectException
  {
    final IRequestWebScopeWithoutResponse aRequestScope = aSWEC.getRequestScope ();
    final Locale aDisplayLocale = aSWEC.getDisplayLocale ();
    final IMenuItemPage aMenuItem = RequestSettings.getMenuItem (aRequestScope);
    final LayoutExecutionContext aLEC = new LayoutExecutionContext (aSWEC, aMenuItem);
    final HCHead aHead = aHtml.head ();
    final HCBody aBody = aHtml.body ();

    // Add menu item in page title
    aHead.setPageTitle (StringHelper.getConcatenatedOnDemand (CPDPublisher.getApplicationTitle (),
                                                              " - ",
                                                              aMenuItem.getDisplayText (aDisplayLocale)));
    AppCommonUI.addFavIcons (aHead);

    final IHCNode aNode = getContent (aLEC);
    aBody.addChild (aNode);
  }
}
