package com.helger.pd.searchapi;

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.io.file.FileSystemIterator;
import com.helger.pd.searchapi.v1.ResultListType;

/**
 * Test class for class {@link PDResultListMarshaller}.
 *
 * @author Philip Helger
 */
public final class PDResultListMarshallerTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDResultListMarshallerTest.class);

  @Test
  public void testBasic ()
  {
    final PDResultListMarshaller m = new PDResultListMarshaller ();
    for (final File f : new FileSystemIterator (new File ("src/test/resources/external/test-xml/good")))
      if (f.getName ().endsWith (".xml"))
      {
        LOGGER.info ("Reading " + f.getName ());

        final ResultListType res = m.read (f);
        assertNotNull (res);

        final byte [] bytes = m.getAsBytes (res);
        assertNotNull (bytes);
      }
  }
}
