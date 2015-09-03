package com.helger.pyp.indexer.rest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.helger.pyp.indexer.ClientCertificateValidator;

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

  @GET
  @Path ("{participantID}")
  public Response createOrUpdateParticipant (@Context @Nonnull final HttpServletRequest aHttpServletRequest,
                                             @PathParam ("participantID") @Nonnull final String sParticipantID)
  {
    final Response aResponse = _checkClientCertificate (aHttpServletRequest);
    if (aResponse != null)
      return aResponse;

    return Response.ok ().entity ("Got it: '" + sParticipantID + "'").build ();
  }
}
