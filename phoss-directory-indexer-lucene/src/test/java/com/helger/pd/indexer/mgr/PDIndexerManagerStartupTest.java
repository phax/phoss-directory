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
package com.helger.pd.indexer.mgr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.base.concurrent.ThreadHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.pd.indexer.businesscard.PDExtendedBusinessCard;
import com.helger.pd.indexer.index.EIndexerWorkItemType;
import com.helger.pd.indexer.index.IIndexerWorkItem;
import com.helger.pd.indexer.index.IndexerWorkItem;
import com.helger.pd.indexer.lucene.PDLuceneIndexerTestRule;
import com.helger.peppol.businesscard.generic.PDBusinessCard;
import com.helger.peppol.businesscard.generic.PDBusinessEntity;
import com.helger.peppol.businesscard.generic.PDIdentifier;
import com.helger.peppol.businesscard.generic.PDName;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.photon.io.WebFileIO;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.convert.MicroTypeConverter;
import com.helger.xml.microdom.serialize.MicroWriter;

/**
 * Test class for the startup order of {@link PDIndexerManager}.
 *
 * @author Philip Helger
 */
public final class PDIndexerManagerStartupTest
{
  @Rule
  public final TestRule m_aRule = new PDLuceneIndexerTestRule ();

  /**
   * @param aParticipantID
   *        PID
   * @param aErrorHdl
   *        Required for interface compatibility
   * @return Mock BC
   */
  @NonNull
  private static PDExtendedBusinessCard _createMockBC (@NonNull final IParticipantIdentifier aParticipantID,
                                                       @NonNull final Consumer <String> aErrorHdl)
  {
    final PDBusinessCard aBI = new PDBusinessCard ();
    aBI.setParticipantIdentifier (new PDIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME, "9915:mock"));
    final PDBusinessEntity aEntity = new PDBusinessEntity ();
    aEntity.names ().add (new PDName ("Philip's mock Peppol receiver"));
    aEntity.setCountryCode ("AT");
    aEntity.identifiers ().add (new PDIdentifier (aParticipantID.getScheme (), aParticipantID.getValue ()));
    aBI.businessEntities ().add (aEntity);
    return new PDExtendedBusinessCard (aBI,
                                       new CommonsArrayList <> (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getAsDocumentTypeIdentifier ()));
  }

  @Test
  public void testPersistedWorkItemsAreOnlyReadOnStartIndexing () throws IOException
  {
    final IParticipantIdentifier aParticipantID = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:startup");

    // Simulate a shutdown with a non-empty indexer work queue
    final File aWorkItemFile = WebFileIO.getDataIO ().getFile ("indexer-work-items.xml");
    {
      final IIndexerWorkItem aWorkItem = new IndexerWorkItem (aParticipantID,
                                                              EIndexerWorkItemType.CREATE_UPDATE,
                                                              "unit-test",
                                                              PDIndexerManager.HOST_LOCALHOST);
      final IMicroDocument aDoc = new MicroDocument ();
      final IMicroElement eRoot = aDoc.addElement ("root");
      eRoot.addChild (MicroTypeConverter.convertToMicroElement (aWorkItem, "item"));
      assertTrue (MicroWriter.writeToFile (aDoc, aWorkItemFile).isSuccess ());
    }

    // Creating the managers must not touch the persisted work items yet, because the BusinessCard
    // provider might not be present so far
    PDMetaManager.getInstance ();
    final PDIndexerManager aIndexerMgr = PDMetaManager.getIndexerMgr ();
    assertTrue ("The persisted work items must not be read upon construction", aWorkItemFile.exists ());
    assertEquals (0, aIndexerMgr.getIndexerWorkQueue ().getQueueLength ());
    assertFalse (PDMetaManager.getStorageMgr ().containsEntry (aParticipantID));

    // This is what the application does after all the managers were created
    PDMetaManager.setBusinessCardProvider (PDIndexerManagerStartupTest::_createMockBC);
    aIndexerMgr.startIndexing ();

    assertFalse ("The persisted work items must be read when the indexing is started", aWorkItemFile.exists ());

    ThreadHelper.sleep (2000);
    assertTrue (PDMetaManager.getStorageMgr ().containsEntry (aParticipantID));

    // A second invocation must be ignored
    aIndexerMgr.startIndexing ();
  }
}
