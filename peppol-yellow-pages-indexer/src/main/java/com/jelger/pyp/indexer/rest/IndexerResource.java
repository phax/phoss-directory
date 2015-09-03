package com.jelger.pyp.indexer.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Indexer resource (exposed at "1.0" path)
 *
 * @author Philip Helger
 */
@Path ("1.0")
public class IndexerResource
{
  @GET
  @Path ("{participantID}")
  @Produces (MediaType.TEXT_PLAIN)
  public String createOrUpdateParticipant (@PathParam ("participantID") final String sParticipantID)
  {
    return "Got it: '" + sParticipantID + "'";
  }
}
