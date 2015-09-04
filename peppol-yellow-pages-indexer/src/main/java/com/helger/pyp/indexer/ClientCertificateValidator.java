/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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
package com.helger.pyp.indexer;

import java.security.KeyStore;
import java.security.cert.CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.PresentForCodeCoverage;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.utils.KeyStoreHelper;
import com.helger.pyp.settings.PYPSettings;

/**
 * Extract certificates from HTTP requests. These are the client certificates
 * submitted by the user.
 *
 * @author Philip Helger
 */
@Immutable
public final class ClientCertificateValidator
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (ClientCertificateValidator.class);

  @PresentForCodeCoverage
  private static final ClientCertificateValidator s_aInstance = new ClientCertificateValidator ();

  private static X509Certificate s_aPeppolSMPRootCert;
  private static X509Certificate s_aPeppolSMPRootCertAlternative;

  /** Sorted list with all issuers we're accepting. Never empty. */
  private static List <X500Principal> s_aSearchIssuers = new ArrayList <> ();

  private static void _initCertificateIssuers ()
  {
    // Get the certificate issuer we need
    final String sIssuerToSearch = PYPSettings.getClientCertIssuer ();
    if (StringHelper.hasNoText (sIssuerToSearch))
      throw new InitializationException ("The settings file is missing the entry for the client certificate issuer");

    // Throws a runtime exception on syntax error anyway :)
    s_aSearchIssuers.add (new X500Principal (sIssuerToSearch));

    // Optional alternative issuer
    final String sIssuerToSearchAlternative = PYPSettings.getClientCertIssuerAlternative ();
    if (StringHelper.hasText (sIssuerToSearchAlternative))
    {
      // Throws a runtime exception on syntax error anyway :)
      s_aSearchIssuers.add (new X500Principal (sIssuerToSearchAlternative));
    }

    s_aLogger.info ("The following client certificate issuer(s) are valid: " + s_aSearchIssuers);
  }

  private static void _initRootCert ()
  {
    // Get data from config file
    final String sTrustStorePath = PYPSettings.getTruststoreLocation ();
    final String sTrustStorePassword = PYPSettings.getTruststorePassword ();
    final String sTrustStoreAlias = PYPSettings.getTruststoreAlias ();

    // Load keystores
    try
    {
      final KeyStore aKS = KeyStoreHelper.loadKeyStore (sTrustStorePath, sTrustStorePassword);
      s_aPeppolSMPRootCert = (X509Certificate) aKS.getCertificate (sTrustStoreAlias);
    }
    catch (final Throwable t)
    {
      final String sErrorMsg = "Failed to read trust store from '" + sTrustStorePath + "'";
      s_aLogger.error (sErrorMsg);
      throw new InitializationException (sErrorMsg, t);
    }

    // Check if both root certificates could be loaded
    if (s_aPeppolSMPRootCert == null)
      throw new InitializationException ("Failed to resolve alias '" + sTrustStoreAlias + "' in trust store!");
    s_aLogger.info ("Root certificate loaded successfully from trust store '" +
                    sTrustStorePath +
                    "' with alias '" +
                    sTrustStoreAlias +
                    "'");
  }

  private static void _initRootCertAlternative ()
  {
    // Get data from config file
    final String sTrustStorePath = PYPSettings.getTruststoreLocationAlternative ();
    final String sTrustStorePassword = PYPSettings.getTruststorePasswordAlternative ();
    final String sTrustStoreAlias = PYPSettings.getTruststoreAliasAlternative ();

    if (StringHelper.hasText (sTrustStorePath) &&
        StringHelper.hasText (sTrustStorePath) &&
        StringHelper.hasText (sTrustStorePath))
    {
      // Load keystores
      try
      {
        final KeyStore aKS = KeyStoreHelper.loadKeyStore (sTrustStorePath, sTrustStorePassword);
        s_aPeppolSMPRootCertAlternative = (X509Certificate) aKS.getCertificate (sTrustStoreAlias);
      }
      catch (final Throwable t)
      {
        final String sErrorMsg = "Failed to read alternative trust store from '" + sTrustStorePath + "'";
        s_aLogger.error (sErrorMsg);
        throw new InitializationException (sErrorMsg, t);
      }

      // Check if both root certificates could be loaded
      if (s_aPeppolSMPRootCertAlternative == null)
        throw new InitializationException ("Failed to resolve alias '" +
                                           sTrustStoreAlias +
                                           "' in alternative trust store!");
      s_aLogger.info ("Alternative root certificate loaded successfully from trust store '" +
                      sTrustStorePath +
                      "' with alias '" +
                      sTrustStoreAlias +
                      "'");
    }
  }

  static
  {
    _initCertificateIssuers ();
    _initRootCert ();
    _initRootCertAlternative ();
  }

  private ClientCertificateValidator ()
  {}

  /**
   * @param aCert
   *        The certificate to validate. May not be <code>null</code>.
   * @param aTrustedRootCert
   *        The trusted root certificate. E.g. the PEPPOL or the OpenPEPPOL SMP
   *        root certificate.
   * @param aCRLs
   *        A non-<code>null</code> list with revocation lists to handle
   * @param aDT
   *        The date and time which should be used for checking. May be
   *        <code>null</code> to indicate "now".
   * @return <code>null</code> in case of success, the error message otherwise!
   */
  @Nullable
  private static String _verifyCertificate (@Nonnull final X509Certificate aCert,
                                            @Nonnull final X509Certificate aTrustedRootCert,
                                            @Nonnull final Collection <CRL> aCRLs,
                                            @Nullable final Date aDT)
  {
    if (aCert.hasUnsupportedCriticalExtension ())
      return "Certificate has unsupported critical extension";

    // Verify the current certificate using the issuer certificate
    try
    {
      aCert.verify (aTrustedRootCert.getPublicKey ());
    }
    catch (final Exception ex)
    {
      return ex.getMessage ();
    }

    // Check timely validity (at a certain date/time or simply now)
    try
    {
      if (aDT != null)
        aCert.checkValidity (aDT);
      else
        aCert.checkValidity ();
    }
    catch (final Exception ex)
    {
      return ex.getMessage ();
    }

    // Check passed revocation lists
    if (aCRLs != null)
      for (final CRL aCRL : aCRLs)
        if (aCRL.isRevoked (aCert))
          return "Certificate is revoked according to " + aCRL.toString ();

    // null means OK :)
    return null;
  }

  /**
   * Extract certificates from request and validate them.
   *
   * @param aHttpRequest
   *        The HTTP request to use.
   * @return <code>true</code> if valid, <code>false</code> otherwise.
   */
  public static boolean isClientCertificateValid (@Nonnull final HttpServletRequest aHttpRequest)
  {
    // This is how to get client certificate from request
    final Object aValue = aHttpRequest.getAttribute ("javax.servlet.request.X509Certificate");
    if (aValue == null)
    {
      s_aLogger.warn ("No client certificates present in the request");
      return false;
    }

    // type check
    if (!(aValue instanceof X509Certificate []))
      throw new IllegalStateException ("Request value is not of type X509Certificate[] but of " + aValue.getClass ());

    // Main checking
    return isClientCertificateValid ((X509Certificate []) aValue);
  }

  public static boolean isClientCertificateValid (@Nullable final X509Certificate [] aRequestCerts)
  {
    if (ArrayHelper.isEmpty (aRequestCerts))
    {
      // Empty array
      s_aLogger.warn ("No client certificates passed for validation");
      return false;
    }

    // OK, we have a non-empty, type checked Certificate array

    // TODO: determine CRLs
    final Collection <CRL> aCRLs = new ArrayList <CRL> ();

    // Verify for "now"
    final Date aVerificationDate = new Date ();

    // Search the certificate from the request matching our required issuers
    X509Certificate aClientCertToVerify = null;
    {
      for (final X509Certificate aCert : aRequestCerts)
      {
        final X500Principal aIssuer = aCert.getIssuerX500Principal ();
        if (s_aSearchIssuers.contains (aIssuer))
        {
          s_aLogger.info ("  Using the following client certificate issuer for verification: '" + aIssuer + "'");
          aClientCertToVerify = aCert;
          break;
        }
      }
      // Do we have a certificate to verify?
      if (aClientCertToVerify == null)
        throw new IllegalStateException ("Found no client certificate that was issued by one of the required issuers.");
    }

    // This is the main verification process against the PEPPOL SMP root
    // certificate
    String sVerifyErrorMsg = _verifyCertificate (aClientCertToVerify, s_aPeppolSMPRootCert, aCRLs, aVerificationDate);
    if (sVerifyErrorMsg == null)
    {
      s_aLogger.info ("  Passed client certificate is valid");
      return true;
    }

    // try alternative (if present)
    if (s_aPeppolSMPRootCertAlternative != null)
    {
      final String sPeppolVerifyMsgAlternative = _verifyCertificate (aClientCertToVerify,
                                                                     s_aPeppolSMPRootCertAlternative,
                                                                     aCRLs,
                                                                     aVerificationDate);
      if (sPeppolVerifyMsgAlternative == null)
      {
        s_aLogger.info ("  Passed client certificate is valid (alternative)");
        return true;
      }

      sVerifyErrorMsg = sVerifyErrorMsg + ' ' + sPeppolVerifyMsgAlternative;
    }

    s_aLogger.warn ("Client certificate is invalid: " +
                    sVerifyErrorMsg +
                    "; root certificate serial=" +
                    s_aPeppolSMPRootCert.getSerialNumber ().toString (16) +
                    "; root certficate issuer=" +
                    s_aPeppolSMPRootCert.getIssuerX500Principal ().getName ());
    return false;
  }
}
