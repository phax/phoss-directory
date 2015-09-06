package com.helger.pyp.indexer;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;

public class ReIndexWorkItem
{
  public ReIndexWorkItem (@Nonnull final IndexerWorkItem aWorkItem)
  {
    ValueEnforcer.notNull (aWorkItem, "WorkItem");
  }
}
