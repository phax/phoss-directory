package com.helger.pyp.indexer;

import java.io.File;

import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.scope.mock.ScopeTestRule;
import com.helger.peppol.smpclient.SMPClientConfiguration;
import com.helger.photon.basic.app.io.WebIOIntIDFactory;
import com.helger.photon.basic.mock.PhotonBasicWebTestRule;
import com.helger.pyp.indexer.mgr.PYPMetaManager;
import com.helger.pyp.settings.PYPSettings;

/**
 * Special PYP test rule with the correct data path from the settings file.
 *
 * @author Philip Helger
 */
public class PYPTestRule extends PhotonBasicWebTestRule
{
  public PYPTestRule ()
  {
    super (new File (PYPSettings.getDataPath ()), ScopeTestRule.STORAGE_PATH);
  }

  @Override
  public void before ()
  {
    super.before ();
    GlobalIDFactory.setPersistentIntIDFactory (new WebIOIntIDFactory ("pyp-ids.dat"));
    // Ensure the network system properties are assigned
    SMPClientConfiguration.getConfigFile ().applyAllNetworkSystemProperties ();
    // Initialize managers
    PYPMetaManager.getInstance ();
  }
}
