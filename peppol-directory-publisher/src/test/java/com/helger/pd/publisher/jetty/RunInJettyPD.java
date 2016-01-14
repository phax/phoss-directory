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
package com.helger.pd.publisher.jetty;

import java.io.File;

import javax.annotation.concurrent.Immutable;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.FragmentConfiguration;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;

import com.helger.commons.system.SystemProperties;

/**
 * Run as a standalone web application in Jetty on port 8080.<br>
 * http://localhost:8080/
 *
 * @author Philip Helger
 */
@Immutable
public final class RunInJettyPD
{
  private static final String RESOURCE_PREFIX = "target/webapp-classes";

  public static void main (final String [] args) throws Exception
  {
    run (8080);
  }

  public static void run (final int nPort) throws Exception
  {
    if (System.getSecurityManager () != null)
      throw new IllegalStateException ("Security Manager is set but not supported - aborting!");

    // Must be directly called on System to have an effect!
    System.setProperty ("log4j2.disable.jmx", "true");

    // Create main server
    final Server aServer = new Server ();
    // Create connector on Port
    final ServerConnector aConnector = new ServerConnector (aServer);
    aConnector.setPort (nPort);
    aConnector.setIdleTimeout (30000);
    aServer.setConnectors (new Connector [] { aConnector });

    final WebAppContext aWebAppCtx = new WebAppContext ();
    aWebAppCtx.setDescriptor (RESOURCE_PREFIX + "/WEB-INF/web.xml");
    aWebAppCtx.setResourceBase (RESOURCE_PREFIX);
    aWebAppCtx.setContextPath ("/");
    aWebAppCtx.setTempDirectory (new File (SystemProperties.getTmpDir () +
                                           '/' +
                                           RunInJettyPD.class.getName ()));
    aWebAppCtx.setParentLoaderPriority (true);
    aWebAppCtx.setThrowUnavailableOnStartupException (true);
    aWebAppCtx.setCopyWebInf (true);
    aWebAppCtx.setCopyWebDir (true);
    // Required for fragment loading to be performed
    aWebAppCtx.setExtraClasspath (SystemProperties.getJavaClassPath ());
    // Required for annotation processing
    aWebAppCtx.setConfigurations (new Configuration [] { new WebInfConfiguration (),
                                                         new WebXmlConfiguration (),
                                                         new MetaInfConfiguration (),
                                                         new FragmentConfiguration (),
                                                         new AnnotationConfiguration (),
                                                         new JettyWebXmlConfiguration () });
    aServer.setHandler (aWebAppCtx);

    // Setting final properties
    // Stops the server when ctrl+c is pressed (registers to
    // Runtime.addShutdownHook)
    aServer.setStopAtShutdown (true);
    // Starting shutdown listener thread
    if (nPort == 8080)
      new JettyMonitor ().start ();

    try
    {
      aServer.start ();
      if (aWebAppCtx.isFailed ())
      {
        aServer.stop ();
        System.err.println ("Failed to start context. Server has been shut down!");
      }
      else
        aServer.join ();
    }
    catch (final Exception ex)
    {
      ex.printStackTrace ();
    }
  }
}
