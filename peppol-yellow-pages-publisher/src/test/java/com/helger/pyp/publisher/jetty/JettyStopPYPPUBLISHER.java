/**
 * Copyright (C) 2006-2015 BRZ GmbH
 * http://www.brz.gv.at
 *
 * All rights reserved
 */
package com.helger.pyp.publisher.jetty;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.charset.CCharset;
import com.helger.commons.io.stream.StreamHelper;

public final class JettyStopPYPPUBLISHER
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (JettyStopPYPPUBLISHER.class);

  public static void main (final String [] args) throws IOException
  {
    final Socket s = new Socket (InetAddress.getByName (null), JettyMonitor.STOP_PORT);
    try
    {
      s.setSoLinger (false, 0);

      final OutputStream out = s.getOutputStream ();
      s_aLogger.info ("Sending jetty stop request");
      out.write ((JettyMonitor.STOP_KEY + "\r\nstop\r\n").getBytes (CCharset.CHARSET_ISO_8859_1_OBJ));
      out.flush ();
    }
    catch (final ConnectException ex)
    {
      s_aLogger.warn ("Jetty is not running");
    }
    finally
    {
      StreamHelper.close (s);
    }
  }
}
