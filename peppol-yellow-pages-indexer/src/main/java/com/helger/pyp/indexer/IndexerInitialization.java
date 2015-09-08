/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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
package com.helger.pyp.indexer;

import com.helger.peppol.smpclient.SMPClientConfiguration;
import com.helger.pyp.storage.PYPStorageManager;

/**
 * Initialize every necessary to get the PYP Indexer up and running
 *
 * @author Philip Helger
 */
public final class IndexerInitialization
{
  private IndexerInitialization ()
  {}

  public static void initIndexer ()
  {
    // Ensure the network system properties are assigned
    SMPClientConfiguration.getConfigFile ().applyAllNetworkSystemProperties ();
    // Initialize managers
    IndexerManager.getInstance ();
    PYPStorageManager.getInstance ();
  }
}
