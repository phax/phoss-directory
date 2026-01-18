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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.function.Consumer;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.WillNotClose;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.io.stream.NonClosingOutputStream;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringImplode;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.CommonsTreeSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.collection.commons.ICommonsSortedSet;
import com.helger.csv.CSVWriter;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.web.PDTWebDateHelper;
import com.helger.io.file.FileHelper;
import com.helger.mime.CMimeType;
import com.helger.mime.IMimeType;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.settings.PDServerConfiguration;
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
import com.helger.security.messagedigest.EMessageDigestAlgorithm;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.serialize.write.XMLWriterSettings;

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
  private static final String S3_FOLDER_NAME = "export1/";
  private static final String INTERNAL_EXPORT_ALL_BUSINESSCARDS_XML_FULL = S3_FOLDER_NAME +
                                                                           "export-all-businesscards.xml";
  private static final String INTERNAL_EXPORT_ALL_BUSINESSCARDS_XML_NO_DOC_TYPES = S3_FOLDER_NAME +
                                                                                   "export-all-businesscards-no-doc-types.xml";
  private static final String INTERNAL_EXPORT_ALL_BUSINESSCARDS_JSON = S3_FOLDER_NAME + "export-all-businesscards.json";
  private static final String INTERNAL_EXPORT_ALL_BUSINESSCARDS_CSV = S3_FOLDER_NAME + "export-all-businesscards.csv";
  private static final String INTERNAL_EXPORT_ALL_PARTICIPANTS_XML = S3_FOLDER_NAME + "export-all-participants.xml";
  private static final String INTERNAL_EXPORT_ALL_PARTICIPANTS_JSON = S3_FOLDER_NAME + "export-all-participants.json";
  private static final String INTERNAL_EXPORT_ALL_PARTICIPANTS_CSV = S3_FOLDER_NAME + "export-all-participants.csv";

  // Rest
  private static final Logger LOGGER = LoggerFactory.getLogger (ExportAllManager.class);

  private ExportAllManager ()
  {}

  @NonNull
  private static ESuccess _runWithTempFileOnS3 (@NonNull final String sS3Filename,
                                                @NonNull final IMimeType aContentType,
                                                @NonNull final Consumer <OutputStream> aByteProducer) throws IOException
  {
    // 1. Create a temp file
    final File fTemp = File.createTempFile ("pd-", ".xml");

    try
    {
      // 2. Write data to temp file
      final MessageDigest aMD = EMessageDigestAlgorithm.SHA_256.createMessageDigest ();
      try (final OutputStream aFOS = FileHelper.getBufferedOutputStream (fTemp);
           final DigestOutputStream aDOS = new DigestOutputStream (aFOS, aMD))
      {
        aByteProducer.accept (aDOS);
      }

      final byte [] aHashBytes = aMD.digest ();
      LOGGER.info ("Finished writing temp file '" + fTemp.getAbsolutePath () + "' - now upload to S3");

      // 3. Now upload the temp file to S3
      final String sBucketName = PDServerConfiguration.getS3BucketName ();
      final String sTempFilename = sS3Filename + ".temp";

      try
      {
        // Upload; this call reads from the PipedInputStream while producer writes
        // Throws a runtime exception in case of error
        S3Helper.putS3Object (sBucketName, sTempFilename, aContentType, fTemp, aHashBytes);
      }
      catch (final Throwable ex)
      {
        LOGGER.error ("Failed to initially upload to S3", ex);
        return ESuccess.FAILURE;
      }

      // As S3 has no rename, we need to do copy and delete
      // 4. Delete the original file, if it exists
      S3Helper.deleteS3Object (sBucketName, sS3Filename);

      // 5. copy the temp file to the new file
      if (S3Helper.copyS3Object (sBucketName, sTempFilename, sS3Filename).isFailure ())
      {
        LOGGER.error ("Failed to copy on S3 '" + sBucketName + "' / '" + sTempFilename + "' to '" + sS3Filename + "'");
        return ESuccess.FAILURE;
      }

      // 6. Delete the temp file
      S3Helper.deleteS3Object (sBucketName, sTempFilename);
      LOGGER.info ("Finished S3 uploading");
      return ESuccess.SUCCESS;
    }
    finally
    {
      fTemp.delete ();
    }
  }

  @NonNull
  public static InputStream streamBusinessCardXMLFull ()
  {
    final String sBucketName = PDServerConfiguration.getS3BucketName ();
    return S3Helper.getS3Object (sBucketName, INTERNAL_EXPORT_ALL_BUSINESSCARDS_XML_FULL);
  }

  @NonNull
  public static InputStream streamBusinessCardXMLNoDocTypes ()
  {
    final String sBucketName = PDServerConfiguration.getS3BucketName ();
    return S3Helper.getS3Object (sBucketName, INTERNAL_EXPORT_ALL_BUSINESSCARDS_XML_NO_DOC_TYPES);
  }

  private static void _redirectTo (@NonNull final String sKey, @NonNull final UnifiedResponse aUR)
  {
    // Get data directly from S3
    aUR.setRedirect (S3Helper.S3_PUBLIC_URL + sKey);
  }

  @NonNull
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
  @NonNull
  public static IMicroDocument queryAllContainedBusinessCardsAsXML (@NonNull final Query aQuery,
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

    return ExportHelper.getAllBusinessCardsAsUIXML (aMap, bIncludeDocTypes);
  }

  @NonNull
  private static ESuccess _writeFileBusinessCardXML (@NonNull final ICommonsSortedSet <String> aAllParticipantIDs,
                                                     @NonNull @WillNotClose final OutputStream aOS,
                                                     final boolean bIncludeDocTypes)
  {
    final IIdentifierFactory aIF = PDMetaManager.getIdentifierFactory ();
    final PDStorageManager aStorageMgr = PDMetaManager.getStorageMgr ();
    final XMLOutputFactory aXmlOutputFactory = XMLOutputFactory.newInstance ();
    try
    {
      final XMLStreamWriter aXmlWriter = aXmlOutputFactory.createXMLStreamWriter (aOS);

      aXmlWriter.setDefaultNamespace (ExportHelper.XML_EXPORT_NS_URI_V3);

      // XML root
      aXmlWriter.writeStartDocument (XMLWriterSettings.DEFAULT_XML_CHARSET, "1.0");

      aXmlWriter.writeStartElement (ExportHelper.XML_EXPORT_NS_URI_V3, "root");
      aXmlWriter.writeAttribute ("xmlns", ExportHelper.XML_EXPORT_NS_URI_V3);
      aXmlWriter.writeAttribute ("version", "3");
      aXmlWriter.writeAttribute ("creationdt",
                                 PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentZonedDateTimeUTC ()));
      aXmlWriter.writeAttribute ("codeListSupported", EPredefinedDocumentTypeIdentifier.CODE_LIST_VERSION);

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
            ExportHelper.exportSingleBusinessCard (aParticipantID, aEntitiesPerPI, bIncludeDocTypes, aXmlWriter);
          }
        }
      }

      // root
      aXmlWriter.writeEndElement ();
      aXmlWriter.writeEndDocument ();

      LOGGER.info ("Successfully wrote all BCs as XML (" + (bIncludeDocTypes ? "full" : "no doctypes") + ")");
      return ESuccess.SUCCESS;
    }
    catch (final IOException | XMLStreamException ex)
    {
      LOGGER.error ("Failed to export all BCs as XML (" + (bIncludeDocTypes ? "full" : "no doctypes") + ")", ex);
      return ESuccess.FAILURE;
    }
  }

  @NonNull
  static ESuccess writeFileBusinessCardXMLFull (@NonNull final ICommonsSortedSet <String> aAllParticipantIDs) throws IOException
  {
    return _runWithTempFileOnS3 (INTERNAL_EXPORT_ALL_BUSINESSCARDS_XML_FULL,
                                 CMimeType.APPLICATION_XML,
                                 aOS -> _writeFileBusinessCardXML (aAllParticipantIDs, aOS, true));
  }

  /**
   * Stream the stored XML file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void redirectToBusinessCardXMLFull (@NonNull final UnifiedResponse aUR)
  {
    _redirectTo (INTERNAL_EXPORT_ALL_BUSINESSCARDS_XML_FULL, aUR);
  }

  @NonNull
  static ESuccess writeFileBusinessCardXMLNoDocTypes (@NonNull final ICommonsSortedSet <String> aAllParticipantIDs) throws IOException
  {
    return _runWithTempFileOnS3 (INTERNAL_EXPORT_ALL_BUSINESSCARDS_XML_NO_DOC_TYPES,
                                 CMimeType.APPLICATION_XML,
                                 aOS -> _writeFileBusinessCardXML (aAllParticipantIDs, aOS, false));
  }

  /**
   * Stream the stored XML file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void redirectToBusinessCardXMLNoDocTypes (@NonNull final UnifiedResponse aUR)
  {
    _redirectTo (INTERNAL_EXPORT_ALL_BUSINESSCARDS_XML_NO_DOC_TYPES, aUR);
  }

  @NonNull
  static ESuccess writeFileBusinessCardJSON (@NonNull final ICommonsSortedSet <String> aAllParticipantIDs) throws IOException
  {
    return _runWithTempFileOnS3 (INTERNAL_EXPORT_ALL_BUSINESSCARDS_JSON, CMimeType.APPLICATION_JSON, aOS -> {
      final PDStorageManager aStorageMgr = PDMetaManager.getStorageMgr ();
      final boolean bIncludeDocTypes = true;

      try (final Writer aWriter = StreamHelper.createWriter (new NonClosingOutputStream (aOS), StandardCharsets.UTF_8);
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

        LOGGER.info ("Successfully wrote all BusinessCards as JSON");
      }
      catch (final IOException ex)
      {
        LOGGER.error ("Failed to export all BusinessCards as JSON", ex);
      }
    });
  }

  /**
   * Stream the stored JSON file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void redirectToBusinessCardJSON (@NonNull final UnifiedResponse aUR)
  {
    _redirectTo (INTERNAL_EXPORT_ALL_BUSINESSCARDS_JSON, aUR);
  }

  private static void _unify (@NonNull @WillNotClose final CSVWriter aCSVWriter)
  {
    aCSVWriter.setSeparatorChar (';');
  }

  @NonNull
  static ESuccess writeFileBusinessCardCSV (@NonNull final ICommonsSortedSet <String> aAllParticipantIDs) throws IOException
  {
    return _runWithTempFileOnS3 (INTERNAL_EXPORT_ALL_BUSINESSCARDS_CSV, CMimeType.TEXT_CSV, aOS -> {
      final PDStorageManager aStorageMgr = PDMetaManager.getStorageMgr ();

      try (final CSVWriter aCSVWriter = new CSVWriter (StreamHelper.createWriter (new NonClosingOutputStream (aOS),
                                                                                  StandardCharsets.ISO_8859_1)))
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
        LOGGER.info ("Successfully exported all BCs as CSV");
      }
      catch (final IOException ex)
      {
        LOGGER.error ("Failed to export all BCs as CSV", ex);
      }
    });
  }

  /**
   * Stream the stored CSV file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void redirectToBusinessCardCSV (@NonNull final UnifiedResponse aUR)
  {
    _redirectTo (INTERNAL_EXPORT_ALL_BUSINESSCARDS_CSV, aUR);
  }

  @NonNull
  static ESuccess writeFileParticipantXML (@NonNull final ICommonsSortedSet <String> aAllParticipantIDs) throws IOException
  {
    return _runWithTempFileOnS3 (INTERNAL_EXPORT_ALL_PARTICIPANTS_XML, CMimeType.APPLICATION_XML, aOS -> {
      final IIdentifierFactory aIF = PDMetaManager.getIdentifierFactory ();
      final XMLOutputFactory aXmlOutputFactory = XMLOutputFactory.newInstance ();
      try
      {
        final XMLStreamWriter aXmlWriter = aXmlOutputFactory.createXMLStreamWriter (aOS);

        final String sNamespaceURI = "http://www.peppol.eu/schema/pd/participant-generic/201910/";
        aXmlWriter.setDefaultNamespace (sNamespaceURI);

        // XML root
        aXmlWriter.writeStartDocument (XMLWriterSettings.DEFAULT_XML_CHARSET, "1.0");

        aXmlWriter.writeStartElement (sNamespaceURI, "root");
        aXmlWriter.writeAttribute ("xmlns", sNamespaceURI);
        aXmlWriter.writeAttribute ("version", "1");
        aXmlWriter.writeAttribute ("creationdt",
                                   PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentZonedDateTimeUTC ()));
        aXmlWriter.writeAttribute ("count", Integer.toString (aAllParticipantIDs.size ()));

        // For all participants
        for (final String sParticipantID : aAllParticipantIDs)
        {
          final IParticipantIdentifier aPI = aIF.parseParticipantIdentifier (sParticipantID);

          aXmlWriter.writeEmptyElement (sNamespaceURI, "participantID");
          // Should never happen because PIs are parsed before added into the source set
          if (aPI != null)
          {
            aXmlWriter.writeAttribute ("scheme", aPI.getScheme ());
            aXmlWriter.writeAttribute ("value", aPI.getValue ());
          }
        }

        // root
        aXmlWriter.writeEndElement ();

        aXmlWriter.writeEndDocument ();

        LOGGER.info ("Successfully wrote all Participants as XML");
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Failed to export all Participants as XML", ex);
      }
    });
  }

  /**
   * Stream the stored XML file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void redirectToParticipantXML (@NonNull final UnifiedResponse aUR)
  {
    _redirectTo (INTERNAL_EXPORT_ALL_PARTICIPANTS_XML, aUR);
  }

  @NonNull
  static ESuccess writeFileParticipantJSON (@NonNull final ICommonsSortedSet <String> aAllParticipantIDs) throws IOException
  {
    return _runWithTempFileOnS3 (INTERNAL_EXPORT_ALL_PARTICIPANTS_JSON, CMimeType.APPLICATION_JSON, aOS -> {
      try (final Writer aWriter = StreamHelper.createWriter (new NonClosingOutputStream (aOS), StandardCharsets.UTF_8);
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
        LOGGER.info ("Successfully wrote all Participants as JSON");
      }
      catch (final IOException ex)
      {
        LOGGER.error ("Failed to export all Participants as JSON", ex);
      }
    });
  }

  /**
   * Stream the stored JSON file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void redirectToParticipantJSON (@NonNull final UnifiedResponse aUR)
  {
    _redirectTo (INTERNAL_EXPORT_ALL_PARTICIPANTS_JSON, aUR);
  }

  @NonNull
  static ESuccess writeFileParticipantCSV (@NonNull final ICommonsSortedSet <String> aAllParticipantIDs) throws IOException
  {
    return _runWithTempFileOnS3 (INTERNAL_EXPORT_ALL_PARTICIPANTS_CSV, CMimeType.TEXT_CSV, aOS -> {
      try (final CSVWriter aCSVWriter = new CSVWriter (StreamHelper.createWriter (new NonClosingOutputStream (aOS),
                                                                                  StandardCharsets.ISO_8859_1)))
      {
        _unify (aCSVWriter);
        aCSVWriter.writeNext ("Participant ID");
        for (final String sParticipantID : aAllParticipantIDs)
          aCSVWriter.writeNext (sParticipantID);

        aCSVWriter.flush ();
        LOGGER.info ("Successfully wrote all Participants as CSV");
      }
      catch (final IOException ex)
      {
        LOGGER.error ("Failed to export all Participants as CSV", ex);
      }
    });
  }

  /**
   * Stream the stored CSV file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void redirectToParticipantCSV (@NonNull final UnifiedResponse aUR)
  {
    _redirectTo (INTERNAL_EXPORT_ALL_PARTICIPANTS_CSV, aUR);
  }
}
