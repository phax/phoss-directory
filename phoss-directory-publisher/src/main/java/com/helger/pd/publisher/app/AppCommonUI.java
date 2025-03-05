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
package com.helger.pd.publisher.app;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.EHttpMethod;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.locale.LocaleCache;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.SimpleURL;
import com.helger.css.property.CCSSProperties;
import com.helger.css.propertyvalue.CCSSValue;
import com.helger.html.css.DefaultCSSClassProvider;
import com.helger.html.css.ICSSClassProvider;
import com.helger.html.hc.html.embedded.HCImg;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCEditPassword;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.metadata.EHCLinkType;
import com.helger.html.hc.html.metadata.HCHead;
import com.helger.html.hc.html.metadata.HCLink;
import com.helger.html.jquery.JQuery;
import com.helger.html.jquery.JQueryAjaxBuilder;
import com.helger.html.jscode.JSAnonymousFunction;
import com.helger.html.jscode.JSAssocArray;
import com.helger.html.jscode.JSPackage;
import com.helger.html.jscode.JSParam;
import com.helger.html.jscode.html.JSHtml;
import com.helger.pd.indexer.settings.PDServerConfiguration;
import com.helger.pd.publisher.CPDPublisher;
import com.helger.pd.publisher.ajax.AjaxExecutorPublicLogin;
import com.helger.pd.publisher.ajax.CAjax;
import com.helger.photon.app.url.LinkHelper;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.ext.BootstrapSystemMessage;
import com.helger.photon.bootstrap4.form.BootstrapForm;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.form.EBootstrapFormType;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.execcontext.LayoutExecutionContext;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.login.CLogin;
import com.helger.photon.uictrls.datatables.DataTablesLengthMenu;
import com.helger.photon.uictrls.datatables.EDataTablesFilterType;
import com.helger.photon.uictrls.datatables.ajax.AjaxExecutorDataTables;
import com.helger.photon.uictrls.datatables.ajax.AjaxExecutorDataTablesI18N;
import com.helger.photon.uictrls.datatables.plugins.DataTablesPluginSearchHighlight;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

@Immutable
public final class AppCommonUI
{
  // Logo parts
  public static final ICSSClassProvider CSS_CLASS_LOGO1 = DefaultCSSClassProvider.create ("logo1");
  public static final ICSSClassProvider CSS_CLASS_LOGO2 = DefaultCSSClassProvider.create ("logo2");

  public static final Locale DEFAULT_LOCALE = LocaleCache.getInstance ().getLocale ("en", "US");

  private static final DataTablesLengthMenu LENGTH_MENU = new DataTablesLengthMenu ().addItem (25)
                                                                                     .addItem (50)
                                                                                     .addItem (100)
                                                                                     .addItemAll ();

  private static final ICommonsList <HCLink> DEFAULT_FAV_ICONS = new CommonsArrayList <> ();

  static
  {
    final String sFavIcon16x16 = PDServerConfiguration.getConfig ().getAsString ("webapp.favicon.png.16x16");
    if (StringHelper.hasText (sFavIcon16x16))
      DEFAULT_FAV_ICONS.add (new HCLink ().setRel (EHCLinkType.ICON)
                                          .setType (CMimeType.IMAGE_PNG)
                                          .setSizes ("16x16")
                                          .setHref (new SimpleURL (sFavIcon16x16)));

    final String sFavIcon32x32 = PDServerConfiguration.getConfig ().getAsString ("webapp.favicon.png.32x32");
    if (StringHelper.hasText (sFavIcon32x32))
      DEFAULT_FAV_ICONS.add (new HCLink ().setRel (EHCLinkType.ICON)
                                          .setType (CMimeType.IMAGE_PNG)
                                          .setSizes ("32x32")
                                          .setHref (new SimpleURL (sFavIcon32x32)));

    final String sFavIcon96x96 = PDServerConfiguration.getConfig ().getAsString ("webapp.favicon.png.96x96");
    if (StringHelper.hasText (sFavIcon96x96))
      DEFAULT_FAV_ICONS.add (new HCLink ().setRel (EHCLinkType.ICON)
                                          .setType (CMimeType.IMAGE_PNG)
                                          .setSizes ("96x96")
                                          .setHref (new SimpleURL (sFavIcon96x96)));
  }

  private AppCommonUI ()
  {}

