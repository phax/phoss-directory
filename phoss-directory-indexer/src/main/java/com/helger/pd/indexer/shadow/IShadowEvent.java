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
package com.helger.pd.indexer.shadow;

import java.time.LocalDateTime;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.pd.indexer.index.EIndexerWorkItemType;

import jakarta.annotation.Nonnull;

/**
 * Interface for a shadow event that represents an indexer operation to be replicated to a
 * downstream service during PD2 migration.
 *
 * @author Mikael Aksamit (mikael@aksamit.se)
 */
public interface IShadowEvent extends IHasID <String>
{
  /**
   * @return The unique event ID (UUID). Never <code>null</code>.
   */
  @Nonnull
  @Nonempty
  String getEventID ();

  /**
   * @return When this shadow event was created. Never <code>null</code>.
   */
  @Nonnull
  LocalDateTime getCreatedAt ();

  /**
   * @return The indexer operation type. Never <code>null</code>.
   */
  @Nonnull
  EIndexerWorkItemType getOperation ();

  /**
   * @return The participant identifier. Never <code>null</code>.
   */
  @Nonnull
  @Nonempty
  String getParticipantID ();

  /**
   * @return The requesting host. Never <code>null</code>.
   */
  @Nonnull
  @Nonempty
  String getRequestingHost ();

  /**
   * @return The SHA-256 fingerprint of the client certificate. Never <code>null</code>.
   */
  @Nonnull
  @Nonempty
  String getCertSHA256Fingerprint ();

  /**
   * @return The subject DN of the client certificate. Never <code>null</code>.
   */
  @Nonnull
  @Nonempty
  String getCertSubjectDN ();

  /**
   * @return The issuer DN of the client certificate. Never <code>null</code>.
   */
  @Nonnull
  @Nonempty
  String getCertIssuerDN ();
}
