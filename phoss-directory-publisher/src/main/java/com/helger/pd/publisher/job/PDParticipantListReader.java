/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.job;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.numeric.mutable.MutableInt;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedSet;
import com.helger.diagnostics.error.IError;
import com.helger.io.file.FileHelper;
import com.helger.io.resource.FileSystemResource;
import com.helger.io.stream.StreamHelperExt;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.xml.sax.CollectingSAXErrorHandler;
import com.helger.xml.serialize.read.SAXReader;
import com.helger.xml.serialize.read.SAXReaderSettings;

/**
 * Reads a list of participant identifiers from an uploaded file. Two formats are supported and the
 * format is detected from the first non-whitespace character of the file:
 * <ul>
 * <li>XML - every element called <code>participant</code> with the attributes <code>scheme</code>
 * and <code>value</code> is used, no matter where in the document it appears. That covers the
 * participant list as well as the full Business Card export.</li>
 * <li>Text - one URI encoded participant identifier per line, e.g.
 * <code>iso6523-actorid-upis::9915:test</code>. Lines are trimmed, empty lines and lines starting
 * with <code>#</code> are ignored.</li>
 * </ul>
 *
 * @author Philip Helger
 * @since 0.17.2
 */
public final class PDParticipantListReader
{
  /** The format of the file that was read */
  public enum EFormat
  {
    XML,
    TEXT
  }

  /**
   * The outcome of reading a participant list file.
   *
   * @author Philip Helger
   */
  public static final class ReadResult
  {
    private final EFormat m_eFormat;
    private final ESuccess m_eSuccess;
    private final ICommonsOrderedSet <IParticipantIdentifier> m_aParticipantIDs;
    private final ICommonsList <String> m_aInvalidEntries;
    private final ICommonsList <IError> m_aErrors;
    private final int m_nDuplicateCount;

    ReadResult (@NonNull final EFormat eFormat,
                @NonNull final ESuccess eSuccess,
                @NonNull final ICommonsOrderedSet <IParticipantIdentifier> aParticipantIDs,
                @NonNull final ICommonsList <String> aInvalidEntries,
                @NonNull final ICommonsList <IError> aErrors,
                @Nonnegative final int nDuplicateCount)
    {
      m_eFormat = eFormat;
      m_eSuccess = eSuccess;
      m_aParticipantIDs = aParticipantIDs;
      m_aInvalidEntries = aInvalidEntries;
      m_aErrors = aErrors;
      m_nDuplicateCount = nDuplicateCount;
    }

    /**
     * @return The detected format of the file. Never <code>null</code>.
     */
    @NonNull
    public EFormat getFormat ()
    {
      return m_eFormat;
    }

    /**
     * @return {@link ESuccess#FAILURE} if the file could not be read completely. Participant IDs
     *         may nevertheless have been read.
     */
    @NonNull
    public ESuccess getSuccess ()
    {
      return m_eSuccess;
    }

    /**
     * @return All unique participant IDs, in the order in which they appeared in the file. Never
     *         <code>null</code>.
     */
    @NonNull
    @ReturnsMutableObject
    public ICommonsOrderedSet <IParticipantIdentifier> participantIDs ()
    {
      return m_aParticipantIDs;
    }

    /**
     * @return All entries that could not be converted to a participant identifier, in the layout in
     *         which they appeared in the file. Never <code>null</code>.
     */
    @NonNull
    @ReturnsMutableCopy
    public ICommonsList <String> getAllInvalidEntries ()
    {
      return m_aInvalidEntries.getClone ();
    }

    /**
     * @return All errors that occurred while parsing the file. Only XML files can produce these.
     *         Never <code>null</code>.
     */
    @NonNull
    @ReturnsMutableCopy
    public ICommonsList <IError> getAllErrors ()
    {
      return m_aErrors.getClone ();
    }

    /**
     * @return The number of participant IDs that appeared more than once in the file.
     */
    @Nonnegative
    public int getDuplicateCount ()
    {
      return m_nDuplicateCount;
    }

