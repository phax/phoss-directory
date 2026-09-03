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
package com.helger.pd.publisher.exportall;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.CheckForSigned;
import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.VisibleForTesting;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.functional.IThrowingConsumer;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.state.ESuccess;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.CommonsTreeSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.collection.commons.ICommonsSortedSet;
import com.helger.http.CHttpHeader;
import com.helger.io.file.FileHelper;
import com.helger.io.file.FilenameHelper;
import com.helger.mime.IMimeType;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.searchindex.query.IPDIndexQuery;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryMatchAll;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryTerm;
import com.helger.pd.indexer.settings.PDServerConfiguration;
import com.helger.pd.indexer.storage.PDStorageManager;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.pd.indexer.storage.field.PDField;
import com.helger.pd.publisher.CPDPublisher;
import com.helger.pd.publisher.aws.S3Helper;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.xml.microdom.IMicroDocument;

@ThreadSafe
public final class ExportAllManager
{
  // Filenames for download
  public static final String EXTERNAL_EXPORT_ALL_BUSINESSCARDS_XML_FULL = "directory-export-business-cards.xml";
  public static final String EXTERNAL_EXPORT_ALL_BUSINESSCARDS_XML_NO_DOC_TYPES = "directory-export-business-cards-no-doc-types.xml";
  public static final String EXTERNAL_EXPORT_ALL_BUSINESSCARDS_JSON = "directory-export-business-cards.json";
  public static final String EXTERNAL_EXPORT_ALL_BUSINESSCARDS_CSV = "directory-export-business-cards.csv";
  public static final String EXTERNAL_EXPORT_ALL_PARTICIPANTS_XML = "directory-export-participants.xml";
  public static final String EXTERNAL_EXPORT_ALL_PARTICIPANTS_JSON = "directory-export-participants.json";
  public static final String EXTERNAL_EXPORT_ALL_PARTICIPANTS_CSV = "directory-export-participants.csv";

  // Internal filenames
  private static final String S3_FOLDER_NAME = "export1/";
  private static final String INTERNAL_BUSINESSCARDS_XML_FULL = S3_FOLDER_NAME + "export-all-businesscards.xml";
  private static final String INTERNAL_BUSINESSCARDS_XML_NO_DOC_TYPES = S3_FOLDER_NAME +
                                                                        "export-all-businesscards-no-doc-types.xml";
  private static final String INTERNAL_BUSINESSCARDS_JSON = S3_FOLDER_NAME + "export-all-businesscards.json";
  private static final String INTERNAL_BUSINESSCARDS_CSV = S3_FOLDER_NAME + "export-all-businesscards.csv";
  private static final String INTERNAL_PARTICIPANTS_XML = S3_FOLDER_NAME + "export-all-participants.xml";
  private static final String INTERNAL_PARTICIPANTS_JSON = S3_FOLDER_NAME + "export-all-participants.json";
  private static final String INTERNAL_PARTICIPANTS_CSV = S3_FOLDER_NAME + "export-all-participants.csv";

  private static final String MAX_AGE_24H = "max-age=86400";

  // Number of participants after which the export progress is reported
  private static final int PROGRESS_STEP = 25_000;

  // Rest
  private static final Logger LOGGER = LoggerFactory.getLogger (ExportAllManager.class);

  private ExportAllManager ()
  {}

  /**
   * The runtime data of a single export format: the temporary file it is written to, plus the
   * information whether it failed or not.
   *
   * @author Philip Helger
   */
  private static final class ExportTarget
  {
    private final IExportAllHandler m_aHandler;
    private final File m_aTempFile;
    private final OutputStream m_aOS;
    private boolean m_bFailed = false;

    ExportTarget (@NonNull final IExportAllHandler aHandler) throws IOException
    {
      m_aHandler = aHandler;
      m_aTempFile = File.createTempFile ("pd-export-", ".tmp");
      m_aOS = FileHelper.getBufferedOutputStream (m_aTempFile);
      if (m_aOS == null)
        throw new IOException ("Failed to open the temporary file '" +
                               m_aTempFile.getAbsolutePath () +
                               "' for writing");
    }
  }

