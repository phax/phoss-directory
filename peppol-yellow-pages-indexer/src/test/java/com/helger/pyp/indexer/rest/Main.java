package com.helger.pyp.indexer.rest;

import java.net.URI;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Main class.
 */
final class Main
{
  // Base URI the Grizzly HTTP server will listen on
  public static final String BASE_URI = "https://localhost:9090/unittest/";

  /**
   * Starts Grizzly HTTP server exposing JAX-RS resources defined in this
   * application.
   *
   * @return Grizzly HTTP server.
   */
  public static HttpServer startRegularServer ()
  {
    // create a resource config that scans for JAX-RS resources and providers
    // in com.example package
    final ResourceConfig rc = new ResourceConfig ().packages (com.helger.pyp.indexer.rest.IndexerResource.class.getPackage ()
                                                                                                               .getName ());

    // create and start a new instance of grizzly http server
    // exposing the Jersey application at BASE_URI
    return GrizzlyHttpServerFactory.createHttpServer (URI.create (BASE_URI), rc);
  }

  /**
   * Starts Grizzly HTTP server exposing JAX-RS resources defined in this
   * application.
   *
   * @return Grizzly HTTP server.
   */
  public static HttpServer startSecureServer ()
  {
    if (false)
      System.setProperty ("javax.net.debug", "all");

    // create a resource config that scans for JAX-RS resources and providers
    // in com.example package
    final ResourceConfig rc = new ResourceConfig ().packages (com.helger.pyp.indexer.rest.IndexerResource.class.getPackage ()
                                                                                                               .getName ());

    final SSLContextConfigurator sslCon = new SSLContextConfigurator ();
    sslCon.setKeyStoreFile ("src/test/resources/test-https-keystore.jks");
    sslCon.setKeyStorePass ("password");
    sslCon.setKeyStoreType ("JKS");

    // create and start a new instance of grizzly https server
    // exposing the Jersey application at BASE_URI
    return GrizzlyHttpServerFactory.createHttpServer (URI.create (BASE_URI),
                                                      rc,
                                                      true,
                                                      new SSLEngineConfigurator (sslCon, false, false, false),
                                                      true);
  }
}
