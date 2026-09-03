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
package com.helger.pd.publisher.exportall;

import java.io.OutputStream;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.WillNotClose;
import com.helger.collection.commons.ICommonsList;
import com.helger.mime.IMimeType;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * Handler for a single export format. All the handlers of one export run are fed with the data of
 * all participants in a single pass, so that the search index needs to be queried only once per
 * participant, independent of the number of export formats.
 *
 * @author Philip Helger
 */
public interface IExportAllHandler
{
  /**
   * @return The name of this export format, for logging and status display only. Neither
   *         <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  String getDisplayName ();

  /**
   * @return The name of the file to be created on S3. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  String getS3Filename ();

  /**
   * @return The content type of the created file. Never <code>null</code>.
   */
  @NonNull
  IMimeType getContentType ();

  /**
   * @return <code>true</code> if this handler needs the Business Entities of each participant,
   *         <code>false</code> if the participant ID is sufficient. If no handler of an export run
   *         needs them, the search index is not queried per participant at all.
   */
  boolean isBusinessEntityDataNeeded ();

  /**
   * Start the export. This is called once before the first participant.
   *
   * @param aOS
   *        The output stream to write to. The stream is closed by the caller. May not be
   *        <code>null</code>.
   * @param nParticipantCount
   *        The total number of participants to be exported. Always &ge; 0.
   * @throws Exception
   *         On error
   */
  void onStart (@NonNull @WillNotClose OutputStream aOS, @Nonnegative int nParticipantCount) throws Exception;

  /**
   * Export a single participant. This is called once per participant, in the ascending order of the
   * participant IDs.
   *
   * @param sParticipantID
   *        The URI encoded representation of the participant ID. Neither <code>null</code> nor
   *        empty.
   * @param aParticipantID
   *        The participant ID. May not be <code>null</code>.
   * @param aEntities
   *        All the Business Entities of the participant. May not be <code>null</code> but maybe
   *        empty, if the participant was deleted in the meantime or if no handler of the export run
   *        needs the Business Entities. The list is reused for the next participant, so it may not
   *        be stored by the handler.
   * @throws Exception
   *         On error
   */
  void onParticipant (@NonNull @Nonempty String sParticipantID,
                      @NonNull IParticipantIdentifier aParticipantID,
                      @NonNull ICommonsList <PDStoredBusinessEntity> aEntities) throws Exception;

  /**
   * End the export. This is called once after the last participant. All buffered data must be
   * written to the output stream provided in
   * {@link #onStart(OutputStream, int)}.
   *
   * @throws Exception
   *         On error
   */
  void onEnd () throws Exception;
}
