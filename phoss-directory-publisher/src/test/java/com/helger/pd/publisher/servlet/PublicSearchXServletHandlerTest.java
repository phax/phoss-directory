/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.servlet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.xml.validation.Validator;

import org.junit.Test;

import com.helger.io.resource.FileSystemResource;
import com.helger.xml.sax.CollectingSAXErrorHandler;
import com.helger.xml.schema.XMLSchemaCache;

/**
 * Test for class {@link PublicSearchXServletHandler}
 *
 * @author Philip Helger
 */
public final class PublicSearchXServletHandlerTest
{
  @Test
  public void testParseXSDExportV1 ()
  {
    // Demo validation
    final CollectingSAXErrorHandler aErrHdl = new CollectingSAXErrorHandler ();
    final Validator v = new XMLSchemaCache (aErrHdl).getValidator (new FileSystemResource ("src/main/webapp/files/directory-export-v1.xsd"));
    assertNotNull (v);
    assertTrue (aErrHdl.getErrorList ().toString (), aErrHdl.getErrorList ().isEmpty ());
  }

  @Test
  public void testParseXSDExportV2 ()
  {
    // Demo validation
    final CollectingSAXErrorHandler aErrHdl = new CollectingSAXErrorHandler ();
    final Validator v = new XMLSchemaCache (aErrHdl).getValidator (new FileSystemResource ("src/main/webapp/files/directory-export-v2.xsd"));
    assertNotNull (v);
    assertTrue (aErrHdl.getErrorList ().toString (), aErrHdl.getErrorList ().isEmpty ());
  }

  @Test
  public void testParseXSDSearch ()
  {
    final CollectingSAXErrorHandler aErrHdl = new CollectingSAXErrorHandler ();
    final Validator v = new XMLSchemaCache (aErrHdl).getValidator (new FileSystemResource ("src/main/webapp/files/directory-search-result-list-v1.xsd"));
    assertNotNull (v);
    assertTrue (aErrHdl.getErrorList ().toString (), aErrHdl.getErrorList ().isEmpty ());
  }
}
