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
package com.helger.pd.indexer.index;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;
import com.helger.base.name.IHasDisplayName;

import jakarta.annotation.Nullable;

/**
 * The work item types to use.
 *
 * @author Philip Helger
 */
public enum EIndexerWorkItemType implements IHasID <String>, IHasDisplayName
{
  /** Use for create and/or update of business cards */
  CREATE_UPDATE ("create", "Create/update"),
  /** Use for delete of a business card */
  DELETE ("delete", "Delete"),
  /** Use for internal synchronization (update or delete) of business cards */
  SYNC ("sync", "Synchronize");

  private final String m_sID;
  private final String m_sDisplayName;

  private EIndexerWorkItemType (@NonNull @Nonempty final String sID, @NonNull @Nonempty final String sDisplayName)
  {
    m_sID = sID;
    m_sDisplayName = sDisplayName;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @NonNull
  @Nonempty
  public String getDisplayName ()
  {
    return m_sDisplayName;
  }

  @Nullable
  public static EIndexerWorkItemType getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EIndexerWorkItemType.class, sID);
  }
}
