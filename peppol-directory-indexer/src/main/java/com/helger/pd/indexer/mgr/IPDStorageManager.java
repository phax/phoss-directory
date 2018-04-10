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
package com.helger.pd.indexer.mgr;

import java.io.Closeable;
import java.io.IOException;

import javax.annotation.Nonnull;

import com.helger.commons.state.ESuccess;
import com.helger.pd.businesscard.PDExtendedBusinessCard;
import com.helger.pd.indexer.storage.PDDocumentMetaData;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;

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
                                @Nonnull PDDocumentMetaData aMetaData) throws IOException;

  /**
   * Delete an existing entry.
   *
   * @param aParticipantID
   *        Participant ID to be deleted.
   * @param aMetaData
   *        The entry metadata. Basically only for logging purposes.
   * @return {@link ESuccess#SUCCESS} upon success, {@link ESuccess#FAILURE} on
   *         error.
   * @throws IOException
   *         in case of IO error
   */
  @Nonnull
  ESuccess deleteEntry (@Nonnull IParticipantIdentifier aParticipantID,
                        @Nonnull PDDocumentMetaData aMetaData) throws IOException;
}
