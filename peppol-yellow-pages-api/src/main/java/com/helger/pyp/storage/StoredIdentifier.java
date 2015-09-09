/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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
package com.helger.pyp.storage;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;

/**
 * This class represents a single identifier as stored by Lucene consisting of a
 * type and a value.
 *
 * @author Philip Helger
 */
@Immutable
public final class StoredIdentifier
{
  private final String m_sType;
  private final String m_sValue;

  public StoredIdentifier (@Nonnull @Nonempty final String sType, @Nonnull @Nonempty final String sValue)
  {
    m_sType = ValueEnforcer.notEmpty (sType, "Type");
    m_sValue = ValueEnforcer.notEmpty (sValue, "Value");
  }

  @Nonnull
  @Nonempty
  public String getType ()
  {
    return m_sType;
  }

  @Nonnull
  @Nonempty
  public String getValue ()
  {
    return m_sValue;
  }
}
