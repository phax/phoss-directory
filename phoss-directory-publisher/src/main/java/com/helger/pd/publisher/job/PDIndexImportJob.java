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

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.IError;
import com.helger.pd.indexer.index.EIndexerWorkItemType;
import com.helger.pd.indexer.mgr.PDIndexerManager;
import com.helger.pd.indexer.mgr.PDIndexerManager.BulkQueueResult;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.storage.CPDStorage;
import com.helger.pd.publisher.app.AppCommonUI;
import com.helger.pd.publisher.job.PDParticipantListReader.ReadResult;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.photon.mgrs.longrun.LongRunningJobResult;
import com.helger.photon.security.lock.SingleRunLock;
import com.helger.text.ReadOnlyMultilingualText;

/**
 * A long running job that reads all participant IDs from a previously uploaded file and queues them
 * for indexing. Contrary to a synchronous import this does not block an HTTP thread, and it does not
 * build a UI list with one entry per participant - which is unusable for the tens of thousands of
 * participants such a file typically contains.
 *
 * @author Philip Helger
 * @since 0.17.2
 */
public class PDIndexImportJob extends AbstractPDParticipantFileJob
{
  /** The type of this long running job */
  public static final String JOB_TYPE = "index-import";

  /** The process wide lock ensuring, that only a single import runs at a time. */
  public static final SingleRunLock LOCK = new SingleRunLock ("Participant index import");

  /** The prefix of all uploaded import files */
  public static final String UPLOAD_FILENAME_PREFIX = "pd-index-import-";

  private static final Logger LOGGER = LoggerFactory.getLogger (PDIndexImportJob.class);

  public PDIndexImportJob (@NonNull final File aUploadedFile, @NonNull @Nonempty final String sUserID)
  {
    super (JOB_TYPE,
           new ReadOnlyMultilingualText (AppCommonUI.DEFAULT_LOCALE, "Import participants for indexing"),
           sUserID,
           aUploadedFile,
           LOCK);
  }

  /**
   * @return A new unique file to which the uploaded data can be written. Never <code>null</code>.
   */
  @NonNull
  public static File createUploadFile ()
  {
    return createUploadFile (UPLOAD_FILENAME_PREFIX);
  }

  @NonNull
  public LongRunningJobResult createLongRunningJobResult ()
  {
    final ReadResult aReadResult = PDParticipantListReader.readParticipantList (getUploadedFile (),
                                                                               PDMetaManager.getIdentifierFactory ());
    final ICommonsList <String> aInvalidEntries = aReadResult.getAllInvalidEntries ();

    // Some things may have been read even in case of a parsing error
    final BulkQueueResult aQueueResult = PDMetaManager.getIndexerMgr ()
                                                      .queueWorkItems (aReadResult.participantIDs (),
                                                                       EIndexerWorkItemType.CREATE_UPDATE,
                                                                       CPDStorage.OWNER_IMPORT_TRIGGERED,
                                                                       PDIndexerManager.HOST_LOCALHOST);

    final StringBuilder aSB = new StringBuilder ();
    aSB.append ("Uploaded file: ").append (getUploadedFile ().getName ()).append ('\n');
    aSB.append ("Detected format: ").append (aReadResult.getFormat ()).append ('\n');
    aSB.append ("Unique participant IDs read: ").append (aReadResult.participantIDs ().size ()).append ('\n');
    aSB.append ("Duplicates in the file: ").append (aReadResult.getDuplicateCount ()).append ('\n');
    aSB.append ("Queued for indexing: ").append (aQueueResult.getQueuedCount ()).append ('\n');
    aSB.append ("Already in the indexing queue: ").append (aQueueResult.getNotQueuedCount ()).append ('\n');
    aSB.append ("Syntactically invalid participant IDs: ").append (aInvalidEntries.size ()).append ('\n');

    if (aReadResult.getSuccess ().isFailure ())
    {
      aSB.append ("\nErrors parsing the provided file:\n");
      for (final IError aError : aReadResult.getAllErrors ())
      {
        final String sMsg = aError.getAsString (AppCommonUI.DEFAULT_LOCALE);
        LOGGER.error ("  " + sMsg);
        aSB.append ("  ").append (sMsg).append ('\n');
      }
    }

    for (final String sInvalidEntry : aInvalidEntries.subList (0,
                                                               Math.min (getMaxResultDetails (),
                                                                         aInvalidEntries.size ())))
      LOGGER.error ("Failed to convert '" + sInvalidEntry + "' to a participant identifier");

    appendDetails (aSB, "Syntactically invalid participant IDs", aInvalidEntries);

    // The participant IDs that were already queued are deliberately not logged one by one - that is
    // what made the synchronous import slow
    appendDetails (aSB,
                   "Already in the indexing queue",
                   aQueueResult.getAllNotQueued ().getAllMapped (IParticipantIdentifier::getURIEncoded));

    return LongRunningJobResult.createText (aSB.toString ());
  }
}
