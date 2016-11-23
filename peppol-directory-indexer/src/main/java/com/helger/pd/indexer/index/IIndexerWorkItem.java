/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
import com.helger.commons.type.ITypedObject;
import com.helger.datetime.domain.IHasCreationDateTime;
import com.helger.pd.indexer.storage.PDDocumentMetaData;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;

/**
 * Base interface for indexer work item with only reading methods.
 *
 * @author Philip Helger
 */
public interface IIndexerWorkItem extends ITypedObject <String>, Serializable, IHasCreationDateTime
{
  /**
   * @return The participant identifier it is all about.
   */
  @Nonnull
  IParticipantIdentifier getParticipantID ();

  /**
   * @return The action type to execute. Never <code>null</code>.
   */
  @Nonnull
  EIndexerWorkItemType getType ();

  /**
   * @return The ID of the client (based on the provided certificate) that
   *         requested this action. Never <code>null</code>.
   */
  @Nonnull
  @Nonempty
  String getOwnerID ();

  /**
   * @return The IP address/host name of the host requesting this work item. If
   *         this action is triggered by the scheduled SML exchange, this should
   *         be <code>automatic</code>.
   */
  @Nonnull
  String getRequestingHost ();

  @Nonnull
  @Nonempty
  default String getLogText ()
  {
    return getOwnerID () + "@" + getType () + "[" + getParticipantID ().getURIEncoded () + "]";
  }

  @Nonnull
  @ReturnsMutableCopy
  default PDDocumentMetaData getAsMetaData ()
  {
    return new PDDocumentMetaData (getCreationDateTime (), getOwnerID (), getRequestingHost ());
  }
}
