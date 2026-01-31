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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.xml.validation.Validator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.io.resource.FileSystemResource;
import com.helger.io.resource.IReadableResource;
import com.helger.pd.publisher.PDPublisherTestRule;
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
}
