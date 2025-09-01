package com.helger.pd.indexer.storage;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ESuccess;
import com.helger.pd.indexer.businesscard.PDExtendedBusinessCard;
import com.helger.pd.indexer.mgr.IPDStorageManager;
import com.helger.pd.indexer.storage.model.PDStoredMetaData;
import com.helger.peppolid.IParticipantIdentifier;

import jakarta.annotation.Nonnull;

public class PDMultiStorageManager implements IPDStorageManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDMultiStorageManager.class);
  private final IPDStorageManager m_aMgr1;
  private final IPDStorageManager m_aMgr2;

  public PDMultiStorageManager (@Nonnull final IPDStorageManager aMgr1, @Nonnull final IPDStorageManager aMgr2)
  {
    ValueEnforcer.notNull (aMgr1, "Mgr1");
    ValueEnforcer.notNull (aMgr2, "Mgr2");
    m_aMgr1 = aMgr1;
    m_aMgr2 = aMgr2;
  }

  public void close () throws IOException
  {
    try
    {
      m_aMgr1.close ();
    }
    finally
    {
      m_aMgr2.close ();
    }
  }

  public ESuccess createOrUpdateEntry (final IParticipantIdentifier aParticipantID,
                                       final PDExtendedBusinessCard aExtBI,
                                       final PDStoredMetaData aMetaData) throws IOException
  {
    final ESuccess e1 = m_aMgr1.createOrUpdateEntry (aParticipantID, aExtBI, aMetaData);
    final ESuccess e2 = m_aMgr2.createOrUpdateEntry (aParticipantID, aExtBI, aMetaData);
    if (e1 != e2)
      LOGGER.warn ("Different StorageManager result for createOrUpdateEntry: " + e1 + " and " + e2);
    return e1;
  }

  public int deleteEntry (final IParticipantIdentifier aParticipantID,
                          final PDStoredMetaData aMetaData,
                          final boolean bVerifyOwner) throws IOException
  {
    final int n1 = m_aMgr1.deleteEntry (aParticipantID, aMetaData, bVerifyOwner);
    final int n2 = m_aMgr2.deleteEntry (aParticipantID, aMetaData, bVerifyOwner);
    if (n1 != n2)
      LOGGER.warn ("Different StorageManager result for deleteEntry: " + n1 + " and " + n2);
    return n1;
  }
}
