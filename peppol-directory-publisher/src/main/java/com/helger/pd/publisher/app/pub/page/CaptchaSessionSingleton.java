package com.helger.pd.publisher.app.pub.page;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.web.scope.singleton.AbstractSessionWebSingleton;

public final class CaptchaSessionSingleton extends AbstractSessionWebSingleton
{
  private boolean m_bChecked = false;

  @Deprecated
  @UsedViaReflection
  public CaptchaSessionSingleton ()
  {}

  @Nonnull
  public static CaptchaSessionSingleton getInstance ()
  {
    return getSessionSingleton (CaptchaSessionSingleton.class);
  }

  public boolean isChecked ()
  {
    return m_bChecked;
  }

  public void setChecked ()
  {
    m_bChecked = true;
  }
}
