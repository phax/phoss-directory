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
  public static final SearchRateLimit INSTANCE = new SearchRateLimit ();
  private static final Logger LOGGER = LoggerFactory.getLogger (SearchRateLimit.class);

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
