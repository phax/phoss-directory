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
import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.numeric.mutable.MutableInt;
import com.helger.base.state.ESuccess;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedSet;
import com.helger.datetime.util.PDTIOHelper;
import com.helger.diagnostics.error.IError;
import com.helger.io.file.FileIOError;
import com.helger.io.file.FileOperationManager;
import com.helger.io.file.FilenameHelper;
import com.helger.io.resource.FileSystemResource;
import com.helger.pd.indexer.index.EIndexerWorkItemType;
import com.helger.pd.indexer.mgr.PDIndexerManager;
import com.helger.pd.indexer.mgr.PDIndexerManager.BulkQueueResult;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.storage.CPDStorage;
import com.helger.pd.publisher.app.AppCommonUI;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.photon.io.WebFileIO;
import com.helger.photon.mgrs.longrun.AbstractLongRunningJobRunnable;
import com.helger.photon.mgrs.longrun.LongRunningJobResult;
import com.helger.photon.security.lock.SingleRunLock;
import com.helger.text.ReadOnlyMultilingualText;
import com.helger.web.scope.mgr.WebScoped;
import com.helger.xml.sax.CollectingSAXErrorHandler;
import com.helger.xml.serialize.read.SAXReader;
import com.helger.xml.serialize.read.SAXReaderSettings;

/**
 * A long running job that reads all participant IDs from a previously uploaded XML file and queues
 * them for indexing. Contrary to a synchronous import this does not block an HTTP thread, and it
 * does not build a UI list with one entry per participant - which is unusable for the tens of
 * thousands of participants such a file typically contains.<br>
 * The caller must acquire {@link #LOCK} before starting this job - the job itself releases the lock
 * and deletes the import file when it is done.
 *
 * @author Philip Helger
 * @since 0.17.2
 */
public class PDIndexImportJob extends AbstractLongRunningJobRunnable
{
  /** The type of this long running job */
  public static final String JOB_TYPE = "index-import";

  /** The process wide lock ensuring, that only a single import runs at a time. */
  public static final SingleRunLock LOCK = new SingleRunLock ("Participant index import");

  /** The name of the directory below the data path, in which the uploaded files are stored */
  public static final String IMPORT_DIRECTORY = "index-import";

  /** The prefix of all uploaded import files */
  public static final String IMPORT_FILENAME_PREFIX = "pd-index-import-";

  /** The extension of all uploaded import files */
  public static final String IMPORT_FILENAME_EXTENSION = ".xml";

  /** The maximum number of participant IDs that are listed in the job result */
  public static final int MAX_RESULT_DETAILS = 100;

  private static final String ELEMENT_PARTICIPANT = "participant";
  private static final String ATTR_SCHEME = "scheme";
  private static final String ATTR_VALUE = "value";

  private static final Logger LOGGER = LoggerFactory.getLogger (PDIndexImportJob.class);

  private final File m_aImportFile;

  public PDIndexImportJob (@NonNull final File aImportFile, @NonNull @Nonempty final String sUserID)
  {
    super (JOB_TYPE,
           new ReadOnlyMultilingualText (AppCommonUI.DEFAULT_LOCALE, "Import participants for indexing"),
           () -> sUserID);
    ValueEnforcer.notNull (aImportFile, "ImportFile");
    ValueEnforcer.notEmpty (sUserID, "UserID");
    m_aImportFile = aImportFile;
  }

  /**
   * @return The directory in which all uploaded import files are stored. Never <code>null</code>.
   *         The directory may not yet exist.
   */
  @NonNull
  public static File getImportDirectory ()
  {
    return WebFileIO.getDataIO ().getFile (IMPORT_DIRECTORY);
  }

  /**
   * Create the import directory (if it is not yet present) and return a new unique file in it, to
   * which the uploaded data can be written.
   *
   * @return The file to write the uploaded data to. It does not yet exist. Never <code>null</code>.
   */
  @NonNull
  public static File createImportFile ()
  {
    final FileIOError aError = WebFileIO.getDataIO ().createDirectory (IMPORT_DIRECTORY, true);
    if (aError.isFailure ())
      throw new IllegalStateException ("Failed to create the import directory: " + aError.toString ());

    final String sFilename = IMPORT_FILENAME_PREFIX +
                             PDTIOHelper.getCurrentLocalDateTimeForFilename () +
                             "-" +
                             UUID.randomUUID ().toString () +
                             IMPORT_FILENAME_EXTENSION;

    // Ensure the resulting name is a valid filename on all platforms
    final String sSecureFilename = FilenameHelper.getAsSecureValidASCIIFilename (sFilename);
    if (sSecureFilename == null)
      throw new IllegalStateException ("Failed to create a valid import filename from '" + sFilename + "'");

    return new File (getImportDirectory (), sSecureFilename);
  }

