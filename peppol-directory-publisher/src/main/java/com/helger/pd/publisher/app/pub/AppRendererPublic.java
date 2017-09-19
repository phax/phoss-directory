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
package com.helger.pd.publisher.app.pub;

import java.util.Locale;

import javax.annotation.Nonnull;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.ISimpleURL;
import com.helger.commons.url.SimpleURL;
import com.helger.css.property.CCSSProperties;
import com.helger.html.css.DefaultCSSClassProvider;
import com.helger.html.css.ICSSClassProvider;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.HC_Target;
import com.helger.html.hc.html.IHCElement;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCP;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCSpan;
import com.helger.html.hc.html.textlevel.HCStrong;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.publisher.app.AppCommonUI;
import com.helger.pd.publisher.app.AppSecurity;
import com.helger.pd.settings.PDServerConfiguration;
import com.helger.photon.basic.app.appid.CApplicationID;
import com.helger.photon.basic.app.appid.PhotonGlobalState;
import com.helger.photon.basic.app.menu.IMenuItemExternal;
import com.helger.photon.basic.app.menu.IMenuItemPage;
import com.helger.photon.basic.app.menu.IMenuObject;
import com.helger.photon.basic.app.menu.IMenuSeparator;
import com.helger.photon.basic.app.menu.IMenuTree;
import com.helger.photon.basic.app.menu.MenuItemDeterminatorCallback;
import com.helger.photon.bootstrap3.CBootstrapCSS;
import com.helger.photon.bootstrap3.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap3.base.BootstrapContainer;
import com.helger.photon.bootstrap3.button.BootstrapButton;
import com.helger.photon.bootstrap3.dropdown.BootstrapDropdownMenu;
import com.helger.photon.bootstrap3.dropdown.BootstrapDropdownMenuItem;
import com.helger.photon.bootstrap3.dropdown.EBootstrapDropdownMenuAlignment;
import com.helger.photon.bootstrap3.ext.BootstrapSystemMessage;
import com.helger.photon.bootstrap3.nav.BootstrapNav;
import com.helger.photon.bootstrap3.navbar.BootstrapNavbar;
import com.helger.photon.bootstrap3.navbar.EBootstrapNavbarPosition;
import com.helger.photon.bootstrap3.navbar.EBootstrapNavbarType;
import com.helger.photon.bootstrap3.uictrls.ext.BootstrapMenuItemRenderer;
import com.helger.photon.bootstrap3.uictrls.ext.BootstrapMenuItemRendererHorz;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.app.context.LayoutExecutionContext;
import com.helger.photon.core.app.layout.CLayout;
import com.helger.photon.core.servlet.AbstractSecureApplicationServlet;
import com.helger.photon.core.servlet.LogoutServlet;
import com.helger.photon.core.url.LinkHelper;
import com.helger.photon.security.login.LoggedInUserManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.util.SecurityHelper;
import com.helger.photon.uicore.html.HCCookieConsent;
import com.helger.photon.uicore.html.google.HCUniversalAnalytics;
import com.helger.photon.uicore.page.IWebPage;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.forcedredirect.ForcedRedirectManager;

/**
 * The viewport renderer (menu + content area)
 *
 * @author Philip Helger
 */
public final class AppRendererPublic
{
  private static final ICSSClassProvider CSS_CLASS_FOOTER_LINKS = DefaultCSSClassProvider.create ("footer-links");

  private static final ICommonsList <IMenuObject> s_aFooterObjects = new CommonsArrayList <> ();

  static
  {
    PhotonGlobalState.getInstance ()
                     .state (CApplicationID.APP_ID_PUBLIC)
                     .getMenuTree ()
                     .iterateAllMenuObjects (aCurrentObject -> {
                       if (aCurrentObject.attrs ().containsKey (CMenuPublic.FLAG_FOOTER))
                         s_aFooterObjects.add (aCurrentObject);
                     });
  }

  private AppRendererPublic ()
  {}

