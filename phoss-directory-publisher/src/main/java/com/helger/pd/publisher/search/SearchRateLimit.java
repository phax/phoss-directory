/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.search;

import java.time.Duration;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.pd.indexer.settings.PDServerConfiguration;

import es.moki.ratelimitj.core.limiter.request.RequestLimitRule;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;
import es.moki.ratelimitj.inmemory.request.InMemorySlidingWindowRequestRateLimiter;

public final class SearchRateLimit
{
  // Before the Instance, because it is used in the constructor
  private static final Logger LOGGER = LoggerFactory.getLogger (SearchRateLimit.class);
  public static final SearchRateLimit INSTANCE = new SearchRateLimit ();

  private final RequestRateLimiter m_aRequestRateLimiter;

  private SearchRateLimit ()
  {
    final long nRequestsPerSec = PDServerConfiguration.getRESTAPIMaxRequestsPerSecond ();
    if (nRequestsPerSec > 0)
    {
      // 2 request per second, per key
      // Note: duration must be > 1 second
      m_aRequestRateLimiter = new InMemorySlidingWindowRequestRateLimiter (RequestLimitRule.of (Duration.ofSeconds (2),
                                                                                                nRequestsPerSec * 2));
      LOGGER.info ("Installed search rate limiter with a maximum of " + nRequestsPerSec + " requests per second");
    }
    else
    {
      m_aRequestRateLimiter = null;
      LOGGER.info ("Search API runs without limit");
    }
  }

  @Nullable
  public RequestRateLimiter rateLimiter ()
  {
    return m_aRequestRateLimiter;
  }
}
