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
package com.helger.pd.publisher.app.secure;

import java.util.Locale;

import javax.annotation.Nonnull;

import com.helger.commons.url.ISimpleURL;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.IHCElement;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.metadata.HCHead;
import com.helger.html.hc.html.textlevel.HCSpan;
import com.helger.html.hc.html.textlevel.HCStrong;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.publisher.CPDPublisher;
import com.helger.pd.publisher.app.AppCommonUI;
import com.helger.pd.publisher.app.pub.AppRendererPublic;
import com.helger.pd.publisher.ui.HCExtImg;
import com.helger.photon.bootstrap3.CBootstrapCSS;
import com.helger.photon.bootstrap3.base.BootstrapContainer;
import com.helger.photon.bootstrap3.breadcrumbs.BootstrapBreadcrumbs;
import com.helger.photon.bootstrap3.breadcrumbs.BootstrapBreadcrumbsProvider;
import com.helger.photon.bootstrap3.button.BootstrapButton;
import com.helger.photon.bootstrap3.grid.BootstrapRow;
import com.helger.photon.bootstrap3.nav.BootstrapNav;
import com.helger.photon.bootstrap3.navbar.BootstrapNavbar;
import com.helger.photon.bootstrap3.navbar.EBootstrapNavbarPosition;
import com.helger.photon.bootstrap3.navbar.EBootstrapNavbarType;
import com.helger.photon.bootstrap3.uictrls.ext.BootstrapMenuItemRenderer;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.app.context.LayoutExecutionContext;
import com.helger.photon.core.app.context.SimpleWebExecutionContext;
import com.helger.photon.core.app.layout.CLayout;
import com.helger.photon.core.app.layout.ILayoutAreaContentProvider;
import com.helger.photon.core.servlet.AbstractPublicApplicationServlet;
import com.helger.photon.core.servlet.LogoutServlet;
import com.helger.photon.core.url.LinkHelper;
import com.helger.photon.security.login.LoggedInUserManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.util.SecurityHelper;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * The viewport renderer (menu + content area)
 *
 * @author Philip Helger
 */
public final class AppRendererSecure implements ILayoutAreaContentProvider <LayoutExecutionContext>
{
  @Nonnull
  private static IHCNode _getNavbar (@Nonnull final SimpleWebExecutionContext aSWEC)
  {
    final Locale aDisplayLocale = aSWEC.getDisplayLocale ();
    final IRequestWebScopeWithoutResponse aRequestScope = aSWEC.getRequestScope ();

    final ISimpleURL aLinkToStartPage = aSWEC.getLinkToMenuItem (aSWEC.getMenuTree ().getDefaultMenuItemID ());

    final BootstrapNavbar aNavbar = new BootstrapNavbar (EBootstrapNavbarType.STATIC_TOP, true, aDisplayLocale);
    aNavbar.getContainer ().setFluid (true);
    aNavbar.addBrand (new HCNodeList ().addChild (new HCSpan ().addClass (AppCommonUI.CSS_CLASS_LOGO1)
                                                               .addChild (AppCommonUI.getApplicationTitle ()))
                                       .addChild (new HCSpan ().addClass (AppCommonUI.CSS_CLASS_LOGO2)
                                                               .addChild (" Administration")),
                      aLinkToStartPage);

    {
      final BootstrapNav aNav = new BootstrapNav ();
      final IUser aUser = LoggedInUserManager.getInstance ().getCurrentUser ();
      aNav.addButton (new BootstrapButton ().addChild ("Goto public area")
                                            .setOnClick (LinkHelper.getURLWithContext (AbstractPublicApplicationServlet.SERVLET_DEFAULT_PATH +
                                                                                       "/")));
      aNav.addItem (new HCSpan ().addClass (CBootstrapCSS.NAVBAR_TEXT)
                                 .addChild ("Welcome ")
                                 .addChild (new HCStrong ().addChild (SecurityHelper.getUserDisplayName (aUser,
                                                                                                         aDisplayLocale))));

      aNav.addButton (new BootstrapButton ().setOnClick (LinkHelper.getURLWithContext (aRequestScope,
                                                                                       LogoutServlet.SERVLET_DEFAULT_PATH))
                                            .addChild (EPhotonCoreText.LOGIN_LOGOUT.getDisplayText (aDisplayLocale)));
      aNavbar.addNav (EBootstrapNavbarPosition.COLLAPSIBLE_RIGHT, aNav);
    }
    return aNavbar;
  }

  @Nonnull
  public static IHCNode getMenuContent (@Nonnull final LayoutExecutionContext aLEC)
  {
    final IHCElement <?> ret = BootstrapMenuItemRenderer.createSideBarMenu (aLEC);

    return new HCNodeList ().addChild (ret)
                            .addChild (new HCDiv ().addChild (new HCExtImg (CPDPublisher.IMG_LOGO_PEPPOL)));
  }

  @Nonnull
  public IHCNode getContent (@Nonnull final LayoutExecutionContext aLEC, @Nonnull final HCHead aHead)
  {
    final HCNodeList ret = new HCNodeList ();

    // Header
    ret.addChild (_getNavbar (aLEC));

    final BootstrapContainer aOuterContainer = ret.addAndReturnChild (new BootstrapContainer ().setFluid (true));

    // Breadcrumbs
    {
      final BootstrapBreadcrumbs aBreadcrumbs = BootstrapBreadcrumbsProvider.createBreadcrumbs (aLEC);
      aBreadcrumbs.addClass (CBootstrapCSS.HIDDEN_XS);
      aOuterContainer.addChild (aBreadcrumbs);
    }

    // Content
    {
      final BootstrapRow aRow = aOuterContainer.addAndReturnChild (new BootstrapRow ());
      final HCDiv aCol1 = aRow.createColumn (12, 4, 4, 3);
      final HCDiv aCol2 = aRow.createColumn (12, 8, 8, 9);

      // left
      // We need a wrapper span for easy AJAX content replacement
      aCol1.addChild (new HCSpan ().setID (CLayout.LAYOUT_AREAID_MENU).addChild (getMenuContent (aLEC)));
      aCol1.addChild (new HCDiv ().setID (CLayout.LAYOUT_AREAID_SPECIAL));

      // content - determine is exactly same as for view
      aCol2.addChild (AppRendererPublic.getPageContent (aLEC));
    }

    return ret;
  }
}
