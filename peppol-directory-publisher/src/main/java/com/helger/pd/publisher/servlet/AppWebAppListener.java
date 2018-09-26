/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.servlet;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;

import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.vendor.VendorInfo;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.publisher.CPDPublisher;
import com.helger.pd.publisher.ajax.CAjax;
import com.helger.pd.publisher.app.AppCommonUI;
import com.helger.pd.publisher.app.AppInternalErrorHandler;
import com.helger.pd.publisher.app.AppSecurity;
import com.helger.pd.publisher.app.PDPMetaManager;
import com.helger.pd.publisher.app.pub.MenuPublic;
import com.helger.pd.publisher.app.secure.MenuSecure;
import com.helger.pd.publisher.exportall.ExportAllBusinessCardsJob;
import com.helger.pd.settings.PDServerConfiguration;
import com.helger.photon.basic.app.appid.CApplicationID;
import com.helger.photon.basic.app.appid.PhotonGlobalState;
import com.helger.photon.basic.app.locale.ILocaleManager;
import com.helger.photon.basic.app.menu.MenuTree;
import com.helger.photon.basic.app.request.RequestParameterHandlerURLPathNamed;
import com.helger.photon.basic.app.request.RequestParameterManager;
import com.helger.photon.basic.configfile.ConfigurationFile;
import com.helger.photon.basic.configfile.ConfigurationFileManager;
import com.helger.photon.basic.configfile.EConfigurationFileSyntax;
import com.helger.photon.bootstrap3.servlet.WebAppListenerBootstrap;
import com.helger.photon.core.ajax.IAjaxInvoker;
import com.helger.quartz.SimpleScheduleBuilder;
import com.helger.schedule.quartz.GlobalQuartzScheduler;
import com.helger.schedule.quartz.listener.LoggingJobListener;
import com.helger.schedule.quartz.trigger.JDK8TriggerBuilder;
import com.helger.servlet.ServletContextPathHolder;

/**
 * This listener is invoked during the servlet initialization. This is basically
 * a ServletContextListener.
 *
 * @author Philip Helger
 */
public final class AppWebAppListener extends WebAppListenerBootstrap
{
  @Override
  protected String getInitParameterDebug (@Nonnull final ServletContext aSC)
  {
    return PDServerConfiguration.getGlobalDebug ();
  }

  @Override
  protected String getInitParameterProduction (@Nonnull final ServletContext aSC)
  {
    return PDServerConfiguration.getGlobalProduction ();
  }

  @Override
  protected String getDataPath (@Nonnull final ServletContext aSC)
  {
    return PDServerConfiguration.getDataPath ();
  }

  @Override
  protected boolean shouldCheckFileAccess (@Nonnull final ServletContext aSC)
  {
    return PDServerConfiguration.isCheckFileAccess ();
  }

  @Override
  protected void initGlobalSettings ()
  {
    // Internal stuff:
    VendorInfo.setVendorName ("Philip Helger");
    VendorInfo.setVendorURL ("http://www.helger.com");
    VendorInfo.setVendorEmail ("pd@helger.com");
    VendorInfo.setVendorLocation ("Vienna, Austria");
    VendorInfo.setInceptionYear (2015);

    if (PDServerConfiguration.isForceRoot ())
    {
      // Enforce an empty context path according to the specs!
      ServletContextPathHolder.setCustomContextPath ("");
    }

    RequestParameterManager.getInstance ().setParameterHandler (new RequestParameterHandlerURLPathNamed ());
    AppInternalErrorHandler.doSetup ();

    final ConfigurationFileManager aCfgMgr = ConfigurationFileManager.getInstance ();
    aCfgMgr.registerConfigurationFile (new ConfigurationFile (new ClassPathResource ("log4j2.xml")).setDescription ("log4j configuration file")
                                                                                                   .setSyntaxHighlightLanguage (EConfigurationFileSyntax.XML));
    if (PDServerConfiguration.getConfigFile ().isRead ())
    {
      aCfgMgr.registerConfigurationFile (new ConfigurationFile (PDServerConfiguration.getConfigFile ()
                                                                                     .getReadResource ()).setDescription (CPDPublisher.getApplication () +
                                                                                                                          " properties")
                                                                                                         .setSyntaxHighlightLanguage (EConfigurationFileSyntax.PROPERTIES));
    }

    // Job scheduling etc
    if (GlobalDebug.isDebugMode ())
      GlobalQuartzScheduler.getInstance ().addJobListener (new LoggingJobListener ());
  }

  @Override
  public void initLocales (@Nonnull final ILocaleManager aLocaleMgr)
  {
    aLocaleMgr.registerLocale (AppCommonUI.DEFAULT_LOCALE);
    aLocaleMgr.setDefaultLocale (AppCommonUI.DEFAULT_LOCALE);
  }

  @Override
  public void initAjax (@Nonnull final IAjaxInvoker aAjaxInvoker)
  {
    CAjax.initAjax (aAjaxInvoker);
  }

  @Override
  protected void initMenu ()
  {
    // Create all menu items
    {
      final MenuTree aMenuTree = new MenuTree ();
      MenuPublic.init (aMenuTree);
      PhotonGlobalState.state (CApplicationID.APP_ID_PUBLIC).setMenuTree (aMenuTree);
    }
    {
      final MenuTree aMenuTree = new MenuTree ();
      MenuSecure.init (aMenuTree);
      PhotonGlobalState.state (CApplicationID.APP_ID_SECURE).setMenuTree (aMenuTree);
    }
  }

  @Override
  protected void initSecurity ()
  {
    // Set all security related stuff
    AppSecurity.init ();
  }

  @Override
  protected void initUI ()
  {
    // UI stuff
    AppCommonUI.init ();
  }

  @Override
  protected void initManagers ()
  {
    // Load managers
    PDMetaManager.getInstance ();
    PDPMetaManager.getInstance ();

    GlobalQuartzScheduler.getInstance ()
                         .scheduleJob (ExportAllBusinessCardsJob.class.getName (),
                                       JDK8TriggerBuilder.newTrigger ()
                                                         .startNow ()
                                                         .withSchedule (GlobalDebug.isDebugMode () ? SimpleScheduleBuilder.repeatMinutelyForever (2)
                                                                                                   : SimpleScheduleBuilder.repeatHourlyForever (6)),
                                       ExportAllBusinessCardsJob.class,
                                       null);
  }
}
