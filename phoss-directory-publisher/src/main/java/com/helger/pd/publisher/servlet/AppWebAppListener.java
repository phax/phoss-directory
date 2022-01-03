/*
 * Copyright (C) 2015-2022 Philip Helger (www.helger.com)
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

import com.helger.commons.ValueEnforcer;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.vendor.VendorInfo;
import com.helger.html.meta.MetaElement;
import com.helger.pd.indexer.clientcert.ClientCertificateValidator;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.settings.PDServerConfiguration;
import com.helger.pd.publisher.ajax.CAjax;
import com.helger.pd.publisher.app.AppCommonUI;
import com.helger.pd.publisher.app.AppInternalErrorHandler;
import com.helger.pd.publisher.app.AppSecurity;
import com.helger.pd.publisher.app.PDPMetaManager;
import com.helger.pd.publisher.app.pub.MenuPublic;
import com.helger.pd.publisher.app.secure.MenuSecure;
import com.helger.pd.publisher.exportall.ExportAllDataJob;
import com.helger.pd.publisher.updater.SyncAllBusinessCardsJob;
import com.helger.photon.ajax.IAjaxRegistry;
import com.helger.photon.app.html.PhotonMetaElements;
import com.helger.photon.bootstrap4.servlet.WebAppListenerBootstrap;
import com.helger.photon.core.appid.CApplicationID;
import com.helger.photon.core.appid.PhotonGlobalState;
import com.helger.photon.core.configfile.ConfigurationFile;
import com.helger.photon.core.configfile.ConfigurationFileManager;
import com.helger.photon.core.configfile.EConfigurationFileSyntax;
import com.helger.photon.core.locale.ILocaleManager;
import com.helger.photon.core.menu.MenuTree;
import com.helger.photon.core.requestparam.RequestParameterHandlerURLPathNamed;
import com.helger.photon.core.requestparam.RequestParameterManager;
import com.helger.quartz.SimpleScheduleBuilder;
import com.helger.quartz.TriggerKey;
import com.helger.schedule.quartz.GlobalQuartzScheduler;
import com.helger.schedule.quartz.listener.LoggingJobListener;
import com.helger.schedule.quartz.trigger.JDK8TriggerBuilder;
import com.helger.servlet.ServletContextPathHolder;
import com.helger.xservlet.requesttrack.RequestTrackerSettings;

/**
 * This listener is invoked during the servlet initialization. This is basically
 * a ServletContextListener.
 *
 * @author Philip Helger
 */
public final class AppWebAppListener extends WebAppListenerBootstrap
{
  private TriggerKey m_aExportJobTrigger;
  private TriggerKey m_aSyncJobTrigger;

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

    if (GlobalDebug.isProductionMode ())
      ValueEnforcer.setEnabled (false);

    // By disabling these settings, less audits are created hence less calls are
    // blocking
    RequestTrackerSettings.setLongRunningRequestsCheckEnabled (false);
    RequestTrackerSettings.setParallelRunningRequestsCheckEnabled (false);

    RequestParameterManager.getInstance ().setParameterHandler (new RequestParameterHandlerURLPathNamed ());
    AppInternalErrorHandler.doSetup ();

    final ConfigurationFileManager aCfgMgr = ConfigurationFileManager.getInstance ();
    aCfgMgr.registerConfigurationFile (new ConfigurationFile (new ClassPathResource ("log4j2.xml")).setDescription ("log4j configuration file")
                                                                                                   .setSyntaxHighlightLanguage (EConfigurationFileSyntax.XML));
    aCfgMgr.registerAll (PDServerConfiguration.getConfig ());

    // Job scheduling etc
    if (GlobalDebug.isDebugMode ())
      GlobalQuartzScheduler.getInstance ().addJobListener (new LoggingJobListener ());
  }

  @Override
  protected void initLocales (@Nonnull final ILocaleManager aLocaleMgr)
  {
    aLocaleMgr.registerLocale (AppCommonUI.DEFAULT_LOCALE);
    aLocaleMgr.setDefaultLocale (AppCommonUI.DEFAULT_LOCALE);
  }

  @Override
  protected void initAjax (final IAjaxRegistry aAjaxRegistry)
  {
    CAjax.initAjax (aAjaxRegistry);
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
    PhotonMetaElements.registerMetaElementForGlobal (MetaElement.createMeta ("theme-color", "#ffffff"));
  }

  @Override
  protected void initManagers ()
  {
    // Load managers
    PDMetaManager.getInstance ();
    PDPMetaManager.getInstance ();
    // Ensure this is loaded as well, to identify configuration errors as soon
    // as possible. Ignore the result. It's just about the static class
    // initialization
    ClientCertificateValidator.getAllRootCerts ();
  }

  @Override
  protected void initJobs ()
  {
    // In production: avoid creating too much load directly after startup
    final boolean bDebug = GlobalDebug.isDebugMode ();
    m_aExportJobTrigger = GlobalQuartzScheduler.getInstance ()
                                               .scheduleJob (ExportAllDataJob.class.getName (),
                                                             JDK8TriggerBuilder.newTrigger ()
                                                                               .startAt (bDebug ? PDTFactory.getCurrentLocalDateTime ()
                                                                                                : PDTFactory.getCurrentLocalDateTime ()
                                                                                                            .plusHours (1))
                                                                               .withSchedule (bDebug ? SimpleScheduleBuilder.repeatMinutelyForever (2)
                                                                                                     : SimpleScheduleBuilder.repeatHourlyForever (24)),
                                                             ExportAllDataJob.class,
                                                             null);

    if (PDServerConfiguration.isSyncAllBusinessCards ())
    {
      // Schedule the sync job every hour - it keeps track of the last sync
      // internally
      m_aSyncJobTrigger = GlobalQuartzScheduler.getInstance ()
                                               .scheduleJob (SyncAllBusinessCardsJob.class.getName (),
                                                             JDK8TriggerBuilder.newTrigger ()
                                                                               .startAt (PDTFactory.getCurrentLocalDateTime ()
                                                                                                   .plusMinutes (2))
                                                                               .withSchedule (SimpleScheduleBuilder.repeatHourlyForever (1)),
                                                             SyncAllBusinessCardsJob.class,
                                                             null);
    }
  }

  @Override
  protected void beforeContextDestroyed (final ServletContext aSC)
  {
    // Explicitly stop
    if (m_aExportJobTrigger != null)
      GlobalQuartzScheduler.getInstance ().unscheduleJob (m_aExportJobTrigger);
    if (m_aSyncJobTrigger != null)
      GlobalQuartzScheduler.getInstance ().unscheduleJob (m_aSyncJobTrigger);

    super.beforeContextDestroyed (aSC);
  }
}
