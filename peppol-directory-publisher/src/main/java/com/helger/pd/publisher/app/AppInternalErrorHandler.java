/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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

import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.quartz.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.lru.LRUSet;
import com.helger.commons.email.EmailAddress;
import com.helger.commons.scope.mgr.ScopeManager;
import com.helger.photon.basic.app.dao.impl.AbstractDAO;
import com.helger.photon.basic.app.request.ApplicationRequestManager;
import com.helger.photon.basic.longrun.ILongRunningJob;
import com.helger.photon.core.ajax.servlet.AbstractAjaxServlet;
import com.helger.photon.core.app.error.InternalErrorBuilder;
import com.helger.photon.core.app.error.InternalErrorHandler;
import com.helger.photon.core.app.error.callback.AbstractErrorCallback;
import com.helger.photon.core.mgr.PhotonCoreManager;
import com.helger.photon.core.requesttrack.ILongRunningRequestCallback;
import com.helger.photon.core.requesttrack.IParallelRunningRequestCallback;
import com.helger.photon.core.requesttrack.RequestTracker;
import com.helger.photon.core.requesttrack.TrackedRequest;
import com.helger.photon.core.smtp.CNamedSMTPSettings;
import com.helger.photon.core.smtp.NamedSMTPSettings;
import com.helger.schedule.job.AbstractJob;
import com.helger.schedule.job.IJobExceptionCallback;
import com.helger.smtp.settings.ISMTPSettings;
import com.helger.web.scope.IRequestWebScope;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.web.scope.mgr.WebScopeManager;

public final class AppInternalErrorHandler extends AbstractErrorCallback implements IJobExceptionCallback, ILongRunningRequestCallback, IParallelRunningRequestCallback
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AppInternalErrorHandler.class);

  private final Set <String> m_aHandledLongRunning = new LRUSet <String> (1000);
  private boolean m_bParallelBelowLimit = true;

  @Nonnull
  private static Locale _getSafeDisplayLocale ()
  {
    try
    {
      // This may fail, if a weird application context is used
      return ApplicationRequestManager.getRequestMgr ().getRequestDisplayLocale ();
    }
    catch (final IllegalStateException ex)
    {
      // I just want to know, where and how this happens...
      final IRequestWebScope aRequestScope = WebScopeManager.getRequestScopeOrNull ();
      final String sAppID = aRequestScope == null ? "<no request scope present>"
                                                  : ScopeManager.getRequestApplicationID (aRequestScope);
      s_aLogger.warn ("Failed to retrieve default locale for application ID '" + sAppID + "'");
      return AppCommonUI.DEFAULT_LOCALE;
    }
  }

  @Override
  protected void onError (@Nonnull final Throwable t,
                          @Nullable final IRequestWebScopeWithoutResponse aRequestScope,
                          @Nonnull @Nonempty final String sErrorCode)
  {
    final Locale aDisplayLocale = _getSafeDisplayLocale ();
    new InternalErrorBuilder ().setThrowable (t)
                               .setRequestScope (aRequestScope)
                               .addCustomData ("ErrorCode", sErrorCode)
                               .setDisplayLocale (aDisplayLocale)
                               .handle ();
  }

  public void onScheduledJobException (@Nonnull final Throwable t,
                                       @Nullable final String sJobClassName,
                                       @Nonnull final Job aJob)
  {
    onError (t,
             null,
             "Error executing" +
                   (aJob instanceof ILongRunningJob ? " long running" : "") +
                   " job " +
                   sJobClassName +
                   ": " +
                   aJob);
  }

  public void onLongRunningRequest (@Nonnull final String sUniqueRequestID,
                                    @Nonnull final IRequestWebScope aRequestScope,
                                    final long nRunningMilliseconds)
  {
    if (m_aHandledLongRunning.add (sUniqueRequestID))
    {
      new InternalErrorBuilder ().addCustomData ("error message", "Long running request")
                                 .addCustomData ("request ID", sUniqueRequestID)
                                 .addCustomData ("request URL", aRequestScope.getURL ())
                                 .addCustomData ("running milli seconds", Long.toString (nRunningMilliseconds))
                                 .handle ();
    }
  }

  public void onParallelRunningRequests (final int nParallelRequests, @Nonnull final List <TrackedRequest> aRequests)
  {
    if (m_bParallelBelowLimit)
    {
      // Send mail only once per threshold exemption
      new InternalErrorBuilder ().addCustomData ("error message", "Too many parallel requests")
                                 .addCustomData ("Parallel requests", Integer.toString (nParallelRequests))
                                 .handle ();
      m_bParallelBelowLimit = false;
    }
  }

  public void onParallelRunningRequestsBelowLimit ()
  {
    m_bParallelBelowLimit = true;
  }

  public static void doSetup ()
  {
    // Set global internal error handlers
    final AppInternalErrorHandler aIntErrHdl = new AppInternalErrorHandler ();
    AbstractAjaxServlet.getExceptionCallbacks ().addCallback (aIntErrHdl);
    AbstractDAO.getExceptionHandlersRead ().addCallback (aIntErrHdl);
    AbstractDAO.getExceptionHandlersWrite ().addCallback (aIntErrHdl);
    AbstractJob.getExceptionCallbacks ().addCallback (aIntErrHdl);
    RequestTracker.getLongRunningRequestCallbacks ().addCallback (aIntErrHdl);
    RequestTracker.getParallelRunningRequestCallbacks ().addCallback (aIntErrHdl);

    final NamedSMTPSettings aNamedSettings = PhotonCoreManager.getSMTPSettingsMgr ()
                                                              .getSettings (CNamedSMTPSettings.NAMED_SMTP_SETTINGS_DEFAULT_ID);
    final ISMTPSettings aSMTPSettings = aNamedSettings == null ? null : aNamedSettings.getSMTPSettings ();
    InternalErrorHandler.setSMTPSenderAddress (new EmailAddress ("pyp@helger.com", AppCommonUI.getApplicationTitle ()));
    InternalErrorHandler.setSMTPReceiverAddress (new EmailAddress ("philip@helger.com", "Philip"));
    InternalErrorHandler.setSMTPSettings (aSMTPSettings);
  }
}
