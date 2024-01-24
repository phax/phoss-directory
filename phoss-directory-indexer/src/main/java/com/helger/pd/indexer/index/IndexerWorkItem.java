/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.index;

import java.time.LocalDateTime;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.type.ObjectType;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;

/**
 * This class represents a single work item for the indexer. This is the default
 * implementation of {@link IIndexerWorkItem}.
 *
 * @author Philip Helger
 */
@Immutable
public final class IndexerWorkItem implements IIndexerWorkItem
{
  public static final ObjectType OT = new ObjectType ("IndexerWorkItem");

  private final String m_sID;
  private final LocalDateTime m_aCreationDT;
  private final IParticipantIdentifier m_aParticipantID;
  private final EIndexerWorkItemType m_eType;
  private final String m_sOwnerID;
  private final String m_sRequestingHost;

  public IndexerWorkItem (@Nonnull final IParticipantIdentifier aParticpantID,
                          @Nonnull final EIndexerWorkItemType eType,
                          @Nonnull @Nonempty final String sOwnerID,
                          @Nonnull @Nonempty final String sRequestingHost)
  {
    this (GlobalIDFactory.getNewPersistentStringID (),
          PDTFactory.getCurrentLocalDateTime (),
          aParticpantID,
          eType,
          sOwnerID,
          sRequestingHost);
  }

  IndexerWorkItem (@Nonnull @Nonempty final String sID,
                   @Nonnull final LocalDateTime aCreationDT,
                   @Nonnull final IParticipantIdentifier aParticpantID,
                   @Nonnull final EIndexerWorkItemType eType,
                   @Nonnull @Nonempty final String sOwnerID,
                   @Nonnull @Nonempty final String sRequestingHost)
  {
    ValueEnforcer.notNull (sID, "ID");
    ValueEnforcer.notNull (aCreationDT, "CreationDT");
    ValueEnforcer.notNull (aParticpantID, "ParticpantID");
    ValueEnforcer.notNull (eType, "Type");
    ValueEnforcer.notNull (sOwnerID, "OwnerID");
    ValueEnforcer.notNull (sRequestingHost, "RequestingHost");

    m_sID = sID;
    m_aCreationDT = aCreationDT;
    // Ensure all objects have the same type
    // No need to use the IIdentifierFactory here, since the participant is
    // already structured
    m_aParticipantID = new SimpleParticipantIdentifier (aParticpantID);
    m_eType = eType;
    m_sOwnerID = sOwnerID;
    m_sRequestingHost = sRequestingHost;
  }

  @Nonnull
  public ObjectType getObjectType ()
  {
    return OT;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nonnull
  public LocalDateTime getCreationDateTime ()
  {
    return m_aCreationDT;
  }

  @Nonnull
  public IParticipantIdentifier getParticipantID ()
  {
    return m_aParticipantID;
  }

  @Nonnull
  public EIndexerWorkItemType getType ()
  {
    return m_eType;
  }

  @Nonnull
  @Nonempty
  public String getOwnerID ()
  {
    return m_sOwnerID;
  }

  @Nonnull
  public String getRequestingHost ()
  {
    return m_sRequestingHost;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final IndexerWorkItem rhs = (IndexerWorkItem) o;
    return m_aParticipantID.equals (rhs.m_aParticipantID) && m_eType.equals (rhs.m_eType);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aParticipantID).append (m_eType).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ID", m_sID)
                                       .append ("CreationDT", m_aCreationDT)
                                       .append ("ParticipantID", m_aParticipantID)
                                       .append ("Type", m_eType)
                                       .append ("OwnerID", m_sOwnerID)
                                       .append ("RequestingHost", m_sRequestingHost)
                                       .getToString ();
  }
}
