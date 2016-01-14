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
package com.helger.pd.indexer.mgr;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.annotation.VisibleForTesting;
import com.helger.commons.callback.IThrowingCallableWithParameter;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.lang.ClassHelper;
import com.helger.commons.scope.IScope;
import com.helger.commons.scope.singleton.AbstractGlobalSingleton;
import com.helger.pd.indexer.lucene.PDLucene;
import com.helger.pd.indexer.storage.PDStorageManager;
import com.helger.photon.basic.app.dao.impl.DAOException;

/**
 * The PEPPOL Directory meta manager. It consists all the other managers for the
 * PEPPOL Directory indexing.
 *
 * @author Philip Helger
 */
public final class PDMetaManager extends AbstractGlobalSingleton
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PDMetaManager.class);

  private static IThrowingCallableWithParameter <PDIndexerManager, PDStorageManager, DAOException> s_aFactoryIndexerMgr = aStorageMgr -> new PDIndexerManager (aStorageMgr).readAndQueueInitialData ();

  private PDLucene m_aLucene;
  private PDStorageManager m_aStorageMgr;
  private PDIndexerManager m_aIndexerMgr;

  /**
   * Set the factory used to create the {@link PDIndexerManager} instance.
   *
   * @param aFactoryIndexerMgr
   *        The factory to use. May not be <code>null</code>.
   */
  @VisibleForTesting
  public static void setIndexerMgrFactory (@Nonnull final IThrowingCallableWithParameter <PDIndexerManager, PDStorageManager, DAOException> aFactoryIndexerMgr)
  {
    ValueEnforcer.notNull (aFactoryIndexerMgr, "FactoryIndexerMgr");
    s_aFactoryIndexerMgr = aFactoryIndexerMgr;
  }

  @Deprecated
  @UsedViaReflection
  public PDMetaManager ()
  {}

  @Override
  protected void onAfterInstantiation (@Nonnull final IScope aScope)
  {
    try
    {
      m_aLucene = new PDLucene ();
      m_aStorageMgr = new PDStorageManager (m_aLucene);
      m_aIndexerMgr = s_aFactoryIndexerMgr.call (m_aStorageMgr);
      if (m_aIndexerMgr == null)
        throw new IllegalStateException ("Failed to create IndexerManager using factory " + s_aFactoryIndexerMgr);

      s_aLogger.info (ClassHelper.getClassLocalName (this) + " was initialized");
    }
    catch (final Exception ex)
    {
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

  @Nonnull
  public static PDLucene getLucene ()
  {
    return getInstance ().m_aLucene;
  }

  @Nonnull
  public static PDStorageManager getStorageMgr ()
  {
    return getInstance ().m_aStorageMgr;
  }

  @Nonnull
  public static PDIndexerManager getIndexerMgr ()
  {
    return getInstance ().m_aIndexerMgr;
  }
}
