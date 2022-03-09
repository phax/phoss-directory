/*
 * Copyright (C) 2015-2022 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.storage;

import java.time.LocalDateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.regex.RegExHelper;
import com.helger.commons.string.ToStringGenerator;

/**
 * This class contains all the metadata stored in a
 * {@link PDStoredBusinessEntity}.
 *
 * @author Philip Helger
 */
@Immutable
public final class PDStoredMetaData
{
  private final LocalDateTime m_aCreationDT;
  private final String m_sOwnerID;
  private final String m_sRequestingHost;

  public PDStoredMetaData (@Nonnull final LocalDateTime aCreationDT,
                           @Nonnull @Nonempty final String sOwnerID,
                           @Nonnull @Nonempty final String sRequestingHost)
  {
    ValueEnforcer.notNull (aCreationDT, "CreationDT");
    ValueEnforcer.notNull (sOwnerID, "OwnerID");
    ValueEnforcer.notNull (sRequestingHost, "RequestingHost");

    m_aCreationDT = aCreationDT;
    m_sOwnerID = sOwnerID;
    m_sRequestingHost = sRequestingHost;
  }

  /**
   * @return The date time when the object was queued for indexing. Never
   *         <code>null</code>.
   */
  @Nonnull
  public LocalDateTime getCreationDT ()
  {
    return m_aCreationDT;
  }

  /**
   * @return The ID of the client (based on the provided certificate) that
   *         requested this action. Never <code>null</code>. E.g.
   *         <code>CN=PSG000155,O=SGNIC,C=SG:47186c3e6d05cffbd340f3f51e2425cc</code>
   */
  @Nonnull
  @Nonempty
  public String getOwnerID ()
  {
    return m_sOwnerID;
  }

  @Nullable
  public static String getOwnerIDSeatNumber (@Nonnull @Nonempty final String sOwnerID)
  {
    final String [] aMatches = RegExHelper.getAllMatchingGroupValues ("CN=P[A-Z]{2}([^,]+),.*", sOwnerID);
    return aMatches == null || aMatches.length != 1 ? null : aMatches[0];
  }

  @Nullable
  public String getOwnerIDSeatNumber ()
  {
    return getOwnerIDSeatNumber (m_sOwnerID);
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

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("CreationDT", m_aCreationDT)
                                       .append ("OwnerID", m_sOwnerID)
                                       .append ("RequestingHost", m_sRequestingHost)
                                       .getToString ();
  }
}