  @NonNull
  private static ESuccess _uploadToS3 (@NonNull final String sS3Filename,
                                       @NonNull final IMimeType aContentType,
                                       @NonNull final File aTempFile)
  {
    LOGGER.info ("Finished writing temp file '" + aTempFile.getAbsolutePath () + "' - now upload to S3");

    final String sBucketName = PDServerConfiguration.getS3BucketName ();
    final String sTempFilename = sS3Filename + ".temp";
    final String sContentDisposition = "attachment; filename=\"" + FilenameHelper.getWithoutPath (sS3Filename) + "\"";

    try
    {
      // Upload the temp file
      // Throws a runtime exception in case of error
      S3Helper.putS3Object (sBucketName, sTempFilename, aTempFile, aContentType, sContentDisposition);
    }
    catch (final Throwable ex)
    {
      LOGGER.error ("Failed to initially upload to S3", ex);
      return ESuccess.FAILURE;
    }

    // As S3 has no rename, we need to do copy and delete
    // 1. Delete the original file, if it exists
    S3Helper.deleteS3Object (sBucketName, sS3Filename);

    // 2. copy the temp file to the new file
    if (S3Helper.copyS3Object (sBucketName, sTempFilename, sS3Filename, aContentType, sContentDisposition).isFailure ())
    {
      LOGGER.error ("Failed to copy on S3 '" + sBucketName + "' / '" + sTempFilename + "' to '" + sS3Filename + "'");
      return ESuccess.FAILURE;
    }

    // 3. Delete the temp file
    S3Helper.deleteS3Object (sBucketName, sTempFilename);
    LOGGER.info ("Finished S3 uploading");
    return ESuccess.SUCCESS;
  }

  /**
   * Invoke the provided action on a single export target. If the action fails, the target is marked
   * as failed and no further action is invoked on it, so that the other export formats are not
   * affected by a single failing one.
   *
   * @param aTarget
   *        The target to work on. May not be <code>null</code>.
   * @param aAction
   *        The action to be invoked. May not be <code>null</code>.
   */
  private static void _invoke (@NonNull final ExportTarget aTarget,
                               @NonNull final IThrowingConsumer <IExportAllHandler, Exception> aAction)
  {
    if (aTarget.m_bFailed)
      return;

    try
    {
      aAction.accept (aTarget.m_aHandler);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Failed to export '" + aTarget.m_aHandler.getDisplayName () + "'", ex);
      aTarget.m_bFailed = true;
    }
  }

  /**
   * @return A list with all export formats that are enabled in this build. Never <code>null</code>
   *         but maybe empty.
   */
  @NonNull
  @ReturnsMutableCopy
  static ICommonsList <IExportAllHandler> createAllEnabledExportHandlers ()
  {
    final ICommonsList <IExportAllHandler> ret = new CommonsArrayList <> ();
    if (CPDPublisher.EXPORT_BUSINESS_CARDS_XML)
    {
      ret.add (new ExportAllHandlerBusinessCardXML ("Business Cards as XML (full)",
                                                    INTERNAL_BUSINESSCARDS_XML_FULL,
                                                    true));
      ret.add (new ExportAllHandlerBusinessCardXML ("Business Cards as XML (no doc types)",
                                                    INTERNAL_BUSINESSCARDS_XML_NO_DOC_TYPES,
                                                    false));
    }
    if (CPDPublisher.EXPORT_BUSINESS_CARDS_JSON)
      ret.add (new ExportAllHandlerBusinessCardJSON ("Business Cards as JSON", INTERNAL_BUSINESSCARDS_JSON));
    if (CPDPublisher.EXPORT_BUSINESS_CARDS_CSV)
      ret.add (new ExportAllHandlerBusinessCardCSV ("Business Cards as CSV", INTERNAL_BUSINESSCARDS_CSV));
    if (CPDPublisher.EXPORT_PARTICIPANTS_XML)
      ret.add (new ExportAllHandlerParticipantXML ("Participants as XML", INTERNAL_PARTICIPANTS_XML));
    if (CPDPublisher.EXPORT_PARTICIPANTS_JSON)
      ret.add (new ExportAllHandlerParticipantJSON ("Participants as JSON", INTERNAL_PARTICIPANTS_JSON));
    if (CPDPublisher.EXPORT_PARTICIPANTS_CSV)
      ret.add (new ExportAllHandlerParticipantCSV ("Participants as CSV", INTERNAL_PARTICIPANTS_CSV));
    return ret;
  }