  private static void _addNavbarLoginLogout (@Nonnull final LayoutExecutionContext aLEC,
                                             @Nonnull final BootstrapNavbar aNavbar)
  {
    final IRequestWebScopeWithoutResponse aRequestScope = aLEC.getRequestScope ();
    final Locale aDisplayLocale = aLEC.getDisplayLocale ();

    // Documentation
    {
      final BootstrapNav aNav = new BootstrapNav ();
      final BootstrapDropdownMenu aDropDown = aNav.addDropdownMenu ("Documentation");
      aDropDown.addMenuItem (new BootstrapDropdownMenuItem ().setLabel ("Introduction")
                                                             .setLinkAction (aLEC.getLinkToMenuItem (CMenuPublic.MENU_DOCS_INTRODUCTION)));
      aDropDown.addMenuItem (new BootstrapDropdownMenuItem ().setLabel ("How to use it")
                                                             .setLinkAction (aLEC.getLinkToMenuItem (CMenuPublic.MENU_DOCS_HOW_TO)));
      aDropDown.addMenuItem (new BootstrapDropdownMenuItem ().setLabel ("REST API")
                                                             .setLinkAction (aLEC.getLinkToMenuItem (CMenuPublic.MENU_DOCS_REST_API)));
      aDropDown.addMenuItem (new BootstrapDropdownMenuItem ().setLabel ("Specification draft (PDF)")
                                                             .setLinkAction (LinkHelper.getURLWithContext ("/files/OpenPEPPOL Directory 2016-12-05.pdf")));
      aDropDown.addMenuItem (new BootstrapDropdownMenuItem ().setLabel ("Guide for SMP providers (PDF)")
                                                             .setLinkAction (LinkHelper.getURLWithContext ("/files/OpenPEPPOL Directory for SMP providers 2016-12-05.pdf")));
      aNavbar.addNav (EBootstrapNavbarPosition.COLLAPSIBLE_LEFT, aNav);
    }

    // Support
    {
      final BootstrapNav aNav = new BootstrapNav ();
      final BootstrapDropdownMenu aDropDown = aNav.addDropdownMenu ("Support");
      aDropDown.addMenuItem (new BootstrapDropdownMenuItem ().setLabel ("Contact us")
                                                             .setLinkAction (aLEC.getLinkToMenuItem (CMenuPublic.MENU_SUPPORT_CONTACT_US)));
      aDropDown.addMenuItem (new BootstrapDropdownMenuItem ().setLabel ("Compliant SMP implementations")
                                                             .setLinkAction (aLEC.getLinkToMenuItem (CMenuPublic.MENU_SUPPORT_SMP_IMPLEMENTATIONS)));
      aDropDown.addMenuItem (new BootstrapDropdownMenuItem ().setLabel ("Issue tracker (external)")
                                                             .setLinkAction (new SimpleURL ("https://github.com/phax/peppol-directory/issues"))
                                                             .setTarget (HC_Target.BLANK));
      aNavbar.addNav (EBootstrapNavbarPosition.COLLAPSIBLE_LEFT, aNav);
    }

    // About
    {
      final BootstrapNav aNav = new BootstrapNav ();
      final BootstrapDropdownMenu aDropDown = aNav.addDropdownMenu ("About");
      aDropDown.addMenuItem (new BootstrapDropdownMenuItem ().setLabel ("About PEPPOL Directory")
                                                             .setLinkAction (aLEC.getLinkToMenuItem (CMenuPublic.MENU_ABOUT)));
      aNavbar.addNav (EBootstrapNavbarPosition.COLLAPSIBLE_LEFT, aNav);
    }

    // Login etc
    {
      final BootstrapNav aNav = new BootstrapNav ();
      final IUser aUser = LoggedInUserManager.getInstance ().getCurrentUser ();
      if (aUser != null)
      {
        if (SecurityHelper.hasUserRole (aUser.getID (), AppSecurity.ROLE_CONFIG_ID))
        {
          aNav.addButton (new BootstrapButton ().addChild ("Goto secure area")
                                                .setOnClick (LinkHelper.getURLWithContext (AbstractSecureApplicationServlet.SERVLET_DEFAULT_PATH +
                                                                                           "/")));
        }

        aNav.addText (new HCSpan ().addChild ("Welcome ")
                                   .addChild (new HCStrong ().addChild (SecurityHelper.getUserDisplayName (aUser,
                                                                                                           aDisplayLocale))));
        aNav.addButton (new BootstrapButton ().setOnClick (LinkHelper.getURLWithContext (aRequestScope,
                                                                                         LogoutServlet.SERVLET_DEFAULT_PATH))
                                              .addChild (EPhotonCoreText.LOGIN_LOGOUT.getDisplayText (aDisplayLocale)));
      }
      else
      {
        final BootstrapDropdownMenu aDropDown = aNav.addDropdownMenu (EBootstrapDropdownMenuAlignment.RIGHT, "Login");
        {
          // 300px would lead to a messy layout - so 250px is fine
          final HCDiv aDiv = new HCDiv ().addStyle (CCSSProperties.PADDING.newValue ("10px"))
                                         .addStyle (CCSSProperties.WIDTH.newValue ("250px"));
          aDiv.addChild (AppCommonUI.createViewLoginForm (aLEC, null, false).addClass (CBootstrapCSS.NAVBAR_FORM));
          aDropDown.addItem (aDiv);
        }
      }
      aNavbar.addNav (EBootstrapNavbarPosition.COLLAPSIBLE_RIGHT, aNav);
    }
  }

