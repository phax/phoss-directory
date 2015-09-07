package com.helger.pyp.indexer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;

/**
 * This is the global re-index work queue.
 *
 * @author Philip Helger
 */
@NotThreadSafe
final class ReIndexWorkQueue
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (ReIndexWorkQueue.class);

  private final List <ReIndexWorkItem> m_aList = new ArrayList <> ();

  public ReIndexWorkQueue ()
  {}

  public void addItem (@Nonnull final ReIndexWorkItem aItem)
  {
    ValueEnforcer.notNull (aItem, "Item");

    m_aList.add (aItem);
  }

  @Nonnull
  @ReturnsMutableCopy
  public List <ReIndexWorkItem> expireOldEntries ()
  {
    final List <ReIndexWorkItem> aExpiredItems = m_aList.stream ()
                                                        .filter (i -> i.isExpired ())
                                                        .collect (Collectors.toList ());
    if (!aExpiredItems.isEmpty ())
    {
      for (final ReIndexWorkItem aExpiredItem : aExpiredItems)
        m_aList.remove (aExpiredItem);
      s_aLogger.info ("Expired " + aExpiredItems.size () + " items");
    }
    return aExpiredItems;
  }

  @Nonnull
  @ReturnsMutableCopy
  public List <ReIndexWorkItem> getAllItemsForReIndex (@Nonnull final LocalDateTime aDT)
  {
    return m_aList.stream ().filter (i -> i.isRetryPossible (aDT)).collect (Collectors.toList ());
  }
}
