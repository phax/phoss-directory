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
package com.helger.pyp.indexer.rest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.pyp.indexer.EIndexerWorkItemType;
import com.helger.pyp.indexer.IndexerManager;

/**
 * Indexer resource (exposed at "1.0" path)
 *
 * @author Philip Helger
 */
@Path ("1.0")
public class IndexerResource
{
  /**
   * Check if the current request contains a client certificate.
   *
   * @param aHttpServletRequest
   *        The current servlet request.
   * @return A non-<code>null</code> error {@link Response} if no valid client
   *         certificate is present, <code>null</code> if everything is fine.
   */
  @Nullable
  private static Response _checkClientCertificate (@Nonnull final HttpServletRequest aHttpServletRequest)
  {
    boolean bValid = false;
    try
    {
      bValid = ClientCertificateValidator.isClientCertificateValid (aHttpServletRequest);
    }
    catch (final RuntimeException ex)
    {}

    if (!bValid)
      return Response.status (Response.Status.FORBIDDEN).build ();

    return null;
  }

  @PUT
  public Response createOrUpdateParticipant (@Context @Nonnull final HttpServletRequest aHttpServletRequest,
                                             @Nonnull final String sParticipantID)
  {
    final Response aResponse = _checkClientCertificate (aHttpServletRequest);
    if (aResponse != null)
      return aResponse;

    // Parse identifier
    final SimpleParticipantIdentifier aPI = SimpleParticipantIdentifier.createFromURIPart (sParticipantID);

    // Queue for handling
    IndexerManager.getInstance ().queueWorkItem (aPI, EIndexerWorkItemType.CREATE_UPDATE);

    // And done
    return Response.noContent ().build ();
  }
}
