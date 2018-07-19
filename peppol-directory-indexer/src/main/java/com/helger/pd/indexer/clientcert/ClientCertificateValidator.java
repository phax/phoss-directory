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
package com.helger.pd.indexer.clientcert;

import java.security.KeyStore;
import java.security.cert.CRL;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Arrays;

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
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.exception.InitializationException;
import com.helger.pd.settings.PDConfiguredTrustStore;
import com.helger.pd.settings.PDServerConfiguration;
import com.helger.security.keystore.KeyStoreHelper;

/**
 * Extract provided certificates from HTTPS requests. These are the client
 * certificates submitted by the user. Client requests are only present in HTTPS
 * requests and will never be present in pure HTTP requests.
 *
 * @author Philip Helger
 */
@Immutable
public final class ClientCertificateValidator
{
  /** The client ID to be used, if client certificate validator is disabled! */
  public static final String INSECURE_DEBUG_CLIENT = "insecure-debug-client";

  private static final Logger LOGGER = LoggerFactory.getLogger (ClientCertificateValidator.class);

  @PresentForCodeCoverage
  private static final ClientCertificateValidator s_aInstance = new ClientCertificateValidator ();

  private static boolean s_bIsCheckDisabled = !PDServerConfiguration.isClientCertificateValidationActive ();

  /**
   * All PEPPOL root certificates from the truststore configuration. Never
   * empty.
   */
  private static ICommonsList <X509Certificate> s_aPeppolSMPRootCerts = new CommonsArrayList <> ();

  /** Sorted list with all issuers we're accepting. Never empty. */
  private static final ICommonsList <X500Principal> s_aSearchIssuers = new CommonsArrayList <> ();

  /**
   * This method is only for testing purposes to disable the complete client
   * certificate check, so that the tests can be performed, even if no SMP
   * certificate for testing is present.
   *
   * @param bCheckDisabled
   *        <code>true</code> to always return <code>true</code> for the client
   *        certificate check, <code>false</code> to enable client certificate
   *        check.
   * @see PDServerConfiguration#isClientCertificateValidationActive()
   */
  @VisibleForTesting
  public static void allowAllForTests (final boolean bCheckDisabled)
  {
    s_bIsCheckDisabled = bCheckDisabled;
  }

  private static void _initCertificateIssuers ()
  {
    // Get the certificate issuer we need
    final ICommonsList <String> aIssuersToSearch = PDServerConfiguration.getAllClientCertIssuer ();

    // Throws a runtime exception on syntax error anyway :)
    for (final String sIssuerToSearch : aIssuersToSearch)
      s_aSearchIssuers.add (new X500Principal (sIssuerToSearch));

    if (s_aSearchIssuers.isEmpty ())
    {
      if (s_bIsCheckDisabled)
        LOGGER.warn ("The configuration file contains no entry for the client certificate issuer");
      else
        throw new InitializationException ("The configuration file is missing the entry for the client certificate issuer");
    }
    else
      LOGGER.info ("The following client certificate issuer(s) are valid: " + s_aSearchIssuers);
  }

  private static void _initCerts ()
  {
    // Get data from config file
    for (final PDConfiguredTrustStore aTS : PDServerConfiguration.getAllTrustStores ())
    {
      X509Certificate aCert;
      try
      {
        final KeyStore aKS = KeyStoreHelper.loadKeyStoreDirect (aTS.getType (), aTS.getPath (), aTS.getPassword ());
        aCert = (X509Certificate) aKS.getCertificate (aTS.getAlias ());
      }
      catch (final Throwable t)
      {
        final String sErrorMsg = "Failed to read trust store from '" + aTS.getPath () + "'";
        LOGGER.error (sErrorMsg);
        throw new InitializationException (sErrorMsg, t);
      }

      // Check if both root certificates could be loaded
      if (aCert == null)
        throw new InitializationException ("Failed to resolve alias '" +
                                           aTS.getAlias () +
                                           "' in trust store '" +
                                           aTS.getPath () +
                                           "'!");
      s_aPeppolSMPRootCerts.add (aCert);

      LOGGER.info ("Root certificate loaded successfully from trust store '" +
                      aTS.getPath () +
                      "' with alias '" +
                      aTS.getAlias () +
                      "'; root certificate serial=" +
                      aCert.getSerialNumber ().toString (16) +
                      "; root certficate issuer=" +
                      aCert.getIssuerX500Principal ().getName ());
    }

    if (s_aPeppolSMPRootCerts.isEmpty ())
    {
      if (s_bIsCheckDisabled)
        LOGGER.warn ("Server configuration contains no trusted root certificate configuration!");
      else
        throw new InitializationException ("Server configuration contains no trusted root certificate configuration!");
    }
  }

