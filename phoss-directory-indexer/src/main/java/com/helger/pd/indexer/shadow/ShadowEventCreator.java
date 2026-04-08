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

import java.security.cert.X509Certificate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringHelper;
import com.helger.datetime.helper.PDTFactory;
import com.helger.pd.indexer.index.EIndexerWorkItemType;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.settings.PDServerConfiguration;
import com.helger.peppolid.IParticipantIdentifier;

import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Creates shadow events for indexer operations and persists them to the
 * durable outbox queue. This is called after the indexer has successfully
 * queued a work item.
 * <p>
 * Shadow event creation failures are logged but never propagate to affect the
 * original request.
 * </p>
 * <p>
 * Configuration values are cached at startup to avoid repeated resolution on
 * the hot request path.
 * </p>
 *
 * @author Mikael Aksamit
 */
public final class ShadowEventCreator
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ShadowEventCreator.class);

  private static volatile boolean s_bShadowingEnabled = false;
  private static volatile boolean s_bConfigInitialized = false;

  private ShadowEventCreator ()
  {}

  /**
   * Initialize the shadowing configuration cache. Called once at startup from
   * PDMetaManager.
   */
  public static void initializeConfiguration ()
  {
    s_bShadowingEnabled = PDServerConfiguration.isIndexerShadowingEnabled ();
    if (s_bShadowingEnabled)
    {
      final String sURL = PDServerConfiguration.getIndexerShadowingURL ();
      if (StringHelper.isEmpty (sURL))
      {
        LOGGER.error ("Indexer shadowing is enabled but URL is not configured - shadowing will be DISABLED");
        s_bShadowingEnabled = false;
      }
    }
    s_bConfigInitialized = true;

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Shadow event creator initialized: shadowing " +
                    (s_bShadowingEnabled ? "ENABLED" : "DISABLED"));
  }

  /**
   * Create and persist a shadow event for an indexer operation.
   *
   * @param aHttpRequest
   *        The HTTP servlet request containing the client certificate. May not
   *        be <code>null</code>.
   * @param aParticipantID
   *        The participant identifier. May not be <code>null</code>.
   * @param eOperation
   *        The indexer operation type. May not be <code>null</code>.
   * @param sRequestingHost
   *        The requesting host. May not be <code>null</code>.
   */
  public static void createShadowEvent (@Nonnull final HttpServletRequest aHttpRequest,
                                        @Nonnull final IParticipantIdentifier aParticipantID,
                                        @Nonnull final EIndexerWorkItemType eOperation,
                                        @Nonnull @Nonempty final String sRequestingHost)
  {
    // Fast path: check cached config flag (volatile read, no synchronization)
    if (!s_bShadowingEnabled)
      return;

    // Defensive: ensure initialization happened
    if (!s_bConfigInitialized)
    {
      LOGGER.error ("Shadow event creator not initialized - skipping event creation");
      return;
    }

    try
    {
      final X509Certificate aCert = CertificateHelper.extractClientCertificate (aHttpRequest);
      if (aCert == null)
      {
        LOGGER.error ("Cannot create shadow event: no client certificate available");
        return;
      }

      final String sSHA256Fingerprint = CertificateHelper.computeSHA256Fingerprint (aCert);
      if (sSHA256Fingerprint == null)
      {
        LOGGER.error ("Cannot create shadow event: failed to compute SHA-256 fingerprint");
        return;
      }

      final String sSubjectDN = aCert.getSubjectX500Principal ().getName ();
      final String sIssuerDN = aCert.getIssuerX500Principal ().getName ();

      final ShadowEvent aEvent = new ShadowEvent (UUID.randomUUID ().toString (),
                                                  PDTFactory.getCurrentLocalDateTime (),
                                                  eOperation,
                                                  aParticipantID.getURIEncoded (),
                                                  sRequestingHost,
                                                  sSHA256Fingerprint,
                                                  sSubjectDN,
                                                  sIssuerDN);

      final ShadowEventList aEventList = PDMetaManager.getShadowEventList ();
      if (aEventList == null)
      {
        LOGGER.error ("Cannot create shadow event: ShadowEventList not initialized");
        return;
      }

      aEventList.addEvent (aEvent);

      if (LOGGER.isInfoEnabled ())
        LOGGER.info ("Created shadow event " +
                     aEvent.getEventID () +
                     " for " +
                     eOperation.getDisplayName () +
                     " of " +
                     aParticipantID.getURIEncoded ());
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Failed to create shadow event (non-fatal - original request succeeded)", ex);
    }
  }
}
