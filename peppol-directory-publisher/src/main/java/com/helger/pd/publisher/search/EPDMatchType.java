/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.search;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;
import com.helger.commons.name.IHasDisplayName;
import com.helger.commons.string.StringHelper;

/**
 * Defines the different search match types used in the different query terms.
 *
 * @author Philip Helger
 */
public enum EPDMatchType implements IHasID <String>, IHasDisplayName
{
  EXACT_MATCH_CS ("emcs", "Exact match (case sensitive)", EqualsHelper::equals),
  EXACT_MATCH_CI ("emci", "Exact match (case insensitive)", EqualsHelper::equalsIgnoreCase),
  PARTIAL_MATCH_CI ("pmci",
                    "Partial match (case insensitive)",
                    (x, y) -> StringHelper.hasText (x) && StringHelper.hasText (y) && _unify (x).contains (_unify (y))),
  STARTSWITH_MATCH_CI ("swci",
                       "Starts with match (case insensitive)",
                       (x, y) -> StringHelper.hasText (x) &&
                                 StringHelper.hasText (y) &&
                                 _unify (x).startsWith (_unify (y)));

  private final String m_sID;
  private final String m_sDisplayName;
  private final IPDStringMatcher m_aMatcher;

  @Nullable
  static String _unify (@Nullable final String s)
  {
    return s == null ? null : s.toUpperCase (Locale.US);
  }

  private EPDMatchType (@Nonnull @Nonempty final String sID,
                        @Nonnull @Nonempty final String sDisplayName,
                        @Nonnull final IPDStringMatcher aMatcher)
  {
    m_sID = sID;
    m_sDisplayName = sDisplayName;
    m_aMatcher = aMatcher;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nonnull
  @Nonempty
  public String getDisplayName ()
  {
    return m_sDisplayName;
  }

  public boolean matches (@Nullable final String s1, @Nullable final String s2)
  {
    return m_aMatcher.matches (s1, s2);
  }

  @Nullable
  public static EPDMatchType getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EPDMatchType.class, sID);
  }
}