  @Nonnull
  private static BootstrapNavbar _getNavbar (@Nonnull final LayoutExecutionContext aLEC)
  {
    final Locale aDisplayLocale = aLEC.getDisplayLocale ();
    final ISimpleURL aLinkToStartPage = aLEC.getLinkToMenuItem (aLEC.getMenuTree ().getDefaultMenuItemID ());

    final BootstrapNavbar aNavbar = new BootstrapNavbar (EBootstrapNavbarType.STATIC_TOP, true, aDisplayLocale);
    aNavbar.getContainer ().setFluid (true);
    aNavbar.addBrand (new HCSpan ().addClass (AppCommonUI.CSS_CLASS_LOGO1)
                                   .addChild (AppCommonUI.getApplicationTitle ()),
                      aLinkToStartPage);

    _addNavbarLoginLogout (aLEC, aNavbar);
    return aNavbar;
  }

  @Nonnull
  public static IHCNode getMenuContent (@Nonnull final LayoutExecutionContext aLEC)
  {
    // Main menu
    final IMenuTree aMenuTree = aLEC.getMenuTree ();
    final MenuItemDeterminatorCallback aCallback = new MenuItemDeterminatorCallback (aMenuTree,
                                                                                     aLEC.getSelectedMenuItemID ())
    {
      @Override
      protected boolean isMenuItemValidToBeDisplayed (@Nonnull final IMenuObject aMenuObj)
      {
        // Don't show items that belong to the footer
        if (aMenuObj.attrs ().containsKey (CMenuPublic.FLAG_FOOTER))
          return false;

        // Use default code
        return super.isMenuItemValidToBeDisplayed (aMenuObj);
      }
    };
    final IHCElement <?> aMenu = BootstrapMenuItemRenderer.createSideBarMenu (aLEC, aCallback);
    return aMenu;
  }

