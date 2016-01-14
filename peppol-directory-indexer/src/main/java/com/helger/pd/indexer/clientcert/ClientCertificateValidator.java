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
package com.helger.pd.indexer.clientcert;

import java.security.KeyStore;
import java.security.cert.CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.PresentForCodeCoverage;
import com.helger.commons.annotation.VisibleForTesting;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.string.StringHelper;
import com.helger.pd.settings.PDSettings;
import com.helger.peppol.utils.KeyStoreHelper;

/**
 * Extract certificates from HTTP requests. These are the client certificates
 * submitted by the user.
 *
 * @author Philip Helger
 */
@Immutable
public final class ClientCertificateValidator
{
  /** The client ID to be used, if client certificate validator is disabled! */
  public static final String INSECURE_DEBUG_CLIENT = "insecure-debug-client";

  private static final Logger s_aLogger = LoggerFactory.getLogger (ClientCertificateValidator.class);

  @PresentForCodeCoverage
  private static final ClientCertificateValidator s_aInstance = new ClientCertificateValidator ();

  private static boolean s_bCheckDisabled = !PDSettings.isClientCertificateValidationActive ();
  private static X509Certificate s_aPeppolSMPRootCert;
  private static X509Certificate s_aPeppolSMPRootCertAlternative;

  /** Sorted list with all issuers we're accepting. Never empty. */
  private static List <X500Principal> s_aSearchIssuers = new ArrayList <> ();

  /**
   * This method is only for testing purposes to disable the complete client
   * certificate check, so that the tests can be performed, even if no SMP
   * certificate for testing is present.
   *
   * @param bAllowAll
   *        <code>true</code> to always return <code>true</code> for the client
   *        certificate check
   */
  @VisibleForTesting
  public static void allowAllForTests (final boolean bAllowAll)
  {
    s_bCheckDisabled = bAllowAll;
  }

  private static void _initCertificateIssuers ()
  {
    // Get the certificate issuer we need
    final String sIssuerToSearch = PDSettings.getClientCertIssuer ();
    if (StringHelper.hasNoText (sIssuerToSearch))
      throw new InitializationException ("The settings file is missing the entry for the client certificate issuer");

    // Throws a runtime exception on syntax error anyway :)
    s_aSearchIssuers.add (new X500Principal (sIssuerToSearch));

    // Optional alternative issuer
    final String sIssuerToSearchAlternative = PDSettings.getClientCertIssuerAlternative ();
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
    final String sTrustStorePath = PDSettings.getTruststoreLocation ();
    final String sTrustStorePassword = PDSettings.getTruststorePassword ();
    final String sTrustStoreAlias = PDSettings.getTruststoreAlias ();

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
                    "'; root certificate serial=" +
                    s_aPeppolSMPRootCert.getSerialNumber ().toString (16) +
                    "; root certficate issuer=" +
                    s_aPeppolSMPRootCert.getIssuerX500Principal ().getName ());
  }

  private static void _initRootCertAlternative ()
  {
    // Get data from config file
    final String sTrustStorePath = PDSettings.getTruststoreLocationAlternative ();
    final String sTrustStorePassword = PDSettings.getTruststorePasswordAlternative ();
    final String sTrustStoreAlias = PDSettings.getTruststoreAliasAlternative ();

    if (StringHelper.hasText (sTrustStorePath) &&
        StringHelper.hasText (sTrustStorePassword) &&
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
                      "'; root certificate serial=" +
                      s_aPeppolSMPRootCertAlternative.getSerialNumber ().toString (16) +
                      "; root certficate issuer=" +
                      s_aPeppolSMPRootCertAlternative.getIssuerX500Principal ().getName ());
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

  @Nullable
  static String getClientUniqueID (@Nonnull final X509Certificate aCert)
  {
    try
    {
      // subject principal name must be in the order CN=XX,O=YY,C=ZZ
      // In some JDK versions it is O=YY,CN=XX,C=ZZ instead (e.g. 1.6.0_45)
      final LdapName aLdapName = new LdapName (aCert.getSubjectX500Principal ().getName ());

      // Make a map from type to name
      final Map <String, Rdn> aParts = new HashMap <String, Rdn> ();
      for (final Rdn aRdn : aLdapName.getRdns ())
        aParts.put (aRdn.getType (), aRdn);

      // Re-order - least important item comes first (=reverse order)!
      final String sSubjectName = new LdapName (CollectionHelper.newList (aParts.get ("C"),
                                                                          aParts.get ("O"),
                                                                          aParts.get ("CN"))).toString ();

      // subject-name + ":" + serial number hexstring
      return sSubjectName + ':' + aCert.getSerialNumber ().toString (16);
    }
    catch (final Exception ex)
    {
      s_aLogger.error ("Failed to parse '" + aCert.getSubjectX500Principal ().getName () + "'", ex);
      return null;
    }
  }

  /**
   * Extract certificates from request and validate them.
   *
   * @param aHttpRequest
   *        The HTTP request to use.
   * @return Never <code>null</code>.
   */
  @Nonnull
  public static ClientCertificateValidationResult verifyClientCertificate (@Nonnull final HttpServletRequest aHttpRequest)
  {
    if (s_bCheckDisabled)
    {
      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Client certificate is considered valid because the 'allow all' for tests is set!");
      return ClientCertificateValidationResult.createSuccess (INSECURE_DEBUG_CLIENT);
    }

    // This is how to get client certificate from request
    final Object aValue = aHttpRequest.getAttribute ("javax.servlet.request.X509Certificate");
    if (aValue == null)
    {
      s_aLogger.warn ("No client certificates present in the request");
      return ClientCertificateValidationResult.createFailure ();
    }

    // type check
    if (!(aValue instanceof X509Certificate []))
      throw new IllegalStateException ("Request value is not of type X509Certificate[] but of " + aValue.getClass ());

    // Main checking
    final X509Certificate [] aRequestCerts = (X509Certificate []) aValue;
    if (ArrayHelper.isEmpty (aRequestCerts))
    {
      // Empty array
      s_aLogger.warn ("No client certificates passed for validation");
      return ClientCertificateValidationResult.createFailure ();
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

    final String sClientID = getClientUniqueID (aClientCertToVerify);

    // This is the main verification process against the PEPPOL SMP root
    // certificate
    String sVerifyErrorMsg = _verifyCertificate (aClientCertToVerify, s_aPeppolSMPRootCert, aCRLs, aVerificationDate);
    if (sVerifyErrorMsg == null)
    {
      s_aLogger.info ("  Passed client certificate is valid");
      return ClientCertificateValidationResult.createSuccess (sClientID);
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
        return ClientCertificateValidationResult.createSuccess (sClientID);
      }

      sVerifyErrorMsg += ' ' + sPeppolVerifyMsgAlternative;
    }

    s_aLogger.warn ("Client certificate is invalid: " + sVerifyErrorMsg);
    return ClientCertificateValidationResult.createFailure ();
  }
}
