package com.helger.pd.indexer.mgr;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.helger.pd.businessinformation.PDExtendedBusinessInformation;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;

/**
 * Test class for class {@link SMPBusinessInformationProvider}.
 *
 * @author Philip Helger
 */
public class SMPBusinessInformationProviderTest
{
  @Test
  public void testFetch ()
  {
    final SMPBusinessInformationProvider aBI = new SMPBusinessInformationProvider ();
    final PDExtendedBusinessInformation aExtBI = aBI.getBusinessInformation (SimpleParticipantIdentifier.createWithDefaultScheme ("9915:test"));
    assertNotNull (aExtBI);
    System.out.println (aExtBI);
  }
}
