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
package com.helger.pd.publisher.ui;

import java.util.Locale;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public final class IndefiniteArticleHelper
{
  // Add known exceptions where pronunciation doesn't match the first letter
  private static final String [] AN_EXCEPTIONS = { "honest", "hour", "honor", "honour", "heir" };

  private static final String [] A_EXCEPTIONS = { "eulogy",
                                                  "euphemism",
                                                  "european",
                                                  "one",
                                                  "unicorn",
                                                  "ubiquitous",
                                                  "university" };

  private IndefiniteArticleHelper ()
  {}

  @Nonnull
  public static String getIndefiniteArticle (@Nullable final String sWord)
  {
    if (sWord == null || sWord.isEmpty ())
    {
      // default fallback
      return "a";
    }

    final String sLower = sWord.toLowerCase (Locale.US);

    // Check for explicit "an" exceptions
    for (final String sException : AN_EXCEPTIONS)
      if (sLower.startsWith (sException))
        return "an";

    // Check for explicit "a" exceptions
    for (final String sException : A_EXCEPTIONS)
      if (sLower.startsWith (sException))
        return "a";

    // Check first letter
    final char cFirstChar = sLower.charAt (0);
    if ("aeiou".indexOf (cFirstChar) >= 0)
      return "an";

    return "a";
  }
}
