/**
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.businesscard;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.HttpHost;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.VisibleForTesting;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.functional.ISupplier;
import com.helger.commons.http.CHttp;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.HttpClientSettings;
import com.helger.pd.businesscard.generic.PDBusinessCard;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.settings.PDServerConfiguration;
import com.helger.peppol.sml.ESMPAPIType;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.smpclient.bdxr1.BDXRClientReadOnly;
import com.helger.smpclient.bdxr2.BDXR2ClientReadOnly;
import com.helger.smpclient.exception.SMPClientException;
import com.helger.smpclient.httpclient.AbstractGenericSMPClient;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.smpclient.url.ISMPURLProvider;
import com.helger.smpclient.url.SMPDNSResolutionException;

/**
 * The SMP based {@link IPDBusinessCardProvider} implementation. An SMP lookup
 * of the ServiceGroup is performed, and the <code>Extension</code> element is
 * parsed for the elements as specified in the Peppol Directory specification.
 *
 * @author Philip Helger
 */
public class SMPBusinessCardProvider implements IPDBusinessCardProvider
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPBusinessCardProvider.class);
  private static final Consumer <String> UNHANDLED_HREF_HANDLER = x -> LOGGER.error ("Failed to get document type from href '" + x + "'");

  private final ESMPAPIType m_eSMPMode;
  private final URI m_aSMPURI;
  private final ISMPURLProvider m_aURLProvider;
  private final ISupplier <? extends ICommonsList <? extends ISMLInfo>> m_aSMLInfoProvider;

  /**
   * Constructor.
   *
   * @param eSMPMode
   *        SMP Mode to use.
   * @param aSMPURI
   *        The URI of the SMP. May be <code>null</code> to use SML/DNS lookup.
   *        If this parameter is non-<code>null</code> the SML parameter MUST be
   *        <code>null</code>. This parameter is only needed for testing
   *        purposes, if a network consists of a single SMP and has NO DNS
   *        setup!
   * @param aURLProvider
   *        The URL provider to be used. Must be non-<code>null</code> if SML is
   *        to be used.
   */
  protected SMPBusinessCardProvider (@Nonnull final ESMPAPIType eSMPMode,
                                     @Nullable final URI aSMPURI,
                                     @Nullable final ISMPURLProvider aURLProvider,
                                     @Nullable final ISupplier <? extends ICommonsList <? extends ISMLInfo>> aSMLInfoProvider)
  {
    ValueEnforcer.notNull (eSMPMode, "SMPMode");
    if (aSMPURI != null)
    {
      ValueEnforcer.isNull (aURLProvider, "URL provider must be null if an SMP URI is present!");
      ValueEnforcer.isNull (aSMLInfoProvider, "SMLInfoProvider must be null if an SMP URI is present!");
    }
    else
    {
      ValueEnforcer.notNull (aURLProvider, "URL Provider");
    }

    m_eSMPMode = eSMPMode;
    m_aSMPURI = aSMPURI;
    m_aURLProvider = aURLProvider;
    m_aSMLInfoProvider = aSMLInfoProvider;
  }

  public final boolean isFixedSMP ()
  {
    return m_aSMPURI != null;
  }

  @Nullable
  public final URI getFixedSMPURI ()
  {
    return m_aSMPURI;
  }

  @Nullable
  public final ICommonsList <? extends ISMLInfo> getAllSMLsToUse ()
  {
    return m_aSMLInfoProvider == null ? null : m_aSMLInfoProvider.get ();
  }

  /**
   * @return The HttpProxy object to be used by SMP clients based on the Java
   *         System properties "http.proxyHost" and "http.proxyPort". Note:
   *         https is not needed, because SMPs must run on http only.
   */
  @Nullable
  private static HttpHost _getHttpProxy ()
  {
    final String sProxyHost = PDServerConfiguration.getProxyHost ();
    final int nProxyPort = PDServerConfiguration.getProxyPort ();
    if (sProxyHost != null && nProxyPort > 0)
      return new HttpHost (sProxyHost, nProxyPort);

    return null;
  }

  /**
   * @return The proxy credentials to be used. May be <code>null</code>.
   */
  @Nullable
  private static Credentials _getHttpProxyCredentials ()
  {
    final String sProxyUsername = PDServerConfiguration.getProxyUsername ();
    final String sProxyPassword = PDServerConfiguration.getProxyPassword ();
    if (sProxyUsername != null && sProxyPassword != null)
      return new UsernamePasswordCredentials (sProxyUsername, sProxyPassword);

    return null;
  }

  @Nullable
  @VisibleForTesting
  PDExtendedBusinessCard getBusinessCardPeppolSMP (@Nonnull final IParticipantIdentifier aParticipantID,
                                                   @Nonnull final SMPClientReadOnly aSMPClient,
                                                   @Nonnull final HttpClientSettings aHCS)
  {
    LOGGER.info ("Querying BusinessCard for '" +
                 aParticipantID.getURIEncoded () +
                 "' from Peppol SMP '" +
                 aSMPClient.getSMPHostURI () +
                 "'");

    // First query the service group
    com.helger.xsds.peppol.smp1.ServiceGroupType aServiceGroup;
    try
    {
      aServiceGroup = aSMPClient.getServiceGroupOrNull (aParticipantID);
    }
    catch (final SMPClientException ex)
    {
      LOGGER.error ("Error querying SMP for ServiceGroup of '" + aParticipantID.getURIEncoded () + "'", ex);
      return null;
    }

    // If the service group is present, try querying the business card
    final PDBusinessCard aBusinessCard;
    try (final HttpClientManager aHCM = HttpClientManager.create (aHCS))
    {
      // Use the optional business card API
      final HttpGet aRequest = new HttpGet (aSMPClient.getSMPHostURI () + "businesscard/" + aParticipantID.getURIPercentEncoded ());
      aBusinessCard = aHCM.execute (aRequest, new PDSMPHttpResponseHandlerBusinessCard ());
    }
    catch (final IOException ex)
    {
      if ((ex instanceof HttpResponseException && ((HttpResponseException) ex).getStatusCode () == CHttp.HTTP_NOT_FOUND) ||
          ex instanceof UnknownHostException)
      {
        LOGGER.warn ("No BusinessCard available for '" +
                     aParticipantID.getURIEncoded () +
                     "' - not in configured SMK/SML? - " +
                     ex.getMessage ());
      }
      else
        LOGGER.error ("Error querying SMP for BusinessCard of '" + aParticipantID.getURIEncoded () + "'", ex);
      return null;
    }

    if (aBusinessCard == null)
    {
      // No extension present - no need to try again
      LOGGER.warn ("Failed to get SMP BusinessCard of " + aParticipantID.getURIEncoded ());
      return null;
    }

    // Query all document types
    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();
    final ICommonsList <IDocumentTypeIdentifier> aDocumentTypeIDs = SMPClientReadOnly.getAllDocumentTypes (aServiceGroup,
                                                                                                           aIdentifierFactory,
                                                                                                           UNHANDLED_HREF_HANDLER);

    return new PDExtendedBusinessCard (aBusinessCard, aDocumentTypeIDs);

  }

  @Nullable
  @VisibleForTesting
  PDExtendedBusinessCard getBusinessCardBDXR1 (@Nonnull final IParticipantIdentifier aParticipantID,
                                               @Nonnull final BDXRClientReadOnly aSMPClient,
                                               @Nonnull final HttpClientSettings aHCS)
  {
    LOGGER.info ("Querying BusinessCard for '" +
                 aParticipantID.getURIEncoded () +
                 "' from OASIS BDXR SMP v1 '" +
                 aSMPClient.getSMPHostURI () +
                 "'");

    // First query the service group
    com.helger.xsds.bdxr.smp1.ServiceGroupType aServiceGroup;
    try
    {
      aServiceGroup = aSMPClient.getServiceGroupOrNull (aParticipantID);
    }
    catch (final SMPClientException ex)
    {
      LOGGER.error ("Error querying SMP for ServiceGroup of '" + aParticipantID.getURIEncoded () + "'", ex);
      return null;
    }

    // If the service group is present, try querying the business card
    final PDBusinessCard aBusinessCard;
    try (final HttpClientManager aHCM = HttpClientManager.create (aHCS))
    {
      // Use the optional business card API
      final HttpGet aRequest = new HttpGet (aSMPClient.getSMPHostURI () + "businesscard/" + aParticipantID.getURIPercentEncoded ());
      aBusinessCard = aHCM.execute (aRequest, new PDSMPHttpResponseHandlerBusinessCard ());
    }
    catch (final IOException ex)
    {
      if ((ex instanceof HttpResponseException && ((HttpResponseException) ex).getStatusCode () == CHttp.HTTP_NOT_FOUND) ||
          ex instanceof UnknownHostException)
      {
        LOGGER.warn ("No BusinessCard available for '" +
                     aParticipantID.getURIEncoded () +
                     "' - not in configured SMK/SML? - " +
                     ex.getMessage ());
      }
      else
        LOGGER.error ("Error querying SMP for BusinessCard of '" + aParticipantID.getURIEncoded () + "'", ex);
      return null;
    }

    if (aBusinessCard == null)
    {
      // No extension present - no need to try again
      LOGGER.warn ("Failed to get SMP BusinessCard of " + aParticipantID.getURIEncoded ());
      return null;
    }

    // Query all document types
    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();
    final ICommonsList <IDocumentTypeIdentifier> aDocumentTypeIDs = BDXRClientReadOnly.getAllDocumentTypes (aServiceGroup,
                                                                                                            aIdentifierFactory,
                                                                                                            UNHANDLED_HREF_HANDLER);

    return new PDExtendedBusinessCard (aBusinessCard, aDocumentTypeIDs);
  }

  @Nullable
  @VisibleForTesting
  PDExtendedBusinessCard getBusinessCardBDXR2 (@Nonnull final IParticipantIdentifier aParticipantID,
                                               @Nonnull final BDXR2ClientReadOnly aSMPClient,
                                               @Nonnull final HttpClientSettings aHCS)
  {
    LOGGER.info ("Querying BusinessCard for '" +
                 aParticipantID.getURIEncoded () +
                 "' from OASIS BDXR SMP v2 '" +
                 aSMPClient.getSMPHostURI () +
                 "'");

    // First query the service group
    com.helger.xsds.bdxr.smp2.ServiceGroupType aServiceGroup;
    try
    {
      aServiceGroup = aSMPClient.getServiceGroupOrNull (aParticipantID);
    }
    catch (final SMPClientException ex)
    {
      LOGGER.error ("Error querying SMP for ServiceGroup of '" + aParticipantID.getURIEncoded () + "'", ex);
      return null;
    }

    // If the service group is present, try querying the business card
    final PDBusinessCard aBusinessCard;
    try (final HttpClientManager aHCM = HttpClientManager.create (aHCS))
    {
      // Use the optional business card API
      // FIXME is the path "bdxr-smp-2" needed? Well, the PD is not yet
      // specified for this SMP type....
      final HttpGet aRequest = new HttpGet (aSMPClient.getSMPHostURI () + "businesscard/" + aParticipantID.getURIPercentEncoded ());
      aBusinessCard = aHCM.execute (aRequest, new PDSMPHttpResponseHandlerBusinessCard ());
    }
    catch (final IOException ex)
    {
      if ((ex instanceof HttpResponseException && ((HttpResponseException) ex).getStatusCode () == CHttp.HTTP_NOT_FOUND) ||
          ex instanceof UnknownHostException)
      {
        LOGGER.warn ("No BusinessCard available for '" +
                     aParticipantID.getURIEncoded () +
                     "' - not in configured SMK/SML? - " +
                     ex.getMessage ());
      }
      else
        LOGGER.error ("Error querying SMP for BusinessCard of '" + aParticipantID.getURIEncoded () + "'", ex);
      return null;
    }

    if (aBusinessCard == null)
    {
      // No extension present - no need to try again
      LOGGER.warn ("Failed to get SMP BusinessCard of " + aParticipantID.getURIEncoded ());
      return null;
    }

    // Query all document types
    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();
    final ICommonsList <IDocumentTypeIdentifier> aDocumentTypeIDs = BDXR2ClientReadOnly.getAllDocumentTypes (aServiceGroup,
                                                                                                             aIdentifierFactory);

    return new PDExtendedBusinessCard (aBusinessCard, aDocumentTypeIDs);
  }

  private void _configureSMPClient (@Nonnull final AbstractGenericSMPClient <?> aSMPClient)
  {
    aSMPClient.httpClientSettings ().setProxyHost (_getHttpProxy ()).setProxyCredentials (_getHttpProxyCredentials ());
    // Eat all we can get
    aSMPClient.setXMLSchemaValidation (false);
  }

  @Nullable
  public PDExtendedBusinessCard getBusinessCard (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    final HttpClientSettings aHCS = new HttpClientSettings ().setProxyHost (_getHttpProxy ())
                                                             .setProxyCredentials (_getHttpProxyCredentials ());
    PDExtendedBusinessCard aBC;

    if (m_aSMPURI != null)
    {
      // Use a preselected SMP URI
      switch (m_eSMPMode)
      {
        case PEPPOL:
        {
          final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (m_aSMPURI);
          _configureSMPClient (aSMPClient);
          aBC = getBusinessCardPeppolSMP (aParticipantID, aSMPClient, aHCS);
          break;
        }
        case OASIS_BDXR_V1:
        {
          final BDXRClientReadOnly aSMPClient = new BDXRClientReadOnly (m_aSMPURI);
          _configureSMPClient (aSMPClient);
          aBC = getBusinessCardBDXR1 (aParticipantID, aSMPClient, aHCS);
          break;
        }
        case OASIS_BDXR_V2:
        {
          final BDXR2ClientReadOnly aSMPClient = new BDXR2ClientReadOnly (m_aSMPURI);
          _configureSMPClient (aSMPClient);
          aBC = getBusinessCardBDXR2 (aParticipantID, aSMPClient, aHCS);
          break;
        }
        default:
          throw new IllegalStateException ("Unsupported SMP mode " + m_eSMPMode);
      }
    }
    else
    {
      // SML auto detect
      aBC = null;
      for (final ISMLInfo aSML : m_aSMLInfoProvider.get ())
      {
        // Create SMP client and query SMP
        switch (m_eSMPMode)
        {
          case PEPPOL:
          {
            try
            {
              final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (m_aURLProvider, aParticipantID, aSML);
              _configureSMPClient (aSMPClient);
              aBC = getBusinessCardPeppolSMP (aParticipantID, aSMPClient, aHCS);
            }
            catch (final SMPDNSResolutionException ex)
            {
              // Happens if a non-existing URL is queried
            }
            break;
          }
          case OASIS_BDXR_V1:
          {
            try
            {
              final BDXRClientReadOnly aSMPClient = new BDXRClientReadOnly (m_aURLProvider, aParticipantID, aSML);
              _configureSMPClient (aSMPClient);
              aBC = getBusinessCardBDXR1 (aParticipantID, aSMPClient, aHCS);
            }
            catch (final SMPDNSResolutionException ex)
            {
              // Happens if a non-existing URL is queried
            }
            break;
          }
          case OASIS_BDXR_V2:
          {
            try
            {
              final BDXR2ClientReadOnly aSMPClient = new BDXR2ClientReadOnly (m_aURLProvider, aParticipantID, aSML);
              _configureSMPClient (aSMPClient);
              aBC = getBusinessCardBDXR2 (aParticipantID, aSMPClient, aHCS);
            }
            catch (final SMPDNSResolutionException ex)
            {
              // Happens if a non-existing URL is queried
            }
            break;
          }
          default:
            throw new IllegalStateException ("Unsupported SMP mode " + m_eSMPMode);
        }

        // Found one?
        if (aBC != null)
          break;
      }
    }

    if (aBC != null)
      LOGGER.info ("Found BusinessCard for '" +
                   aParticipantID.getURIEncoded () +
                   "' with " +
                   aBC.getBusinessCard ().businessEntities ().size () +
                   " entities and " +
                   aBC.getDocumentTypeCount () +
                   " document types");
    return aBC;
  }

  @Nonnull
  public static SMPBusinessCardProvider createWithSMLAutoDetect (@Nonnull final ESMPAPIType eSMPMode,
                                                                 @Nonnull final ISMPURLProvider aURLProvider,
                                                                 @Nullable final ISupplier <? extends ICommonsList <? extends ISMLInfo>> aSMLInfoProvider)
  {
    ValueEnforcer.notNull (eSMPMode, "SMPMode");
    ValueEnforcer.notNull (aURLProvider, "URLProvider");
    ValueEnforcer.notNull (aSMLInfoProvider, "SMLInfoProvider");
    return new SMPBusinessCardProvider (eSMPMode, null, aURLProvider, aSMLInfoProvider);
  }

  @Nonnull
  public static SMPBusinessCardProvider createForFixedSMP (@Nonnull final ESMPAPIType eSMPMode, @Nonnull final URI aSMPURI)
  {
    ValueEnforcer.notNull (eSMPMode, "SMPMode");
    ValueEnforcer.notNull (aSMPURI, "SMP URI");
    return new SMPBusinessCardProvider (eSMPMode, aSMPURI, null, null);
  }
}
