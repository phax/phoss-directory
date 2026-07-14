/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.exportall;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.pd.indexer.settings.PDServerConfiguration;

import es.moki.ratelimitj.core.limiter.request.RequestLimitRule;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;
import es.moki.ratelimitj.inmemory.request.InMemorySlidingWindowRequestRateLimiter;

public final class ExportRateLimit
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ExportRateLimit.class);
  public static final ExportRateLimit INSTANCE = new ExportRateLimit ();

  private final RequestRateLimiter m_aRateLimiter;

  private ExportRateLimit ()
  {
    final long nRequestsPerDay = PDServerConfiguration.getExportMaxRequestsPerDay ();
    m_aRateLimiter = new InMemorySlidingWindowRequestRateLimiter (RequestLimitRule.of (Duration.ofHours (24),
                                                                                       nRequestsPerDay));
    LOGGER.info ("Installed export rate limiter: max " + nRequestsPerDay + " requests per IP per file per 24 hours");
  }

  public boolean isOverLimit (final String sKey)
  {
    return m_aRateLimiter.overLimitWhenIncremented (sKey);
  }
}
