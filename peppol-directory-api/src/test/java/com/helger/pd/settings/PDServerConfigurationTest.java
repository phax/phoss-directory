package com.helger.pd.settings;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.ICommonsList;

/**
 * Test class for class {@link PDServerConfiguration}.
 *
 * @author Philip Helger
 */
public final class PDServerConfigurationTest
{
  @Test
  public void testClientCerts ()
  {
    final ICommonsList <String> aList = PDServerConfiguration.getAllClientCertIssuer ();
    assertEquals (4, aList.size ());
    // Check if all items are unique
    assertEquals (4, new CommonsHashSet <> (aList).size ());
    assertEquals ("CN=PEPPOL SERVICE METADATA PUBLISHER CA, O=NATIONAL IT AND TELECOM AGENCY, C=DK", aList.get (0));
    assertEquals ("CN=PEPPOL SERVICE METADATA PUBLISHER TEST CA, OU=FOR TEST PURPOSES ONLY, O=NATIONAL IT AND TELECOM AGENCY, C=DK",
                  aList.get (1));
    assertEquals ("CN=PEPPOL SERVICE METADATA PUBLISHER CA - G2,O=OpenPEPPOL AISBL,C=BE", aList.get (2));
    assertEquals ("CN=PEPPOL SERVICE METADATA PUBLISHER TEST CA - G2,OU=FOR TEST ONLY,O=OpenPEPPOL AISBL,C=BE",
                  aList.get (3));
  }

  @Test
  public void testTrustStores ()
  {
    final ICommonsList <PDConfiguredTrustStore> aList = PDServerConfiguration.getAllTrustStores ();
    assertEquals (4, aList.size ());
  }
}