  /**
   * Export all the provided participants into all the provided export formats. The search index is
   * queried only once per participant - independent of the number of export formats - and the
   * resulting data is passed to all the export formats. Each export format is written to a
   * temporary file first and is only uploaded to S3, if it was created successfully.
   *
   * @param aAllParticipantIDs
   *        The URI encoded IDs of all participants to be exported, in ascending order. May not be
   *        <code>null</code>.
   * @param aHandlers
   *        All the export formats to be created. May not be <code>null</code> but maybe empty.
   * @param aStatusConsumer
   *        Consumer for the current export status, for display in the UI. May not be
   *        <code>null</code>.
   * @return A list with the display names of all export formats that failed. Never
   *         <code>null</code> but maybe empty.
   * @throws IOException
   *         If the search index could not be queried or if no temporary file could be created. In
   *         that case nothing is uploaded to S3 at all.
   */
  @NonNull
  @ReturnsMutableCopy
  static ICommonsList <String> exportAll (@NonNull final ICommonsSortedSet <String> aAllParticipantIDs,
                                          @NonNull final List <IExportAllHandler> aHandlers,
                                          @NonNull final Consumer <? super String> aStatusConsumer) throws IOException
  {
    ValueEnforcer.notNull (aAllParticipantIDs, "AllParticipantIDs");
    ValueEnforcer.notNull (aHandlers, "Handlers");
    ValueEnforcer.notNull (aStatusConsumer, "StatusConsumer");

    final ICommonsList <String> aFailedHandlerNames = new CommonsArrayList <> ();
    if (aHandlers.isEmpty ())
    {
      LOGGER.warn ("No export format is enabled - nothing to export");
      return aFailedHandlerNames;
    }

    // Query the Business Entities only if at least one export format needs them
    boolean bNeedEntities = false;
    for (final IExportAllHandler aHandler : aHandlers)
      if (aHandler.isBusinessEntityDataNeeded ())
      {
        bNeedEntities = true;
        break;
      }

    final IIdentifierFactory aIF = PDMetaManager.getIdentifierFactory ();
    final PDStorageManager aStorageMgr = PDMetaManager.getStorageMgr ();
    final String sSearchFieldName = PDField.PARTICIPANT_ID.getFieldName ();
    final int nParticipantCount = aAllParticipantIDs.size ();
    final ICommonsList <PDStoredBusinessEntity> aEntities = new CommonsArrayList <> ();

    final ICommonsList <ExportTarget> aTargets = new CommonsArrayList <> ();
    try
    {
      // Create one temporary file per export format
      for (final IExportAllHandler aHandler : aHandlers)
        aTargets.add (new ExportTarget (aHandler));

      try
      {
        for (final ExportTarget aTarget : aTargets)
          _invoke (aTarget, x -> x.onStart (aTarget.m_aOS, nParticipantCount));

        int nParticipantIndex = 0;
        for (final String sParticipantID : aAllParticipantIDs)
        {
          final IParticipantIdentifier aParticipantID = aIF.parseParticipantIdentifier (sParticipantID);

          // Should never happen, because the IDs were parsed before they were added to the set
          if (aParticipantID == null)
            LOGGER.warn ("Failed to parse the participant ID '" + sParticipantID + "' - ignoring it");
          else
          {
            // Query the search index only once for all export formats
            aEntities.clear ();
            if (bNeedEntities)
              aStorageMgr.searchAllDocuments (new PDIndexQueryTerm (sSearchFieldName, sParticipantID),
                                              -1,
                                              aEntities::add);

            for (final ExportTarget aTarget : aTargets)
              _invoke (aTarget, x -> x.onParticipant (sParticipantID, aParticipantID, aEntities));
          }

          ++nParticipantIndex;
          if ((nParticipantIndex % PROGRESS_STEP) == 0)
          {
            final String sStatus = "Exported " + nParticipantIndex + " of " + nParticipantCount + " participants";
            LOGGER.info (sStatus);
            aStatusConsumer.accept (sStatus);
          }
        }

        for (final ExportTarget aTarget : aTargets)
          _invoke (aTarget, x -> x.onEnd ());
      }
      finally
      {
        // Ensure all the data is on disk, also in case of an error
        for (final ExportTarget aTarget : aTargets)
          StreamHelper.close (aTarget.m_aOS);
      }

      // Only now upload the successfully created files
      for (final ExportTarget aTarget : aTargets)
      {
        final String sDisplayName = aTarget.m_aHandler.getDisplayName ();
        if (aTarget.m_bFailed)
        {
          LOGGER.error ("Not uploading '" + sDisplayName + "' because the export failed");
          aFailedHandlerNames.add (sDisplayName);
        }
        else
        {
          aStatusConsumer.accept ("Uploading '" + sDisplayName + "'");
          if (_uploadToS3 (aTarget.m_aHandler.getS3Filename (),
                           aTarget.m_aHandler.getContentType (),
                           aTarget.m_aTempFile).isFailure ())
          {
            aFailedHandlerNames.add (sDisplayName);
          }
        }
      }
    }
    finally
    {
      for (final ExportTarget aTarget : aTargets)
      {
        StreamHelper.close (aTarget.m_aOS);
        aTarget.m_aTempFile.delete ();
      }
    }

    return aFailedHandlerNames;
  }

  @NonNull
  @VisibleForTesting
  static InputStream streamBusinessCardXMLFull ()
  {
    final String sBucketName = PDServerConfiguration.getS3BucketName ();
    return S3Helper.getS3Object (sBucketName, INTERNAL_BUSINESSCARDS_XML_FULL);
  }

  @NonNull
  @VisibleForTesting
  static InputStream streamBusinessCardXMLNoDocTypes ()
  {
    final String sBucketName = PDServerConfiguration.getS3BucketName ();
    return S3Helper.getS3Object (sBucketName, INTERNAL_BUSINESSCARDS_XML_NO_DOC_TYPES);
  }

