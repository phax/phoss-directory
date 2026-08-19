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
package com.helger.pd.indexer.opensearch;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.Immutable;
import com.helger.config.IConfig;
import com.helger.pd.indexer.settings.PDServerConfiguration;

import jakarta.annotation.Nullable;

/**
 * This class contains all the OpenSearch specific configuration properties of the Peppol Directory
 * Server. All properties are read from the central {@link PDServerConfiguration}.
 *
 * @author Philip Helger
 * @since 0.16.0
 */
@Immutable
public final class PDOpenSearchConfiguration
{
  /** The default name of the OpenSearch index to be used */
  public static final String DEFAULT_INDEX_NAME = "phoss-directory";

  /** The default AWS signing service name - "es" is a managed OpenSearch Service domain */
  public static final String DEFAULT_AWS_SERVICE_NAME = "es";

  /** The default number of primary shards of a newly created index */
  public static final int DEFAULT_INDEX_SHARDS = 1;

  /** The default number of replicas of a newly created index */
  public static final int DEFAULT_INDEX_REPLICAS = 1;

  /** The default number of documents read per scroll request */
  public static final int DEFAULT_SCROLL_PAGE_SIZE = 1_000;

  /** The default number of minutes a scroll context is kept alive */
  public static final int DEFAULT_SCROLL_TIMEOUT_MINUTES = 5;

  private PDOpenSearchConfiguration ()
  {}

  @NonNull
  private static IConfig _getConfig ()
  {
    return PDServerConfiguration.getConfig ();
  }

  /**
   * Read value of <code>opensearch.endpoint</code>. This property is mandatory if the OpenSearch
   * index is used.
   *
   * @return The URL of the OpenSearch endpoint, e.g.
   *         <code>https://search-x-y.eu-west-1.es.amazonaws.com</code> or
   *         <code>http://localhost:9200</code>. May be <code>null</code>.
   */
  @Nullable
  public static String getEndpointURL ()
  {
    return _getConfig ().getAsString ("opensearch.endpoint");
  }

  /**
   * Read value of <code>opensearch.index</code>. Defaults to {@link #DEFAULT_INDEX_NAME}.
   *
   * @return The name of the OpenSearch index that contains the business entities.
   */
  @NonNull
  @Nonempty
  public static String getIndexName ()
  {
    return _getConfig ().getAsString ("opensearch.index", DEFAULT_INDEX_NAME);
  }

  /**
   * Read value of <code>opensearch.auth</code>. Defaults to
   * {@link EPDOpenSearchAuthType#AWS_SIGV4}.
   *
   * @return The authentication type to be used. Never <code>null</code>.
   * @throws IllegalStateException
   *         If the configured value is unknown
   */
  @NonNull
  public static EPDOpenSearchAuthType getAuthType ()
  {
    final String sAuthType = _getConfig ().getAsString ("opensearch.auth");
    if (sAuthType == null)
      return EPDOpenSearchAuthType.AWS_SIGV4;

    final EPDOpenSearchAuthType ret = EPDOpenSearchAuthType.getFromIDOrNull (sAuthType);
    if (ret == null)
      throw new IllegalStateException ("The value '" + sAuthType + "' of opensearch.auth is unknown");
    return ret;
  }

  /**
   * Read value of <code>opensearch.aws.region</code>. This property is mandatory if
   * {@link EPDOpenSearchAuthType#AWS_SIGV4} is used.
   *
   * @return The AWS region the OpenSearch domain lives in, e.g. <code>eu-west-1</code>. May be
   *         <code>null</code>.
   */
  @Nullable
  public static String getAWSRegion ()
  {
    return _getConfig ().getAsString ("opensearch.aws.region");
  }

  /**
   * Read value of <code>opensearch.aws.service</code>. Defaults to
   * {@link #DEFAULT_AWS_SERVICE_NAME}.
   *
   * @return The AWS service name to sign the requests for. Use <code>es</code> for a managed AWS
   *         OpenSearch Service domain and <code>aoss</code> for an AWS OpenSearch Serverless
   *         collection.
   */
  @NonNull
  @Nonempty
  public static String getAWSServiceName ()
  {
    return _getConfig ().getAsString ("opensearch.aws.service", DEFAULT_AWS_SERVICE_NAME);
  }

  /**
   * Read value of <code>opensearch.index.autocreate</code>. Defaults to <code>true</code>.
   *
   * @return <code>true</code> if the index should be created with the Peppol Directory mapping in
   *         case it does not exist yet.
   */
  public static boolean isIndexAutoCreate ()
  {
    return _getConfig ().getAsBoolean ("opensearch.index.autocreate", true);
  }

  /**
   * Read value of <code>opensearch.index.shards</code>. Defaults to {@link #DEFAULT_INDEX_SHARDS}.
   * Only relevant if the index is created by this application.
   *
   * @return The number of primary shards of a newly created index. Always &gt; 0.
   */
  @Nonnegative
  public static int getIndexShards ()
  {
    final int ret = _getConfig ().getAsInt ("opensearch.index.shards", DEFAULT_INDEX_SHARDS);
    if (ret <= 0)
      throw new IllegalStateException ("The opensearch.index.shards property must be > 0!");
    return ret;
  }

  /**
   * Read value of <code>opensearch.index.replicas</code>. Defaults to
   * {@link #DEFAULT_INDEX_REPLICAS}. Only relevant if the index is created by this application.
   *
   * @return The number of replicas of a newly created index. Always &ge; 0.
   */
  @Nonnegative
  public static int getIndexReplicas ()
  {
    final int ret = _getConfig ().getAsInt ("opensearch.index.replicas", DEFAULT_INDEX_REPLICAS);
    if (ret < 0)
      throw new IllegalStateException ("The opensearch.index.replicas property must be >= 0!");
    return ret;
  }

  /**
   * Read value of <code>opensearch.scroll.pagesize</code>. Defaults to
   * {@link #DEFAULT_SCROLL_PAGE_SIZE}.
   *
   * @return The number of documents that are read per scroll request when all matching documents
   *         are requested. Always &gt; 0.
   */
  @Nonnegative
  public static int getScrollPageSize ()
  {
    final int ret = _getConfig ().getAsInt ("opensearch.scroll.pagesize", DEFAULT_SCROLL_PAGE_SIZE);
    if (ret <= 0)
      throw new IllegalStateException ("The opensearch.scroll.pagesize property must be > 0!");
    return ret;
  }

  /**
   * Read value of <code>opensearch.scroll.timeout.minutes</code>. Defaults to
   * {@link #DEFAULT_SCROLL_TIMEOUT_MINUTES}.
   *
   * @return The number of minutes a scroll context is kept alive on the server. Always &gt; 0.
   */
  @Nonnegative
  public static int getScrollTimeoutMinutes ()
  {
    final int ret = _getConfig ().getAsInt ("opensearch.scroll.timeout.minutes", DEFAULT_SCROLL_TIMEOUT_MINUTES);
    if (ret <= 0)
      throw new IllegalStateException ("The opensearch.scroll.timeout.minutes property must be > 0!");
    return ret;
  }
}
