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
package com.helger.pd.publisher.search;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;
import com.helger.base.name.IHasDisplayName;
import com.helger.mime.CMimeType;
import com.helger.mime.IMimeType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Defines the search REST service output format.
 *
 * @author Philip Helger
 */
public enum EPDOutputFormat implements IHasID <String>, IHasDisplayName
{
  XML ("xml", "XML", CMimeType.APPLICATION_XML, ".xml"),
  JSON ("json", "JSON", CMimeType.APPLICATION_JSON, ".json");

  private final String m_sID;
  private final String m_sDisplayName;
  private final IMimeType m_aMimeType;
  private final String m_sFileExtension;

  private EPDOutputFormat (@Nonnull @Nonempty final String sID,
                           @Nonnull @Nonempty final String sDisplayName,
                           @Nonnull final IMimeType aMimeType,
                           @Nonnull @Nonempty final String sFileExtension)
  {
    m_sID = sID;
    m_sDisplayName = sDisplayName;
    m_aMimeType = aMimeType;
    m_sFileExtension = sFileExtension;
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

  /**
   * @return The MIME type to be used for this format. Never <code>null</code>.
   */
  @Nonnull
  public IMimeType getMimeType ()
  {
    return m_aMimeType;
  }

  /**
   * @return The filename extension for this output format. Neither <code>null</code> nor empty and
   *         always starting with a dot!
   */
  @Nonnull
  @Nonempty
  public String getFileExtension ()
  {
    return m_sFileExtension;
  }

  @Nullable
  public static EPDOutputFormat getFromIDCaseInsensitiveOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDCaseInsensitiveOrNull (EPDOutputFormat.class, sID);
  }

  @Nullable
  public static EPDOutputFormat getFromIDCaseInsensitiveOrDefault (@Nullable final String sID,
                                                                   @Nullable final EPDOutputFormat eDefault)
  {
    return EnumHelper.getFromIDCaseInsensitiveOrDefault (EPDOutputFormat.class, sID, eDefault);
  }
}
