/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
import com.helger.html.hc.html.IHCElement;
import com.helger.html.hc.html.embedded.HCImg;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCP;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.metadata.HCHead;
import com.helger.html.hc.html.root.HCHtml;
import com.helger.html.hc.html.sections.HCBody;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCSpan;
import com.helger.html.hc.html.textlevel.HCStrong;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.settings.PDServerConfiguration;
import com.helger.pd.publisher.CPDPublisher;
import com.helger.pd.publisher.app.AppCommonUI;
import com.helger.pd.publisher.app.AppSecurity;
import com.helger.pd.publisher.servlet.ExportDeliveryHttpHandler;
import com.helger.pd.publisher.servlet.ExportServlet;
import com.helger.photon.app.url.LinkHelper;
import com.helger.photon.bootstrap4.CBootstrapCSS;
import com.helger.photon.bootstrap4.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.dropdown.BootstrapDropdownMenu;
import com.helger.photon.bootstrap4.ext.BootstrapSystemMessage;
import com.helger.photon.bootstrap4.layout.BootstrapContainer;
import com.helger.photon.bootstrap4.navbar.BootstrapNavbar;
import com.helger.photon.bootstrap4.navbar.BootstrapNavbarNav;
import com.helger.photon.bootstrap4.navbar.BootstrapNavbarToggleable;
import com.helger.photon.bootstrap4.uictrls.ext.BootstrapMenuItemRenderer;
import com.helger.photon.bootstrap4.uictrls.ext.BootstrapMenuItemRendererHorz;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.appid.CApplicationID;
import com.helger.photon.core.appid.PhotonGlobalState;
import com.helger.photon.core.appid.RequestSettings;
import com.helger.photon.core.execcontext.ISimpleWebExecutionContext;
import com.helger.photon.core.execcontext.LayoutExecutionContext;
import com.helger.photon.core.html.AbstractSWECHTMLProvider;
import com.helger.photon.core.html.CLayout;
import com.helger.photon.core.menu.IMenuItemExternal;
import com.helger.photon.core.menu.IMenuItemPage;
import com.helger.photon.core.menu.IMenuObject;
import com.helger.photon.core.menu.IMenuSeparator;
import com.helger.photon.core.menu.IMenuTree;
import com.helger.photon.core.menu.MenuItemDeterminatorCallback;
import com.helger.photon.core.servlet.AbstractSecureApplicationServlet;
import com.helger.photon.core.servlet.LogoutServlet;
import com.helger.photon.security.login.LoggedInUserManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.util.SecurityHelper;
import com.helger.photon.uicore.html.HCCookieConsent;
import com.helger.photon.uicore.html.google.HCUniversalAnalytics;
import com.helger.photon.uicore.page.IWebPage;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.forcedredirect.ForcedRedirectException;
import com.helger.xservlet.forcedredirect.ForcedRedirectManager;

/**
 * Main class for creating HTML output
 *
 * @author Philip Helger
 */
public class PublicHTMLProvider extends AbstractSWECHTMLProvider
{
  private static final ICSSClassProvider CSS_CLASS_FOOTER_LINKS = DefaultCSSClassProvider.create ("footer-links");
  private static final String VENDOR_NAME = PDServerConfiguration.getVendorName ();
  private static final String VENDOR_URL = PDServerConfiguration.getVendorURL ();

  private static final ICommonsList <IMenuObject> s_aFooterObjects = new CommonsArrayList <> ();

  static
  {
    PhotonGlobalState.state (CApplicationID.APP_ID_PUBLIC).getMenuTree ().iterateAllMenuObjects (aCurrentObject -> {
      if (aCurrentObject.attrs ().containsKey (CMenuPublic.FLAG_FOOTER))
        s_aFooterObjects.add (aCurrentObject);
    });
  }

