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
package com.helger.pd.publisher.app;

import java.net.URI;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.lang.ClassHelper;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.mgr.SMPBusinessCardProvider;
import com.helger.pd.settings.PDServerConfiguration;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.photon.core.app.error.InternalErrorBuilder;
import com.helger.scope.IScope;
import com.helger.scope.singleton.AbstractGlobalSingleton;

/**
 * PD Publisher meta manager
 *
 * @author Philip Helger
 */
public final class PDPMetaManager extends AbstractGlobalSingleton
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PDPMetaManager.class);

  private static final String SML_INFO_XML = "sml-info.xml";

  private SMLInfoManager m_aSMLInfoMgr;

  @Deprecated
  @UsedViaReflection
  public PDPMetaManager ()
  {}

  @Override
  protected void onAfterInstantiation (@Nonnull final IScope aScope)
  {
    try
    {
      m_aSMLInfoMgr = new SMLInfoManager (SML_INFO_XML);

      // TODO add managers here
      final URI aFixedSMPURI = PDServerConfiguration.getFixedSMPURI ();
      if (aFixedSMPURI != null)
      {
        // Use only the configured SMP
        PDMetaManager.setBusinessCardProvider (SMPBusinessCardProvider.createForFixedSMP (PDServerConfiguration.getSMPMode (),
                                                                                          aFixedSMPURI));
      }
      else
      {
        // Check if an SML is configure.
        final ISMLInfo aSML = PDServerConfiguration.getSMLToUse (m_aSMLInfoMgr::getSMLInfoOfID);
        if (aSML != null)
        {
          // Use only the configured SML
          // By default both official PEPPOL SMLs are queried!
          PDMetaManager.setBusinessCardProvider (SMPBusinessCardProvider.createWithDefinedSML (PDServerConfiguration.getSMPMode (),
                                                                                               aSML,
                                                                                               PDServerConfiguration.getURLProvider ()));
        }
        else
        {
          // Auto detect SMLs
          PDMetaManager.setBusinessCardProvider (SMPBusinessCardProvider.createWithSMLAutoDetect (PDServerConfiguration.getSMPMode (),
                                                                                                  PDServerConfiguration.getURLProvider (),
                                                                                                  m_aSMLInfoMgr::getAll));
        }
      }

      s_aLogger.info (ClassHelper.getClassLocalName (this) + " was initialized");
    }
    catch (final Exception ex)
    {
      if (GlobalDebug.isProductionMode ())
      {
        new InternalErrorBuilder ().setThrowable (ex)
                                   .addErrorMessage (ClassHelper.getClassLocalName (this) + " init failed")
                                   .handle ();
      }

      throw new InitializationException ("Failed to init " + ClassHelper.getClassLocalName (this), ex);
    }
  }

  @Nonnull
  public static PDPMetaManager getInstance ()
  {
    return getGlobalSingleton (PDPMetaManager.class);
  }

  @Nonnull
  public static ISMLInfoManager getSMLInfoMgr ()
  {
    return getInstance ().m_aSMLInfoMgr;
  }
}
