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
package com.helger.pd.indexer.conformance;

import java.io.File;

import org.slf4j.bridge.SLF4JBridgeHandler;

import com.helger.annotation.style.OverrideOnDemand;
import com.helger.base.id.factory.GlobalIDFactory;
import com.helger.pd.indexer.settings.PDServerConfiguration;
import com.helger.photon.app.mock.PhotonAppWebTestRule;
import com.helger.photon.io.WebIOIntIDFactory;
import com.helger.scope.mock.ScopeTestRule;

/**
 * Special Peppol Directory test rule with the correct data path from the settings file. It is
 * independent of the search index implementation in use - implementations that need to clean up
 * local state before a test should override {@link #deletePreviousIndexData()}.
 *
 * @author Philip Helger
 */
public class PDIndexerTestRule extends PhotonAppWebTestRule
{
  static
  {
    SLF4JBridgeHandler.removeHandlersForRootLogger ();
    SLF4JBridgeHandler.install ();
  }

  public PDIndexerTestRule ()
  {
    super (new File (PDServerConfiguration.getDataPath ()), ScopeTestRule.STORAGE_PATH.getAbsolutePath ());
  }

  /**
   * Delete all the data a previous test run left behind. The default implementation does nothing,
   * because a remote search index has no local state. Search index implementations that store their
   * data locally should override this method.
   */
  @OverrideOnDemand
  protected void deletePreviousIndexData ()
  {}

  @Override
  public void before ()
  {
    super.before ();
    deletePreviousIndexData ();
    GlobalIDFactory.setPersistentIntIDFactory (new WebIOIntIDFactory ("pd-ids.dat"));
  }
}
