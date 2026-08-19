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

import static org.junit.Assume.assumeTrue;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.junit.Rule;
import org.junit.rules.TestRule;

import com.helger.pd.indexer.conformance.AbstractPDStorageManagerConformanceTest;
import com.helger.pd.indexer.conformance.PDIndexerTestRule;
import com.helger.pd.indexer.searchindex.IPDIndex;

/**
 * The storage manager conformance test on top of {@link PDOpenSearchIndex}. It is skipped if no
 * OpenSearch is reachable at {@link PDOpenSearchTestHelper#ENDPOINT_URL} - see
 * <code>docs/opensearch.md</code> for the Docker command line that starts a matching OpenSearch
 * instance.
 *
 * @author Philip Helger
 */
public final class PDOpenSearchStorageManagerConformanceTest extends AbstractPDStorageManagerConformanceTest
{
  @Rule
  public final TestRule m_aRule = new PDIndexerTestRule ();

  @Override
  @NonNull
  protected IPDIndex createIndex () throws IOException
  {
    assumeTrue ("No OpenSearch is reachable at " + PDOpenSearchTestHelper.ENDPOINT_URL,
                PDOpenSearchTestHelper.isTestOpenSearchAvailable ());
    return PDOpenSearchTestHelper.createTestIndex ();
  }
}
