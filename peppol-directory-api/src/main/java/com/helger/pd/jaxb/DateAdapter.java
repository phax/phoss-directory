/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
 */
package com.helger.pd.jaxb;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.helger.commons.annotation.PresentForCodeCoverage;
import com.helger.datetime.format.PDTFromString;

/**
 * This class is used for converting between XML time elements and Java Date
 * objects. It is internally used in the JAXB bindings.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Immutable
public final class DateAdapter
{
  @PresentForCodeCoverage
  private static final DateAdapter s_aInstance = new DateAdapter ();

  private DateAdapter ()
  {}

  @Nullable
  public static LocalDate getLocalDateFromXSD (@Nullable final String sValue)
  {
    final DateTimeFormatter aDTF = ISODateTimeFormat.dateParser ();
    return PDTFromString.getLocalDateFromString (sValue, aDTF);
  }

  @Nullable
  public static String getLocalDateAsStringXSD (@Nullable final LocalDate aLocalDate)
  {
    return aLocalDate == null ? null : ISODateTimeFormat.date ().withOffsetParsed ().print (aLocalDate);
  }
}