/*
 * Copyright (C) 2015-2022 Philip Helger (www.helger.com)
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

import java.io.Serializable;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.name.IHasDisplayName;
import com.helger.commons.type.ITypedObject;
import com.helger.datetime.domain.IHasCreationDateTime;
import com.helger.pd.indexer.storage.PDStoredMetaData;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * Base interface for indexer work item with only reading methods.
 *
 * @author Philip Helger
 */
public interface IIndexerWorkItem extends ITypedObject <String>, Serializable, IHasCreationDateTime, IHasDisplayName
{
  /**
   * Special requesting host if triggered by a scheduled SML run
   *
   * @see #getRequestingHost()
   */
  String REQUESTING_HOST_SML = "automatic";

  /**
   * @return The participant identifier it is all about. May not be
   *         <code>null</code>.
   */
  @Nonnull
  IParticipantIdentifier getParticipantID ();

  /**
   * @return The action type to execute. Never <code>null</code>.
   */
  @Nonnull
  EIndexerWorkItemType getType ();

  /**
   * @return The ID of the client (=SMP; based on the provided client
   *         certificate) that requested this action. Never <code>null</code>.
   */
  @Nonnull
  @Nonempty
  String getOwnerID ();

  /**
   * @return The IP address/host name of the host requesting this work item. If
   *         this action is triggered by the scheduled SML exchange, this should
   *         be <code>{@value #REQUESTING_HOST_SML}</code>.
   */
  @Nonnull
  String getRequestingHost ();

  /**
   * @return A special pre-build log prefix used when logging something about
   *         this object.
   */
  @Nonnull
  @Nonempty
  default String getLogText ()
  {
    return getOwnerID () + "@" + getType () + "[" + getParticipantID ().getURIEncoded () + "]";
  }

  /**
   * @return The information of this as a {@link PDStoredMetaData} object to be
   *         used by the storage engine.
   */
  @Nonnull
  @ReturnsMutableCopy
  default PDStoredMetaData getAsMetaData ()
  {
    return new PDStoredMetaData (getCreationDateTime (), getOwnerID (), getRequestingHost ());
  }

  @Nonnull
  @Nonempty
  default String getDisplayName ()
  {
    return getLogText ();
  }
}
