package com.helger.pyp.indexer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.scope.singleton.AbstractGlobalSingleton;

/**
 * This is the global re-index work queue.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class ReIndexWorkQueue extends AbstractGlobalSingleton
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (ReIndexWorkQueue.class);

  private final List <ReIndexWorkItem> m_aList = new ArrayList <> ();

  @Deprecated
  @UsedViaReflection
  public ReIndexWorkQueue ()
  {}

  @Nonnull
  public static ReIndexWorkQueue getInstance ()
  {
    return getGlobalSingleton (ReIndexWorkQueue.class);
  }

  public void addItem (@Nonnull final ReIndexWorkItem aItem)
  {
    ValueEnforcer.notNull (aItem, "Item");

    m_aRWLock.writeLock ().lock ();
    try
    {
      m_aList.add (aItem);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
  }

  public void expireOldEntries ()
  {
    m_aRWLock.writeLock ().lock ();
    try
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
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
  }
}
