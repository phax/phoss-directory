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
package com.helger.pd.indexer.shadow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.http.CHttp;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.HttpClientSettings;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.pd.indexer.settings.PDServerConfiguration;

import jakarta.annotation.Nonnull;

/**
 * Sends shadow events to the downstream replicator service via HTTP POST with
 * JSON payload.
 *
 * @author Mikael Aksamit
 */
public final class ShadowEventSender
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ShadowEventSender.class);

  private ShadowEventSender ()
  {}

  /**
   * Build the JSON payload for a shadow event.
   *
   * @param aEvent
   *        The shadow event. May not be <code>null</code>.
   * @return The JSON object. Never <code>null</code>.
   */
  @Nonnull
  private static IJsonObject _buildEventPayload (@Nonnull final IShadowEvent aEvent)
  {
    final IJsonObject aJson = new JsonObject ();
    aJson.add ("eventId", aEvent.getEventID ());
    aJson.add ("createdAt", aEvent.getCreatedAt ().toString ());
    aJson.add ("operation", aEvent.getOperation ().getID ());
    aJson.add ("participantId", aEvent.getParticipantID ());
    aJson.add ("requestingHost", aEvent.getRequestingHost ());

    final IJsonObject aCertJson = new JsonObject ();
    aCertJson.add ("sha256Fingerprint", aEvent.getCertSHA256Fingerprint ());
    aCertJson.add ("subjectDN", aEvent.getCertSubjectDN ());
    aCertJson.add ("issuerDN", aEvent.getCertIssuerDN ());
    aJson.add ("clientCertificate", aCertJson);

    return aJson;
  }

  /**
   * Send a shadow event to the downstream service.
   *
   * @param sDownstreamURL
   *        The downstream URL. May not be <code>null</code>.
   * @param aEvent
   *        The shadow event to send. May not be <code>null</code>.
   * @return The HTTP status code returned by the downstream service.
   * @throws IOException
   *         If the HTTP request fails
   */
  public static int sendEvent (@Nonnull @Nonempty final String sDownstreamURL,
                               @Nonnull final IShadowEvent aEvent) throws IOException
  {
    final IJsonObject aPayload = _buildEventPayload (aEvent);
    final String sJsonPayload = aPayload.getAsJsonString ();

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Sending shadow event " + aEvent.getEventID () + " to " + sDownstreamURL);

    final HttpPost aPost = new HttpPost (sDownstreamURL);
    aPost.setEntity (new StringEntity (sJsonPayload, ContentType.APPLICATION_JSON.withCharset (StandardCharsets.UTF_8)));

    final String sSecret = PDServerConfiguration.getIndexerShadowingSecret ();
    if (sSecret != null)
      aPost.setHeader ("X-Shadow-Secret", sSecret);

    final int nTimeoutMS = PDServerConfiguration.getIndexerShadowingTimeoutMS ();
    final HttpClientSettings aSettings = new HttpClientSettings ();
    aSettings.setConnectTimeout (Timeout.ofMilliseconds (nTimeoutMS));
    aSettings.setResponseTimeout (Timeout.ofMilliseconds (nTimeoutMS));
    try (final HttpClientManager aHttpClientMgr = HttpClientManager.create (aSettings))
    {
      final Integer aStatusCode = aHttpClientMgr.execute (aPost, aResponse -> {
        final int nCode = aResponse.getCode ();

        if (nCode >= CHttp.HTTP_OK && nCode < CHttp.HTTP_MULTIPLE_CHOICES)
        {
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Successfully sent shadow event " + aEvent.getEventID () + " (HTTP " + nCode + ")");
        }
        else
        {
          LOGGER.warn ("Downstream returned HTTP " + nCode + " for shadow event " + aEvent.getEventID ());
        }

        return nCode;
      });

      if (aStatusCode == null)
        throw new IOException ("HTTP client returned null status code for shadow event " + aEvent.getEventID ());

      return aStatusCode;
    }
  }
}
