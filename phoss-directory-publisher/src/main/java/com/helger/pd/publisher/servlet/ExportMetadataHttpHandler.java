/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.servlet;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.OverridingMethodsMustInvokeSuper;
import com.helger.base.state.EContinue;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsMap;
import com.helger.http.CHttp;
import com.helger.http.CHttpHeader;
import com.helger.pd.indexer.settings.PDServerConfiguration;
import com.helger.pd.publisher.aws.S3Helper;
import com.helger.pd.publisher.exportall.ExportAllManager;
import com.helger.pd.publisher.exportall.ExportRateLimit;
import com.helger.photon.core.servlet.AbstractObjectDeliveryHttpHandler;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

import jakarta.servlet.http.HttpServletResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

/**
 * Answers HEAD requests on the export endpoints with the metadata of the stored export, so that a
 * consumer can decide whether a new export is available before spending one of its few daily
 * download slots.
 * <p>
 * A HEAD deliberately does <b>not</b> issue the redirect that GET issues. Where the export URLs are
 * signed, that redirect carries a short lived signature which would be equally usable for a GET, so
 * answering HEAD with a redirect would turn it into an unmetered download path. Instead the stored
 * object's <code>Last-Modified</code>, <code>Content-Length</code> and <code>ETag</code> are
 * returned directly with a 200. This differs from the GET response, which RFC 9110 would normally
 * not expect, but it is the only shape that is both useful and safe here.
 * </p>
 *
 * @author Mikael Aksamit
 * @since 0.18.1
 */
public class ExportMetadataHttpHandler extends AbstractObjectDeliveryHttpHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ExportMetadataHttpHandler.class);

  /** Map of the public export filename to the S3 key holding it */
  private static final ICommonsMap <String, String> KEYS = new CommonsHashMap <> ();
  static
  {
    KEYS.put (ExportDeliveryHttpHandler.SPECIAL_BUSINESS_CARDS_XML_FULL,
              ExportAllManager.INTERNAL_BUSINESSCARDS_XML_FULL);
    KEYS.put (ExportDeliveryHttpHandler.SPECIAL_BUSINESS_CARDS_XML_NO_DOC_TYPES,
              ExportAllManager.INTERNAL_BUSINESSCARDS_XML_NO_DOC_TYPES);
    KEYS.put (ExportDeliveryHttpHandler.SPECIAL_BUSINESS_CARDS_JSON, ExportAllManager.INTERNAL_BUSINESSCARDS_JSON);
    KEYS.put (ExportDeliveryHttpHandler.SPECIAL_BUSINESS_CARDS_CSV, ExportAllManager.INTERNAL_BUSINESSCARDS_CSV);
    KEYS.put (ExportDeliveryHttpHandler.SPECIAL_PARTICIPANTS_XML, ExportAllManager.INTERNAL_PARTICIPANTS_XML);
    KEYS.put (ExportDeliveryHttpHandler.SPECIAL_PARTICIPANTS_JSON, ExportAllManager.INTERNAL_PARTICIPANTS_JSON);
    KEYS.put (ExportDeliveryHttpHandler.SPECIAL_PARTICIPANTS_CSV, ExportAllManager.INTERNAL_PARTICIPANTS_CSV);
  }

  /**
   * Cached metadata of a single export. The exports change once a day, so a short lived cache keeps
   * all but the first HEAD of each interval free of an S3 round trip.
   */
  private record CachedMeta (HeadObjectResponse aHead, Instant aReadAt)
  {
    boolean isExpired (@NonNull final Duration aTTL)
    {
      return aReadAt.plus (aTTL).isBefore (Instant.now ());
    }
  }

  private static final ICommonsMap <String, CachedMeta> CACHE = new CommonsHashMap <> ();

  @Override
  @OverridingMethodsMustInvokeSuper
  public EContinue initRequestState (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                     @NonNull final UnifiedResponse aUnifiedResponse)
  {
    if (super.initRequestState (aRequestScope, aUnifiedResponse).isBreak ())
      return EContinue.BREAK;

    final String sFilename = aRequestScope.attrs ().getAsString (REQUEST_ATTR_OBJECT_DELIVERY_FILENAME);
    if (!KEYS.containsKey (sFilename))
    {
      LOGGER.warn ("Cannot deliver metadata for the resource '" + sFilename + "'");
      aUnifiedResponse.setStatus (HttpServletResponse.SC_NOT_FOUND);
      return EContinue.BREAK;
    }
    return EContinue.CONTINUE;
  }

  @Nullable
  private static HeadObjectResponse _getMetadata (@NonNull final String sKey)
  {
    final Duration aTTL = PDServerConfiguration.getExportMetadataCacheDuration ();
    synchronized (CACHE)
    {
      final CachedMeta aCached = CACHE.get (sKey);
      if (aCached != null && !aCached.isExpired (aTTL))
        return aCached.aHead ();
    }

    final HeadObjectResponse aHead = S3Helper.headS3Object (PDServerConfiguration.getS3BucketName (), sKey);
    if (aHead != null)
      synchronized (CACHE)
      {
        CACHE.put (sKey, new CachedMeta (aHead, Instant.now ()));
      }
    return aHead;
  }

  @Override
  protected void onDeliverResource (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                    @NonNull final UnifiedResponse aUnifiedResponse,
                                    @NonNull final String sFilename) throws IOException
  {
    // A HEAD is far cheaper than a download, so it gets its own, much more generous budget. It is
    // still bounded, so that a misbehaving client cannot drive unlimited S3 requests.
    final String sRateLimitKey = "export-head:" + aRequestScope.getRemoteAddr () + ":" + sFilename;
    if (ExportRateLimit.INSTANCE.isOverHeadLimit (sRateLimitKey))
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Export metadata rate limit exceeded for " + sRateLimitKey);
      aUnifiedResponse.setStatus (CHttp.HTTP_TOO_MANY_REQUESTS)
                      .addCustomResponseHeader (CHttpHeader.RETRY_AFTER, "3600");
      return;
    }

    final HeadObjectResponse aHead = _getMetadata (KEYS.get (sFilename));
    if (aHead == null)
    {
      aUnifiedResponse.setStatus (HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    aUnifiedResponse.setStatus (HttpServletResponse.SC_OK);

    /*
     * The base handler installs an ETag that is constant for every resource until the server is
     * restarted, which would tell a consumer that the export never changes. Replace it with the
     * ETag of the stored object, which changes whenever a new export is published.
     */
    aUnifiedResponse.removeETag ();
    if (aHead.eTag () != null)
      aUnifiedResponse.setETag (aHead.eTag ());

    if (aHead.lastModified () != null)
      aUnifiedResponse.setLastModified (LocalDateTime.ofInstant (aHead.lastModified (), ZoneOffset.UTC));

    // The response itself carries no content, so Content-Length must describe the export rather
    // than this response - it is sent under a distinct header to avoid contradicting the framework.
    aUnifiedResponse.setCustomResponseHeader (CHttpHeader.CACHE_CONTROL, "no-store");
    if (aHead.contentLength () != null)
      aUnifiedResponse.setCustomResponseHeader ("X-Export-Content-Length", aHead.contentLength ().toString ());
    if (aHead.contentType () != null)
      aUnifiedResponse.setCustomResponseHeader ("X-Export-Content-Type", aHead.contentType ());
  }
}
