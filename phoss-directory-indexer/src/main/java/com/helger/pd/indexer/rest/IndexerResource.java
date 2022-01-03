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
package com.helger.pd.indexer.rest;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.url.URLHelper;
import com.helger.pd.indexer.clientcert.ClientCertificateValidationResult;
import com.helger.pd.indexer.clientcert.ClientCertificateValidator;
import com.helger.pd.indexer.index.EIndexerWorkItemType;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.storage.EQueryMode;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;

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
   * Check if the current request contains a client certificate and whether it
   * is valid.
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
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn (sLogPrefix + "Error validating client certificate", ex);
    }
    return ClientCertificateValidationResult.createFailure ();
  }

  @Nonnull
  @Nonempty
  private static String _getRequestingHost (@Nonnull final HttpServletRequest aHttpServletRequest)
  {
    return aHttpServletRequest.getRemoteAddr () + "/" + aHttpServletRequest.getRemoteHost ();
  }

  @Nullable
  private static String _unifyPID (@Nullable final String sParticipantID)
  {
    if (sParticipantID == null)
      return null;

    final String sTrimmed = sParticipantID.trim ();
    return URLHelper.urlDecodeOrDefault (sTrimmed, sTrimmed);
  }

  @PUT
  public Response createOrUpdateParticipant (@Context @Nonnull final HttpServletRequest aHttpServletRequest,
                                             @Nonnull final String sParticipantID)
  {
    final String sLogPrefix = "[createOrUpdateParticipant] ";
    final ClientCertificateValidationResult aResult = _checkClientCertificate (aHttpServletRequest, sLogPrefix);
    if (aResult.isFailure ())
      return Response.status (Response.Status.FORBIDDEN).build ();

    final String sRealParticipantID = _unifyPID (sParticipantID);

    if (LOGGER.isInfoEnabled ())
      LOGGER.info (sLogPrefix + "'" + sRealParticipantID + "'");

    // Parse identifier
    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();
    final IParticipantIdentifier aPI = aIdentifierFactory.parseParticipantIdentifier (sRealParticipantID);
    if (aPI == null)
    {
      if (LOGGER.isErrorEnabled ())
        LOGGER.error (sLogPrefix + "Failed to parse participant identifier '" + sRealParticipantID + "'");
      return Response.status (Status.BAD_REQUEST).build ();
    }

    // Queue for handling
    if (PDMetaManager.getIndexerMgr ()
                     .queueWorkItem (aPI,
                                     EIndexerWorkItemType.CREATE_UPDATE,
                                     aResult.getClientID (),
                                     _getRequestingHost (aHttpServletRequest))
                     .isUnchanged ())
    {
      if (LOGGER.isInfoEnabled ())
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
    final ClientCertificateValidationResult aResult = _checkClientCertificate (aHttpServletRequest, sLogPrefix);
    if (aResult.isFailure ())
      return Response.status (Response.Status.FORBIDDEN).build ();

    final String sRealParticipantID = _unifyPID (sParticipantID);

    if (LOGGER.isInfoEnabled ())
      LOGGER.info (sLogPrefix + "'" + sRealParticipantID + "'");

    // Parse identifier
    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();
    final IParticipantIdentifier aPI = aIdentifierFactory.parseParticipantIdentifier (sRealParticipantID);
    if (aPI == null)
    {
      if (LOGGER.isErrorEnabled ())
        LOGGER.error (sLogPrefix + "Failed to parse participant identifier '" + sRealParticipantID + "'");
      return Response.status (Status.BAD_REQUEST).build ();
    }

    // Don't check for existence of the PI as it might be in the queue for
    // creation

    // Queue for handling
    if (PDMetaManager.getIndexerMgr ()
                     .queueWorkItem (aPI, EIndexerWorkItemType.DELETE, aResult.getClientID (), _getRequestingHost (aHttpServletRequest))
                     .isUnchanged ())
    {
      if (LOGGER.isInfoEnabled ())
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

    if (LOGGER.isInfoEnabled ())
      LOGGER.info (sLogPrefix + "'" + sRealParticipantID + "'");

    // Parse identifier
    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();
    final IParticipantIdentifier aPI = aIdentifierFactory.parseParticipantIdentifier (sRealParticipantID);
    if (aPI == null)
      if (LOGGER.isErrorEnabled ())
        LOGGER.error (sLogPrefix + "Failed to parse participant identifier '" + sRealParticipantID + "'");

    // Queue for handling
    if (!PDMetaManager.getStorageMgr ().containsEntry (aPI, EQueryMode.NON_DELETED_ONLY))
      return Response.status (Response.Status.NOT_FOUND).build ();

    // And done
    return Response.noContent ().build ();
  }
}
