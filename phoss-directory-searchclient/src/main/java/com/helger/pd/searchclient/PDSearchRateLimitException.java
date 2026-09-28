/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.pd.searchclient;

import java.io.IOException;

/**
 * Thrown if the PD search API rejected a request because the client exceeded the server side rate
 * limit (HTTP 429). The number of seconds the server asked the client to wait is available via
 * {@link #getRetryAfterSeconds()}, so a caller can back off instead of retrying immediately.
 *
 * @author Philip Helger
 * @since 0.19.1
 */
public class PDSearchRateLimitException extends IOException
{
  private final int m_nRetryAfterSeconds;

  /**
   * Constructor.
   *
   * @param sMessage
   *        The error message. May be <code>null</code>.
   * @param nRetryAfterSeconds
   *        The number of seconds to wait before retrying, as taken from the "Retry-After" response
   *        header. Use a value &lt; 0 if the server did not provide one.
   */
  public PDSearchRateLimitException (final String sMessage, final int nRetryAfterSeconds)
  {
    super (sMessage);
    m_nRetryAfterSeconds = nRetryAfterSeconds;
  }

  /**
   * @return The number of seconds to wait before retrying, as provided by the server in the
   *         "Retry-After" response header. A value &lt; 0 means the server did not provide one, in
   *         which case the caller has to pick its own back off delay.
   */
  public int getRetryAfterSeconds ()
  {
    return m_nRetryAfterSeconds;
  }

  /**
   * @return <code>true</code> if the server provided a "Retry-After" value, <code>false</code>
   *         otherwise.
   */
  public boolean hasRetryAfterSeconds ()
  {
    return m_nRetryAfterSeconds >= 0;
  }
}
