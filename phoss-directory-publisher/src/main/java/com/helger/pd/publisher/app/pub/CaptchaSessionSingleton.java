/*
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.pd.publisher.app.pub;

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