  @SuppressWarnings ("unchecked")
  @Nonnull
  public static IHCNode getPageContent (@Nonnull final LayoutExecutionContext aLEC)
  {
    final IRequestWebScopeWithoutResponse aRequestScope = aLEC.getRequestScope ();

    // Get the requested menu item
    final IMenuItemPage aSelectedMenuItem = aLEC.getSelectedMenuItem ();

    // Resolve the page of the selected menu item (if found)
    IWebPage <WebPageExecutionContext> aDisplayPage;
    if (aSelectedMenuItem.matchesDisplayFilter ())
    {
      // Only if we have display rights!
      aDisplayPage = (IWebPage <WebPageExecutionContext>) aSelectedMenuItem.getPage ();
    }
    else
    {
      // No rights -> goto start page
      aDisplayPage = (IWebPage <WebPageExecutionContext>) aLEC.getMenuTree ().getDefaultMenuItem ().getPage ();
    }

    final WebPageExecutionContext aWPEC = new WebPageExecutionContext (aLEC, aDisplayPage);

    // Build page content: header + content
    final HCNodeList aPageContainer = new HCNodeList ();

    // First add the system message
    aPageContainer.addChild (BootstrapSystemMessage.createDefault ());

    // Handle 404 case here (see error404.jsp)
    if ("true".equals (aRequestScope.params ().getAsString ("httpError")))
    {
      final String sHttpStatusCode = aRequestScope.params ().getAsString ("httpStatusCode");
      final String sHttpStatusMessage = aRequestScope.params ().getAsString ("httpStatusMessage");
      final String sHttpRequestURI = aRequestScope.params ().getAsString ("httpRequestUri");
      aPageContainer.addChild (new BootstrapErrorBox ().addChild ("HTTP error " +
                                                                  sHttpStatusCode +
                                                                  " (" +
                                                                  sHttpStatusMessage +
                                                                  ")" +
                                                                  (StringHelper.hasText (sHttpRequestURI) ? " for request URI " +
                                                                                                            sHttpRequestURI
                                                                                                          : "")));
    }
    else
    {
      // Add the forced redirect content here
      if (aWPEC.params ().containsKey (ForcedRedirectManager.REQUEST_PARAMETER_PRG_ACTIVE))
        aPageContainer.addChild ((IHCNode) ForcedRedirectManager.getLastForcedRedirectContent (aDisplayPage.getID ()));
    }

    // Add page header
    aPageContainer.addChild (aDisplayPage.getHeaderNode (aWPEC));

    // Main fill page content
    aDisplayPage.getContent (aWPEC);

    // Add page content to result
    aPageContainer.addChild (aWPEC.getNodeList ());
    return aPageContainer;
  }

  @Nonnull
  public static IHCNode getContent (@Nonnull final LayoutExecutionContext aLEC)
  {
    final Locale aDisplayLocale = aLEC.getDisplayLocale ();
    final HCNodeList ret = new HCNodeList ();

    // Header
    ret.addChild (_getNavbar (aLEC));

    final BootstrapContainer aOuterContainer = ret.addAndReturnChild (new BootstrapContainer ().setFluid (false));

    // Content - no menu
    aOuterContainer.addChild (getPageContent (aLEC));

    // Footer
    {
      final BootstrapContainer aDiv = new BootstrapContainer ().setFluid (true).setID (CLayout.LAYOUT_AREAID_FOOTER);

      aDiv.addChild (new HCP ().addChild ("PEPPOL Directory - an ")
                               .addChild (new HCA (new SimpleURL ("http://peppol.eu")).addChild ("OpenPEPPOL AISBL"))
                               .addChild (" service"));
      aDiv.addChild (new HCP ().addChild ("Follow us on Twitter: ")
                               .addChild (new HCA (new SimpleURL ("https://twitter.com/PEPPOLDirectory")).addChild ("@PEPPOLDirectory")));

      final BootstrapMenuItemRendererHorz aRenderer = new BootstrapMenuItemRendererHorz (aDisplayLocale);
      final HCUL aUL = aDiv.addAndReturnChild (new HCUL ().addClass (CSS_CLASS_FOOTER_LINKS));
      for (final IMenuObject aMenuObj : s_aFooterObjects)
      {
        if (aMenuObj instanceof IMenuSeparator)
          aUL.addItem (aRenderer.renderSeparator (aLEC, (IMenuSeparator) aMenuObj));
        else
          if (aMenuObj instanceof IMenuItemPage)
            aUL.addItem (aRenderer.renderMenuItemPage (aLEC, (IMenuItemPage) aMenuObj, false, false, false));
          else
            if (aMenuObj instanceof IMenuItemExternal)
              aUL.addItem (aRenderer.renderMenuItemExternal (aLEC, (IMenuItemExternal) aMenuObj, false, false, false));
            else
              throw new IllegalStateException ("Unsupported menu object type!");
      }
      ret.addChild (aDiv);
    }

    if (GlobalDebug.isProductionMode ())
    {
      final String sAccountID = PDServerConfiguration.isTestVersion () ? "UA-55419519-3" : "UA-55419519-2";
      ret.addChild (new HCUniversalAnalytics (sAccountID, false, false, false, false));
    }

    ret.addChild (HCCookieConsent.createBottomDefault ("#000", "#0f0", "#0f0", null));

    return ret;
  }
}
