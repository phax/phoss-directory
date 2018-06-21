/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.mgr;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHost;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.VisibleForTesting;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.url.SimpleURL;
import com.helger.commons.url.URLHelper;
import com.helger.httpclient.HttpClientHelper;
import com.helger.pd.businesscard.IPDBusinessCardProvider;
import com.helger.pd.businesscard.PDExtendedBusinessCard;
import com.helger.pd.businesscard.generic.PDBusinessCard;
import com.helger.pd.settings.EPDSMPMode;
import com.helger.pd.settings.PDServerConfiguration;
import com.helger.peppol.bdxrclient.BDXRClientReadOnly;
import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smpclient.SMPClientReadOnly;
import com.helger.peppol.smpclient.exception.SMPClientException;
import com.helger.peppol.url.IPeppolURLProvider;

/**
 * The SMP based {@link IPDBusinessCardProvider} implementation. An SMP lookup
 * of the ServiceGroup is performed, and the <code>Extension</code> element is
 * parsed for the elements as specified in the PEPPOL Directory specification.
 *
 * @author Philip Helger
 */
public class SMPBusinessCardProvider implements IPDBusinessCardProvider
{
  private static final String URL_PART_SERVICES = "/services/";
  private static final Logger s_aLogger = LoggerFactory.getLogger (SMPBusinessCardProvider.class);

  private final EPDSMPMode m_eSMPMode;
  private final URI m_aSMPURI;
  private final IPeppolURLProvider m_aURLProvider;
  private final Supplier <? extends Iterable <? extends ISMLInfo>> m_aSMLInfoProvider;

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
  protected SMPBusinessCardProvider (@Nonnull final EPDSMPMode eSMPMode,
                                     @Nullable final URI aSMPURI,
                                     @Nullable final IPeppolURLProvider aURLProvider,
                                     @Nullable final Supplier <? extends Iterable <? extends ISMLInfo>> aSMLInfoProvider)
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

