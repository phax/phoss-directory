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
package com.helger.pd.indexer.reindex;

import java.time.LocalDateTime;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.type.ObjectType;
import com.helger.pd.indexer.index.IIndexerWorkItem;
import com.helger.pd.indexer.settings.PDServerConfiguration;

/**
 * The default implementation of {@link IReIndexWorkItem}.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class ReIndexWorkItem implements IReIndexWorkItem
{
  public static final ObjectType OT = new ObjectType ("ReIndexWorkItem");

  private final IIndexerWorkItem m_aWorkItem;
  private final LocalDateTime m_aMaxRetryDT;
  private int m_nRetries;
  private LocalDateTime m_aPreviousRetryDT;
  private LocalDateTime m_aNextRetryDT;

  public ReIndexWorkItem (@Nonnull final IIndexerWorkItem aWorkItem)
  {
    // The next retry happens from now in the configured number of minutes
    this (aWorkItem,
          aWorkItem.getCreationDateTime ().plusHours (PDServerConfiguration.getReIndexMaxRetryHours ()),
          0,
          (LocalDateTime) null,
          PDTFactory.getCurrentLocalDateTime ().plusMinutes (PDServerConfiguration.getReIndexRetryMinutes ()));
  }

  /**
   * Constructor
   *
   * @param aWorkItem
   *        The original work item to be handled.
   * @param aMaxRetryDT
   *        The latest date time until which a retry is feasible.
   * @param nRetries
   *        The number of retries so far. Must be &ge; 0.
   * @param aPreviousRetryDT
   *        The last retry time. May be <code>null</code> if no retry happened
   *        so far.
   * @param aNextRetryDT
   *        The next retry time. Must be &ge; now.
   */
  ReIndexWorkItem (@Nonnull final IIndexerWorkItem aWorkItem,
                   @Nonnull final LocalDateTime aMaxRetryDT,
                   final int nRetries,
                   @Nullable final LocalDateTime aPreviousRetryDT,
                   @Nonnull final LocalDateTime aNextRetryDT)
  {
    m_aWorkItem = ValueEnforcer.notNull (aWorkItem, "WorkItem");
    m_aMaxRetryDT = ValueEnforcer.notNull (aMaxRetryDT, "MaxRetryDT");
    m_nRetries = ValueEnforcer.isGE0 (nRetries, "Retries");
    m_aPreviousRetryDT = aPreviousRetryDT;
    if (nRetries > 0)
      ValueEnforcer.notNull (aPreviousRetryDT, "PreviousRetryDT");
    m_aNextRetryDT = ValueEnforcer.notNull (aNextRetryDT, "NextRetryDT");
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
    return m_aWorkItem.getID ();
  }

  @Nonnull
  public IIndexerWorkItem getWorkItem ()
  {
    return m_aWorkItem;
  }

  @Nonnull
  public LocalDateTime getMaxRetryDT ()
  {
    return m_aMaxRetryDT;
  }

  @Nonnegative
  public int getRetryCount ()
  {
    return m_nRetries;
  }

  @Nullable
  public LocalDateTime getPreviousRetryDT ()
  {
    return m_aPreviousRetryDT;
  }

  @Nonnull
  public LocalDateTime getNextRetryDT ()
  {
    return m_aNextRetryDT;
  }

  /**
   * Increment the number of retries and update the previous and the next retry
   * datetime.
   */
  public void incRetryCount ()
  {
    m_nRetries++;
    m_aPreviousRetryDT = PDTFactory.getCurrentLocalDateTime ();
    m_aNextRetryDT = m_aPreviousRetryDT.plusMinutes (PDServerConfiguration.getReIndexRetryMinutes ());
  }

  @Nonnull
  @Nonempty
  public String getLogText ()
  {
    return m_aWorkItem.getLogText () + " " + m_nRetries + " retries so far";
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final ReIndexWorkItem rhs = (ReIndexWorkItem) o;
    return getID ().equals (rhs.getID ());
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (getID ()).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("WorkItem", m_aWorkItem)
                                       .append ("MaxRetryDT", m_aMaxRetryDT)
                                       .append ("Retries", m_nRetries)
                                       .append ("PreviousRetryDT", m_aPreviousRetryDT)
                                       .append ("NextRetryDT", m_aNextRetryDT)
                                       .getToString ();
  }
}
