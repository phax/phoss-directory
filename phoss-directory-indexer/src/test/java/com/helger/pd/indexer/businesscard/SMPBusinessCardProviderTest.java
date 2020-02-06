/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.businesscard;

import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.functional.ISupplier;
import com.helger.commons.url.URLHelper;
import com.helger.pd.indexer.settings.PDServerConfiguration;
import com.helger.peppol.sml.ESML;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.photon.app.mock.PhotonAppWebTestRule;
import com.helger.smpclient.peppol.SMPClientReadOnly;

/**
 * Test class for class {@link SMPBusinessCardProvider}.
 *
 * @author Philip Helger
 */
public final class SMPBusinessCardProviderTest
{
  private static final Logger LOG = LoggerFactory.getLogger (SMPBusinessCardProviderTest.class);
  private static final ISupplier <ICommonsList <ESML>> SML_SUPPLIER = () -> new CommonsArrayList <> (ESML.values ());

  @Rule
  public final TestRule m_aRule = new PhotonAppWebTestRule ();

  @Test
  public void testFetch ()
  {
    final SMPBusinessCardProvider aBI = SMPBusinessCardProvider.createWithSMLAutoDetect (PDServerConfiguration.getSMPMode (),
                                                                                         PDServerConfiguration.getURLProvider (),
                                                                                         SML_SUPPLIER);
    final PDExtendedBusinessCard aExtBI = aBI.getBusinessCard (PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:test"));
    assertNotNull (aExtBI);
    LOG.info (aExtBI.toString ());
  }

  @Test
  @Ignore ("Dont hammer server :)")
  public void test9905LeckmaPeppol ()
  {
    final SMPBusinessCardProvider aBI = SMPBusinessCardProvider.createWithSMLAutoDetect (PDServerConfiguration.getSMPMode (),
                                                                                         PDServerConfiguration.getURLProvider (),
                                                                                         SML_SUPPLIER);
    final PDExtendedBusinessCard aExtBI = aBI.getBusinessCard (PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9905:leckma-peppol"));
    assertNotNull (aExtBI);
    LOG.info (aExtBI.toString ());
  }

  @Test
  @Ignore ("Only for on demand testing :)")
  public void testFetchLocal ()
  {
    final SMPBusinessCardProvider aBI = SMPBusinessCardProvider.createWithSMLAutoDetect (PDServerConfiguration.getSMPMode (),
                                                                                         PDServerConfiguration.getURLProvider (),
                                                                                         SML_SUPPLIER);
    final PDExtendedBusinessCard aExtBI = aBI.getBusinessCardPeppolSMP (PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9999:ghx"),
                                                                        new SMPClientReadOnly (URLHelper.getAsURI ("http://localhost:90")));
    assertNotNull (aExtBI);
    LOG.info (aExtBI.toString ());
  }

  @Test
  @Ignore ("Only for on demand testing :)")
  public void testBabelway ()
  {
    final SMPBusinessCardProvider aBI = SMPBusinessCardProvider.createWithSMLAutoDetect (PDServerConfiguration.getSMPMode (),
                                                                                         PDServerConfiguration.getURLProvider (),
                                                                                         SML_SUPPLIER);
    final PDExtendedBusinessCard aExtBI = aBI.getBusinessCardPeppolSMP (PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9956:0471349823"),
                                                                        new SMPClientReadOnly (URLHelper.getAsURI ("https://int.babelway.net/smp/")));
    assertNotNull (aExtBI);
    LOG.info (aExtBI.toString ());
  }
}
