/**
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.storage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.pd.businesscard.generic.PDName;

/**
 * This class represents a single multilingual name as stored by Lucene
 * consisting of a name and a language.
 *
 * @author Philip Helger
 */
@Immutable
public final class PDStoredMLName
{
  private final String m_sName;
  private final String m_sLanguageCode;

  public PDStoredMLName (@Nonnull @Nonempty final String sName)
  {
    this (sName, (String) null);
  }

  public PDStoredMLName (@Nonnull @Nonempty final String sName, @Nullable final String sLanguageCode)
  {
    m_sName = ValueEnforcer.notEmpty (sName, "Name");
    m_sLanguageCode = sLanguageCode;
  }

  @Nonnull
  @Nonempty
  public String getName ()
  {
    return m_sName;
  }

  @Nullable
  public String getLanguageCode ()
  {
    return m_sLanguageCode;
  }

  public boolean hasLanguageCode ()
  {
    return StringHelper.hasText (m_sLanguageCode);
  }

  public boolean hasLanguageCode (@Nullable final String sLanguageCode)
  {
    return EqualsHelper.equals (m_sLanguageCode, sLanguageCode);
  }

  @Nonnull
  public PDName getAsGenericObject ()
  {
    return new PDName (m_sName, m_sLanguageCode);
  }

  @Nonnull
  @Nonempty
  public String getNameAndLanguageCode ()
  {
    if (hasLanguageCode ())
      return m_sName + " (" + m_sLanguageCode + ")";
    return m_sName;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PDStoredMLName rhs = (PDStoredMLName) o;
    return m_sName.equals (rhs.m_sName) && EqualsHelper.equals (m_sLanguageCode, rhs.m_sLanguageCode);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sName).append (m_sLanguageCode).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Name", m_sName).appendIfNotNull ("LanguageCode", m_sLanguageCode).getToString ();
  }
}
