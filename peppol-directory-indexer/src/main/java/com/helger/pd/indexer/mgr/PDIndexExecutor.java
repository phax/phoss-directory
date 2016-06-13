package com.helger.pd.indexer.mgr;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.state.ESuccess;
import com.helger.pd.businesscard.PDExtendedBusinessCard;
import com.helger.pd.indexer.index.IIndexerWorkItem;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;

final class PDIndexExecutor
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PDIndexExecutor.class);

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
   * @param aSuccessHandler
   *        A callback that is invoked upon success only.
   * @param aFailureHandler
   *        A callback that is invoked upon failure only.
   * @return {@link ESuccess}
   */
  @Nonnull
  public static ESuccess executeWorkItem (@Nonnull final IPDStorageManager aStorageMgr,
                                          @Nonnull final IIndexerWorkItem aWorkItem,
                                          @Nonnull final Consumer <IIndexerWorkItem> aSuccessHandler,
                                          @Nonnull final Consumer <IIndexerWorkItem> aFailureHandler)
  {
    s_aLogger.info ("Execute work item " + aWorkItem.getLogText ());

    try
    {
      final IParticipantIdentifier aParticipantID = aWorkItem.getParticipantID ();

      ESuccess eSuccess;
      switch (aWorkItem.getType ())
      {
        case CREATE_UPDATE:
        {
          // Get BI from participant (e.g. from SMP)
          final PDExtendedBusinessCard aBI = PDMetaManager.getBusinessCardProvider ().getBusinessCard (aParticipantID);
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
          eSuccess = aStorageMgr.deleteEntry (aParticipantID, aWorkItem.getAsMetaData ());
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
      s_aLogger.error ("Error in executing work item " + aWorkItem.getLogText (), ex);
      // Fall through
    }

    // Invoke failur handler
    aFailureHandler.accept (aWorkItem);

    return ESuccess.FAILURE;
  }
}
