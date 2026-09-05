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
package com.helger.pd.publisher.app.secure;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * A single participant contained in the search index, together with the number of business entities
 * that are stored for it. This is the row type of the "Participant list" page.
 *
 * @author Philip Helger
 * @since 0.17.3
 */
@Immutable
public final class ContainedParticipant
{
  private final IParticipantIdentifier m_aParticipantID;
  private final int m_nEntityCount;

  public ContainedParticipant (@NonNull final IParticipantIdentifier aParticipantID,
                               @Nonnegative final int nEntityCount)
  {
    m_aParticipantID = ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    m_nEntityCount = ValueEnforcer.isGE0 (nEntityCount, "EntityCount");
  }

  /**
   * @return The participant identifier. Never <code>null</code>.
   */
  @NonNull
  public IParticipantIdentifier getParticipantID ()
  {
    return m_aParticipantID;
  }

  /**
   * @return The number of business entities that are stored in the search index for this
   *         participant. Always &ge; 0.
   */
  @Nonnegative
  public int getEntityCount ()
  {
    return m_nEntityCount;
  }
}
