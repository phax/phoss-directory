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
import java.util.Comparator;
import java.util.function.Consumer;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.WillNotClose;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.concurrent.SimpleReadWriteLock;
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
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.pd.indexer.storage.PDStoredContact;
import com.helger.pd.indexer.storage.PDStoredIdentifier;
import com.helger.pd.indexer.storage.PDStoredMLName;
import com.helger.pd.indexer.storage.field.PDField;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
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

  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();

  private ExportAllManager ()
  {}

  @Nonnull
  public static IMicroDocument queryAllContainedBusinessCardsAsXML (@Nonnull final Query aQuery,
                                                                    final boolean bIncludeDocTypes) throws IOException
  {
    // Query all and group by participant ID
    final ICommonsOrderedMap <IParticipantIdentifier, ICommonsList <PDStoredBusinessEntity>> aMap = new CommonsLinkedHashMap <> ();
    PDMetaManager.getStorageMgr ().searchAllDocuments (aQuery, -1, aEntity -> {
      if (!aEntity.hasParticipantID ())
        return;

      aMap.computeIfAbsent (aEntity.getParticipantID (), k -> new CommonsArrayList <> ()).add (aEntity);
    });

    return ExportHelper.getAsXML (aMap, bIncludeDocTypes);
  }

  @Nonnull
  public static IMicroDocument queryAllContainedBusinessCardsAsXML (final boolean bIncludeDocTypes) throws IOException
  {
    final Query aQuery = new MatchAllDocsQuery ();
    return queryAllContainedBusinessCardsAsXML (aQuery, bIncludeDocTypes);
  }

  @Nonnull
  private static File _getInternalFileBusinessCardXMLFull ()
  {
    return WebFileIO.getDataIO ().getFile (INTERNAL_EXPORT_ALL_BUSINESSCARDS_XML_FULL);
  }

  @Nonnull
  static ESuccess writeFileBusinessCardXMLFull () throws IOException
  {
    final IMicroDocument aDoc = queryAllContainedBusinessCardsAsXML (true);
    final File f = _getInternalFileBusinessCardXMLFull ();

    // Do it in a write lock!
    RW_LOCK.writeLock ().lock ();
    try
    {
      if (MicroWriter.writeToFile (aDoc, f).isFailure ())
      {
        LOGGER.error ("Failed to export all BCs as XML (full) to " + f.getAbsolutePath ());
        return ESuccess.FAILURE;
      }
      LOGGER.info ("Successfully wrote all BCs as XML (full) to " + f.getAbsolutePath ());
    }
    finally
    {
      RW_LOCK.writeLock ().unlock ();
    }
    return ESuccess.SUCCESS;
  }

  private static void _streamFileTo (@Nonnull final File f, @Nonnull final UnifiedResponse aUR)
  {
    // Do it in a read lock!
    RW_LOCK.readLock ().lock ();
    try
    {
      // setContent(IReadableResource) is lazy
      aUR.setContent (new FileSystemResource (f));
      final long nFileLen = f.length ();
      if (nFileLen > 0)
        aUR.setCustomResponseHeader (CHttpHeader.CONTENT_LENGTH, Long.toString (nFileLen));
    }
    finally
    {
      RW_LOCK.readLock ().unlock ();
    }
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
  static ESuccess writeFileBusinessCardXMLNoDocTypes () throws IOException
  {
    final IMicroDocument aDoc = queryAllContainedBusinessCardsAsXML (false);
    final File f = _getInternalFileBusinessCardXMLNoDocTypes ();

    // Do it in a write lock!
    RW_LOCK.writeLock ().lock ();
    try
    {
      if (MicroWriter.writeToFile (aDoc, f).isFailure ())
      {
        LOGGER.error ("Failed to export all BCs as XML (no doctypes) to " + f.getAbsolutePath ());
        return ESuccess.FAILURE;
      }
      LOGGER.info ("Successfully wrote all BCs as XML (no doctypes) to " + f.getAbsolutePath ());
    }
    finally
    {
      RW_LOCK.writeLock ().unlock ();
    }
    return ESuccess.SUCCESS;
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
  public static IJsonObject queryAllContainedBusinessCardsAsJSON (final boolean bIncludeDocTypes) throws IOException
  {
    final Query aQuery = new MatchAllDocsQuery ();

    // Query all and group by participant ID
    final ICommonsOrderedMap <IParticipantIdentifier, ICommonsList <PDStoredBusinessEntity>> aMap = new CommonsLinkedHashMap <> ();
    PDMetaManager.getStorageMgr ().searchAllDocuments (aQuery, -1, aEntity -> {
      if (!aEntity.hasParticipantID ())
        return;

      aMap.computeIfAbsent (aEntity.getParticipantID (), k -> new CommonsArrayList <> ()).add (aEntity);
    });

    return ExportHelper.getAsJSON (aMap, bIncludeDocTypes);
  }

  @Nonnull
  static ESuccess writeFileBusinessCardJSON () throws IOException
  {
    final IJsonObject aObj = queryAllContainedBusinessCardsAsJSON (true);
    final File f = _getInternalFileBusinessCardJSON ();

    // Do it in a write lock!
    RW_LOCK.writeLock ().lock ();
    try (final Writer aWriter = FileHelper.getBufferedWriter (f, StandardCharsets.UTF_8))
    {
      new JsonWriter ().writeToWriterAndClose (aObj, aWriter);
      LOGGER.info ("Successfully wrote all BusinessCards as JSON to " + f.getAbsolutePath ());
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Failed to export all BusinessCards as JSON to " + f.getAbsolutePath (), ex);
    }
    finally
    {
      RW_LOCK.writeLock ().unlock ();
    }
    return ESuccess.SUCCESS;
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

  public static void queryAllContainedBusinessCardsAsCSV (@Nonnull @WillNotClose final CSVWriter aCSVWriter) throws IOException
  {
    _unify (aCSVWriter);

    final Query aQuery = new MatchAllDocsQuery ();

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

    final Consumer <? super PDStoredBusinessEntity> aConsumer = aEntity -> {
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
    PDMetaManager.getStorageMgr ().searchAllDocuments (aQuery, -1, aConsumer);
    aCSVWriter.flush ();
  }

  @Nonnull
  private static File _getInternalFileBusinessCardCSV ()
  {
    return WebFileIO.getDataIO ().getFile (INTERNAL_EXPORT_ALL_BUSINESSCARDS_CSV);
  }

  @Nonnull
  static ESuccess writeFileBusinessCardCSV ()
  {
    final File f = _getInternalFileBusinessCardCSV ();

    // Do it in a write lock!
    RW_LOCK.writeLock ().lock ();
    try (final CSVWriter aCSVWriter = new CSVWriter (FileHelper.getBufferedWriter (f, StandardCharsets.ISO_8859_1)))
    {
      queryAllContainedBusinessCardsAsCSV (aCSVWriter);
      LOGGER.info ("Successfully exported all BCs as CSV to " + f.getAbsolutePath ());
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Failed to export all BCs as CSV to " + f.getAbsolutePath (), ex);
    }
    finally
    {
      RW_LOCK.writeLock ().unlock ();
    }
    return ESuccess.SUCCESS;
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
  public static IMicroDocument queryAllContainedParticipantsAsXML () throws IOException
  {
    final Query aQuery = new MatchAllDocsQuery ();

    // Query all and group by participant ID
    final ICommonsSortedSet <IParticipantIdentifier> aSet = new CommonsTreeSet <> (Comparator.comparing (IParticipantIdentifier::getURIEncoded));
    PDMetaManager.getStorageMgr ().searchAll (aQuery, -1, PDField.PARTICIPANT_ID::getDocValue, aSet::add);

    // XML root
    final IMicroDocument aDoc = new MicroDocument ();
    final String sNamespaceURI = "http://www.peppol.eu/schema/pd/participant-generic/201910/";
    final IMicroElement aRoot = aDoc.addElementNS (sNamespaceURI, "root");
    aRoot.setAttribute ("version", "1");
    aRoot.setAttribute ("creationdt", PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentZonedDateTimeUTC ()));
    aRoot.setAttribute ("count", aSet.size ());

    // For all participants
    for (final IParticipantIdentifier aParticipantID : aSet)
    {
      aRoot.addElementNS (sNamespaceURI, "participantID")
           .setAttribute ("scheme", aParticipantID.getScheme ())
           .setAttribute ("value", aParticipantID.getValue ());
    }
    return aDoc;
  }

  @Nonnull
  static ESuccess writeFileParticipantXML () throws IOException
  {
    final IMicroDocument aDoc = queryAllContainedParticipantsAsXML ();
    final File f = _getInternalFileParticipantXML ();

    // Do it in a write lock!
    RW_LOCK.writeLock ().lock ();
    try
    {
      if (MicroWriter.writeToFile (aDoc, f).isFailure ())
      {
        LOGGER.error ("Failed to export all Participants as XML to " + f.getAbsolutePath ());
        return ESuccess.FAILURE;
      }
      LOGGER.info ("Successfully wrote all Participants as XML to " + f.getAbsolutePath ());
    }
    finally
    {
      RW_LOCK.writeLock ().unlock ();
    }
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
  public static IJsonObject queryAllContainedParticipantsAsJSON () throws IOException
  {
    final Query aQuery = new MatchAllDocsQuery ();

    // Query all and group by participant ID
    final ICommonsSortedSet <IParticipantIdentifier> aSet = new CommonsTreeSet <> (Comparator.comparing (IParticipantIdentifier::getURIEncoded));
    PDMetaManager.getStorageMgr ().searchAll (aQuery, -1, PDField.PARTICIPANT_ID::getDocValue, aSet::add);

    // XML root
    final IJsonObject aObj = new JsonObject ();
    aObj.add ("version", 1);
    aObj.add ("creationdt", PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentZonedDateTimeUTC ()));
    aObj.add ("count", aSet.size ());

    // For all participants
    final IJsonArray aArray = new JsonArray ();
    for (final IParticipantIdentifier aParticipantID : aSet)
      aArray.add (aParticipantID.getURIEncoded ());
    aObj.add ("participants", aArray);

    return aObj;
  }

  @Nonnull
  static ESuccess writeFileParticipantJSON () throws IOException
  {
    final IJsonObject aObj = queryAllContainedParticipantsAsJSON ();
    final File f = _getInternalFileParticipantJSON ();

    // Do it in a write lock!
    RW_LOCK.writeLock ().lock ();
    try (final Writer aWriter = FileHelper.getBufferedWriter (f, StandardCharsets.UTF_8))
    {
      new JsonWriter ().writeToWriterAndClose (aObj, aWriter);
      LOGGER.info ("Successfully wrote all Participants as JSON to " + f.getAbsolutePath ());
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Failed to export all Participants as JSON to " + f.getAbsolutePath (), ex);
    }
    finally
    {
      RW_LOCK.writeLock ().unlock ();
    }
    return ESuccess.SUCCESS;
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

  public static void queryAllContainedParticipantsAsCSV (@Nonnull @WillNotClose final CSVWriter aCSVWriter) throws IOException
  {
    _unify (aCSVWriter);

    final Query aQuery = new MatchAllDocsQuery ();

    aCSVWriter.writeNext ("Participant ID");

    final Consumer <? super IParticipantIdentifier> aConsumer = aEntity -> {
      aCSVWriter.writeNext (aEntity.getURIEncoded ());
    };
    PDMetaManager.getStorageMgr ().searchAll (aQuery, -1, PDField.PARTICIPANT_ID::getDocValue, aConsumer);
    aCSVWriter.flush ();
  }

  @Nonnull
  static ESuccess writeFileParticipantCSV ()
  {
    final File f = _getInternalFileParticipantCSV ();

    // Do it in a write lock!
    RW_LOCK.writeLock ().lock ();
    try (final CSVWriter aCSVWriter = new CSVWriter (FileHelper.getBufferedWriter (f, StandardCharsets.ISO_8859_1)))
    {
      queryAllContainedParticipantsAsCSV (aCSVWriter);
      LOGGER.info ("Successfully wrote all Participants as CSV to " + f.getAbsolutePath ());
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Failed to export all Participants as CSV to " + f.getAbsolutePath (), ex);
    }
    finally
    {
      RW_LOCK.writeLock ().unlock ();
    }
    return ESuccess.SUCCESS;
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