  private static void _addNavbarLoginLogout (@Nonnull final LayoutExecutionContext aLEC,
                                             @Nonnull final BootstrapNavbar aNavbar)
  {
    final IRequestWebScopeWithoutResponse aRequestScope = aLEC.getRequestScope ();
    final Locale aDisplayLocale = aLEC.getDisplayLocale ();

    // Documentation
    {
      final BootstrapNavbarNav aNav = aNavbar.addAndReturnNav ();
      final BootstrapDropdownMenu aDropDown = new BootstrapDropdownMenu ();
      aDropDown.createAndAddItem ()
               .addChild ("Introduction")
               .setHref (aLEC.getLinkToMenuItem (CMenuPublic.MENU_DOCS_INTRODUCTION));
      aDropDown.createAndAddItem ()
               .addChild ("How to use it")
               .setHref (aLEC.getLinkToMenuItem (CMenuPublic.MENU_DOCS_HOW_TO));
      aDropDown.createAndAddItem ()
               .addChild ("REST API")
               .setHref (aLEC.getLinkToMenuItem (CMenuPublic.MENU_DOCS_REST_API));
      aDropDown.createAndAddItem ()
               .addChild ("Export data")
               .setHref (aLEC.getLinkToMenuItem (CMenuPublic.MENU_DOCS_EXPORT_ALL));
      aDropDown.createAndAddItem ()
               .addChild ("Specification v1.1 (PDF)")
               .setHref (LinkHelper.getURLWithContext ("/files/PEPPOL-EDN-Directory-1.1-2018-07-17.pdf"));
      aDropDown.createAndAddItem ()
               .addChild ("Guide for SMP providers (PDF)")
               .setHref (LinkHelper.getURLWithContext ("/files/OpenPEPPOL Directory for SMP providers 2016-12-05.pdf"));
      aNav.addItem ().addNavDropDown ("Documentation", aDropDown);
    }

    // Support
    {
      final BootstrapNavbarNav aNav = aNavbar.addAndReturnNav ();
      final BootstrapDropdownMenu aDropDown = new BootstrapDropdownMenu ();
      aDropDown.createAndAddItem ()
               .addChild ("Contact us")
               .setHref (aLEC.getLinkToMenuItem (CMenuPublic.MENU_SUPPORT_CONTACT_US));
      aDropDown.createAndAddItem ()
               .addChild ("Compliant SMP implementations")
               .setHref (aLEC.getLinkToMenuItem (CMenuPublic.MENU_SUPPORT_SMP_IMPLEMENTATIONS));
      aDropDown.createAndAddItem ()
               .addChild ("Issue tracker (external)")
               .setHref (new SimpleURL ("https://github.com/phax/phoss-directory/issues"))
               .setTargetBlank ();
      aNav.addItem ().addNavDropDown ("Support", aDropDown);
    }

    // About
    {
      final BootstrapNavbarNav aNav = aNavbar.addAndReturnNav ();
      final BootstrapDropdownMenu aDropDown = new BootstrapDropdownMenu ();
      aDropDown.createAndAddItem ()
               .addChild ("About " + CPDPublisher.getApplication ())
               .setHref (aLEC.getLinkToMenuItem (CMenuPublic.MENU_ABOUT));
      aNav.addItem ().addNavDropDown ("About", aDropDown);
    }

    final BootstrapNavbarToggleable aToggleable = aNavbar.addAndReturnToggleable ();

    // Login etc
    {
      final IUser aUser = LoggedInUserManager.getInstance ().getCurrentUser ();
      if (aUser != null)
      {
        aToggleable.addAndReturnText ()
                   .addClass (CBootstrapCSS.ML_AUTO)
                   .addClass (CBootstrapCSS.MR_2)
                   .addChild ("Welcome ")
                   .addChild (new HCStrong ().addChild (SecurityHelper.getUserDisplayName (aUser, aDisplayLocale)));
        if (SecurityHelper.hasUserRole (aUser.getID (), AppSecurity.ROLE_CONFIG_ID))
        {
          aToggleable.addChild (new BootstrapButton ().addClass (CBootstrapCSS.MR_2)
                                                      .addChild ("Goto secure area")
                                                      .setOnClick (LinkHelper.getURLWithContext (AbstractSecureApplicationServlet.SERVLET_DEFAULT_PATH +
                                                                                                 "/")));
        }
        aToggleable.addChild (new BootstrapButton ().setOnClick (LinkHelper.getURLWithContext (aRequestScope,
                                                                                               LogoutServlet.SERVLET_DEFAULT_PATH))
                                                    .addChild (EPhotonCoreText.LOGIN_LOGOUT.getDisplayText (aDisplayLocale))
                                                    .addStyle (CCSSProperties.MARGIN_RIGHT.newValue ("8px")));
      }
      else
      {
        final BootstrapNavbarNav aNav = aToggleable.addAndReturnNav ();
        final BootstrapDropdownMenu aDropDown = new BootstrapDropdownMenu ();
        {
          final HCDiv aDiv = new HCDiv ().addClass (CBootstrapCSS.P_2)
                                         .addStyle (CCSSProperties.MIN_WIDTH.newValue ("400px"));
          aDiv.addChild (AppCommonUI.createViewLoginForm (aLEC, null));
          aDropDown.addChild (aDiv);
        }
        aNav.addItem ().addNavDropDown ("Login", aDropDown);
      }
    }
  }

