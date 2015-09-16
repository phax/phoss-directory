package com.helger.pyp.indexer.mgr;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.pyp.businessinformation.PYPExtendedBusinessInformation;

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
    final PYPExtendedBusinessInformation aExtBI = aBI.getBusinessInformation (SimpleParticipantIdentifier.createWithDefaultScheme ("9915:test"));
    assertNotNull (aExtBI);
    System.out.println (aExtBI);
  }
}
