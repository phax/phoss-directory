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
package com.helger.pd.indexer.rest;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.equals.EqualsHelper;
import com.helger.pd.indexer.clientcert.ClientCertificateValidationResult;
import com.helger.pd.indexer.clientcert.ClientCertificateValidator;
import com.helger.pd.indexer.index.EIndexerWorkItemType;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.url.codec.URLCoder;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * Indexer resource (exposed at "/1.0" path)
 *
 * @author Philip Helger
 */
@Path ("1.0")
public class IndexerResource
{
  private static final Logger LOGGER = LoggerFactory.getLogger (IndexerResource.class);

  /**
   * Check if the current request contains a client certificate and whether it is valid.
   *
   * @param aHttpServletRequest
   *        The current servlet request. May not be <code>null</code>.
   * @param sLogPrefix
   *        The context - for logging only. May not be <code>null</code>.
   * @return The validation result
   */
  @Nonnull
  private static ClientCertificateValidationResult _checkClientCertificate (@Nonnull final HttpServletRequest aHttpServletRequest,
                                                                            @Nonnull final String sLogPrefix)
  {
    try
    {
      return ClientCertificateValidator.verifyClientCertificate (aHttpServletRequest, sLogPrefix);
    }
    catch (final Exception ex)
    {
      // Use Throwable to track "class load error" issue
      LOGGER.warn (sLogPrefix + "Error validating client certificate", ex);
    }
    return ClientCertificateValidationResult.createFailure ();
  }

  @Nonnull
  @Nonempty
  private static String _getRequestingHost (@Nonnull final HttpServletRequest aHttpServletRequest)
  {
    final String sRemoteAddr = aHttpServletRequest.getRemoteAddr ();
    final String sRemoteHost = aHttpServletRequest.getRemoteHost ();
    if (EqualsHelper.equals (sRemoteAddr, sRemoteHost))
      return sRemoteAddr;
    return sRemoteAddr + '/' + sRemoteHost;
  }

  @Nullable
  private static String _unifyPID (@Nullable final String sParticipantID)
  {
    if (sParticipantID == null)
      return null;

    final String sTrimmed = sParticipantID.trim ();
    return URLCoder.urlDecodeOrDefault (sTrimmed, sTrimmed);
  }

  @PUT
  public Response createOrUpdateParticipant (@Context @Nonnull final HttpServletRequest aHttpServletRequest,
                                             @Nonnull final String sParticipantID)
  {
    final String sLogPrefix = "[createOrUpdateParticipant] ";
    final ClientCertificateValidationResult aCertValidationResult = _checkClientCertificate (aHttpServletRequest,
                                                                                             sLogPrefix);
    if (aCertValidationResult.isFailure ())
      return Response.status (Response.Status.FORBIDDEN).build ();

    final String sRealParticipantID = _unifyPID (sParticipantID);

    LOGGER.info (sLogPrefix + "'" + sRealParticipantID + "'");

    // Parse identifier
    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();
    final IParticipantIdentifier aPI = aIdentifierFactory.parseParticipantIdentifier (sRealParticipantID);
    if (aPI == null)
    {
      LOGGER.error (sLogPrefix + "Failed to parse participant identifier '" + sRealParticipantID + "'");
      return Response.status (Status.BAD_REQUEST).build ();
    }
    // Queue for handling
    if (PDMetaManager.getIndexerMgr ()
                     .queueWorkItem (aPI,
                                     EIndexerWorkItemType.CREATE_UPDATE,
                                     aCertValidationResult.getClientID (),
                                     _getRequestingHost (aHttpServletRequest))
                     .isUnchanged ())
    {
      LOGGER.info (sLogPrefix + "Ignoring duplicate CREATE/UPDATE request for '" + aPI.getURIEncoded () + "'");
    }
    // And done
    return Response.noContent ().build ();
  }

  @DELETE
  @Path ("{participantID}")
  public Response deleteParticipant (@Context @Nonnull final HttpServletRequest aHttpServletRequest,
                                     @PathParam ("participantID") @Nonnull final String sParticipantID)
  {
    final String sLogPrefix = "[deleteParticipant] ";
    final ClientCertificateValidationResult aCertValidationResult = _checkClientCertificate (aHttpServletRequest,
                                                                                             sLogPrefix);
    if (aCertValidationResult.isFailure ())
      return Response.status (Response.Status.FORBIDDEN).build ();

    final String sRealParticipantID = _unifyPID (sParticipantID);

    LOGGER.info (sLogPrefix + "'" + sRealParticipantID + "'");

    // Parse identifier
    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();
    final IParticipantIdentifier aPI = aIdentifierFactory.parseParticipantIdentifier (sRealParticipantID);
    if (aPI == null)
    {
      LOGGER.error (sLogPrefix + "Failed to parse participant identifier '" + sRealParticipantID + "'");
      return Response.status (Status.BAD_REQUEST).build ();
    }
    // Don't check for existence of the PI as it might be in the queue for
    // creation

    // Queue for handling
    if (PDMetaManager.getIndexerMgr ()
                     .queueWorkItem (aPI,
                                     EIndexerWorkItemType.DELETE,
                                     aCertValidationResult.getClientID (),
                                     _getRequestingHost (aHttpServletRequest))
                     .isUnchanged ())
    {
      LOGGER.info (sLogPrefix + "Ignoring duplicate DELETE request for '" + aPI.getURIEncoded () + "'");
    }
    // And done
    return Response.noContent ().build ();
  }

  @GET
  @Path ("{participantID}")
  public Response checkParticipantExistence (@Context @Nonnull final HttpServletRequest aHttpServletRequest,
                                             @PathParam ("participantID") @Nonnull final String sParticipantID) throws IOException
  {
    final String sLogPrefix = "[checkParticipantExistence] ";
    final ClientCertificateValidationResult aResult = _checkClientCertificate (aHttpServletRequest, sLogPrefix);
    if (aResult.isFailure ())
      return Response.status (Response.Status.FORBIDDEN).build ();

    final String sRealParticipantID = _unifyPID (sParticipantID);

    LOGGER.info (sLogPrefix + "'" + sRealParticipantID + "'");

    // Parse identifier
    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();
    final IParticipantIdentifier aPI = aIdentifierFactory.parseParticipantIdentifier (sRealParticipantID);
    if (aPI == null)
      LOGGER.error (sLogPrefix + "Failed to parse participant identifier '" + sRealParticipantID + "'");

    // Queue for handling
    if (!PDMetaManager.getStorageMgr ().containsEntry (aPI))
      return Response.status (Response.Status.NOT_FOUND).build ();

    // And done
    return Response.noContent ().build ();
  }
}
