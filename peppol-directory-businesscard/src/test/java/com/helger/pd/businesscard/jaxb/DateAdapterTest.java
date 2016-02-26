/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.pd.businesscard.jaxb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.joda.time.LocalDate;
import org.junit.Test;

import com.helger.datetime.PDTFactory;
import com.helger.pd.businesscard.jaxb.DateAdapter;

/**
 * Test class for class {@link DateAdapter}.
 *
 * @author Philip Helger
 */
public final class DateAdapterTest
{
  @Test
  public void testBasic ()
  {
    assertNull (DateAdapter.getLocalDateAsStringXSD (null));
    assertNull (DateAdapter.getLocalDateFromXSD (null));

    final LocalDate aLocalDate = PDTFactory.getCurrentLocalDate ();
    final String s = DateAdapter.getLocalDateAsStringXSD (aLocalDate);
    assertNotNull (s);
    final LocalDate aBack = DateAdapter.getLocalDateFromXSD (s);
    assertNotNull (aBack);
    assertEquals (aLocalDate, aBack);
  }
}
