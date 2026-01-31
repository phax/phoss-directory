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
package com.helger.pd.indexer.storage;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.peppol.businesscard.generic.PDIdentifier;

/**
 * This class represents a single identifier as stored by Lucene consisting of a
 * type and a value.
 *
 * @author Philip Helger
 */
@Immutable
public final class PDStoredIdentifier
{
  private final String m_sScheme;
  private final String m_sValue;

  public PDStoredIdentifier (@NonNull @Nonempty final String sScheme, @NonNull @Nonempty final String sValue)
  {
    m_sScheme = ValueEnforcer.notEmpty (sScheme, "Scheme");
    m_sValue = ValueEnforcer.notEmpty (sValue, "Value");
  }

  @NonNull
  @Nonempty
  public String getScheme ()
  {
    return m_sScheme;
  }

  @NonNull
  @Nonempty
  public String getValue ()
  {
    return m_sValue;
  }

  @NonNull
  public PDIdentifier getAsGenericObject ()
  {
    return new PDIdentifier (m_sScheme, m_sValue);
  }

  @NonNull
  @Nonempty
  public String getSchemeAndValue ()
  {
    return m_sScheme + "::" + m_sValue;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PDStoredIdentifier rhs = (PDStoredIdentifier) o;
    return m_sScheme.equals (rhs.m_sScheme) && m_sValue.equals (rhs.m_sValue);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sScheme).append (m_sValue).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Scheme", m_sScheme).append ("Value", m_sValue).getToString ();
  }
}
