package com.helger.pd.indexer.mgr;

import java.io.Closeable;
import java.io.IOException;

import javax.annotation.Nonnull;

import com.helger.commons.state.ESuccess;
import com.helger.pd.businesscard.PDExtendedBusinessCard;
import com.helger.pd.indexer.storage.PDDocumentMetaData;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;

public interface IPDStorageManager extends Closeable
{
  @Nonnull
  ESuccess createOrUpdateEntry (@Nonnull IParticipantIdentifier aParticipantID,
                                @Nonnull PDExtendedBusinessCard aExtBI,
                                @Nonnull PDDocumentMetaData aMetaData) throws IOException;

  @Nonnull
  ESuccess deleteEntry (@Nonnull IParticipantIdentifier aParticipantID,
                        @Nonnull PDDocumentMetaData aMetaData) throws IOException;
}
