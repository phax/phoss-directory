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
package com.helger.pd.indexer.mgr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.style.UsedViaReflection;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.debug.GlobalDebug;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.exception.InitializationException;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.lang.clazz.ClassHelper;
import com.helger.pd.indexer.businesscard.IPDBusinessCardProvider;
import com.helger.pd.indexer.lucene.PDLucene;
import com.helger.pd.indexer.settings.PDServerConfiguration;
import com.helger.pd.indexer.storage.PDStorageManagerLucene;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.photon.core.interror.InternalErrorBuilder;
import com.helger.scope.IScope;
import com.helger.scope.singleton.AbstractGlobalSingleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * The Peppol Directory meta manager. It consists all the other managers for the Peppol Directory
 * indexing.
 *
 * @author Philip Helger
 */
public final class PDMetaManager extends AbstractGlobalSingleton
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDMetaManager.class);

  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();
  // Read only once on startup
  private static final IIdentifierFactory IF = PDServerConfiguration.getIdentifierFactory ();
  @GuardedBy ("s_aRWLock")
  private static IPDBusinessCardProvider s_aBCProvider;

  private PDLucene m_aLucene;
  private IPDStorageManager m_aStorageMgr;
  private IPDIndexerManager m_aIndexerMgr;

  @Deprecated (forRemoval = false)
  @UsedViaReflection
  public PDMetaManager ()
  {}

  @Override
  protected void onAfterInstantiation (@Nonnull final IScope aScope)
  {
    try
    {
      m_aLucene = new PDLucene ();
      m_aStorageMgr = new PDStorageManagerLucene (m_aLucene);
      m_aIndexerMgr = new PDIndexerManagerLucene (m_aStorageMgr);

      LOGGER.info (ClassHelper.getClassLocalName (this) + " was initialized");
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

  @Override
  protected void onDestroy (@Nonnull final IScope aScopeInDestruction)
  {
    StreamHelper.close (m_aLucene);
    StreamHelper.close (m_aStorageMgr);
    StreamHelper.close (m_aIndexerMgr);
  }

  @Nonnull
  public static PDMetaManager getInstance ()
  {
    return getGlobalSingleton (PDMetaManager.class);
  }

  /**
   * @return The global {@link IPDBusinessCardProvider}. May be <code>null</code> .
   */
  @Nullable
  public static IPDBusinessCardProvider getBusinessCardProviderOrNull ()
  {
    return RW_LOCK.readLockedGet ( () -> s_aBCProvider);
  }

  /**
   * @return The global {@link IPDBusinessCardProvider}. Never <code>null</code> .
   */
  @Nonnull
  public static IPDBusinessCardProvider getBusinessCardProvider ()
  {
    final IPDBusinessCardProvider ret = getBusinessCardProviderOrNull ();
    if (ret == null)
      throw new IllegalStateException ("No BusinessCardProvider is present!");
    return ret;
  }

  /**
   * Set the global {@link IPDBusinessCardProvider} that is used for future create/update requests.
   *
   * @param aBCProvider
   *        Business card provider to be used. May not be <code>null</code>.
   */
  public static void setBusinessCardProvider (@Nonnull final IPDBusinessCardProvider aBCProvider)
  {
    ValueEnforcer.notNull (aBCProvider, "BCProvider");
    RW_LOCK.writeLockedGet ( () -> s_aBCProvider = aBCProvider);
  }

  @Nonnull
  public static PDLucene getLucene ()
  {
    return getInstance ().m_aLucene;
  }

  @Nonnull
  public static IPDStorageManager getStorageMgr ()
  {
    return getInstance ().m_aStorageMgr;
  }

  @Nonnull
  public static IPDIndexerManager getIndexerMgr ()
  {
    return getInstance ().m_aIndexerMgr;
  }

  @Nonnull
  public static IIdentifierFactory getIdentifierFactory ()
  {
    return IF;
  }
}