  public static void init ()
  {
    BootstrapDataTables.setConfigurator ( (aLEC, aTable, aDataTables) -> {
      final IRequestWebScopeWithoutResponse aRequestScope = aLEC.getRequestScope ();
      aDataTables.setAutoWidth (false)
                 .setLengthMenu (LENGTH_MENU)
                 .setAjaxBuilder (new JQueryAjaxBuilder ().url (CAjax.DATATABLES.getInvocationURL (aRequestScope))
                                                          .data (new JSAssocArray ().add (AjaxExecutorDataTables.OBJECT_ID,
                                                                                          aTable.getID ())))
                 .setServerFilterType (EDataTablesFilterType.ALL_TERMS_PER_ROW)
                 .setTextLoadingURL (CAjax.DATATABLES_I18N.getInvocationURL (aRequestScope),
                                     AjaxExecutorDataTablesI18N.REQUEST_PARAM_LANGUAGE_ID)
                 .addPlugin (new DataTablesPluginSearchHighlight ());
    });

    // By default allow markdown in system message
    BootstrapSystemMessage.setDefaultUseMarkdown (true);

    // Change logo image?
    final String sLogoImageURL = PDServerConfiguration.getLogoImageURL ();
    if (StringHelper.hasText (sLogoImageURL))
      CPDPublisher.setLogoImageURL (sLogoImageURL);
  }

  public static void addFavIcons (@Nonnull final HCHead aHead)
  {
    aHead.links ().addAll (DEFAULT_FAV_ICONS);
  }

  @Nonnull
  public static BootstrapForm createViewLoginForm (@Nonnull final LayoutExecutionContext aLEC,
                                                   @Nullable final String sPreselectedUserName)
  {
    final Locale aDisplayLocale = aLEC.getDisplayLocale ();
    final IRequestWebScopeWithoutResponse aRequestScope = aLEC.getRequestScope ();

    // Use new IDs for both fields, in case the login stuff is displayed more
    // than once!
    final String sIDUserName = GlobalIDFactory.getNewStringID ();
    final String sIDPassword = GlobalIDFactory.getNewStringID ();
    final String sIDErrorField = GlobalIDFactory.getNewStringID ();

    final BootstrapForm aForm = new BootstrapForm (aLEC).setAction (aLEC.getSelfHref ())
                                                        .setFormType (EBootstrapFormType.DEFAULT);
    aForm.setLeft (3);

    // User name field
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel (EPhotonCoreText.EMAIL_ADDRESS.getDisplayText (aDisplayLocale))
                                                 .setCtrl (new HCEdit (new RequestField (CLogin.REQUEST_ATTR_USERID,
                                                                                         sPreselectedUserName)).setID (sIDUserName)));

    // Password field
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel (EPhotonCoreText.LOGIN_FIELD_PASSWORD.getDisplayText (aDisplayLocale))
                                                 .setCtrl (new HCEditPassword (CLogin.REQUEST_ATTR_PASSWORD).setID (sIDPassword)));

    // Placeholder for error message
    aForm.addChild (new HCDiv ().setID (sIDErrorField).addStyle (CCSSProperties.MARGIN.newValue ("4px 0")));

    // Login button
    final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (new BootstrapButtonToolbar (aLEC));
    final JSPackage aOnClick = new JSPackage ();
    {
      final JSAnonymousFunction aJSSuccess = new JSAnonymousFunction ();
      final JSParam aJSData = aJSSuccess.param ("data");
      aJSSuccess.body ()
                ._if (aJSData.ref (AjaxExecutorPublicLogin.JSON_LOGGEDIN),
                      JSHtml.windowLocationReload (),
                      JQuery.idRef (sIDErrorField).empty ().append (aJSData.ref (AjaxExecutorPublicLogin.JSON_HTML)));

      aOnClick.add (new JQueryAjaxBuilder ().url (CAjax.LOGIN.getInvocationURI (aRequestScope))
                                            .method (EHttpMethod.POST)
                                            .data (new JSAssocArray ().add (CLogin.REQUEST_ATTR_USERID,
                                                                            JQuery.idRef (sIDUserName).val ())
                                                                      .add (CLogin.REQUEST_ATTR_PASSWORD,
                                                                            JQuery.idRef (sIDPassword).val ()))
                                            .success (aJSSuccess)
                                            .build ());
    }
    aOnClick._return (false);
    aToolbar.addSubmitButton (EPhotonCoreText.LOGIN_BUTTON_SUBMIT.getDisplayText (aDisplayLocale), aOnClick);
    return aForm;
  }

  @Nullable
  public static HCImg createLogoImg ()
  {
    final String sSrc = PDServerConfiguration.getAppLogoImagePath ();
    if (StringHelper.hasNoText (sSrc))
      return null;
    return new HCImg ().setSrc (LinkHelper.getURLWithContext (sSrc))
                       .addStyle (CCSSProperties.MARGIN.newValue ("-15px"))
                       .addStyle (CCSSProperties.VERTICAL_ALIGN.newValue (CCSSValue.TOP))
                       .addStyle (CCSSProperties.PADDING.newValue ("0 15px 0 6px"));
  }
}
