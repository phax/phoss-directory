package com.helger.pyp.indexer.rest;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.helger.pyp.indexer.ClientCertificateValidator;

/**
 * Indexer resource (exposed at "1.0" path)
 *
 * @author Philip Helger
 */
@Path ("1.0")
public class IndexerResource
{
  // Jersey will inject proxy of Security Context
  @Context
  private SecurityContext securityContext;

  @Context
  private HttpServletRequest m_aServletRequest;

  /**
   * Check if the current request contains a client certificate.
   * 
   * @return A non-<code>null</code> error {@link Response} if no valid client
   *         certificate is present, <code>null</code> if everything is fine.
   */
  @Nullable
  private Response _checkClientCertificate ()
  {
    boolean bValid = false;
    try
    {
      bValid = ClientCertificateValidator.isClientCertificateValid (m_aServletRequest);
    }
    catch (final RuntimeException ex)
    {}

    if (!bValid)
      return Response.status (Response.Status.FORBIDDEN).build ();

    return null;
  }

  @GET
  @Path ("{participantID}")
  public Response createOrUpdateParticipant (@PathParam ("participantID") final String sParticipantID)
  {
    final Response aResponse = _checkClientCertificate ();
    if (aResponse != null)
      return aResponse;

    return Response.ok ().entity ("Got it: '" + sParticipantID + "'").build ();
  }
}
