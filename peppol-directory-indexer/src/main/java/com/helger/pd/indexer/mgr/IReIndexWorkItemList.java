package com.helger.pd.indexer.mgr;

import java.util.List;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.pd.indexer.domain.ReIndexWorkItem;

public interface IReIndexWorkItemList
{
  @Nonnull
  @ReturnsMutableCopy
  List <ReIndexWorkItem> getAllItems ();
}
