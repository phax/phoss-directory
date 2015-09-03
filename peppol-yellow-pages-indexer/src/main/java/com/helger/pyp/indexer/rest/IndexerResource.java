package com.helger.pyp.indexer.rest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.helger.pyp.indexer.ClientCertificateValidator;

/**
 * Indexer resource (exposed at "1.0" path)
 *
 * @author Philip Helger
 */
@Path ("1.0")
public class IndexerResource
{
  @Context
  private HttpServletRequest m_aServletRequest;

  private void _checkClientCertificate ()
  {
    boolean bValid = false;
    try
    {
      bValid = ClientCertificateValidator.isClientCertificateValid (m_aServletRequest);
    }
    catch (final RuntimeException ex)
    {}

    if (!bValid)
    {}
  }

  @GET
  @Path ("{participantID}")
  @Produces (MediaType.TEXT_PLAIN)
  public String createOrUpdateParticipant (@PathParam ("participantID") final String sParticipantID)
  {
    return "Got it: '" + sParticipantID + "'";
  }
}
