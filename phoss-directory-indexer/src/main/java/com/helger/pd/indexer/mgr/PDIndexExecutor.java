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
package com.helger.pd.indexer.mgr;

import java.util.function.Consumer;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.state.ESuccess;
import com.helger.pd.indexer.businesscard.IPDBusinessCardProvider;
import com.helger.pd.indexer.businesscard.PDExtendedBusinessCard;
import com.helger.pd.indexer.index.IIndexerWorkItem;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * Internal class to execute a single work item. It is invoked by the
 * {@link PDIndexerManager}.
 *
 * @author Philip Helger
 */
final class PDIndexExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDIndexExecutor.class);

  private PDIndexExecutor ()
  {}

  /**
   * This method is responsible for executing the specified work item depending
   * on its type.
   *
   * @param aStorageMgr
   *        Storage manager.
   * @param aWorkItem
   *        The work item to be executed. May not be <code>null</code>.
   * @param nRetryCount
   *        The retry count. For the initial indexing it is 0, for the first
   *        retry 1 etc.
   * @param aSuccessHandler
   *        A callback that is invoked upon success only.
   * @param aFailureHandler
   *        A callback that is invoked upon failure only.
   * @return {@link ESuccess}
   */
  @Nonnull
  public static ESuccess executeWorkItem (@Nonnull final IPDStorageManager aStorageMgr,
                                          @Nonnull final IIndexerWorkItem aWorkItem,
                                          @Nonnegative final int nRetryCount,
                                          @Nonnull final Consumer <? super IIndexerWorkItem> aSuccessHandler,
                                          @Nonnull final Consumer <? super IIndexerWorkItem> aFailureHandler)
  {
    LOGGER.info ("Execute work item " + aWorkItem.getLogText () + " - " + (nRetryCount > 0 ? "retry #" + nRetryCount : "initial try"));

    final IPDBusinessCardProvider aBCProvider = PDMetaManager.getBusinessCardProviderOrNull ();
    if (aBCProvider == null)
    {
      // Maybe null upon shutdown - in that case ignore it and don't reindex
      LOGGER.error ("No BusinessCard Provider is present.");
      return ESuccess.FAILURE;
    }

    try
    {
      final IParticipantIdentifier aParticipantID = aWorkItem.getParticipantID ();

      final ESuccess eSuccess;
      switch (aWorkItem.getType ())
      {
        case CREATE_UPDATE:
        {
          // Get BI from participant (e.g. from SMP)
          final PDExtendedBusinessCard aBI = aBCProvider.getBusinessCard (aParticipantID);
          if (aBI == null)
          {
            // No/invalid extension present - no need to try again
            eSuccess = ESuccess.FAILURE;
          }
          else
          {
            // Got data - put in storage
            eSuccess = aStorageMgr.createOrUpdateEntry (aParticipantID, aBI, aWorkItem.getAsMetaData ());
          }
          break;
        }
        case DELETE:
        {
          // Really delete it
          eSuccess = ESuccess.valueOf (aStorageMgr.deleteEntry (aParticipantID, aWorkItem.getAsMetaData (), true) >= 0);
          break;
        }
        case SYNC:
        {
          // Get BI from participant (e.g. from SMP)
          final PDExtendedBusinessCard aBI = aBCProvider.getBusinessCard (aParticipantID);
          if (aBI == null)
          {
            // No/invalid extension present - delete from index
            eSuccess = ESuccess.valueOf (aStorageMgr.deleteEntry (aParticipantID, aWorkItem.getAsMetaData (), true) >= 0);
          }
          else
          {
            // Got data - put in storage
            eSuccess = aStorageMgr.createOrUpdateEntry (aParticipantID, aBI, aWorkItem.getAsMetaData ());
          }
          break;
        }
        default:
          throw new IllegalStateException ("Unsupported work item type: " + aWorkItem);
      }

      if (eSuccess.isSuccess ())
      {
        // Item handled - remove from overall list
        aSuccessHandler.accept (aWorkItem);

        // And we're done
        return ESuccess.SUCCESS;
      }

      // else error storing data
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error in executing work item " + aWorkItem.getLogText (), ex);
      // Fall through
    }

    // Invoke failure handler
    aFailureHandler.accept (aWorkItem);

    return ESuccess.FAILURE;
  }
}
