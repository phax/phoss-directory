package com.helger.pd.indexer.mgr;

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
   * @param aIndexerMgr
   *        Indexer manager.
   * @param aWorkItem
   *        The work item to be executed. May not be <code>null</code>.
   * @return {@link ESuccess}
   */
  @Nonnull
  public static ESuccess executeWorkItem (@Nonnull final IPDStorageManager aStorageMgr,
                                          @Nonnull final PDIndexerManager aIndexerMgr,
                                          @Nonnull final IIndexerWorkItem aWorkItem)
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
          final PDExtendedBusinessCard aBI = aIndexerMgr.getBusinessCardProvider ().getBusinessCard (aParticipantID);
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
        aIndexerMgr.internalAfterSuccess (aWorkItem);

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

    return ESuccess.FAILURE;
  }
}
