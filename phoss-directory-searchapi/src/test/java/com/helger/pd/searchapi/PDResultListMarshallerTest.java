/*
 * Copyright (C) 2019-2024 Philip Helger (www.helger.com)
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