  /**
   * @return The HttpProxy object to be used by SMP clients based on the Java
   *         System properties "http.proxyHost" and "http.proxyPort". Note:
   *         https is not needed, because SMPs must run on http only.
   */
  @Nullable
  public HttpHost getHttpProxy ()
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
  public Credentials getHttpProxyCredentials ()
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
                                                   @Nonnull final SMPClientReadOnly aSMPClient)
  {
    // Create SMP client
    final HttpHost aProxy = getHttpProxy ();
    final Credentials aProxyCredentials = getHttpProxyCredentials ();
    aSMPClient.setProxy (aProxy);
    aSMPClient.setProxyCredentials (aProxyCredentials);

    s_aLogger.info ("Querying BusinessCard for '" +
                    aParticipantID.getURIEncoded () +
                    "' from SMP '" +
                    aSMPClient.getSMPHostURI () +
                    "'");

    // First query the service group
    com.helger.peppol.smp.ServiceGroupType aServiceGroup;
    try
    {
      aServiceGroup = aSMPClient.getServiceGroupOrNull (aParticipantID);
    }
    catch (final SMPClientException ex)
    {
      s_aLogger.error ("Error querying SMP for ServiceGroup of '" + aParticipantID.getURIEncoded () + "'", ex);
      return null;
    }

    // If the service group is present, try querying the business card
    final PDBusinessCard aBusinessCard;
    try
    {
      // Use the optional business card API
      final HttpGet aRequest = new HttpGet (aSMPClient.getSMPHostURI () +
                                            "businesscard/" +
                                            aParticipantID.getURIPercentEncoded ());
      final HttpContext aContext = HttpClientHelper.createHttpContext (aProxy, aProxyCredentials);
      aBusinessCard = PDMetaManager.getHttpClientMgr ()
                                   .execute (aRequest, aContext, new PDSMPHttpResponseHandlerUnsigned ());
    }
    catch (final IOException ex)
    {
      if ((ex instanceof HttpResponseException &&
           ((HttpResponseException) ex).getStatusCode () == HttpServletResponse.SC_NOT_FOUND) ||
          ex instanceof UnknownHostException)
      {
        s_aLogger.warn ("No BusinessCard available for '" +
                        aParticipantID.getURIEncoded () +
                        "' - not in configured SMK/SML? - " +
                        ex.getMessage ());
      }
      else
        s_aLogger.error ("Error querying SMP for BusinessCard of '" + aParticipantID.getURIEncoded () + "'", ex);
      return null;
    }

    if (aBusinessCard == null)
    {
      // No extension present - no need to try again
      s_aLogger.warn ("Failed to get SMP BusinessCard of " + aParticipantID.getURIEncoded ());
      return null;
    }

    // Query all document types
    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();
    final ICommonsList <IDocumentTypeIdentifier> aDocumentTypeIDs = new CommonsArrayList <> ();
    if (aServiceGroup != null)
      for (final com.helger.peppol.smp.ServiceMetadataReferenceType aRef : aServiceGroup.getServiceMetadataReferenceCollection ()
                                                                                        .getServiceMetadataReference ())
      {
        // Extract the path in case there are parameters or anchors attached
        final String sHref = new SimpleURL (aRef.getHref ()).getPath ();
        final int nIndex = sHref.indexOf (URL_PART_SERVICES);
        if (nIndex < 0)
        {
          s_aLogger.error ("Invalid href when querying service group '" +
                           aParticipantID.getURIEncoded () +
                           "': '" +
                           sHref +
                           "'");
        }
        else
        {
          // URL decode because of encoded '#' and ':' characters
          final String sDocumentTypeID = URLHelper.urlDecode (sHref.substring (nIndex + URL_PART_SERVICES.length ()),
                                                              StandardCharsets.UTF_8);
          final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.parseDocumentTypeIdentifier (sDocumentTypeID);
          if (aDocTypeID == null)
          {
            s_aLogger.error ("Invalid document type when querying service group '" +
                             aParticipantID.getURIEncoded () +
                             "': '" +
                             sDocumentTypeID +
                             "'");
          }
          else
          {
            // Success
            aDocumentTypeIDs.add (aDocTypeID);
          }
        }
      }

    return new PDExtendedBusinessCard (aBusinessCard, aDocumentTypeIDs);
  }

  @Nullable
  @VisibleForTesting
  PDExtendedBusinessCard getBusinessCardBDXR1 (@Nonnull final IParticipantIdentifier aParticipantID,
                                               @Nonnull final BDXRClientReadOnly aSMPClient)
  {
    // Create SMP client
    final HttpHost aProxy = getHttpProxy ();
    final Credentials aProxyCredentials = getHttpProxyCredentials ();
    aSMPClient.setProxy (aProxy);
    aSMPClient.setProxyCredentials (aProxyCredentials);

    s_aLogger.info ("Querying BusinessCard for '" +
                    aParticipantID.getURIEncoded () +
                    "' from SMP '" +
                    aSMPClient.getSMPHostURI () +
                    "'");

    // First query the service group
    com.helger.peppol.bdxr.ServiceGroupType aServiceGroup;
    try
    {
      aServiceGroup = aSMPClient.getServiceGroupOrNull (aParticipantID);
    }
    catch (final SMPClientException ex)
    {
      s_aLogger.error ("Error querying SMP for ServiceGroup of '" + aParticipantID.getURIEncoded () + "'", ex);
      return null;
    }

    // If the service group is present, try querying the business card
    final PDBusinessCard aBusinessCard;
    try
    {
      // Use the optional business card API
      final HttpGet aRequest = new HttpGet (aSMPClient.getSMPHostURI () +
                                            "businesscard/" +
                                            aParticipantID.getURIPercentEncoded ());
      final HttpContext aContext = HttpClientHelper.createHttpContext (aProxy, aProxyCredentials);
      aBusinessCard = PDMetaManager.getHttpClientMgr ()
                                   .execute (aRequest, aContext, new PDSMPHttpResponseHandlerUnsigned ());
    }
    catch (final IOException ex)
    {
      if ((ex instanceof HttpResponseException &&
           ((HttpResponseException) ex).getStatusCode () == HttpServletResponse.SC_NOT_FOUND) ||
          ex instanceof UnknownHostException)
      {
        s_aLogger.warn ("No BusinessCard available for '" +
                        aParticipantID.getURIEncoded () +
                        "' - not in configured SMK/SML? - " +
                        ex.getMessage ());
      }
      else
        s_aLogger.error ("Error querying SMP for BusinessCard of '" + aParticipantID.getURIEncoded () + "'", ex);
      return null;
    }

    if (aBusinessCard == null)
    {
      // No extension present - no need to try again
      s_aLogger.warn ("Failed to get SMP BusinessCard of " + aParticipantID.getURIEncoded ());
      return null;
    }

    // Query all document types
    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();
    final ICommonsList <IDocumentTypeIdentifier> aDocumentTypeIDs = new CommonsArrayList <> ();
    if (aServiceGroup != null)
      for (final com.helger.peppol.bdxr.ServiceMetadataReferenceType aRef : aServiceGroup.getServiceMetadataReferenceCollection ()
                                                                                         .getServiceMetadataReference ())
      {
        // Extract the path in case there are parameters or anchors attached
        final String sHref = new SimpleURL (aRef.getHref ()).getPath ();
        final int nIndex = sHref.indexOf (URL_PART_SERVICES);
        if (nIndex < 0)
        {
          s_aLogger.error ("Invalid href when querying service group '" +
                           aParticipantID.getURIEncoded () +
                           "': '" +
                           sHref +
                           "'");
        }
        else
        {
          // URL decode because of encoded '#' and ':' characters
          final String sDocumentTypeID = URLHelper.urlDecode (sHref.substring (nIndex + URL_PART_SERVICES.length ()),
                                                              StandardCharsets.UTF_8);
          final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.parseDocumentTypeIdentifier (sDocumentTypeID);
          if (aDocTypeID == null)
          {
            s_aLogger.error ("Invalid document type when querying service group '" +
                             aParticipantID.getURIEncoded () +
                             "': '" +
                             sDocumentTypeID +
                             "'");
          }
          else
          {
            // Success
            aDocumentTypeIDs.add (aDocTypeID);
          }
        }
      }

    return new PDExtendedBusinessCard (aBusinessCard, aDocumentTypeIDs);
  }

  @Nullable
  public PDExtendedBusinessCard getBusinessCard (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    PDExtendedBusinessCard aBC;

    if (m_aSMPURI != null)
    {
      // Use a preselected SMP URI
      switch (m_eSMPMode)
      {
        case PEPPOL:
        {
          final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (m_aSMPURI);
          aBC = getBusinessCardPeppolSMP (aParticipantID, aSMPClient);
          break;
        }
        case OASIS_BDXR_v1:
        {
          final BDXRClientReadOnly aSMPClient = new BDXRClientReadOnly (m_aSMPURI);
          aBC = getBusinessCardBDXR1 (aParticipantID, aSMPClient);
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
            final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (m_aURLProvider, aParticipantID, aSML);
            aBC = getBusinessCardPeppolSMP (aParticipantID, aSMPClient);
            break;
          }
          case OASIS_BDXR_v1:
          {
            final BDXRClientReadOnly aSMPClient = new BDXRClientReadOnly (m_aURLProvider, aParticipantID, aSML);
            aBC = getBusinessCardBDXR1 (aParticipantID, aSMPClient);
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
      s_aLogger.info ("Found BusinessCard for '" +
                      aParticipantID.getURIEncoded () +
                      "' with " +
                      aBC.getDocumentTypeCount () +
                      " document types");
    return aBC;
  }

  @Nonnull
  public static SMPBusinessCardProvider createWithSMLAutoDetect (@Nonnull final EPDSMPMode eSMPMode,
                                                                 @Nonnull final IPeppolURLProvider aURLProvider,
                                                                 @Nullable final Supplier <? extends Iterable <? extends ISMLInfo>> aSMLInfoProvider)
  {
    ValueEnforcer.notNull (eSMPMode, "SMPMode");
    ValueEnforcer.notNull (aURLProvider, "URLProvider");
    ValueEnforcer.notNull (aSMLInfoProvider, "SMLInfoProvider");
    return new SMPBusinessCardProvider (eSMPMode, null, aURLProvider, aSMLInfoProvider);
  }

  @Nonnull
  public static SMPBusinessCardProvider createForFixedSMP (@Nonnull final EPDSMPMode eSMPMode,
                                                           @Nonnull final URI aSMPURI)
  {
    ValueEnforcer.notNull (eSMPMode, "SMPMode");
    ValueEnforcer.notNull (aSMPURI, "SMP URI");
    return new SMPBusinessCardProvider (eSMPMode, aSMPURI, null, null);
  }
}
