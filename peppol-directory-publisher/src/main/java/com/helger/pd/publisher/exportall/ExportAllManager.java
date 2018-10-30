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
package com.helger.pd.publisher.exportall;

import java.io.File;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.commons.state.ESuccess;
import com.helger.photon.basic.app.io.WebFileIO;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;

@ThreadSafe
public final class ExportAllManager
{
  private static final String EXPORT_ALL_BUSINESSCARDS_XML = "export-all-businesscards.xml";
  private static final String EXPORT_ALL_BUSINESSCARDS_XLSX = "export-all-businesscards.xlsx";
  private static final Logger LOGGER = LoggerFactory.getLogger (ExportAllManager.class);

  private static final SimpleReadWriteLock s_aRWLock = new SimpleReadWriteLock ();

  private ExportAllManager ()
  {}

  @Nonnull
  private static File _getFileXML ()
  {
    return WebFileIO.getDataIO ().getFile (EXPORT_ALL_BUSINESSCARDS_XML);
  }

  static void writeFileXML (@Nonnull final IMicroDocument aDoc)
  {
    // Do it in a write lock!
    s_aRWLock.writeLock ().lock ();
    try
    {
      final File f = _getFileXML ();
      if (MicroWriter.writeToFile (aDoc, f).isFailure ())
        if (LOGGER.isErrorEnabled ())
          LOGGER.error ("Failed to export all BCs as XML to " + f.getAbsolutePath ());
    }
    finally
    {
      s_aRWLock.writeLock ().unlock ();
    }
  }

  /**
   * Stream the stored XML file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void streamFileXMLTo (@Nonnull final UnifiedResponse aUR)
  {
    // Do it in a read lock!
    s_aRWLock.readLock ().lock ();
    try
    {
      final File f = _getFileXML ();
      // setContent(IReadableResource) is lazy
      aUR.setContent (new FileSystemResource (f));
    }
    finally
    {
      s_aRWLock.readLock ().unlock ();
    }
  }

  @Nonnull
  private static File _getFileExcel ()
  {
    return WebFileIO.getDataIO ().getFile (EXPORT_ALL_BUSINESSCARDS_XLSX);
  }

  static void writeFileExcel (@Nonnull final Function <File, ESuccess> aFileWriter)
  {
    // Do it in a write lock!
    s_aRWLock.writeLock ().lock ();
    try
    {
      final File f = _getFileXML ();

      if (aFileWriter.apply (f).isFailure ())
        if (LOGGER.isErrorEnabled ())
          LOGGER.error ("Failed to export all BCs as XLSX to " + f.getAbsolutePath ());
    }
    finally
    {
      s_aRWLock.writeLock ().unlock ();
    }
  }

  /**
   * Stream the stored Excel file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void streamFileExcelTo (@Nonnull final UnifiedResponse aUR)
  {
    // Do it in a read lock!
    s_aRWLock.readLock ().lock ();
    try
    {
      final File f = _getFileExcel ();
      // setContent(IReadableResource) is lazy
      aUR.setContent (new FileSystemResource (f));
    }
    finally
    {
      s_aRWLock.readLock ().unlock ();
    }
  }
}
