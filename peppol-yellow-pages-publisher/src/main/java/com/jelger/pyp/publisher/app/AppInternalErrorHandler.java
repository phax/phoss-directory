/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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
package com.jelger.pyp.publisher.app;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.quartz.Job;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.email.EmailAddress;
import com.helger.photon.basic.app.dao.impl.AbstractDAO;
import com.helger.photon.basic.app.request.ApplicationRequestManager;
import com.helger.photon.basic.longrun.ILongRunningJob;
import com.helger.photon.core.action.servlet.AbstractActionServlet;
import com.helger.photon.core.ajax.servlet.AbstractAjaxServlet;
import com.helger.photon.core.app.error.InternalErrorBuilder;
import com.helger.photon.core.app.error.InternalErrorHandler;
import com.helger.photon.core.app.error.callback.AbstractErrorCallback;
import com.helger.photon.core.mgr.PhotonCoreManager;
import com.helger.photon.core.smtp.CNamedSMTPSettings;
import com.helger.photon.core.smtp.NamedSMTPSettings;
import com.helger.schedule.job.AbstractJob;
import com.helger.schedule.job.IJobExceptionCallback;
import com.helger.smtp.settings.ISMTPSettings;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

public final class AppInternalErrorHandler extends AbstractErrorCallback implements IJobExceptionCallback
{
  @Override
  protected void onError (@Nonnull final Throwable t,
                          @Nullable final IRequestWebScopeWithoutResponse aRequestScope,
                          @Nonnull @Nonempty final String sErrorCode)
  {
    final Locale aDisplayLocale = ApplicationRequestManager.getRequestMgr ().getRequestDisplayLocale ();
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

  public static void doSetup ()
  {
    // Set global internal error handlers
    final AppInternalErrorHandler aIntErrHdl = new AppInternalErrorHandler ();
    AbstractAjaxServlet.getExceptionCallbacks ().addCallback (aIntErrHdl);
    AbstractActionServlet.getExceptionCallbacks ().addCallback (aIntErrHdl);
    AbstractDAO.getExceptionHandlersRead ().addCallback (aIntErrHdl);
    AbstractDAO.getExceptionHandlersWrite ().addCallback (aIntErrHdl);
    AbstractJob.getExceptionCallbacks ().addCallback (aIntErrHdl);

    final NamedSMTPSettings aNamedSettings = PhotonCoreManager.getSMTPSettingsMgr ()
                                                              .getSettings (CNamedSMTPSettings.NAMED_SMTP_SETTINGS_DEFAULT_ID);
    final ISMTPSettings aSMTPSettings = aNamedSettings == null ? null : aNamedSettings.getSMTPSettings ();
    InternalErrorHandler.setSMTPSenderAddress (new EmailAddress ("pyp@helger.com", CApp.getApplicationTitle ()));
    InternalErrorHandler.setSMTPReceiverAddress (new EmailAddress ("philip@helger.com", "Philip"));
    InternalErrorHandler.setSMTPSettings (aSMTPSettings);
  }
}
