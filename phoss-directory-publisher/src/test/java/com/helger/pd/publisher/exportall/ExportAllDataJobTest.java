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
    aValidator.validate (TransformSourceFactory.create (ExportAllManager._getInternalFileBusinessCardXMLFull ()));
    assertTrue (aHdl.getErrorList ().toString (), aHdl.getErrorList ().containsNoError ());

    aHdl.clearResourceErrors ();
    aValidator.validate (TransformSourceFactory.create (ExportAllManager._getInternalFileBusinessCardXMLNoDocTypes ()));
    assertTrue (aHdl.getErrorList ().toString (), aHdl.getErrorList ().containsNoError ());
  }
}
