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
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.pd.indexer.index.EIndexerWorkItemType;

import jakarta.annotation.Nonnull;

/**
 * Immutable shadow event representing an indexer operation to be replicated to
 * a downstream service during PD2 migration.
 *
 * @author Mikael Aksamit
 */
@Immutable
public final class ShadowEvent implements IShadowEvent
{
  private final String m_sEventID;
  private final LocalDateTime m_aCreatedAt;
  private final EIndexerWorkItemType m_eOperation;
  private final String m_sParticipantID;
  private final String m_sRequestingHost;
  private final String m_sCertSHA256Fingerprint;
  private final String m_sCertSubjectDN;
  private final String m_sCertIssuerDN;

  public ShadowEvent (@Nonnull @Nonempty final String sEventID,
                      @Nonnull final LocalDateTime aCreatedAt,
                      @Nonnull final EIndexerWorkItemType eOperation,
                      @Nonnull @Nonempty final String sParticipantID,
                      @Nonnull @Nonempty final String sRequestingHost,
                      @Nonnull @Nonempty final String sCertSHA256Fingerprint,
                      @Nonnull @Nonempty final String sCertSubjectDN,
                      @Nonnull @Nonempty final String sCertIssuerDN)
  {
    m_sEventID = ValueEnforcer.notEmpty (sEventID, "EventID");
    m_aCreatedAt = ValueEnforcer.notNull (aCreatedAt, "CreatedAt");
    m_eOperation = ValueEnforcer.notNull (eOperation, "Operation");
    m_sParticipantID = ValueEnforcer.notEmpty (sParticipantID, "ParticipantID");
    m_sRequestingHost = ValueEnforcer.notEmpty (sRequestingHost, "RequestingHost");
    m_sCertSHA256Fingerprint = ValueEnforcer.notEmpty (sCertSHA256Fingerprint, "CertSHA256Fingerprint");
    m_sCertSubjectDN = ValueEnforcer.notEmpty (sCertSubjectDN, "CertSubjectDN");
    m_sCertIssuerDN = ValueEnforcer.notEmpty (sCertIssuerDN, "CertIssuerDN");
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sEventID;
  }

  @Nonnull
  @Nonempty
  public String getEventID ()
  {
    return m_sEventID;
  }

  @Nonnull
  public LocalDateTime getCreatedAt ()
  {
    return m_aCreatedAt;
  }

  @Nonnull
  public EIndexerWorkItemType getOperation ()
  {
    return m_eOperation;
  }

  @Nonnull
  @Nonempty
  public String getParticipantID ()
  {
    return m_sParticipantID;
  }

  @Nonnull
  @Nonempty
  public String getRequestingHost ()
  {
    return m_sRequestingHost;
  }

  @Nonnull
  @Nonempty
  public String getCertSHA256Fingerprint ()
  {
    return m_sCertSHA256Fingerprint;
  }

  @Nonnull
  @Nonempty
  public String getCertSubjectDN ()
  {
    return m_sCertSubjectDN;
  }

  @Nonnull
  @Nonempty
  public String getCertIssuerDN ()
  {
    return m_sCertIssuerDN;
  }
}
