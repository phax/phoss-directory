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
package com.helger.pd.publisher.exportall;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.validation.Validator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsTreeSet;
import com.helger.datetime.helper.PDTFactory;
import com.helger.io.resource.FileSystemResource;
import com.helger.io.resource.IReadableResource;
import com.helger.pd.indexer.searchindex.PDIndexDocument;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.pd.indexer.storage.field.PDField;
import com.helger.pd.publisher.PDPublisherTestRule;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.xml.sax.CollectingSAXErrorHandler;
import com.helger.xml.schema.XMLSchemaCache;
import com.helger.xml.transform.TransformSourceFactory;

/**
 * Test class for class {@link ExportAllDataJob}.
 * 
 * @author Philip Helger
 */
public final class ExportAllDataJobTest
{
  @Rule
  public final TestRule m_aRule = new PDPublisherTestRule ();

  @Test
  public void testExportAndRead () throws Exception
  {
    // Synchronously export
    ExportAllDataJob.exportAllBusinessCards ();

    final IReadableResource aXSD = new FileSystemResource ("src/main/webapp/files/directory-export-v3.xsd");
    assertTrue (aXSD.exists ());

    final Validator aValidator = XMLSchemaCache.getInstance ().getValidator (aXSD);
    assertNotNull (aValidator);

    final var aHdl = new CollectingSAXErrorHandler ();
    aValidator.setErrorHandler (aHdl);
    aValidator.validate (TransformSourceFactory.create (ExportAllManager.streamBusinessCardXMLFull ()));
    assertTrue (aHdl.getErrorList ().toString (), aHdl.getErrorList ().containsNoError ());

    aHdl.clearResourceErrors ();
    aValidator.validate (TransformSourceFactory.create (ExportAllManager.streamBusinessCardXMLNoDocTypes ()));
    assertTrue (aHdl.getErrorList ().toString (), aHdl.getErrorList ().containsNoError ());
  }

  @Test
  public void testBusinessCardFormatsShareParticipantQueries () throws Exception
  {
    final var aParticipantIDs = new CommonsTreeSet <String> ();
    aParticipantIDs.add (PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:test1")
                                                          .getURIEncoded ());
    aParticipantIDs.add (PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:test2")
                                                          .getURIEncoded ());

    final AtomicInteger aQueryCount = new AtomicInteger ();
    assertTrue (ExportAllManager.writeAllBusinessCardFiles (aParticipantIDs, true, true, true, sParticipantID -> {
      aQueryCount.incrementAndGet ();

      final IParticipantIdentifier aParticipantID = PeppolIdentifierFactory.INSTANCE.parseParticipantIdentifier (sParticipantID);
      assertNotNull (aParticipantID);
      final PDIndexDocument aDoc = new PDIndexDocument ();
      aDoc.add (PDField.PARTICIPANT_ID.getAsField (aParticipantID));
      aDoc.add (PDField.NAME.getAsField ("Test entity"));
      aDoc.add (PDField.COUNTRY_CODE.getAsField ("AT"));
      aDoc.add (PDField.METADATA_CREATIONDT.getAsField (PDTFactory.getCurrentLocalDateTime ()));
      aDoc.add (PDField.METADATA_OWNERID.getAsField ("unit-test"));
      aDoc.add (PDField.METADATA_REQUESTING_HOST.getAsField ("localhost"));
      return new CommonsArrayList <> (PDStoredBusinessEntity.create (aDoc));
    }).isEmpty ());

    // Two participants and four output files must result in two, not eight, index queries
    assertEquals (aParticipantIDs.size (), aQueryCount.get ());
  }
}