  /**
   * Append at most {@link #MAX_RESULT_DETAILS} entries to the provided result text, so that the job
   * result stays small enough to be persisted. An import file may well contain tens of thousands of
   * participants, and listing all of them is of no use to anybody.
   *
   * @param aSB
   *        The result text to append to. May not be <code>null</code>.
   * @param sTitle
   *        The headline to use. May not be <code>null</code>.
   * @param aEntries
   *        The entries to be listed. May not be <code>null</code>.
   */
  private static void _appendDetails (@NonNull final StringBuilder aSB,
                                      @NonNull final String sTitle,
                                      @NonNull final ICommonsList <String> aEntries)
  {
    if (aEntries.isEmpty ())
      return;

    aSB.append ('\n').append (sTitle).append (" (").append (aEntries.size ()).append ("):\n");
    for (final String sEntry : aEntries.subList (0, Math.min (MAX_RESULT_DETAILS, aEntries.size ())))
      aSB.append ("  ").append (sEntry).append ('\n');
    if (aEntries.size () > MAX_RESULT_DETAILS)
      aSB.append ("  ... and ").append (aEntries.size () - MAX_RESULT_DETAILS).append (" more\n");
  }

  @NonNull
  public LongRunningJobResult createLongRunningJobResult ()
  {
    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();

    // Parse first, so that the identifiers are unique before anything is queued
    final ICommonsOrderedSet <IParticipantIdentifier> aAllParticipantIDs = new CommonsLinkedHashSet <> ();
    final ICommonsList <String> aInvalidIDs = new CommonsArrayList <> ();
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
        if (sQName.equals (ELEMENT_PARTICIPANT))
        {
          final String sScheme = aAttributes.getValue (ATTR_SCHEME);
          final String sValue = aAttributes.getValue (ATTR_VALUE);
          final IParticipantIdentifier aParticipantID = aIdentifierFactory.createParticipantIdentifier (sScheme,
                                                                                                        sValue);
          if (aParticipantID == null)
            aInvalidIDs.add (sScheme + "::" + sValue);
          else
            if (!aAllParticipantIDs.add (aParticipantID))
              aDuplicateCount.inc ();
        }
      }
    });

    LOGGER.info ("Importing participant IDs from '" + m_aImportFile.getAbsolutePath () + "'");

    final ESuccess eSuccess = SAXReader.readXMLSAX (new FileSystemResource (m_aImportFile), aSettings);

    LOGGER.info ("Finished reading the XML file. Found " +
                 aAllParticipantIDs.size () +
                 " unique participant IDs; duplicates: " +
                 aDuplicateCount.intValue () +
                 "; invalid: " +
                 aInvalidIDs.size () +
                 "; XML errors: " +
                 aErrorHandler.getErrorList ().size ());

    // Some things may have been read even in case of a parsing error
    final BulkQueueResult aQueueResult = PDMetaManager.getIndexerMgr ()
                                                      .queueWorkItems (aAllParticipantIDs,
                                                                       EIndexerWorkItemType.CREATE_UPDATE,
                                                                       CPDStorage.OWNER_IMPORT_TRIGGERED,
                                                                       PDIndexerManager.HOST_LOCALHOST);

    final StringBuilder aSB = new StringBuilder ();
    aSB.append ("Imported file: ").append (m_aImportFile.getName ()).append ('\n');
    aSB.append ("Unique participant IDs read: ").append (aAllParticipantIDs.size ()).append ('\n');
    aSB.append ("Duplicates in the file: ").append (aDuplicateCount.intValue ()).append ('\n');
    aSB.append ("Queued for indexing: ").append (aQueueResult.getQueuedCount ()).append ('\n');
    aSB.append ("Already in the indexing queue: ").append (aQueueResult.getNotQueuedCount ()).append ('\n');
    aSB.append ("Syntactically invalid participant IDs: ").append (aInvalidIDs.size ()).append ('\n');

    if (eSuccess.isFailure ())
    {
      aSB.append ("\nErrors parsing the provided XML:\n");
      for (final IError aError : aErrorHandler.getErrorList ())
      {
        final String sMsg = aError.getAsString (AppCommonUI.DEFAULT_LOCALE);
        LOGGER.error ("  " + sMsg);
        aSB.append ("  ").append (sMsg).append ('\n');
      }
    }

    for (final String sInvalidID : aInvalidIDs.subList (0, Math.min (MAX_RESULT_DETAILS, aInvalidIDs.size ())))
      LOGGER.error ("Failed to convert '" + sInvalidID + "' to a participant identifier");

    _appendDetails (aSB, "Syntactically invalid participant IDs", aInvalidIDs);

    // The participant IDs that were already queued are deliberately not logged one by one - that is
    // what made the synchronous import slow
    _appendDetails (aSB,
                    "Already in the indexing queue",
                    aQueueResult.getAllNotQueued ().getAllMapped (IParticipantIdentifier::getURIEncoded));

    return LongRunningJobResult.createText (aSB.toString ());
  }

  @Override
  public void run ()
  {
    // A Web Scope is needed for storing the job result
    try (final WebScoped w = new WebScoped ())
    {
      super.run ();
    }
    finally
    {
      // The uploaded file is of no use anymore
      if (FileOperationManager.INSTANCE.deleteFileIfExisting (m_aImportFile).isFailure ())
        LOGGER.warn ("Failed to delete the import file '" + m_aImportFile.getAbsolutePath () + "'");

      // Always release, even if the job failed
      LOCK.release ();
    }
  }
}
