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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;

import org.apache.hc.core5.http.HttpHost;
import org.jspecify.annotations.NonNull;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;

/**
 * Helper to create a {@link PDOpenSearchIndex} for the local test OpenSearch. See
 * <code>docs/opensearch.md</code> for the Docker command line that starts a matching instance.
 *
 * @author Philip Helger
 */
@Immutable
public final class PDOpenSearchTestHelper
{
  /** The host of the local test OpenSearch */
  public static final String HOST = "localhost";

  /** The port of the local test OpenSearch */
  public static final int PORT = 9200;

  /** The endpoint of the local test OpenSearch */
  public static final String ENDPOINT_URL = "http://" + HOST + ":" + PORT;

  /** The dedicated index name of the tests - never the production one */
  public static final String INDEX_NAME = "phoss-directory-test";

  private static final Logger LOGGER = LoggerFactory.getLogger (PDOpenSearchTestHelper.class);
  private static final int CONNECT_TIMEOUT_MS = 1_000;
  // Only check once per JVM - the result cannot change during a test run
  private static final boolean AVAILABLE = _checkAvailable ();

  private PDOpenSearchTestHelper ()
  {}

  private static boolean _checkAvailable ()
  {
    try (final Socket aSocket = new Socket ())
    {
      aSocket.connect (new InetSocketAddress (HOST, PORT), CONNECT_TIMEOUT_MS);
      LOGGER.info ("Using the OpenSearch at " + ENDPOINT_URL + " for the conformance tests");
      return true;
    }
    catch (final IOException ex)
    {
      LOGGER.warn ("No OpenSearch is reachable at " +
                   ENDPOINT_URL +
                   " - the OpenSearch conformance tests are skipped. See docs/opensearch.md");
      return false;
    }
  }

  /**
   * @return <code>true</code> if an OpenSearch is reachable at {@link #ENDPOINT_URL}, so that the
   *         conformance tests can be executed.
   */
  public static boolean isTestOpenSearchAvailable ()
  {
    return AVAILABLE;
  }

  @NonNull
  public static PDOpenSearchIndex createTestIndex () throws IOException
  {
    final OpenSearchTransport aTransport = ApacheHttpClient5TransportBuilder.builder (HttpHost.create (URI.create (ENDPOINT_URL)))
                                                                            .build ();
    return new PDOpenSearchIndex (aTransport, INDEX_NAME, ENDPOINT_URL);
  }
}
