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
import com.helger.datetime.web.PDTWebDateHelper;
import com.helger.http.CHttpHeader;
import com.helger.io.file.FileHelper;
import com.helger.io.resource.FileSystemResource;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriter;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.storage.PDStorageManager;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.pd.indexer.storage.PDStoredContact;
import com.helger.pd.indexer.storage.PDStoredIdentifier;
import com.helger.pd.indexer.storage.PDStoredMLName;
import com.helger.pd.indexer.storage.field.PDField;
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

import jakarta.annotation.Nonnull;

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

  private static void _streamFileTo (@Nonnull final File f, @Nonnull final UnifiedResponse aUR)
  {
    // setContent(IReadableResource) is lazy
    aUR.setContent (new FileSystemResource (f));
    final long nFileLen = f.length ();
    if (nFileLen > 0)
      aUR.setCustomResponseHeader (CHttpHeader.CONTENT_LENGTH, Long.toString (nFileLen));
  }

  @Nonnull
  @Nonempty
  public static ICommonsSortedSet <String> getAllStoredParticipantIDs () throws IOException
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
  private static File _getInternalFileBusinessCardXMLFull ()
  {
    return WebFileIO.getDataIO ().getFile (INTERNAL_EXPORT_ALL_BUSINESSCARDS_XML_FULL);
  }

  @Nonnull
  private static ESuccess _writeFileBusinessCardXML (@Nonnull final ICommonsSortedSet <String> aAllParticipantIDs,
                                                     @Nonnull final File f,
                                                     final boolean bIncludeDocTypes) throws IOException
  {
    final IIdentifierFactory aIF = PDMetaManager.getIdentifierFactory ();
    final PDStorageManager aStorageMgr = PDMetaManager.getStorageMgr ();

    final IMicroDocument aDoc = ExportHelper.createXML ();
    final IMicroElement aRoot = aDoc.getDocumentElement ();

    for (final String sParticipantID : aAllParticipantIDs)
    {
      final IParticipantIdentifier aParticipantID = aIF.parseParticipantIdentifier (sParticipantID);

      // Should never happen because PIs are parsed before added into the source set
      if (aParticipantID != null)
      {
        // Search all entities of the current participant ID
        final ICommonsList <PDStoredBusinessEntity> aEntitiesPerPI = new CommonsArrayList <> ();
        aStorageMgr.searchAllDocuments (new TermQuery (new Term (PDField.PARTICIPANT_ID.getFieldName (),
                                                                 sParticipantID)), -1, aEntitiesPerPI::add);
        aRoot.addChild (ExportHelper.createMicroElement (aParticipantID, aEntitiesPerPI, bIncludeDocTypes));
      }
    }

    if (MicroWriter.writeToFile (aDoc, f).isFailure ())
    {
      LOGGER.error ("Failed to export all BCs as XML (" +
                    (bIncludeDocTypes ? "full" : "no doctypes") +
                    ") to " +
                    f.getAbsolutePath ());
      return ESuccess.FAILURE;
    }
    LOGGER.info ("Successfully wrote all BCs as XML (" +
                 (bIncludeDocTypes ? "full" : "no doctypes") +
                 ") to " +
                 f.getAbsolutePath ());
    return ESuccess.SUCCESS;
  }

  @Nonnull
  static ESuccess writeFileBusinessCardXMLFull (@Nonnull final ICommonsSortedSet <String> aAllParticipantIDs) throws IOException
  {
    final File f = _getInternalFileBusinessCardXMLFull ();
    return _writeFileBusinessCardXML (aAllParticipantIDs, f, true);
  }

  /**
   * Stream the stored XML file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void streamFileBusinessCardXMLFullTo (@Nonnull final UnifiedResponse aUR)
  {
    _streamFileTo (_getInternalFileBusinessCardXMLFull (), aUR);
  }

  @Nonnull
  private static File _getInternalFileBusinessCardXMLNoDocTypes ()
  {
    return WebFileIO.getDataIO ().getFile (INTERNAL_EXPORT_ALL_BUSINESSCARDS_XML_NO_DOC_TYPES);
  }

  @Nonnull
  static ESuccess writeFileBusinessCardXMLNoDocTypes (@Nonnull final ICommonsSortedSet <String> aAllParticipantIDs) throws IOException
  {
    final File f = _getInternalFileBusinessCardXMLNoDocTypes ();
    return _writeFileBusinessCardXML (aAllParticipantIDs, f, false);
  }

  /**
   * Stream the stored XML file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void streamFileBusinessCardXMLNoDocTypesTo (@Nonnull final UnifiedResponse aUR)
  {
    _streamFileTo (_getInternalFileBusinessCardXMLNoDocTypes (), aUR);
  }

  @Nonnull
  private static File _getInternalFileBusinessCardJSON ()
  {
    return WebFileIO.getDataIO ().getFile (INTERNAL_EXPORT_ALL_BUSINESSCARDS_JSON);
  }

  @Nonnull
  static ESuccess writeFileBusinessCardJSON (@Nonnull final ICommonsSortedSet <String> aAllParticipantIDs)
  {
    final PDStorageManager aStorageMgr = PDMetaManager.getStorageMgr ();
    final File f = _getInternalFileBusinessCardJSON ();

    try (final Writer aWriter = FileHelper.getBufferedWriter (f, StandardCharsets.UTF_8))
    {
      // JSON root
      final IJsonObject aObj = new JsonObject ();
      aObj.add ("version", 2);
      aObj.add ("creationdt", PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentZonedDateTimeUTC ()));
      aObj.add ("participantCount", aAllParticipantIDs.size ());
      aObj.add ("codeListSupported", EPredefinedDocumentTypeIdentifier.CODE_LIST_VERSION);

      final IJsonArray aBCs = new JsonArray ();
      for (final String sParticipantID : aAllParticipantIDs)
      {
        // Search all entities of the current participant ID
        final ICommonsList <PDStoredBusinessEntity> aEntitiesPerPI = new CommonsArrayList <> ();
        aStorageMgr.searchAllDocuments (new TermQuery (new Term (PDField.PARTICIPANT_ID.getFieldName (),
                                                                 sParticipantID)), -1, aEntitiesPerPI::add);

        final IJsonObject aBC = new JsonObject ();
        aBC.add ("pid", sParticipantID);

        final IJsonArray aBEs = new JsonArray ();
        for (final PDStoredBusinessEntity aSBE : aEntitiesPerPI)
          aBEs.add (ExportHelper.createJsonObject (aSBE));
        aBC.add ("entities", aBEs);

        // Add all Document types (if wanted)
        if (true)
        {
          final IJsonArray aDocTypes = new JsonArray ();
          if (aEntitiesPerPI.isNotEmpty ())
            for (final IDocumentTypeIdentifier aDocTypeID : aEntitiesPerPI.getFirstOrNull ().documentTypeIDs ())
              aDocTypes.add (ExportHelper.createJsonObject (aDocTypeID));
          aBC.add ("docTypes", aDocTypes);
        }

        aBCs.add (aBC);

      }
      aObj.add ("bc", aBCs);

      new JsonWriter ().writeToWriterAndClose (aObj, aWriter);
      LOGGER.info ("Successfully wrote all BusinessCards as JSON to " + f.getAbsolutePath ());
      return ESuccess.SUCCESS;
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Failed to export all BusinessCards as JSON to " + f.getAbsolutePath (), ex);
      return ESuccess.FAILURE;
    }
  }

  /**
   * Stream the stored JSON file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void streamFileBusinessCardJSONTo (@Nonnull final UnifiedResponse aUR)
  {
    _streamFileTo (_getInternalFileBusinessCardJSON (), aUR);
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
  static ESuccess writeFileBusinessCardCSV (@Nonnull final ICommonsSortedSet <String> aAllParticipantIDs)
  {
    final PDStorageManager aStorageMgr = PDMetaManager.getStorageMgr ();
    final File f = _getInternalFileBusinessCardCSV ();

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
                              aEntity.getRegistrationDate () == null ? "" : aEntity.getRegistrationDate ().toString (),
                              StringImplode.imploder ()
                                           .source (aEntity.documentTypeIDs (), IDocumentTypeIdentifier::getURIEncoded)
                                           .separator ('\n')
                                           .build ());
      };

      for (final String sParticipantID : aAllParticipantIDs)
      {
        aStorageMgr.searchAllDocuments (new TermQuery (new Term (PDField.PARTICIPANT_ID.getFieldName (),
                                                                 sParticipantID)), -1, aCSVConsumer);
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
  }

  /**
   * Stream the stored CSV file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void streamFileBusinessCardCSVTo (@Nonnull final UnifiedResponse aUR)
  {
    _streamFileTo (_getInternalFileBusinessCardCSV (), aUR);
  }

  @Nonnull
  private static File _getInternalFileParticipantXML ()
  {
    return WebFileIO.getDataIO ().getFile (INTERNAL_EXPORT_ALL_PARTICIPANTS_XML);
  }

  @Nonnull
  static ESuccess writeFileParticipantXML (@Nonnull final ICommonsSortedSet <String> aAllParticipantIDs)
  {
    final IIdentifierFactory aIF = PDMetaManager.getIdentifierFactory ();
    final File f = _getInternalFileParticipantXML ();

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

    if (MicroWriter.writeToFile (aDoc, f).isFailure ())
    {
      LOGGER.error ("Failed to export all Participants as XML to " + f.getAbsolutePath ());
      return ESuccess.FAILURE;
    }
    LOGGER.info ("Successfully wrote all Participants as XML to " + f.getAbsolutePath ());
    return ESuccess.SUCCESS;
  }

  /**
   * Stream the stored XML file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void streamFileParticipantXMLTo (@Nonnull final UnifiedResponse aUR)
  {
    _streamFileTo (_getInternalFileParticipantXML (), aUR);
  }

  @Nonnull
  private static File _getInternalFileParticipantJSON ()
  {
    return WebFileIO.getDataIO ().getFile (INTERNAL_EXPORT_ALL_PARTICIPANTS_JSON);
  }

  @Nonnull
  static ESuccess writeFileParticipantJSON (@Nonnull final ICommonsSortedSet <String> aAllParticipantIDs)
  {
    final File f = _getInternalFileParticipantJSON ();

    try (final Writer aWriter = FileHelper.getBufferedWriter (f, StandardCharsets.UTF_8))
    {
      // JSON root
      final IJsonObject aObj = new JsonObject ();
      aObj.add ("version", 1);
      aObj.add ("creationdt", PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentZonedDateTimeUTC ()));
      aObj.add ("count", aAllParticipantIDs.size ());

      // For all participants
      final IJsonArray aArray = new JsonArray ();
      for (final String sParticipantID : aAllParticipantIDs)
        aArray.add (sParticipantID);
      aObj.add ("participants", aArray);

      new JsonWriter ().writeToWriterAndClose (aObj, aWriter);
      LOGGER.info ("Successfully wrote all Participants as JSON to " + f.getAbsolutePath ());
      return ESuccess.SUCCESS;
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Failed to export all Participants as JSON to " + f.getAbsolutePath (), ex);
      return ESuccess.FAILURE;
    }
  }

  /**
   * Stream the stored JSON file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void streamFileParticipantJSONTo (@Nonnull final UnifiedResponse aUR)
  {
    _streamFileTo (_getInternalFileParticipantJSON (), aUR);
  }

  @Nonnull
  private static File _getInternalFileParticipantCSV ()
  {
    return WebFileIO.getDataIO ().getFile (INTERNAL_EXPORT_ALL_PARTICIPANTS_CSV);
  }

  @Nonnull
  static ESuccess writeFileParticipantCSV (@Nonnull final ICommonsSortedSet <String> aAllParticipantIDs)
  {
    final File f = _getInternalFileParticipantCSV ();

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
  }

  /**
   * Stream the stored CSV file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void streamFileParticipantCSVTo (@Nonnull final UnifiedResponse aUR)
  {
    _streamFileTo (_getInternalFileParticipantCSV (), aUR);
  }
}
