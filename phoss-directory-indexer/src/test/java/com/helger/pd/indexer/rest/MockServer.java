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
package com.helger.pd.indexer.rest;

import java.net.URI;
import java.util.Map;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.uri.UriComponent;

import com.helger.base.io.stream.StreamHelper;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsMap;
import com.helger.io.resource.ClassPathResource;
import com.helger.peppol.security.PeppolTrustStores;
import com.helger.security.keystore.EKeyStoreType;

import jakarta.annotation.Nonnull;
import jakarta.servlet.Servlet;

/**
 * Main class.
 */
final class MockServer
{
  // Base URI the Grizzly HTTP server will listen on
  public static final String BASE_URI_HTTP = "http://localhost:9090/unittest/";
  public static final String BASE_URI_HTTPS = "https://localhost:9090/unittest/";

  @Nonnull
  private static WebappContext _createContext (final URI u,
                                               final Class <? extends Servlet> aServletClass,
                                               final Servlet aServlet,
                                               final Map <String, String> aInitParams,
                                               final Map <String, String> aContextInitParams)
  {
    String path = u.getPath ();
    if (path == null)
      throw new IllegalArgumentException ("The URI path, of the URI " + u + ", must be non-null");
    if (path.isEmpty ())
      throw new IllegalArgumentException ("The URI path, of the URI " + u + ", must be present");
    if (path.charAt (0) != '/')
      throw new IllegalArgumentException ("The URI path, of the URI " + u + ". must start with a '/'");
    path = String.format ("/%s", UriComponent.decodePath (u.getPath (), true).get (1).toString ());

    final WebappContext aContext = new WebappContext ("GrizzlyContext", path);
    ServletRegistration registration;
    if (aServletClass != null)
      registration = aContext.addServlet (aServletClass.getName (), aServletClass);
    else
      registration = aContext.addServlet (aServlet.getClass ().getName (), aServlet);
    registration.addMapping ("/*");

    if (aContextInitParams != null)
      for (final Map.Entry <String, String> e : aContextInitParams.entrySet ())
        aContext.setInitParameter (e.getKey (), e.getValue ());

    if (aInitParams != null)
      registration.setInitParameters (aInitParams);

    return aContext;
  }

  @Nonnull
  private static WebappContext _createContext (final String sURI)
  {
    final ICommonsMap <String, String> aInitParams = new CommonsHashMap <> ();
    aInitParams.put ("jersey.config.server.provider.packages",
                     com.helger.pd.indexer.rest.IndexerResource.class.getPackage ().getName ());
    return _createContext (URI.create (sURI), ServletContainer.class, null, aInitParams, null);
  }

  /**
   * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
   *
   * @return Grizzly HTTP server.
   */
  @Nonnull
  public static HttpServer startRegularServer ()
  {
    final WebappContext aContext = _createContext (BASE_URI_HTTP);

    // create and start a new instance of grizzly http server
    // exposing the Jersey application at BASE_URI
    final HttpServer ret = GrizzlyHttpServerFactory.createHttpServer (URI.create (BASE_URI_HTTP));
    aContext.deploy (ret);
    return ret;
  }

  /**
   * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
   *
   * @return Grizzly HTTP server.
   */
  public static HttpServer startSecureServer ()
  {
    if (false)
      System.setProperty ("javax.net.debug", "all");

    final WebappContext aContext = _createContext (BASE_URI_HTTPS);

    final SSLContextConfigurator aSSLContext = new SSLContextConfigurator ();
    aSSLContext.setKeyStoreFile ("src/test/resources/test-https-keystore.jks");
    aSSLContext.setKeyStorePass ("password");
    aSSLContext.setKeyStoreType (EKeyStoreType.JKS.getID ());
    aSSLContext.setTrustStoreBytes (StreamHelper.getAllBytes (new ClassPathResource (PeppolTrustStores.Config2025.TRUSTSTORE_SMP_TEST_CLASSPATH)));
    aSSLContext.setTrustStorePass (PeppolTrustStores.TRUSTSTORE_PASSWORD);
    aSSLContext.setTrustStoreType (PeppolTrustStores.Config2025.TRUSTSTORE_TYPE.getID ());
    aSSLContext.setSecurityProtocol ("TLSv1.2");

    final SSLEngineConfigurator aSSLEngine = new SSLEngineConfigurator (aSSLContext);
    aSSLEngine.setClientMode (false);
    aSSLEngine.setNeedClientAuth (true);
    aSSLEngine.setEnabledCipherSuites (new String [] { "TLS_RSA_WITH_AES_128_CBC_SHA",
                                                       "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                                                       "TLS_RSA_WITH_AES_128_CBC_SHA256",
                                                       "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                                                       "TLS_RSA_WITH_AES_128_CBC_SHA" });

    // create and start a new instance of grizzly https server
    // exposing the Jersey application at BASE_URI
    final HttpServer ret = GrizzlyHttpServerFactory.createHttpServer (URI.create (BASE_URI_HTTPS),
                                                                      (GrizzlyHttpContainer) null,
                                                                      true,
                                                                      aSSLEngine,
                                                                      true);
    aContext.deploy (ret);
    return ret;
  }
}