  @NonNull
  @Nonempty
  static ICommonsSortedSet <String> getAllStoredParticipantIDs () throws IOException
  {
    final ICommonsSortedSet <String> ret = new CommonsTreeSet <> ();
    PDMetaManager.getStorageMgr ().searchAll (PDIndexQueryMatchAll.INSTANCE, -1, doc -> {
      final IParticipantIdentifier aPID = PDField.PARTICIPANT_ID.getDocValue (doc);
      if (aPID != null)
      {
        // Only take the ones that can be parsed, but store as a string, so that it be more easily
        // used as a query param later on
        ret.add (aPID.getURIEncoded ());
      }
    });
    return ret;
  }

  // This is only used for the on-demand export of UI search results
  @NonNull
  public static IMicroDocument queryAllContainedBusinessCardsAsXML (@NonNull final IPDIndexQuery aQuery,
                                                                    @CheckForSigned final int nMaxResultCount,
                                                                    final boolean bIncludeDocTypes) throws IOException
  {
    final PDStorageManager aStorageMgr = PDMetaManager.getStorageMgr ();

    // Query all and group by participant ID
    final ICommonsOrderedMap <IParticipantIdentifier, ICommonsList <PDStoredBusinessEntity>> aMap = new CommonsLinkedHashMap <> ();
    aStorageMgr.searchAllDocuments (aQuery, nMaxResultCount, aEntity -> {
      if (!aEntity.hasParticipantID ())
        return;

      aMap.computeIfAbsent (aEntity.getParticipantID (), _ -> new CommonsArrayList <> ()).add (aEntity);
    });

    return ExportHelper.getAllBusinessCardsAsUIXML (aMap, bIncludeDocTypes);
  }

  /**
   * Stream the stored XML file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void redirectToBusinessCardXMLFull (@NonNull final UnifiedResponse aUR)
  {
    // Get data directly from S3
    aUR.setRedirect (S3Helper.S3_PUBLIC_URL + INTERNAL_BUSINESSCARDS_XML_FULL);
    aUR.addCustomResponseHeader (CHttpHeader.CACHE_CONTROL, MAX_AGE_24H);
  }

  /**
   * Stream the stored XML file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void redirectToBusinessCardXMLNoDocTypes (@NonNull final UnifiedResponse aUR)
  {
    // Get data directly from S3
    aUR.setRedirect (S3Helper.S3_PUBLIC_URL + INTERNAL_BUSINESSCARDS_XML_NO_DOC_TYPES);
    aUR.addCustomResponseHeader (CHttpHeader.CACHE_CONTROL, MAX_AGE_24H);
  }

  /**
   * Stream the stored JSON file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void redirectToBusinessCardJSON (@NonNull final UnifiedResponse aUR)
  {
    // Get data directly from S3
    aUR.setRedirect (S3Helper.S3_PUBLIC_URL + INTERNAL_BUSINESSCARDS_JSON);
    aUR.addCustomResponseHeader (CHttpHeader.CACHE_CONTROL, MAX_AGE_24H);
  }

  /**
   * Stream the stored CSV file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void redirectToBusinessCardCSV (@NonNull final UnifiedResponse aUR)
  {
    // Get data directly from S3
    aUR.setRedirect (S3Helper.S3_PUBLIC_URL + INTERNAL_BUSINESSCARDS_CSV);
    aUR.addCustomResponseHeader (CHttpHeader.CACHE_CONTROL, MAX_AGE_24H);
  }

  /**
   * Stream the stored XML file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void redirectToParticipantXML (@NonNull final UnifiedResponse aUR)
  {
    // Get data directly from S3
    aUR.setRedirect (S3Helper.S3_PUBLIC_URL + INTERNAL_PARTICIPANTS_XML);
    aUR.addCustomResponseHeader (CHttpHeader.CACHE_CONTROL, MAX_AGE_24H);
  }

  /**
   * Stream the stored JSON file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void redirectToParticipantJSON (@NonNull final UnifiedResponse aUR)
  {
    // Get data directly from S3
    aUR.setRedirect (S3Helper.S3_PUBLIC_URL + INTERNAL_PARTICIPANTS_JSON);
    aUR.addCustomResponseHeader (CHttpHeader.CACHE_CONTROL, MAX_AGE_24H);
  }

  /**
   * Stream the stored CSV file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void redirectToParticipantCSV (@NonNull final UnifiedResponse aUR)
  {
    // Get data directly from S3
    aUR.setRedirect (S3Helper.S3_PUBLIC_URL + INTERNAL_PARTICIPANTS_CSV);
    aUR.addCustomResponseHeader (CHttpHeader.CACHE_CONTROL, MAX_AGE_24H);
  }
}
