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

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.IError;
import com.helger.pd.indexer.mgr.PDIndexerManager.RemoveWorkItemsResult;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.storage.PDStorageManager;
import com.helger.pd.publisher.app.AppCommonUI;
import com.helger.pd.publisher.job.PDParticipantListReader.ReadResult;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.photon.mgrs.longrun.LongRunningJobResult;
import com.helger.photon.security.lock.SingleRunLock;
import com.helger.text.ReadOnlyMultilingualText;

/**
 * A long running job that reads all participant IDs from a previously uploaded file and deletes them
 * from the search index. Deleting a participant needs a couple of index queries, so deleting
 * thousands of them must not happen in an HTTP thread.<br>
 * Exactly like the "Manually delete participant" page, the entries are deleted without verifying
 * the owner, so that entries owned by an SMP can be removed as well.
 *
 * @author Philip Helger
 * @since 0.17.2
 */
public class PDIndexDeleteJob extends AbstractPDParticipantFileJob
{
  /** The type of this long running job */
  public static final String JOB_TYPE = "index-delete";

  /** The process wide lock ensuring, that only a single bulk deletion runs at a time. */
  public static final SingleRunLock LOCK = new SingleRunLock ("Participant bulk delete");

  /** The prefix of all uploaded deletion files */
  public static final String UPLOAD_FILENAME_PREFIX = "pd-index-delete-";

  /** Every n-th deleted participant a progress message is logged */
  private static final int LOG_PROGRESS_EVERY = 1000;

  private static final Logger LOGGER = LoggerFactory.getLogger (PDIndexDeleteJob.class);

  public PDIndexDeleteJob (@NonNull final File aUploadedFile, @NonNull @Nonempty final String sUserID)
  {
    super (JOB_TYPE,
           new ReadOnlyMultilingualText (AppCommonUI.DEFAULT_LOCALE, "Delete participants from the index"),
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

    // Withdraw all pending work items first - an already queued create/update work item would
    // simply put the participant back into the index right after it was deleted
    final RemoveWorkItemsResult aRemoveResult = PDMetaManager.getIndexerMgr ()
                                                             .removeWorkItems (aReadResult.participantIDs ());

    final PDStorageManager aStorageMgr = PDMetaManager.getStorageMgr ();
    final ICommonsList <String> aNotFound = new CommonsArrayList <> ();
    final ICommonsList <String> aFailed = new CommonsArrayList <> ();
    int nDeletedParticipants = 0;
    int nDeletedDocuments = 0;
    int nIndex = 0;

    LOGGER.info ("Deleting " + aReadResult.participantIDs ().size () + " participants from the index");

    for (final IParticipantIdentifier aParticipantID : aReadResult.participantIDs ())
    {
      ++nIndex;
      if ((nIndex % LOG_PROGRESS_EVERY) == 0)
        LOGGER.info ("Deleting participant #" + nIndex + " of " + aReadResult.participantIDs ().size ());

      int nDeleted;
      try
      {
        // Same as the "Manually delete participant" page - no owner verification, so that entries
        // owned by an SMP can be deleted as well
        nDeleted = aStorageMgr.deleteEntry (aParticipantID, null, false);
      }
      catch (final IOException ex)
      {
        LOGGER.error ("Error deleting participant ID '" + aParticipantID.getURIEncoded () + "'", ex);
        nDeleted = -1;
      }

      if (nDeleted < 0)
        aFailed.add (aParticipantID.getURIEncoded ());
      else
        if (nDeleted == 0)
          aNotFound.add (aParticipantID.getURIEncoded ());
        else
        {
          ++nDeletedParticipants;
          nDeletedDocuments += nDeleted;
        }
    }

    LOGGER.info ("Finished deleting. Deleted " +
                 nDeletedParticipants +
                 " participants with " +
                 nDeletedDocuments +
                 " documents; not in the index: " +
                 aNotFound.size () +
                 "; errors: " +
                 aFailed.size ());

    final StringBuilder aSB = new StringBuilder ();
    aSB.append ("Uploaded file: ").append (getUploadedFile ().getName ()).append ('\n');
    aSB.append ("Detected format: ").append (aReadResult.getFormat ()).append ('\n');
    aSB.append ("Unique participant IDs read: ").append (aReadResult.participantIDs ().size ()).append ('\n');
    aSB.append ("Duplicates in the file: ").append (aReadResult.getDuplicateCount ()).append ('\n');
    aSB.append ("Deleted participants: ").append (nDeletedParticipants).append ('\n');
    aSB.append ("Deleted index documents: ").append (nDeletedDocuments).append ('\n');
    aSB.append ("Not contained in the index: ").append (aNotFound.size ()).append ('\n');
    aSB.append ("Deletions that failed: ").append (aFailed.size ()).append ('\n');
    aSB.append ("Syntactically invalid participant IDs: ").append (aInvalidEntries.size ()).append ('\n');
    aSB.append ("Withdrawn pending work items: ")
       .append (aRemoveResult.getTotalCount ())
       .append (" (queue: ")
       .append (aRemoveResult.getQueueCount ())
       .append (", re-index list: ")
       .append (aRemoveResult.getReIndexListCount ())
       .append (", dead list: ")
       .append (aRemoveResult.getDeadListCount ())
       .append (")\n");

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
    appendDetails (aSB, "Deletions that failed", aFailed);
    appendDetails (aSB, "Not contained in the index", aNotFound);

    return LongRunningJobResult.createText (aSB.toString ());
  }
}
