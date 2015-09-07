package com.helger.pyp.indexer;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.joda.time.LocalDateTime;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.string.ToStringGenerator;
import com.helger.datetime.PDTFactory;
import com.helger.pyp.settings.PYPSettings;

/**
 * This class holds a single item to be re-indexed. It is only invoked if
 * regular indexing failed.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class ReIndexWorkItem
{
  private final IndexerWorkItem m_aWorkItem;
  private final LocalDateTime m_aMaxRetryDT;
  private int m_nRetries;
  private LocalDateTime m_aPreviousRetryDT;
  private LocalDateTime m_aNextRetryDT;

  public ReIndexWorkItem (@Nonnull final IndexerWorkItem aWorkItem)
  {
    // The next retry happens from now in the configured number of minutes
    this (aWorkItem,
          aWorkItem.getCreationDT ().plusHours (PYPSettings.getReIndexMaxRetryHours ()),
          0,
          (LocalDateTime) null,
          PDTFactory.getCurrentLocalDateTime ().plusMinutes (PYPSettings.getReIndexRetryMinutes ()));
  }

  ReIndexWorkItem (@Nonnull final IndexerWorkItem aWorkItem,
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

  /**
   * @return <code>true</code> if this item is to be expired, because the
   *         retry-time has been exceeded.
   */
  public boolean isExpired ()
  {
    return m_aMaxRetryDT.isBefore (PDTFactory.getCurrentLocalDateTime ());
  }

  /**
   * @param aDT
   *        The date time to check
   * @return <code>true</code> if the time for the next retry is here.
   */
  public boolean isRetryPossible (@Nonnull final LocalDateTime aDT)
  {
    return m_aNextRetryDT.isBefore (aDT);
  }

  public void incRetryCount ()
  {
    m_nRetries++;
    m_aPreviousRetryDT = PDTFactory.getCurrentLocalDateTime ();
    m_aNextRetryDT = m_aPreviousRetryDT.plusMinutes (PYPSettings.getReIndexRetryMinutes ());
  }

  /**
   * @return The original work item. Never <code>null</code>.
   */
  @Nonnull
  public IndexerWorkItem getWorkItem ()
  {
    return m_aWorkItem;
  }

  /**
   * @return The maximum date and time until which the retry of this item
   *         occurs.
   */
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

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("WorkItem", m_aWorkItem)
                                       .append ("MaxRetryDT", m_aMaxRetryDT)
                                       .append ("Retries", m_nRetries)
                                       .append ("PreviousRetryDT", m_aPreviousRetryDT)
                                       .append ("NextRetryDT", m_aNextRetryDT)
                                       .toString ();
  }
}
