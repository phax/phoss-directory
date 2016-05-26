package com.helger.pd.indexer.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.type.ITypedObject;
import com.helger.datetime.PDTFactory;

public interface IReIndexWorkItem extends ITypedObject <String>, Serializable
{
  /**
   * @return The original work item. Never <code>null</code>.
   */
  @Nonnull
  IndexerWorkItem getWorkItem ();

  /**
   * @return The maximum date and time until which the retry of this item
   *         occurs. Never <code>null</code>.
   */
  @Nonnull
  LocalDateTime getMaxRetryDT ();

  /**
   * @return <code>true</code> if this item is to be expired, because the
   *         retry-time has been exceeded, <code>false</code> otherwise.
   */
  default boolean isExpired ()
  {
    return getMaxRetryDT ().isBefore (PDTFactory.getCurrentLocalDateTime ());
  }

  /**
   * @return The number of retries performed so far. This counter does NOT
   *         include the original try! Always &ge; 0.
   */
  @Nonnegative
  int getRetryCount ();

  /**
   * @return The previous retry date time. If no retry happened so far, this
   *         will be <code>null</code>.
   */
  @Nullable
  LocalDateTime getPreviousRetryDT ();

  /**
   * @return <code>true</code> if a retry has already happened,
   *         <code>false</code> otherwise.
   */
  default boolean hasPreviousRetryDT ()
  {
    return getPreviousRetryDT () != null;
  }

  /**
   * @return The next retry date time. Never <code>null</code>.
   */
  @Nonnull
  LocalDateTime getNextRetryDT ();

  /**
   * @param aDT
   *        The date time to check
   * @return <code>true</code> if the time for the next retry is here.
   */
  default boolean isRetryPossible (@Nonnull final LocalDateTime aDT)
  {
    return getNextRetryDT ().isBefore (aDT);
  }

  @Nonnull
  @Nonempty
  String getLogText ();
}
