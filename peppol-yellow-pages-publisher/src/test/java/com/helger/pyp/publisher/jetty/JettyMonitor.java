/**
 * Copyright (C) 2006-2015 BRZ GmbH
 * http://www.brz.gv.at
 *
 * All rights reserved
 */
package com.helger.pyp.publisher.jetty;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.DevelopersNote;
import com.helger.commons.charset.CCharset;

public final class JettyMonitor extends Thread
{
  public static final int STOP_PORT = 8079;
  public static final String STOP_KEY = "secret";

  private static final Logger s_aLogger = LoggerFactory.getLogger (JettyMonitor.class);
  private final int m_nPort;
  private final String m_sKey;
  private final ServerSocket m_aServerSocket;

  public JettyMonitor () throws IOException
  {
    this (STOP_PORT, STOP_KEY);
  }

  private JettyMonitor (final int nPort, final String sKey) throws IOException
  {
    m_nPort = nPort;
    m_sKey = sKey;
    setDaemon (true);
    setName ("JettyStopMonitor");
    m_aServerSocket = new ServerSocket (m_nPort, 1, InetAddress.getByName (null));
    if (m_aServerSocket == null)
      s_aLogger.error ("WARN: Not listening on monitor port: " + m_nPort);
  }

  @Override
  @DevelopersNote ("Consider throwing a runtime exception instead of System.exit (find bugs)")
  public void run ()
  {
    while (true)
    {
      try (final Socket aSocket = m_aServerSocket.accept ())
      {
        final LineNumberReader lin = new LineNumberReader (new InputStreamReader (aSocket.getInputStream (),
                                                                                  CCharset.CHARSET_ISO_8859_1_OBJ));
        final String sKey = lin.readLine ();
        if (!m_sKey.equals (sKey))
          continue;

        final String sCmd = lin.readLine ();
        if ("stop".equals (sCmd))
        {
          try
          {
            aSocket.close ();
            m_aServerSocket.close ();
          }
          catch (final Exception e)
          {
            s_aLogger.error ("Failed to close socket", e);
          }
          System.exit (0);
        }
      }
      catch (final Exception e)
      {
        s_aLogger.error ("Error reading from socket", e);
        break;
      }
    }
  }
}
