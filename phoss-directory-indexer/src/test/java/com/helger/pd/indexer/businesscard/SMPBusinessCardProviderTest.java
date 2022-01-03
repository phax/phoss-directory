/*
 * Copyright (C) 2015-2022 Philip Helger (www.helger.com)
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.function.Supplier;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.url.URLHelper;
import com.helger.httpclient.HttpClientSettings;
import com.helger.json.IJsonObject;
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
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPBusinessCardProviderTest.class);
  private static final Supplier <ICommonsList <ESML>> SML_SUPPLIER = () -> new CommonsArrayList <> (ESML.values ());

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
    assertEquals (1, aExtBI.getBusinessCard ().businessEntities ().size ());
    LOGGER.info (aExtBI.toString ());

    // Test to JSON and back
    final IJsonObject aJson1 = aExtBI.getAsJson ();
    final PDExtendedBusinessCard aExtBC2 = PDExtendedBusinessCard.of (aJson1);
    assertNotNull (aExtBC2);
    assertEquals (aJson1, aExtBC2.getAsJson ());
  }

  @Test
  @Ignore ("Dont hammer server :)")
  public void testFetchRemote ()
  {
    final SMPBusinessCardProvider aBI = SMPBusinessCardProvider.createWithSMLAutoDetect (PDServerConfiguration.getSMPMode (),
                                                                                         PDServerConfiguration.getURLProvider (),
                                                                                         SML_SUPPLIER);
    final PDExtendedBusinessCard aExtBI = aBI.getBusinessCard (PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("0192:816572452"));
    assertNotNull (aExtBI);
    LOGGER.info (aExtBI.toString ());
  }

  @Test
  @Ignore ("Only for on demand testing :)")
  public void testFetchLocal ()
  {
    final SMPBusinessCardProvider aBI = SMPBusinessCardProvider.createWithSMLAutoDetect (PDServerConfiguration.getSMPMode (),
                                                                                         PDServerConfiguration.getURLProvider (),
                                                                                         SML_SUPPLIER);
    final PDExtendedBusinessCard aExtBI = aBI.getBusinessCardPeppolSMP (PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9999:ghx"),
                                                                        new SMPClientReadOnly (URLHelper.getAsURI ("http://localhost:90")),
                                                                        new HttpClientSettings ());
    assertNotNull (aExtBI);
    LOGGER.info (aExtBI.toString ());
  }

  @Test
  @Ignore ("Only for on demand testing :)")
  public void testBabelway ()
  {
    final SMPBusinessCardProvider aBI = SMPBusinessCardProvider.createWithSMLAutoDetect (PDServerConfiguration.getSMPMode (),
                                                                                         PDServerConfiguration.getURLProvider (),
                                                                                         SML_SUPPLIER);
    final PDExtendedBusinessCard aExtBI = aBI.getBusinessCardPeppolSMP (PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9956:0471349823"),
                                                                        new SMPClientReadOnly (URLHelper.getAsURI ("https://int.babelway.net/smp/")),
                                                                        new HttpClientSettings ());
    assertNotNull (aExtBI);
    LOGGER.info (aExtBI.toString ());
  }
}
