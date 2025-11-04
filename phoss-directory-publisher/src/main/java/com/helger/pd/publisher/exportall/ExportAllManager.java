/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.WillNotClose;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.annotation.style.VisibleForTesting;
import com.helger.base.functional.IThrowingFunction;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringImplode;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.CommonsTreeSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.collection.commons.ICommonsSortedSet;
import com.helger.commons.csv.CSVWriter;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.util.PDTIOHelper;
import com.helger.datetime.web.PDTWebDateHelper;
import com.helger.http.CHttpHeader;
import com.helger.io.file.FileHelper;
import com.helger.io.file.FileOperations;
import com.helger.io.resource.FileSystemResource;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.storage.PDStorageManager;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.pd.indexer.storage.PDStoredContact;
import com.helger.pd.indexer.storage.PDStoredIdentifier;
import com.helger.pd.indexer.storage.PDStoredMLName;
import com.helger.pd.indexer.storage.field.PDField;
import com.helger.peppol.ui.types.nicename.NiceNameEntry;
import com.helger.peppol.ui.types.nicename.NiceNameManager;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.photon.io.WebFileIO;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.XMLWriterSettings;

import jakarta.annotation.Nonnull;
import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;

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
  private static final String INTERNAL_EXPORT_ALL_BUSINESSCARDS_XML_FULL = "export-all-businesscards.xml";
  private static final String INTERNAL_EXPORT_ALL_BUSINESSCARDS_XML_NO_DOC_TYPES = "export-all-businesscards-no-doc-types.xml";
  private static final String INTERNAL_EXPORT_ALL_BUSINESSCARDS_JSON = "export-all-businesscards.json";
  private static final String INTERNAL_EXPORT_ALL_BUSINESSCARDS_CSV = "export-all-businesscards.csv";
  private static final String INTERNAL_EXPORT_ALL_PARTICIPANTS_XML = "export-all-participants.xml";
  private static final String INTERNAL_EXPORT_ALL_PARTICIPANTS_JSON = "export-all-participants.json";
  private static final String INTERNAL_EXPORT_ALL_PARTICIPANTS_CSV = "export-all-participants.csv";

  // Rest
  private static final Logger LOGGER = LoggerFactory.getLogger (ExportAllManager.class);

  private ExportAllManager ()
  {}

  @Nonnull
  private static ESuccess _runWithTempFile (@Nonnull final File fTarget,
                                            @Nonnull final IThrowingFunction <File, ESuccess, IOException> aCallback) throws IOException
  {
    final File fTempFile = new File (fTarget.getParentFile (), fTarget.getName () + ".tmp");
    // Write result to temp file
    final ESuccess ret = aCallback.apply (fTempFile);
    if (ret.isFailure ())
    {
      // Operation failed - delete temp file
      FileOperations.deleteFileIfExisting (fTempFile);
    }
    else
    {
      // Goal: replace target file
      FileOperations.deleteFileIfExisting (fTarget);
      if (!fTarget.isFile ())
      {
        // Deletion worked or did not exist in the first place
        FileOperations.renameFile (fTempFile, fTarget);
      }
      else
      {
        // Old target still exists
        // Keep the temp file to handle it manually
        LOGGER.error ("Failed to delete file '" + fTarget.getAbsolutePath () + "' - manually storing temp file");
        FileOperations.renameFile (fTempFile,
                                   new File (fTarget.getParentFile (),
                                             fTarget.getName () +
                                                                       "." +
                                                                       PDTIOHelper.getCurrentLocalDateTimeForFilename ()));
      }
    }
    return ret;
  }

  private static void _streamFileToResponse (@Nonnull final File fSrc, @Nonnull final UnifiedResponse aUR)
  {
    // setContent(IReadableResource) is lazy
    aUR.setContent (new FileSystemResource (fSrc));
    final long nFileLen = fSrc.length ();
    if (nFileLen > 0)
      aUR.setCustomResponseHeader (CHttpHeader.CONTENT_LENGTH, Long.toString (nFileLen));
  }

  @Nonnull
  @Nonempty
  static ICommonsSortedSet <String> getAllStoredParticipantIDs () throws IOException
  {
    final ICommonsSortedSet <String> ret = new CommonsTreeSet <> ();
    PDMetaManager.getStorageMgr ().searchAll (new MatchAllDocsQuery (), -1, doc -> {
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
  @Nonnull
  public static IMicroDocument queryAllContainedBusinessCardsAsXML (@Nonnull final Query aQuery,
                                                                    final boolean bIncludeDocTypes) throws IOException
  {
    final PDStorageManager aStorageMgr = PDMetaManager.getStorageMgr ();

    // Query all and group by participant ID
    final ICommonsOrderedMap <IParticipantIdentifier, ICommonsList <PDStoredBusinessEntity>> aMap = new CommonsLinkedHashMap <> ();
    aStorageMgr.searchAllDocuments (aQuery, -1, aEntity -> {
      if (!aEntity.hasParticipantID ())
        return;

      aMap.computeIfAbsent (aEntity.getParticipantID (), k -> new CommonsArrayList <> ()).add (aEntity);
    });

    return ExportHelper.getAsXML (aMap, bIncludeDocTypes);
  }

  @Nonnull
  @VisibleForTesting
  static File _getInternalFileBusinessCardXMLFull ()
  {
    return WebFileIO.getDataIO ().getFile (INTERNAL_EXPORT_ALL_BUSINESSCARDS_XML_FULL);
  }

  @Nonnull
  private static ESuccess _writeFileBusinessCardXML (@Nonnull final ICommonsSortedSet <String> aAllParticipantIDs,
                                                     @Nonnull final File fTarget,
                                                     final boolean bIncludeDocTypes) throws IOException
  {
    final IIdentifierFactory aIF = PDMetaManager.getIdentifierFactory ();
    final PDStorageManager aStorageMgr = PDMetaManager.getStorageMgr ();

    final IMicroDocument aDoc = ExportHelper.createXML ();
    final IMicroElement aRoot = aDoc.getDocumentElement ();

    final String sSearchTerm = PDField.PARTICIPANT_ID.getFieldName ();
    for (final String sParticipantID : aAllParticipantIDs)
    {
      final IParticipantIdentifier aParticipantID = aIF.parseParticipantIdentifier (sParticipantID);

      // Should never happen because PIs are parsed before added into the source set
      if (aParticipantID != null)
      {
        // Search all entities of the current participant ID
        final ICommonsList <PDStoredBusinessEntity> aEntitiesPerPI = new CommonsArrayList <> ();
        aStorageMgr.searchAllDocuments (new TermQuery (new Term (sSearchTerm, sParticipantID)),
                                        -1,
                                        aEntitiesPerPI::add);
        // Otherwise, the PI might have been deleted in the meantime
        if (aEntitiesPerPI.isNotEmpty ())
        {
          aRoot.addChild (ExportHelper.createMicroElement (aParticipantID, aEntitiesPerPI, bIncludeDocTypes));
        }
      }
    }

    // Safe space - no indent
    if (MicroWriter.writeToFile (aDoc, fTarget, new XMLWriterSettings ().setIndent (EXMLSerializeIndent.NONE))
                   .isFailure ())
    {
      LOGGER.error ("Failed to export all BCs as XML (" +
                    (bIncludeDocTypes ? "full" : "no doctypes") +
                    ") to " +
                    fTarget.getAbsolutePath ());
      return ESuccess.FAILURE;
    }
    LOGGER.info ("Successfully wrote all BCs as XML (" +
                 (bIncludeDocTypes ? "full" : "no doctypes") +
                 ") to " +
                 fTarget.getAbsolutePath ());
    return ESuccess.SUCCESS;
  }

  @Nonnull
  static ESuccess writeFileBusinessCardXMLFull (@Nonnull final ICommonsSortedSet <String> aAllParticipantIDs) throws IOException
  {
    return _runWithTempFile (_getInternalFileBusinessCardXMLFull (),
                             f -> _writeFileBusinessCardXML (aAllParticipantIDs, f, true));
  }

  /**
   * Stream the stored XML file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void streamFileBusinessCardXMLFullTo (@Nonnull final UnifiedResponse aUR)
  {
    _streamFileToResponse (_getInternalFileBusinessCardXMLFull (), aUR);
  }

  @Nonnull
  @VisibleForTesting
  static File _getInternalFileBusinessCardXMLNoDocTypes ()
  {
    return WebFileIO.getDataIO ().getFile (INTERNAL_EXPORT_ALL_BUSINESSCARDS_XML_NO_DOC_TYPES);
  }

  @Nonnull
  static ESuccess writeFileBusinessCardXMLNoDocTypes (@Nonnull final ICommonsSortedSet <String> aAllParticipantIDs) throws IOException
  {
    return _runWithTempFile (_getInternalFileBusinessCardXMLNoDocTypes (),
                             f -> _writeFileBusinessCardXML (aAllParticipantIDs, f, false));
  }

  /**
   * Stream the stored XML file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void streamFileBusinessCardXMLNoDocTypesTo (@Nonnull final UnifiedResponse aUR)
  {
    _streamFileToResponse (_getInternalFileBusinessCardXMLNoDocTypes (), aUR);
  }

  @Nonnull
  private static File _getInternalFileBusinessCardJSON ()
  {
    return WebFileIO.getDataIO ().getFile (INTERNAL_EXPORT_ALL_BUSINESSCARDS_JSON);
  }

  @Nonnull
  static ESuccess writeFileBusinessCardJSON (@Nonnull final ICommonsSortedSet <String> aAllParticipantIDs) throws IOException
  {
    return _runWithTempFile (_getInternalFileBusinessCardJSON (), f -> {
      final PDStorageManager aStorageMgr = PDMetaManager.getStorageMgr ();
      final boolean bIncludeDocTypes = true;

      try (final Writer aWriter = FileHelper.getBufferedWriter (f, StandardCharsets.UTF_8);
           final JsonGenerator aJsonGen = Json.createGenerator (aWriter))
      {
        final String sSearchTerm = PDField.PARTICIPANT_ID.getFieldName ();

        // JSON root
        aJsonGen.writeStartObject ()
                .write ("version", 2)
                .write ("creationdt", PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentZonedDateTimeUTC ()))
                .write ("participantCount", aAllParticipantIDs.size ())
                .write ("codeListSupported", EPredefinedDocumentTypeIdentifier.CODE_LIST_VERSION)
                .writeStartArray ("bc");

        for (final String sParticipantID : aAllParticipantIDs)
        {
          // Search all entities of the current participant ID
          final ICommonsList <PDStoredBusinessEntity> aEntitiesPerPI = new CommonsArrayList <> ();
          aStorageMgr.searchAllDocuments (new TermQuery (new Term (sSearchTerm, sParticipantID)),
                                          -1,
                                          aEntitiesPerPI::add);

          // Otherwise, the PI might have been deleted in the meantime
          if (aEntitiesPerPI.isEmpty ())
            continue;

          aJsonGen.writeStartObject ().write ("pid", sParticipantID).writeStartArray ("entities");

          for (final PDStoredBusinessEntity aSBE : aEntitiesPerPI)
          {
            aJsonGen.writeStartObject ();
            {
              aJsonGen.writeStartArray ("names");
              for (final PDStoredMLName aName : aSBE.names ())
              {
                aJsonGen.writeStartObject ().write ("name", aName.getName ());
                if (aName.hasLanguageCode ())
                  aJsonGen.write ("lang", aName.getLanguageCode ());
                aJsonGen.writeEnd ();
              }
              aJsonGen.writeEnd ();
            }
            if (aSBE.hasCountryCode ())
              aJsonGen.write ("countryCode", aSBE.getCountryCode ());
            if (aSBE.hasGeoInfo ())
              aJsonGen.write ("geoinfo", aSBE.getGeoInfo ());
            if (aSBE.identifiers ().isNotEmpty ())
            {
              aJsonGen.writeStartArray ("identifiers");
              for (final PDStoredIdentifier aID : aSBE.identifiers ())
              {
                aJsonGen.writeStartObject ()
                        .write ("scheme", aID.getScheme ())
                        .write ("value", aID.getValue ())
                        .writeEnd ();
              }
              aJsonGen.writeEnd ();
            }
            if (aSBE.websiteURIs ().isNotEmpty ())
            {
              aJsonGen.writeStartArray ("websiteURIs");
              for (final String sWebsite : aSBE.websiteURIs ())
                aJsonGen.write (sWebsite);
              aJsonGen.writeEnd ();
            }
            if (aSBE.contacts ().isNotEmpty ())
            {
              aJsonGen.writeStartArray ("contacts");
              for (final PDStoredContact aContact : aSBE.contacts ())
              {
                aJsonGen.writeStartObject ();
                if (aContact.hasType ())
                  aJsonGen.write ("type", aContact.getType ());
                if (aContact.hasName ())
                  aJsonGen.write ("name", aContact.getName ());
                if (aContact.hasPhone ())
                  aJsonGen.write ("phone", aContact.getPhone ());
                if (aContact.hasEmail ())
                  aJsonGen.write ("email", aContact.getEmail ());
                aJsonGen.writeEnd ();
              }
              aJsonGen.writeEnd ();
            }
            if (aSBE.hasAdditionalInformation ())
              aJsonGen.write ("additionalInfo", aSBE.getAdditionalInformation ());
            if (aSBE.hasRegistrationDate ())
              aJsonGen.write ("regdate", PDTWebDateHelper.getAsStringXSD (aSBE.getRegistrationDate ()));
            aJsonGen.writeEnd ();
          }
          aJsonGen.writeEnd ();

          // Add all Document types (if wanted)
          if (bIncludeDocTypes)
          {
            aJsonGen.writeStartArray ("docTypes");
            if (aEntitiesPerPI.isNotEmpty ())
              for (final IDocumentTypeIdentifier aDocTypeID : aEntitiesPerPI.getFirstOrNull ().documentTypeIDs ())
              {
                aJsonGen.writeStartObject ()
                        .write ("scheme", aDocTypeID.getScheme ())
                        .write ("value", aDocTypeID.getValue ());
                final NiceNameEntry aNiceName = NiceNameManager.getDocTypeNiceName (aDocTypeID.getURIEncoded ());
                if (aNiceName == null)
                  aJsonGen.write ("nonStandard", true);
                else
                {
                  aJsonGen.write ("displayName", aNiceName.getName ());
                  // New in JSON v2: use "state" instead of "deprecated"
                  aJsonGen.write ("state", aNiceName.getState ().getID ());
                }
                aJsonGen.writeEnd ();
              }
            aJsonGen.writeEnd ();
          }

          aJsonGen.writeEnd ();
        }

        aJsonGen.writeEnd ().writeEnd ();

        LOGGER.info ("Successfully wrote all BusinessCards as JSON to " + f.getAbsolutePath ());
        return ESuccess.SUCCESS;
      }
      catch (final IOException ex)
      {
        LOGGER.error ("Failed to export all BusinessCards as JSON to " + f.getAbsolutePath (), ex);
        return ESuccess.FAILURE;
      }
    });
  }

  /**
   * Stream the stored JSON file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void streamFileBusinessCardJSONTo (@Nonnull final UnifiedResponse aUR)
  {
    _streamFileToResponse (_getInternalFileBusinessCardJSON (), aUR);
  }

  private static void _unify (@Nonnull @WillNotClose final CSVWriter aCSVWriter)
  {
    aCSVWriter.setSeparatorChar (';');
  }

  @Nonnull
  private static File _getInternalFileBusinessCardCSV ()
  {
    return WebFileIO.getDataIO ().getFile (INTERNAL_EXPORT_ALL_BUSINESSCARDS_CSV);
  }

  @Nonnull
  static ESuccess writeFileBusinessCardCSV (@Nonnull final ICommonsSortedSet <String> aAllParticipantIDs) throws IOException
  {
    return _runWithTempFile (_getInternalFileBusinessCardCSV (), f -> {
      final PDStorageManager aStorageMgr = PDMetaManager.getStorageMgr ();

      try (final CSVWriter aCSVWriter = new CSVWriter (FileHelper.getBufferedWriter (f, StandardCharsets.ISO_8859_1)))
      {
        _unify (aCSVWriter);
        aCSVWriter.writeNext ("Participant ID",
                              "Names (per-row)",
                              "Country code",
                              "Geo info",
                              "Identifier schemes",
                              "Identifier values",
                              "Websites",
                              "Contact type",
                              "Contact name",
                              "Contact phone",
                              "Contact email",
                              "Additional info",
                              "Registration date",
                              "Document types");

        final Consumer <? super PDStoredBusinessEntity> aCSVConsumer = aEntity -> {
          if (!aEntity.hasParticipantID ())
            return;

          aCSVWriter.writeNext (aEntity.getParticipantID ().getURIEncoded (),
                                StringImplode.imploder ()
                                             .source (aEntity.names (), PDStoredMLName::getNameAndLanguageCode)
                                             .separator ('\n')
                                             .build (),
                                aEntity.getCountryCode (),
                                aEntity.getGeoInfo (),
                                StringImplode.imploder ()
                                             .source (aEntity.identifiers (), PDStoredIdentifier::getScheme)
                                             .separator ('\n')
                                             .build (),
                                StringImplode.imploder ()
                                             .source (aEntity.identifiers (), PDStoredIdentifier::getValue)
                                             .separator ('\n')
                                             .build (),
                                StringImplode.imploder ().source (aEntity.websiteURIs ()).separator ('\n').build (),
                                StringImplode.imploder ()
                                             .source (aEntity.contacts (), PDStoredContact::getType)
                                             .separator ('\n')
                                             .build (),
                                StringImplode.imploder ()
                                             .source (aEntity.contacts (), PDStoredContact::getName)
                                             .separator ('\n')
                                             .build (),
                                StringImplode.imploder ()
                                             .source (aEntity.contacts (), PDStoredContact::getPhone)
                                             .separator ('\n')
                                             .build (),
                                StringImplode.imploder ()
                                             .source (aEntity.contacts (), PDStoredContact::getEmail)
                                             .separator ('\n')
                                             .build (),
                                aEntity.getAdditionalInformation (),
                                aEntity.getRegistrationDate () == null ? "" : aEntity.getRegistrationDate ()
                                                                                     .toString (),
                                StringImplode.imploder ()
                                             .source (aEntity.documentTypeIDs (),
                                                      IDocumentTypeIdentifier::getURIEncoded)
                                             .separator ('\n')
                                             .build ());
        };

        final String sSearchTerm = PDField.PARTICIPANT_ID.getFieldName ();
        for (final String sParticipantID : aAllParticipantIDs)
        {
          // If the participant was deleted in the meantime, the consumer is simply not called
          aStorageMgr.searchAllDocuments (new TermQuery (new Term (sSearchTerm, sParticipantID)), -1, aCSVConsumer);
        }

        aCSVWriter.flush ();
        LOGGER.info ("Successfully exported all BCs as CSV to " + f.getAbsolutePath ());
        return ESuccess.SUCCESS;
      }
      catch (final IOException ex)
      {
        LOGGER.error ("Failed to export all BCs as CSV to " + f.getAbsolutePath (), ex);
        return ESuccess.FAILURE;
      }
    });
  }

  /**
   * Stream the stored CSV file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void streamFileBusinessCardCSVTo (@Nonnull final UnifiedResponse aUR)
  {
    _streamFileToResponse (_getInternalFileBusinessCardCSV (), aUR);
  }

  @Nonnull
  private static File _getInternalFileParticipantXML ()
  {
    return WebFileIO.getDataIO ().getFile (INTERNAL_EXPORT_ALL_PARTICIPANTS_XML);
  }

  @Nonnull
  static ESuccess writeFileParticipantXML (@Nonnull final ICommonsSortedSet <String> aAllParticipantIDs) throws IOException
  {
    return _runWithTempFile (_getInternalFileParticipantXML (), f -> {
      final IIdentifierFactory aIF = PDMetaManager.getIdentifierFactory ();

      // XML root
      final IMicroDocument aDoc = new MicroDocument ();
      final String sNamespaceURI = "http://www.peppol.eu/schema/pd/participant-generic/201910/";
      final IMicroElement aRoot = aDoc.addElementNS (sNamespaceURI, "root");
      aRoot.setAttribute ("version", "1");
      aRoot.setAttribute ("creationdt", PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentZonedDateTimeUTC ()));
      aRoot.setAttribute ("count", aAllParticipantIDs.size ());

      // For all participants
      for (final String sParticipantID : aAllParticipantIDs)
      {
        final IParticipantIdentifier aPI = aIF.parseParticipantIdentifier (sParticipantID);
        final IMicroElement ePI = aRoot.addElementNS (sNamespaceURI, "participantID");

        // Should never happen because PIs are parsed before added into the source set
        if (aPI != null)
          ePI.setAttribute ("scheme", aPI.getScheme ()).setAttribute ("value", aPI.getValue ());
      }

      // Safe space - no indent
      if (MicroWriter.writeToFile (aDoc, f, new XMLWriterSettings ().setIndent (EXMLSerializeIndent.NONE)).isFailure ())
      {
        LOGGER.error ("Failed to export all Participants as XML to " + f.getAbsolutePath ());
        return ESuccess.FAILURE;
      }
      LOGGER.info ("Successfully wrote all Participants as XML to " + f.getAbsolutePath ());
      return ESuccess.SUCCESS;
    });
  }

  /**
   * Stream the stored XML file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void streamFileParticipantXMLTo (@Nonnull final UnifiedResponse aUR)
  {
    _streamFileToResponse (_getInternalFileParticipantXML (), aUR);
  }

  @Nonnull
  private static File _getInternalFileParticipantJSON ()
  {
    return WebFileIO.getDataIO ().getFile (INTERNAL_EXPORT_ALL_PARTICIPANTS_JSON);
  }

  @Nonnull
  static ESuccess writeFileParticipantJSON (@Nonnull final ICommonsSortedSet <String> aAllParticipantIDs) throws IOException
  {
    return _runWithTempFile (_getInternalFileParticipantJSON (), f -> {
      try (final Writer aWriter = FileHelper.getBufferedWriter (f, StandardCharsets.UTF_8);
           final JsonGenerator aJsonGen = Json.createGenerator (aWriter))
      {
        // JSON root
        aJsonGen.writeStartObject ()
                .write ("version", 1)
                .write ("creationdt", PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentZonedDateTimeUTC ()))
                .write ("count", aAllParticipantIDs.size ())
                .writeStartArray ("participants");

        // For all participants
        for (final String sParticipantID : aAllParticipantIDs)
          aJsonGen.write (sParticipantID);

        aJsonGen.writeEnd ().writeEnd ();

        // Safe space - no indent
        LOGGER.info ("Successfully wrote all Participants as JSON to " + f.getAbsolutePath ());
        return ESuccess.SUCCESS;
      }
      catch (final IOException ex)
      {
        LOGGER.error ("Failed to export all Participants as JSON to " + f.getAbsolutePath (), ex);
        return ESuccess.FAILURE;
      }
    });
  }

  /**
   * Stream the stored JSON file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void streamFileParticipantJSONTo (@Nonnull final UnifiedResponse aUR)
  {
    _streamFileToResponse (_getInternalFileParticipantJSON (), aUR);
  }

  @Nonnull
  private static File _getInternalFileParticipantCSV ()
  {
    return WebFileIO.getDataIO ().getFile (INTERNAL_EXPORT_ALL_PARTICIPANTS_CSV);
  }

  @Nonnull
  static ESuccess writeFileParticipantCSV (@Nonnull final ICommonsSortedSet <String> aAllParticipantIDs) throws IOException
  {
    return _runWithTempFile (_getInternalFileParticipantCSV (), f -> {
      try (final CSVWriter aCSVWriter = new CSVWriter (FileHelper.getBufferedWriter (f, StandardCharsets.ISO_8859_1)))
      {
        _unify (aCSVWriter);
        aCSVWriter.writeNext ("Participant ID");
        for (final String sParticipantID : aAllParticipantIDs)
          aCSVWriter.writeNext (sParticipantID);

        aCSVWriter.flush ();
        LOGGER.info ("Successfully wrote all Participants as CSV to " + f.getAbsolutePath ());
        return ESuccess.SUCCESS;
      }
      catch (final IOException ex)
      {
        LOGGER.error ("Failed to export all Participants as CSV to " + f.getAbsolutePath (), ex);
        return ESuccess.FAILURE;
      }
    });
  }

  /**
   * Stream the stored CSV file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void streamFileParticipantCSVTo (@Nonnull final UnifiedResponse aUR)
  {
    _streamFileToResponse (_getInternalFileParticipantCSV (), aUR);
  }
}
