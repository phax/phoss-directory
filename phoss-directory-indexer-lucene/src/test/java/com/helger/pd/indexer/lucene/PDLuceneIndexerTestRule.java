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
package com.helger.pd.indexer.lucene;

import com.helger.io.file.FileOperationManager;
import com.helger.pd.indexer.conformance.PDIndexerTestRule;

/**
 * The {@link PDIndexerTestRule} for the Apache Lucene index - it deletes the local index directory
 * of the previous test run.
 *
 * @author Philip Helger
 */
public class PDLuceneIndexerTestRule extends PDIndexerTestRule
{
  @Override
  protected void deletePreviousIndexData ()
  {
    FileOperationManager.INSTANCE.deleteDirRecursiveIfExisting (PDLucene.getLuceneIndexDir ());
  }
}