    @Override
    public String toString ()
    {
      return new ToStringGenerator (this).append ("Format", m_eFormat)
                                         .append ("Success", m_eSuccess)
                                         .append ("ParticipantIDs", m_aParticipantIDs.size ())
                                         .append ("InvalidEntries", m_aInvalidEntries.size ())
                                         .append ("Errors", m_aErrors.size ())
                                         .append ("DuplicateCount", m_nDuplicateCount)
                                         .getToString ();
    }
  }

  /** The name of the XML element that carries a participant identifier */
  public static final String ELEMENT_PARTICIPANT = "participant";
  /** The name of the XML attribute that carries the identifier scheme */
  public static final String ATTR_SCHEME = "scheme";
  /** The name of the XML attribute that carries the identifier value */
  public static final String ATTR_VALUE = "value";
  /** Text lines starting with this character are ignored */
  public static final char COMMENT_CHAR = '#';

  /** The number of leading bytes that are inspected to determine the format */
  private static final int DETECTION_BYTES = 1024;

  private static final Logger LOGGER = LoggerFactory.getLogger (PDParticipantListReader.class);

  private PDParticipantListReader ()
  {}

  /**
   * Determine the format of the provided file by looking at its first non-whitespace character. A
   * leading UTF-8 byte order mark is skipped.
   *
   * @param aFile
   *        The file to be inspected. May not be <code>null</code>.
   * @return {@link EFormat#XML} if the content starts with <code>&lt;</code>, {@link EFormat#TEXT}
   *         otherwise. Never <code>null</code>.
   */
  @NonNull
  public static EFormat detectFormat (@NonNull final File aFile)
  {
    ValueEnforcer.notNull (aFile, "File");

    byte [] aBytes = new byte [0];
    try (final InputStream aIS = FileHelper.getBufferedInputStream (aFile))
    {
      if (aIS != null)
        aBytes = aIS.readNBytes (DETECTION_BYTES);
    }
    catch (final IOException ex)
    {
      LOGGER.warn ("Failed to read the start of '" + aFile.getAbsolutePath () + "' - assuming a text file", ex);
      return EFormat.TEXT;
    }

    for (final byte b : aBytes)
    {
      final int n = b & 0xff;
      // Skip the UTF-8 BOM and all whitespace
      if (n == 0xef || n == 0xbb || n == 0xbf || n <= ' ')
        continue;
      return n == '<' ? EFormat.XML : EFormat.TEXT;
    }

    // Nothing but whitespace - the empty text file is the more forgiving choice
    return EFormat.TEXT;
  }

