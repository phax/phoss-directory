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
package com.helger.pd.publisher.ui;

import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Peppol country helper
 *
 * @author Philip Helger
 */
public final class PACountryCodeHelper
{
  public static final String AU = "AU";
  public static final String BE = "BE";
  public static final String DE = "DE";
  public static final String DK = "DK";
  public static final String FR = "FR";
  public static final String JP = "JP";
  public static final String NZ = "NZ";

  private PACountryCodeHelper ()
  {}

  @Nullable
  public static String getCountryCode (@NonNull final String sValue)
  {
    final String sUCValue = sValue.toUpperCase (Locale.ROOT);

    if (sUCValue.startsWith ("0088:93") || sUCValue.startsWith ("0151:"))
      return AU;

    if (sUCValue.startsWith ("0088:54") ||
        sUCValue.startsWith ("0208:") ||
        sUCValue.startsWith ("9925:") ||
        sUCValue.startsWith ("9918:BE") ||
        sUCValue.startsWith ("9956"))
      return BE;

    if (sUCValue.startsWith ("0088:40") ||
        sUCValue.startsWith ("0088:41") ||
        sUCValue.startsWith ("0088:42") ||
        sUCValue.startsWith ("0088:43") ||
        sUCValue.startsWith ("0088:44") ||
        sUCValue.startsWith ("0204:") ||
        sUCValue.startsWith ("9918:DE") ||
        sUCValue.startsWith ("9930:") ||
        sUCValue.startsWith ("9958:"))
      return DE;

    if (sUCValue.startsWith ("0096:") ||
        sUCValue.startsWith ("0184:") ||
        sUCValue.startsWith ("0198:") ||
        sUCValue.startsWith ("9901:") ||
        sUCValue.startsWith ("9902:") ||
        sUCValue.startsWith ("9904:") ||
        sUCValue.startsWith ("9905:") ||
        sUCValue.startsWith ("9918:DK"))
      return DK;

    if (sUCValue.startsWith ("0002:") ||
        sUCValue.startsWith ("0009:") ||
        sUCValue.startsWith ("0088:30") ||
        sUCValue.startsWith ("0088:31") ||
        sUCValue.startsWith ("0088:32") ||
        sUCValue.startsWith ("0088:33") ||
        sUCValue.startsWith ("0088:34") ||
        sUCValue.startsWith ("0088:35") ||
        sUCValue.startsWith ("0088:36") ||
        sUCValue.startsWith ("0088:37") ||
        sUCValue.startsWith ("0225:") ||
        sUCValue.startsWith ("9957:"))
      return FR;

    if (sUCValue.startsWith ("0088:45") || sUCValue.startsWith ("0188:") || sUCValue.startsWith ("0221:"))
      return JP;

    if (sUCValue.startsWith ("0088:94"))
      return NZ;

    return null;
  }
}
