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

import javax.xml.validation.Validator;

import org.jspecify.annotations.NonNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.io.resource.FileSystemResource;
import com.helger.io.resource.IReadableResource;
import com.helger.pd.publisher.PDPublisherTestRule;
import com.helger.photon.audit.AbstractAuditor;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.audit.IAuditItem;
import com.helger.photon.audit.IAuditor;
import com.helger.photon.audit.mock.MockCurrentUserIDProvider;
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
  /**
   * An auditor that simply remembers all created audit items.
   *
   * @author Philip Helger
   */
  private static final class CollectingAuditor extends AbstractAuditor
  {
    private final ICommonsList <IAuditItem> m_aItems = new CommonsArrayList <> ();

    public CollectingAuditor ()
    {
      super (MockCurrentUserIDProvider.getInstance ());
    }

    @Override
    protected void handleAuditItem (@NonNull final IAuditItem aAuditItem)
    {
      m_aItems.add (aAuditItem);
    }

    @NonNull
    public ICommonsList <IAuditItem> getAllItems ()
    {
      return m_aItems.getClone ();
    }
  }

  @Rule
  public final TestRule m_aRule = new PDPublisherTestRule ();

  @Test
  public void testAuditItems ()
  {
    final CollectingAuditor aAuditor = new CollectingAuditor ();
    final IAuditor aOldAuditor = AuditHelper.getAuditor ();
    AuditHelper.setAuditor (aAuditor);
    try
    {
      // Synchronously export
      ExportAllDataJob.exportAllBusinessCards ();
    }
    finally
    {
      AuditHelper.setAuditor (aOldAuditor);
    }

    // Start, both intermediate steps and end - in that order
    final ICommonsList <String> aActions = aAuditor.getAllItems ().getAllMapped (IAuditItem::getAction);
    assertEquals (4, aActions.size ());
    assertTrue (aActions.toString (), aActions.get (0).contains (ExportAllDataJob.AUDIT_ACTION_START));
    assertTrue (aActions.toString (), aActions.get (1).contains (ExportAllDataJob.AUDIT_ACTION_PARTICIPANT_IDS));
    assertTrue (aActions.toString (), aActions.get (2).contains (ExportAllDataJob.AUDIT_ACTION_FORMATS));
    assertTrue (aActions.toString (), aActions.get (3).contains (ExportAllDataJob.AUDIT_ACTION_END));

    // Every audit item contains the overall duration
    for (final String sAction : aActions)
      assertTrue (sAction, sAction.contains ("PT"));
  }

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
}