  static
  {
    _initCertificateIssuers ();
    _initCerts ();
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
                                            @Nonnull final Iterable <? extends CRL> aCRLs,
                                            @Nullable final LocalDateTime aDT)
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
        aCert.checkValidity (PDTFactory.createDate (aDT));
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
      final ICommonsMap <String, Rdn> aParts = new CommonsHashMap <> ();
      for (final Rdn aRdn : aLdapName.getRdns ())
        aParts.put (aRdn.getType (), aRdn);

      // Re-order - least important item comes first (=reverse order)!
      final String sSubjectName = new LdapName (new CommonsArrayList <> (aParts.get ("C"),
                                                                         aParts.get ("O"),
                                                                         aParts.get ("CN"))).toString ();

      // subject-name + ":" + serial number hexstring
      return sSubjectName + ':' + aCert.getSerialNumber ().toString (16);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Failed to parse '" + aCert.getSubjectX500Principal ().getName () + "'", ex);
      return null;
    }
  }

  /**
   * Extract certificates from request and validate them.
   *
   * @param aHttpRequest
   *        The HTTP request to use. May not be <code>null</code>.
   * @param sLogPrefix
   *        The context - for logging only. May not be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @Nonnull
  public static ClientCertificateValidationResult verifyClientCertificate (@Nonnull final HttpServletRequest aHttpRequest,
                                                                           @Nonnull final String sLogPrefix)
  {
    if (s_bIsCheckDisabled)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug (sLogPrefix +
                         "Client certificate is considered valid because the 'allow all' for tests is set!");
      return ClientCertificateValidationResult.createSuccess (INSECURE_DEBUG_CLIENT);
    }

    // This is how to get client certificate from request
    final Object aValue = aHttpRequest.getAttribute ("javax.servlet.request.X509Certificate");
    if (aValue == null)
    {
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn (sLogPrefix + "No client certificates present in the request");
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
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn (sLogPrefix + "No client certificates passed for validation");
      return ClientCertificateValidationResult.createFailure ();
    }

    // OK, we have a non-empty, type checked Certificate array

    // TODO: determine CRLs
    final ICommonsList <CRL> aCRLs = new CommonsArrayList <> ();

    // Verify for "now"
    final LocalDateTime aVerificationDate = PDTFactory.getCurrentLocalDateTime ();

    // Search the certificate from the request matching our required issuers
    X509Certificate aClientCertToVerify = null;
    {
      for (final X509Certificate aCert : aRequestCerts)
      {
        final X500Principal aIssuer = aCert.getIssuerX500Principal ();
        if (s_aSearchIssuers.contains (aIssuer))
        {
          if (LOGGER.isInfoEnabled ())
            LOGGER.info (sLogPrefix +
                            "  Using the following client certificate issuer for verification: '" +
                            aIssuer +
                            "'");
          aClientCertToVerify = aCert;
          break;
        }
      }
      // Do we have a certificate to verify?
      if (aClientCertToVerify == null)
      {
        throw new IllegalStateException ("Found no client certificate that was issued by one of the " +
                                         s_aSearchIssuers.size () +
                                         " required issuers. Provided certs are: " +
                                         Arrays.toString (aRequestCerts));
      }
    }

    final String sClientID = getClientUniqueID (aClientCertToVerify);

    // This is the main verification process against the PEPPOL SMP root
    // certificate
    for (final X509Certificate aRootCert : s_aPeppolSMPRootCerts)
    {
      final String sVerifyErrorMsg = _verifyCertificate (aClientCertToVerify, aRootCert, aCRLs, aVerificationDate);
      if (sVerifyErrorMsg == null)
      {
        if (LOGGER.isInfoEnabled ())
          LOGGER.info (sLogPrefix + "  Passed client certificate is valid");
        return ClientCertificateValidationResult.createSuccess (sClientID);
      }
    }

    if (LOGGER.isWarnEnabled ())
      LOGGER.warn ("Client certificate is invalid: " + sClientID);
    return ClientCertificateValidationResult.createFailure ();
  }
}
