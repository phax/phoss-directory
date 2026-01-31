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
package com.helger.pd.publisher;

import java.io.File;

import com.helger.base.id.factory.GlobalIDFactory;
import com.helger.base.system.SystemProperties;
import com.helger.pd.indexer.settings.PDServerConfiguration;
import com.helger.photon.app.mock.PhotonAppWebTestRule;
import com.helger.photon.io.WebIOIntIDFactory;
import com.helger.scope.mock.ScopeTestRule;

/**
 * Special Peppol Directory test rule with the correct data path from the settings file.
 *
 * @author Philip Helger
 */
public class PDPublisherTestRule extends PhotonAppWebTestRule
{
  public PDPublisherTestRule ()
  {
    super (new File (PDServerConfiguration.getDataPath ()), ScopeTestRule.STORAGE_PATH.getAbsolutePath ());
  }

  public static void setLocalStackAWSProps ()
  {
    SystemProperties.setPropertyValue ("pd.aws.localstack", "true");
  }

  @Override
  public void before ()
  {
    setLocalStackAWSProps ();
    super.before ();
    GlobalIDFactory.setPersistentIntIDFactory (new WebIOIntIDFactory ("pd-ids.dat"));
  }
}
