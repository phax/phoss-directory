/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.HttpHost;
import org.apache.http.client.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.VisibleForTesting;
import com.helger.commons.charset.CCharset;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.url.SimpleURL;
import com.helger.commons.url.URLHelper;
import com.helger.pd.businesscard.IPDBusinessCardProvider;
import com.helger.pd.businesscard.PDBusinessCardMarshaller;
import com.helger.pd.businesscard.PDBusinessCardType;
import com.helger.pd.businesscard.PDExtendedBusinessCard;
import com.helger.pd.settings.PDSettings;
import com.helger.peppol.httpclient.SMPHttpResponseHandlerUnsigned;
import com.helger.peppol.identifier.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.IdentifierHelper;
import com.helger.peppol.identifier.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.identifier.participant.IPeppolParticipantIdentifier;
import com.helger.peppol.smp.ServiceGroupType;
import com.helger.peppol.smp.ServiceMetadataReferenceType;
import com.helger.peppol.smpclient.SMPClientReadOnly;
import com.helger.peppol.smpclient.exception.SMPClientException;
import com.helger.peppol.smpclient.exception.SMPClientNotFoundException;

/**
 * The SMP based {@link IPDBusinessCardProvider} implementation. An SMP lookup
 * of the ServiceGroup is performed, and the <code>Extension</code> element is
 * parsed for the elements as specified in the PEPPOL Directory specification.
 *
 * @author Philip Helger
 */
public final class SMPBusinessCardProvider implements IPDBusinessCardProvider
{
  private static final String URL_PART_SERVICES = "/services/";
  private static final Logger s_aLogger = LoggerFactory.getLogger (SMPBusinessCardProvider.class);

  /**
   * @return The HttpProxy object to be used by SMP clients based on the Java
   *         System properties "http.proxyHost" and "http.proxyPort". Note:
   *         https is not needed, because SMPs must run on http only.
   */
  @Nullable
  public HttpHost getHttpProxy ()
  {
    final String sProxyHost = PDSettings.getProxyHost ();
    final int nProxyPort = PDSettings.getProxyPort ();
    if (sProxyHost != null && nProxyPort > 0)
      return new HttpHost (sProxyHost, nProxyPort);

    return null;
  }

  @Nullable
  @VisibleForTesting
  PDExtendedBusinessCard getBusinessCard (@Nonnull final IPeppolParticipantIdentifier aParticipantID,
                                          @Nonnull final SMPClientReadOnly aSMPClient)
  {
    // Create SMP client
    aSMPClient.setProxy (getHttpProxy ());

    // First query the service group
    ServiceGroupType aServiceGroup;
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
    final PDBusinessCardType aBusinessCard;
    try
    {
      // Use the optional business card API
      final Request aRequest = Request.Get (aSMPClient.getSMPHostURI () +
                                            "businesscard/" +
                                            aParticipantID.getURIPercentEncoded ());

      aBusinessCard = aSMPClient.executeGenericRequest (aRequest,
                                                        SMPHttpResponseHandlerUnsigned.create (new PDBusinessCardMarshaller ()));
    }
    catch (final SMPClientNotFoundException ex)
    {
      s_aLogger.warn ("No BusinessCard available for '" + aParticipantID.getURIEncoded () + "'");
      return null;
    }
    catch (final SMPClientException ex)
    {
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
    final ICommonsList <IDocumentTypeIdentifier> aDocumentTypeIDs = new CommonsArrayList <> ();
    if (aServiceGroup != null)
      for (final ServiceMetadataReferenceType aRef : aServiceGroup.getServiceMetadataReferenceCollection ()
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
                                                              CCharset.CHARSET_UTF_8_OBJ);
          final SimpleDocumentTypeIdentifier aDocTypeID = IdentifierHelper.createDocumentTypeIdentifierFromURIPartOrNull (sDocumentTypeID);
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
  public PDExtendedBusinessCard getBusinessCard (@Nonnull final IPeppolParticipantIdentifier aParticipantID)
  {
    // Create SMP client
    final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (aParticipantID, PDSettings.getSMLToUse ());
    return getBusinessCard (aParticipantID, aSMPClient);
  }
}
