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

import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.array.ArrayHelper;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Helper class for extracting and processing client certificate information.
 *
 * @author Mikael Aksamit
 */
public final class CertificateHelper
{
  private static final Logger LOGGER = LoggerFactory.getLogger (CertificateHelper.class);

  // Thread-local cache for MessageDigest to avoid repeated getInstance() calls
  private static final ThreadLocal <MessageDigest> SHA256_DIGEST = ThreadLocal.withInitial ( () -> {
    try
    {
      return MessageDigest.getInstance ("SHA-256");
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("SHA-256 algorithm not available", ex);
    }
  });

  private CertificateHelper ()
  {}

  /**
   * Extract the client certificate from an HTTP servlet request.
   *
   * @param aHttpRequest
   *        The HTTP request. May not be <code>null</code>.
   * @return The client certificate, or <code>null</code> if not present.
   */
  @Nullable
  public static X509Certificate extractClientCertificate (@Nonnull final HttpServletRequest aHttpRequest)
  {
    final Object aValue = aHttpRequest.getAttribute ("jakarta.servlet.request.X509Certificate");
    if (aValue == null)
    {
      LOGGER.warn ("No client certificates present in the request");
      return null;
    }

    if (!(aValue instanceof X509Certificate[] aRequestCerts))
    {
      LOGGER.error ("Request certificate attribute is not of type X509Certificate[] but of " + aValue.getClass ());
      return null;
    }

    if (ArrayHelper.isEmpty (aRequestCerts))
    {
      LOGGER.warn ("Client certificate array is empty");
      return null;
    }

    return aRequestCerts[0];
  }

  /**
   * Compute the SHA-256 fingerprint of a certificate.
   *
   * @param aCert
   *        The certificate. May not be <code>null</code>.
   * @return The SHA-256 fingerprint as a lowercase hex string, or
   *         <code>null</code> if computation failed.
   */
  @Nullable
  public static String computeSHA256Fingerprint (@Nonnull final X509Certificate aCert)
  {
    try
    {
      final MessageDigest aDigest = SHA256_DIGEST.get ();
      aDigest.reset (); // Reset state from any previous use
      final byte [] aFingerprint = aDigest.digest (aCert.getEncoded ());
      return HexFormat.of ().formatHex (aFingerprint);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Failed to compute SHA-256 fingerprint for certificate", ex);
      return null;
    }
  }
}
