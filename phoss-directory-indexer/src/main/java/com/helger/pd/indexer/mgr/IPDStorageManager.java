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

import java.io.Closeable;
import java.io.IOException;

import com.helger.annotation.CheckForSigned;
import com.helger.base.state.ESuccess;
import com.helger.pd.indexer.businesscard.PDExtendedBusinessCard;
import com.helger.pd.indexer.storage.PDStoredMetaData;
import com.helger.peppolid.IParticipantIdentifier;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * The abstract storage manager interface. It contains all the actions that can
 * be performed with business cards.
 *
 * @author Philip Helger
 */
public interface IPDStorageManager extends Closeable
{
  /**
   * Create a new entry or update an existing entry.
   *
   * @param aParticipantID
   *        Participant identifier it is all about.
   * @param aExtBI
   *        The extended business card with the document type identifiers.
   * @param aMetaData
   *        The additional meta data to be stored.
   * @return {@link ESuccess#SUCCESS} upon success, {@link ESuccess#FAILURE} on
   *         error.
   * @throws IOException
   *         in case of IO error
   */
  @Nonnull
  ESuccess createOrUpdateEntry (@Nonnull IParticipantIdentifier aParticipantID,
                                @Nonnull PDExtendedBusinessCard aExtBI,
                                @Nonnull PDStoredMetaData aMetaData) throws IOException;

  /**
   * Delete an existing entry (not recoverable).
   *
   * @param aParticipantID
   *        Participant ID to be deleted.
   * @param aMetaData
   *        The entry metadata. Basically only for logging purposes. May be
   *        <code>null</code>.
   * @param bVerifyOwner
   *        <code>true</code> if the owner should be considered,
   *        <code>false</code> if not.
   * @return The number of deleted entries, or -1 in case of failure
   * @throws IOException
   *         in case of IO error
   */
  @CheckForSigned
  int deleteEntry (@Nonnull IParticipantIdentifier aParticipantID,
                   @Nullable PDStoredMetaData aMetaData,
                   boolean bVerifyOwner) throws IOException;
}