  /**
   * Read all participant IDs from an XML file.
   *
   * @param aFile
   *        The file to read. May not be <code>null</code>.
   * @param aIdentifierFactory
   *        The identifier factory to use. May not be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @NonNull
  private static ReadResult _readXML (@NonNull final File aFile, @NonNull final IIdentifierFactory aIdentifierFactory)
  {
    final ICommonsOrderedSet <IParticipantIdentifier> aParticipantIDs = new CommonsLinkedHashSet <> ();
    final ICommonsList <String> aInvalidEntries = new CommonsArrayList <> ();
    final MutableInt aDuplicateCount = new MutableInt (0);

    final SAXReaderSettings aSettings = new SAXReaderSettings ();
    final CollectingSAXErrorHandler aErrorHandler = new CollectingSAXErrorHandler ();
    aSettings.setErrorHandler (aErrorHandler);
    aSettings.setContentHandler (new DefaultHandler ()
    {
      @Override
      public void startElement (final String sURI,
                                final String sLocalName,
                                final String sQName,
                                final Attributes aAttributes)
      {
        if (sQName.equals (ELEMENT_PARTICIPANT) || sLocalName.equals (ELEMENT_PARTICIPANT))
        {
          final String sScheme = aAttributes.getValue (ATTR_SCHEME);
          final String sValue = aAttributes.getValue (ATTR_VALUE);
          final IParticipantIdentifier aParticipantID = aIdentifierFactory.createParticipantIdentifier (sScheme,
                                                                                                        sValue);
          if (aParticipantID == null)
            aInvalidEntries.add (sScheme + "::" + sValue);
          else
            if (!aParticipantIDs.add (aParticipantID))
              aDuplicateCount.inc ();
        }
      }
    });

    final ESuccess eSuccess = SAXReader.readXMLSAX (new FileSystemResource (aFile), aSettings);

    return new ReadResult (EFormat.XML,
                           eSuccess,
                           aParticipantIDs,
                           aInvalidEntries,
                           new CommonsArrayList <> (aErrorHandler.getErrorList ()),
                           aDuplicateCount.intValue ());
  }

  /**
   * Read all participant IDs from a text file - one URI encoded participant ID per line. Lines are
   * trimmed, empty lines and lines starting with {@link #COMMENT_CHAR} are ignored.
   *
   * @param aFile
   *        The file to read. May not be <code>null</code>.
   * @param aIdentifierFactory
   *        The identifier factory to use. May not be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @NonNull
  private static ReadResult _readText (@NonNull final File aFile, @NonNull final IIdentifierFactory aIdentifierFactory)
  {
    final ICommonsOrderedSet <IParticipantIdentifier> aParticipantIDs = new CommonsLinkedHashSet <> ();
    final ICommonsList <String> aInvalidEntries = new CommonsArrayList <> ();
    final MutableInt aDuplicateCount = new MutableInt (0);

    final InputStream aIS = FileHelper.getInputStream (aFile);
    if (aIS == null)
    {
      LOGGER.error ("Failed to open the text file '" + aFile.getAbsolutePath () + "' for reading");
      return new ReadResult (EFormat.TEXT,
                             ESuccess.FAILURE,
                             aParticipantIDs,
                             aInvalidEntries,
                             new CommonsArrayList <> (),
                             0);
    }

    // Closes the InputStream
    StreamHelperExt.readStreamLines (aIS, StandardCharsets.UTF_8, sLine -> {
      final String sTrimmed = sLine.trim ();
      if (StringHelper.isEmpty (sTrimmed) || sTrimmed.charAt (0) == COMMENT_CHAR)
        return;

      final IParticipantIdentifier aParticipantID = aIdentifierFactory.parseParticipantIdentifier (sTrimmed);
      if (aParticipantID == null)
        aInvalidEntries.add (sTrimmed);
      else
        if (!aParticipantIDs.add (aParticipantID))
          aDuplicateCount.inc ();
    });

    return new ReadResult (EFormat.TEXT,
                           ESuccess.SUCCESS,
                           aParticipantIDs,
                           aInvalidEntries,
                           new CommonsArrayList <> (),
                           aDuplicateCount.intValue ());
  }

  /**
   * Read all participant IDs from the provided file, using the format that is detected from the
   * content of the file.
   *
   * @param aFile
   *        The file to read. May not be <code>null</code>.
   * @param aIdentifierFactory
   *        The identifier factory to use. May not be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @NonNull
  public static ReadResult readParticipantList (@NonNull final File aFile,
                                                @NonNull final IIdentifierFactory aIdentifierFactory)
  {
    ValueEnforcer.notNull (aFile, "File");
    ValueEnforcer.notNull (aIdentifierFactory, "IdentifierFactory");

    final EFormat eFormat = detectFormat (aFile);
    LOGGER.info ("Reading participant IDs from '" + aFile.getAbsolutePath () + "' as " + eFormat + " content");

    final ReadResult ret = eFormat == EFormat.XML ? _readXML (aFile, aIdentifierFactory)
                                                  : _readText (aFile, aIdentifierFactory);

    LOGGER.info ("Finished reading the file. Found " +
                 ret.participantIDs ().size () +
                 " unique participant IDs; duplicates: " +
                 ret.getDuplicateCount () +
                 "; invalid: " +
                 ret.getAllInvalidEntries ().size () +
                 "; parsing errors: " +
                 ret.getAllErrors ().size ());

    return ret;
  }
}
