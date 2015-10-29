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
package com.helger.pd.indexer;

import java.io.File;

import org.slf4j.bridge.SLF4JBridgeHandler;

import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.scope.mock.ScopeTestRule;
import com.helger.pd.indexer.lucene.PDLucene;
import com.helger.pd.settings.PDSettings;
import com.helger.peppol.smpclient.SMPClientConfiguration;
import com.helger.photon.basic.app.io.WebFileIO;
import com.helger.photon.basic.app.io.WebIOIntIDFactory;
import com.helger.photon.basic.mock.PhotonBasicWebTestRule;

/**
 * Special PYP test rule with the correct data path from the settings file.
 *
 * @author Philip Helger
 */
public class PYPIndexerTestRule extends PhotonBasicWebTestRule
{
  static
  {
    SLF4JBridgeHandler.removeHandlersForRootLogger ();
    SLF4JBridgeHandler.install ();
  }

  public PYPIndexerTestRule ()
  {
    super (new File (PDSettings.getDataPath ()), ScopeTestRule.STORAGE_PATH);
  }

  @Override
  public void before ()
  {
    super.before ();
    WebFileIO.getFileOpMgr ().deleteDirRecursiveIfExisting (PDLucene.getLuceneIndexDir ());
    GlobalIDFactory.setPersistentIntIDFactory (new WebIOIntIDFactory ("pyp-ids.dat"));
    // Ensure the network system properties are assigned
    SMPClientConfiguration.getConfigFile ().applyAllNetworkSystemProperties ();
  }
}
