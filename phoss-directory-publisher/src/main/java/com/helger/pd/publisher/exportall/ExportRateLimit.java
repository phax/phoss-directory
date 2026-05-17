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
