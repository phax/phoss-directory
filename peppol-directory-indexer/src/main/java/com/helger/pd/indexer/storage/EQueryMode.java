package com.helger.pd.indexer.storage;

import javax.annotation.Nonnull;

import org.apache.lucene.search.Query;

import com.helger.commons.functional.IFunction;

public enum EQueryMode
{
  ALL (q -> q),
  NON_DELETED_ONLY (q -> PDQueryManager.andNotDeleted (q)),
  DELETED_ONLY (q -> PDQueryManager.andDeleted (q));

  private final IFunction <Query, Query> m_aQueryModifier;

  private EQueryMode (@Nonnull final IFunction <Query, Query> aQueryModifier)
  {
    m_aQueryModifier = aQueryModifier;
  }

  @Nonnull
  public Query getEffectiveQuery (@Nonnull final Query aQuery)
  {
    return m_aQueryModifier.apply (aQuery);
  }
}
