/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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

package com.helger.pd.businesscard.generic;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

/**
 * Generic name.
 *
 * @author Philip Helger
 */
@Immutable
public class PDName implements Serializable
{
  private final String m_sName;
  private final String m_sLanguage;

  public static boolean isValidLanguage (@Nullable final String s)
  {
    return s == null || (s.length () == 2 && LanguageCache.getInstance ().containsLanguage (s));
  }

  public PDName (@Nonnull @Nonempty final String sName)
  {
    this (sName, (String) null);
  }

  public PDName (@Nonnull @Nonempty final String sName, @Nullable final String sLanguage)
  {
    ValueEnforcer.notEmpty (sName, "Name");
    ValueEnforcer.isTrue (isValidLanguage (sLanguage), () -> "'" + sLanguage + "' is invalid language");
    m_sName = sName;
    m_sLanguage = sLanguage;
  }

  /**
   * @return The name. May be <code>null</code>.
   */
  @Nonnull
  @Nonempty
  public String getScheme ()
  {
    return m_sName;
  }

  /**
   * @return The language. May be <code>null</code>.
   */
  @Nullable
  public String getLanguage ()
  {
    return m_sLanguage;
  }

  @Nonnull
  public IMicroElement getAsMicroXML (@Nullable final String sNamespaceURI,
                                      @Nonnull @Nonempty final String sElementName)
  {
    final IMicroElement ret = new MicroElement (sNamespaceURI, sElementName);
    ret.setAttribute ("name", m_sName);
    ret.setAttribute ("language", m_sLanguage);
    return ret;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;

    final PDName rhs = (PDName) o;
    return m_sName.equals (rhs.m_sName) && EqualsHelper.equals (m_sLanguage, rhs.m_sLanguage);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sName).append (m_sLanguage).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Name", m_sName)
                                       .appendIfNotNull ("Language", m_sLanguage)
                                       .getToString ();
  }
}
