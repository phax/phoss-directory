package com.helger.pyp.indexer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.joda.time.LocalDateTime;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.string.ToStringGenerator;

/**
 * This is the global re-index work queue.
 *
 * @author Philip Helger
 */
@NotThreadSafe
final class ReIndexWorkItemList
{
  private final List <ReIndexWorkItem> m_aList = new ArrayList <> ();

  public ReIndexWorkItemList ()
  {}

  public void addItem (@Nonnull final ReIndexWorkItem aItem)
  {
    ValueEnforcer.notNull (aItem, "Item");

    m_aList.add (aItem);
  }

  @Nonnull
  @ReturnsMutableCopy
  public List <ReIndexWorkItem> getAndRemoveAllExpiredEntries ()
  {
    final Predicate <ReIndexWorkItem> aPredicate = i -> i.isExpired ();
    final List <ReIndexWorkItem> ret = m_aList.stream ().filter (aPredicate).collect (Collectors.toList ());
    m_aList.remove (aPredicate);
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public List <ReIndexWorkItem> getAndRemoveAllItemsForReIndex (@Nonnull final LocalDateTime aDT)
  {
    final Predicate <ReIndexWorkItem> aPredicate = i -> i.isRetryPossible (aDT);
    final List <ReIndexWorkItem> ret = m_aList.stream ().filter (aPredicate).collect (Collectors.toList ());
    m_aList.removeIf (aPredicate);
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public List <ReIndexWorkItem> getAllItems ()
  {
    return CollectionHelper.newList (m_aList);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("List", m_aList).toString ();
  }
}
