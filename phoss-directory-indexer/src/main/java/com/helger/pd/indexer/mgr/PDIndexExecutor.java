/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonnegative;
import com.helger.base.state.ESuccess;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.pd.indexer.businesscard.IPDBusinessCardProvider;
import com.helger.pd.indexer.businesscard.PDExtendedBusinessCard;
import com.helger.pd.indexer.index.IIndexerWorkItem;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * Internal class to execute a single work item. It is invoked by the {@link PDIndexerManager}.
 *
 * @author Philip Helger
 */
final class PDIndexExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDIndexExecutor.class);

  private PDIndexExecutor ()
  {}

  /**
   * This method is responsible for executing the specified work item depending on its type.
   *
   * @param aStorageMgr
   *        Storage manager.
   * @param aWorkItem
   *        The work item to be executed. May not be <code>null</code>.
   * @param nRetryCount
   *        The retry count. For the initial indexing it is 0, for the first retry 1 etc.
   * @param aSuccessHandler
   *        A callback that is invoked upon success only.
   * @param aFailureHandler
   *        A callback that is invoked upon failure only.
   * @return {@link ESuccess}
   */
  @NonNull
  public static ESuccess executeWorkItem (@NonNull final IPDStorageManager aStorageMgr,
                                          @NonNull final IIndexerWorkItem aWorkItem,
                                          @Nonnegative final int nRetryCount,
                                          @NonNull final Consumer <? super IIndexerWorkItem> aSuccessHandler,
                                          @NonNull final BiConsumer <? super IIndexerWorkItem, ? super ICommonsList <String>> aFailureHandler)
  {
    LOGGER.info ("Execute work item " +
                 aWorkItem.getLogText () +
                 " - " +
                 (nRetryCount > 0 ? "retry #" + nRetryCount : "initial try"));

    final ICommonsList <String> aErrorMsgs = new CommonsArrayList <> ();
    final IPDBusinessCardProvider aBCProvider = PDMetaManager.getBusinessCardProviderOrNull ();
    if (aBCProvider == null)
    {
      final String sErrorMsg = "No BusinessCard Provider is present.";
      // Maybe null upon shutdown - in that case ignore it and don't reindex
      LOGGER.error (sErrorMsg);
      aErrorMsgs.add (sErrorMsg);
    }
    else
    {
      try
      {
        final IParticipantIdentifier aParticipantID = aWorkItem.getParticipantID ();

        final ESuccess eSuccess;
        switch (aWorkItem.getType ())
        {
          case CREATE_UPDATE:
          {
            // Get BI from participant (e.g. from SMP)
            final PDExtendedBusinessCard aBI = aBCProvider.getBusinessCard (aParticipantID, aErrorMsgs::add);
            if (aBI == null)
            {
              // No/invalid extension present - no need to try again
              eSuccess = ESuccess.FAILURE;
              final String sErrorMsg = "Failed to retrieve BusinessCard from SMP";
              aErrorMsgs.add (sErrorMsg);
            }
            else
            {
              // Got data - put in storage
              eSuccess = aStorageMgr.createOrUpdateEntry (aParticipantID, aBI, aWorkItem.getAsMetaData ());
              if (eSuccess.isFailure ())
              {
                final String sErrorMsg = "Successfully retrieved BusinessCard but failed to store the data.";
                aErrorMsgs.add (sErrorMsg);
              }
            }
            break;
          }
          case DELETE:
          {
            // Really delete it
            eSuccess = ESuccess.valueOf (aStorageMgr.deleteEntry (aParticipantID, aWorkItem.getAsMetaData (), true) >=
                                         0);
            if (eSuccess.isFailure ())
            {
              final String sErrorMsg = "Failed to delete the BusinessCard from the index";
              aErrorMsgs.add (sErrorMsg);
            }
            break;
          }
          case SYNC:
          {
            // Get BI from participant (e.g. from SMP)
            final PDExtendedBusinessCard aBI = aBCProvider.getBusinessCard (aParticipantID, aErrorMsgs::add);
            if (aBI == null)
            {
              // No/invalid extension present - delete from index
              eSuccess = ESuccess.valueOf (aStorageMgr.deleteEntry (aParticipantID, aWorkItem.getAsMetaData (), true) >=
                                           0);
              if (eSuccess.isFailure ())
              {
                final String sErrorMsg = "Failed to retrieve the BusinessCard and failed to remove the data from the index.";
                aErrorMsgs.add (sErrorMsg);
              }
            }
            else
            {
              // Got data - put in storage
              eSuccess = aStorageMgr.createOrUpdateEntry (aParticipantID, aBI, aWorkItem.getAsMetaData ());
              if (eSuccess.isFailure ())
              {
                final String sErrorMsg = "Successfully retrieved BusinessCard but failed to store the data.";
                aErrorMsgs.add (sErrorMsg);
              }
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

          LOGGER.info ("Successfully finished executing work item " + aWorkItem.getLogText ());

          // And we're done
          return ESuccess.SUCCESS;
        }

        // else error storing data
      }
      catch (final Exception ex)
      {
        final String sErrorMsg = "Error in executing work item " + aWorkItem.getLogText () + " - " + ex.getMessage ();
        LOGGER.error (sErrorMsg, ex);
        aErrorMsgs.add (sErrorMsg);
        // Fall through
      }
    }

    // Invoke failure handler
    aFailureHandler.accept (aWorkItem, aErrorMsgs);

    LOGGER.warn ("Failure processing executing work item " + aWorkItem.getLogText ());

    return ESuccess.FAILURE;
  }
}
