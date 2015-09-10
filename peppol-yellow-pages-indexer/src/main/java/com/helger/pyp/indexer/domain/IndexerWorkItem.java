/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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
package com.helger.pyp.indexer.domain;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.joda.time.LocalDateTime;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.datetime.PDTFactory;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.identifier.participant.IPeppolParticipantIdentifier;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;

/**
 * This class represents a single work item for the indexer.
 *
 * @author Philip Helger
 */
@Immutable
public final class IndexerWorkItem implements Serializable
{
  private final LocalDateTime m_aCreationDT;
  private final IPeppolParticipantIdentifier m_aParticpantID;
  private final EIndexerWorkItemType m_eType;
  private final String m_sOwnerID;
  private final String m_sRequestingHost;

  public IndexerWorkItem (@Nonnull final IParticipantIdentifier aParticpantID,
                          @Nonnull final EIndexerWorkItemType eType,
                          @Nonnull @Nonempty final String sOwnerID,
                          @Nonnull @Nonempty final String sRequestingHost)
  {
    this (PDTFactory.getCurrentLocalDateTime (), aParticpantID, eType, sOwnerID, sRequestingHost);
  }

  IndexerWorkItem (@Nonnull final LocalDateTime aCreationDT,
                   @Nonnull final IParticipantIdentifier aParticpantID,
                   @Nonnull final EIndexerWorkItemType eType,
                   @Nonnull @Nonempty final String sOwnerID,
                   @Nonnull @Nonempty final String sRequestingHost)
  {
    ValueEnforcer.notNull (aCreationDT, "CreationDT");
    ValueEnforcer.notNull (aParticpantID, "ParticpantID");
    ValueEnforcer.notNull (eType, "Type");
    ValueEnforcer.notNull (sOwnerID, "OwnerID");
    ValueEnforcer.notNull (sRequestingHost, "RequestingHost");

    m_aCreationDT = aCreationDT;
    // Ensure all objects have the same type
    m_aParticpantID = new SimpleParticipantIdentifier (aParticpantID);
    m_eType = eType;
    m_sOwnerID = sOwnerID;
    m_sRequestingHost = sRequestingHost;
  }

  /**
   * @return The date time when the object was queued. Never <code>null</code>.
   */
  @Nonnull
  public LocalDateTime getCreationDT ()
  {
    return m_aCreationDT;
  }

  /**
   * @return The participant identifier it is all about.
   */
  @Nonnull
  public IPeppolParticipantIdentifier getParticipantID ()
  {
    return m_aParticpantID;
  }

  /**
   * @return The action type to execute. Never <code>null</code>.
   */
  @Nonnull
  public EIndexerWorkItemType getType ()
  {
    return m_eType;
  }

  /**
   * @return The ID of the client (based on the provided certificate) that
   *         requested this action. Never <code>null</code>.
   */
  @Nonnull
  public String getOwnerID ()
  {
    return m_sOwnerID;
  }

  /**
   * @return The IP address of the host requesting this work item. If this
   *         action is triggered by the scheduled SML exchange, this should be
   *         <code>automatic</code>.
   */
  @Nonnull
  public String getRequestingHost ()
  {
    return m_sRequestingHost;
  }

  @Nonnull
  @Nonempty
  public String getLogText ()
  {
    return m_sOwnerID + "@" + m_eType + "[" + m_aParticpantID.getURIEncoded () + "]";
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final IndexerWorkItem rhs = (IndexerWorkItem) o;
    return m_aParticpantID.equals (rhs.m_aParticpantID) && m_eType.equals (rhs.m_eType);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aParticpantID).append (m_eType).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("CreationDT", m_aCreationDT)
                                       .append ("ParticipantID", m_aParticpantID)
                                       .append ("Type", m_eType)
                                       .append ("OwnerID", m_sOwnerID)
                                       .append ("RequestingHost", m_sRequestingHost)
                                       .toString ();
  }
}