  @Nonnull
  private static BootstrapNavbar _getNavbar (@Nonnull final LayoutExecutionContext aLEC)
  {
    final ISimpleURL aLinkToStartPage = aLEC.getLinkToMenuItem (aLEC.getMenuTree ().getDefaultMenuItemID ());

    final BootstrapNavbar aNavbar = new BootstrapNavbar ();

    final HCImg aImg = AppCommonUI.createLogoImg ();
    if (aImg != null)
      aNavbar.addBrand (aImg, aLinkToStartPage);

    aNavbar.addBrand (new HCSpan ().addClass (AppCommonUI.CSS_CLASS_LOGO1)
                                   .addChild (CPDPublisher.getApplicationTitle ()),
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
    final HCNodeList ret = new HCNodeList ();

    // First add the system message
    ret.addChild (BootstrapSystemMessage.createDefault ());

    // Handle 404 case here (see error404.jsp)
    if ("true".equals (aRequestScope.params ().getAsString ("httpError")))
    {
      final String sHttpStatusCode = aRequestScope.params ().getAsString ("httpStatusCode");
      final String sHttpStatusMessage = aRequestScope.params ().getAsString ("httpStatusMessage");
      final String sHttpRequestURI = aRequestScope.params ().getAsString ("httpRequestUri");
      ret.addChild (new BootstrapErrorBox ().addChild ("HTTP error " +
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
        ret.addChild ((IHCNode) ForcedRedirectManager.getLastForcedRedirectContent (aDisplayPage.getID ()));
    }

    // Add page header
    ret.addChild (aDisplayPage.getHeaderNode (aWPEC));

    // Main fill page content
    aDisplayPage.getContent (aWPEC);

    // Add page content to result
    ret.addChild (aWPEC.getNodeList ());
    return ret;
  }

  @Nonnull
  public static IHCNode getContent (@Nonnull final LayoutExecutionContext aLEC)
  {
    final Locale aDisplayLocale = aLEC.getDisplayLocale ();
    final IRequestWebScopeWithoutResponse aRequestScope = aLEC.getRequestScope ();
    final HCNodeList ret = new HCNodeList ();

    // Header
    ret.addChild (_getNavbar (aLEC));

    final BootstrapContainer aOuterContainer = ret.addAndReturnChild (new BootstrapContainer ().setFluid (false));

    // Content - no menu
    aOuterContainer.addChild (getPageContent (aLEC));

    // Footer
    {
      final BootstrapContainer aDiv = new BootstrapContainer ().setFluid (true).setID (CLayout.LAYOUT_AREAID_FOOTER);

      aDiv.addChild (new HCP ().addChild (CPDPublisher.getApplication () + " - an ")
                               .addChild (new HCA (new SimpleURL (VENDOR_URL)).addChild (VENDOR_NAME))
                               .addChild (" service"));
      aDiv.addChild (new HCP ().addChild ("Follow us on Twitter: ")
                               .addChild (new HCA (new SimpleURL ("https://twitter.com/PEPPOLDirectory")).addChild ("@PEPPOLDirectory")));
      final HCP aP = new HCP ().addChild ("Download data [");
      aP.addChild (new HCA (LinkHelper.getURLWithContext (aRequestScope,
                                                          ExportServlet.SERVLET_DEFAULT_PATH +
                                                                         ExportDeliveryHttpHandler.SPECIAL_BUSINESS_CARDS_XML_FULL)).addChild ("BusinessCards XML"));
      aP.addChild (" | ")
        .addChild (new HCA (LinkHelper.getURLWithContext (aRequestScope,
                                                          ExportServlet.SERVLET_DEFAULT_PATH +
                                                                         ExportDeliveryHttpHandler.SPECIAL_BUSINESS_CARDS_XML_NO_DOC_TYPES)).addChild ("BusinessCards w/o doctypes XML"));
      if (CPDPublisher.EXPORT_BUSINESS_CARDS_EXCEL)
      {
        aP.addChild (" | ")
          .addChild (new HCA (LinkHelper.getURLWithContext (aRequestScope,
                                                            ExportServlet.SERVLET_DEFAULT_PATH +
                                                                           ExportDeliveryHttpHandler.SPECIAL_BUSINESS_CARDS_EXCEL)).addChild ("BusinessCards Excel"));
      }
      if (CPDPublisher.EXPORT_BUSINESS_CARDS_CSV)
      {
        aP.addChild (" | ")
          .addChild (new HCA (LinkHelper.getURLWithContext (aRequestScope,
                                                            ExportServlet.SERVLET_DEFAULT_PATH +
                                                                           ExportDeliveryHttpHandler.SPECIAL_BUSINESS_CARDS_CSV)).addChild ("BusinessCards CSV"));
      }
      if (CPDPublisher.EXPORT_PARTICIPANTS_XML)
      {
        aP.addChild (" | ")
          .addChild (new HCA (LinkHelper.getURLWithContext (aRequestScope,
                                                            ExportServlet.SERVLET_DEFAULT_PATH +
                                                                           ExportDeliveryHttpHandler.SPECIAL_PARTICIPANTS_XML)).addChild ("Participant IDs XML"));
      }
      if (CPDPublisher.EXPORT_PARTICIPANTS_JSON)
      {
        aP.addChild (" | ")
          .addChild (new HCA (LinkHelper.getURLWithContext (aRequestScope,
                                                            ExportServlet.SERVLET_DEFAULT_PATH +
                                                                           ExportDeliveryHttpHandler.SPECIAL_PARTICIPANTS_JSON)).addChild ("Participant IDs JSON"));
      }
      if (CPDPublisher.EXPORT_PARTICIPANTS_CSV)
      {
        aP.addChild (" | ")
          .addChild (new HCA (LinkHelper.getURLWithContext (aRequestScope,
                                                            ExportServlet.SERVLET_DEFAULT_PATH +
                                                                           ExportDeliveryHttpHandler.SPECIAL_PARTICIPANTS_CSV)).addChild ("Participant IDs CSV"));
      }
      aP.addChild ("]");
      aDiv.addChild (aP);

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

  @Override
  protected void fillBody (@Nonnull final ISimpleWebExecutionContext aSWEC,
                           @Nonnull final HCHtml aHtml) throws ForcedRedirectException
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
