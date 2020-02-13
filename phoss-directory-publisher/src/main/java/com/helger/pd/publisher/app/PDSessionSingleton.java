package com.helger.pd.publisher.app;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.lucene.search.Query;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.web.scope.singleton.AbstractSessionWebSingleton;

public final class PDSessionSingleton extends AbstractSessionWebSingleton
{
  private Query m_aLastQuery;

  @Deprecated
  @UsedViaReflection
  public PDSessionSingleton ()
  {}

  @Nonnull
  public static PDSessionSingleton getInstance ()
  {
    return getSessionSingleton (PDSessionSingleton.class);
  }

  @Nullable
  public Query getLastQuery ()
  {
    return m_aLastQuery;
  }

  public void setLastQuery (@Nullable final Query aLastQuery)
  {
    m_aLastQuery = aLastQuery;
  }
}
