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
 * Central manager for all sub managers
 *
 * @author Philip Helger
 */
public final class PDPMetaManager extends AbstractGlobalSingleton
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PDPMetaManager.class);

  @Deprecated
  @UsedViaReflection
  public PDPMetaManager ()
  {}

  @Override
  protected void onAfterInstantiation (@Nonnull final IScope aScope)
  {
    try
    {
      // TODO add managers here

      final ISMLInfo aSML = PDServerConfiguration.getSMLToUse ();
      if (aSML != null)
      {
        // Use only the configured SML (if any)
        // By default both official PEPPOL SMLs are queried!
        PDMetaManager.setBusinessCardProvider (SMPBusinessCardProvider.createWithDefinedSML (aSML,
                                                                                             PDServerConfiguration.getURLProvider ()));
      }

      s_aLogger.info (ClassHelper.getClassLocalName (this) + " was initialized");
    }
    catch (final Throwable t)
    {
      if (GlobalDebug.isProductionMode ())
      {
        new InternalErrorBuilder ().setThrowable (t)
                                   .addErrorMessage (ClassHelper.getClassLocalName (this) + " init failed")
                                   .handle ();
      }

      throw new InitializationException ("Failed to init " + ClassHelper.getClassLocalName (this), t);
    }
  }

  @Nonnull
  public static PDPMetaManager getInstance ()
  {
    return getGlobalSingleton (PDPMetaManager.class);
  }
}
