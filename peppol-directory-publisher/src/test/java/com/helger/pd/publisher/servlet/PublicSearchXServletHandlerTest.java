package com.helger.pd.publisher.servlet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.xml.validation.Validator;

import org.junit.Test;

import com.helger.commons.io.resource.FileSystemResource;
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
  public void testParseXSDExport ()
  {
    // Demo validation
    final CollectingSAXErrorHandler aErrHdl = new CollectingSAXErrorHandler ();
    final Validator v = new XMLSchemaCache (aErrHdl).getValidator (new FileSystemResource ("src/main/webapp/files/directory-export-v1.xsd"));
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
