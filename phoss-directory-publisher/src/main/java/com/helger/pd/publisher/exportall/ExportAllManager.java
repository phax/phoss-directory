/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.ThreadSafe;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.collection.multimap.MultiLinkedHashMapArrayListBased;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.csv.CSVWriter;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTWebDateHelper;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.pd.businesscard.generic.PDBusinessCard;
import com.helger.pd.businesscard.generic.PDIdentifier;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.storage.EQueryMode;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.pd.indexer.storage.PDStoredContact;
import com.helger.pd.indexer.storage.PDStoredIdentifier;
import com.helger.pd.indexer.storage.PDStoredMLName;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.photon.app.io.WebFileIO;
import com.helger.poi.excel.EExcelVersion;
import com.helger.poi.excel.WorkbookCreationHelper;
import com.helger.poi.excel.style.ExcelStyle;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;

@ThreadSafe
public final class ExportAllManager
{
  // Filenames for download
  public static final String EXTERNAL_EXPORT_ALL_BUSINESSCARDS_XML = "directory-export-business-cards.xml";
  public static final String EXTERNAL_EXPORT_ALL_BUSINESSCARDS_XLSX = "directory-export-business-cards.xlsx";
  public static final String EXTERNAL_EXPORT_ALL_BUSINESSCARDS_CSV = "directory-export-business-cards.csv";

  // Internal filenames
  private static final String INTERNAL_EXPORT_ALL_BUSINESSCARDS_XML = "export-all-businesscards.xml";
  private static final String INTERNAL_EXPORT_ALL_BUSINESSCARDS_XLSX = "export-all-businesscards.xlsx";
  private static final String INTERNAL_EXPORT_ALL_BUSINESSCARDS_CSV = "export-all-businesscards.csv";
  private static final Logger LOGGER = LoggerFactory.getLogger (ExportAllManager.class);

  private static final SimpleReadWriteLock s_aRWLock = new SimpleReadWriteLock ();

  private ExportAllManager ()
  {}

  @Nonnull
  public static IMicroDocument queryAllContainedBusinessCardsAsXML (@Nonnull final EQueryMode eQueryMode,
                                                                    final boolean bIncludeDocTypes) throws IOException
  {
    final Query aQuery = eQueryMode.getEffectiveQuery (new MatchAllDocsQuery ());

    // Query all and group by participant ID
    final MultiLinkedHashMapArrayListBased <IParticipantIdentifier, PDStoredBusinessEntity> aMap = new MultiLinkedHashMapArrayListBased <> ();
    PDMetaManager.getStorageMgr ().searchAllDocuments (aQuery, -1, x -> aMap.putSingle (x.getParticipantID (), x));

    // XML root
    final IMicroDocument aDoc = new MicroDocument ();
    final String sNamespaceURI = "http://www.peppol.eu/schema/pd/businesscard-generic/201907/";
    final IMicroElement aRoot = aDoc.appendElement (sNamespaceURI, "root");
    aRoot.setAttribute ("version", "2");
    aRoot.setAttribute ("creationdt", PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentZonedDateTimeUTC ()));

    // For all BCs
    for (final Map.Entry <IParticipantIdentifier, ICommonsList <PDStoredBusinessEntity>> aEntry : aMap.entrySet ())
    {
      final IParticipantIdentifier aParticipantID = aEntry.getKey ();

      final PDBusinessCard aBC = new PDBusinessCard ();
      aBC.setParticipantIdentifier (new PDIdentifier (aParticipantID.getScheme (), aParticipantID.getValue ()));
      for (final PDStoredBusinessEntity aSBE : aEntry.getValue ())
        aBC.businessEntities ().add (aSBE.getAsBusinessEntity ());
      final IMicroElement eBC = aBC.getAsMicroXML (sNamespaceURI, "businesscard");

      // New in v2 - add all Document types
      if (bIncludeDocTypes && aEntry.getValue ().isNotEmpty ())
        for (final IDocumentTypeIdentifier aDocTypeID : aEntry.getValue ().getFirst ().documentTypeIDs ())
        {
          eBC.appendElement (sNamespaceURI, "doctypeid")
             .setAttribute ("scheme", aDocTypeID.getScheme ())
             .setAttribute ("value", aDocTypeID.getValue ());
        }

      aRoot.appendChild (eBC);
    }

    return aDoc;
  }

  @Nonnull
  private static File _getInternalFileXMLFull ()
  {
    return WebFileIO.getDataIO ().getFile (INTERNAL_EXPORT_ALL_BUSINESSCARDS_XML);
  }

  @Nonnull
  static ESuccess writeFileXMLFull (@Nonnull final IMicroDocument aDoc)
  {
    final File f = _getInternalFileXMLFull ();

    // Do it in a write lock!
    s_aRWLock.writeLock ().lock ();
    try
    {
      if (MicroWriter.writeToFile (aDoc, f).isFailure ())
      {
        if (LOGGER.isErrorEnabled ())
          LOGGER.error ("Failed to export all BCs as XML to " + f.getAbsolutePath ());
        return ESuccess.FAILURE;
      }
      LOGGER.info ("Successfully wrote all BCs as XML to " + f.getAbsolutePath ());
    }
    finally
    {
      s_aRWLock.writeLock ().unlock ();
    }
    return ESuccess.SUCCESS;
  }

  /**
   * Stream the stored XML file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void streamFileXMLFullTo (@Nonnull final UnifiedResponse aUR)
  {
    // Do it in a read lock!
    s_aRWLock.readLock ().lock ();
    try
    {
      final File f = _getInternalFileXMLFull ();
      // setContent(IReadableResource) is lazy
      aUR.setContent (new FileSystemResource (f));
    }
    finally
    {
      s_aRWLock.readLock ().unlock ();
    }
  }

  @Nonnull
  public static WorkbookCreationHelper queryAllContainedBusinessCardsAsExcel (@Nonnull final EQueryMode eQueryMode,
                                                                              final boolean bIncludeDocTypes) throws IOException
  {
    final Query aQuery = eQueryMode.getEffectiveQuery (new MatchAllDocsQuery ());

    final ExcelStyle ES_DATE = new ExcelStyle ().setDataFormat ("yyyy-mm-dd");
    final ExcelStyle ES_WRAP = new ExcelStyle ().setWrapText (true);

    final WorkbookCreationHelper aWBCH = new WorkbookCreationHelper (EExcelVersion.XLSX);
    aWBCH.createNewSheet ();
    aWBCH.addRow ();
    aWBCH.addCell ("Participant ID");
    aWBCH.addCell ("Names (per-row)");
    aWBCH.addCell ("Country code");
    aWBCH.addCell ("Geo info");
    aWBCH.addCell ("Identifier schemes");
    aWBCH.addCell ("Identifier values");
    aWBCH.addCell ("Websites");
    aWBCH.addCell ("Contact type");
    aWBCH.addCell ("Contact name");
    aWBCH.addCell ("Contact phone");
    aWBCH.addCell ("Contact email");
    aWBCH.addCell ("Additional info");
    aWBCH.addCell ("Registration date");
    if (bIncludeDocTypes)
      aWBCH.addCell ("Document types");

    final Consumer <? super PDStoredBusinessEntity> aConsumer = aEntity -> {
      aWBCH.addRow ();
      aWBCH.addCell (aEntity.getParticipantID ().getURIEncoded ());
      aWBCH.addCell (StringHelper.getImplodedMapped ("\n", aEntity.names (), PDStoredMLName::getNameAndLanguageCode));
      aWBCH.addCellStyle (ES_WRAP);
      aWBCH.addCell (aEntity.getCountryCode ());
      aWBCH.addCell (aEntity.getGeoInfo ());
      aWBCH.addCellStyle (ES_WRAP);
      aWBCH.addCell (StringHelper.getImplodedMapped ("\n", aEntity.identifiers (), PDStoredIdentifier::getScheme));
      aWBCH.addCellStyle (ES_WRAP);
      aWBCH.addCell (StringHelper.getImplodedMapped ("\n", aEntity.identifiers (), PDStoredIdentifier::getValue));
      aWBCH.addCellStyle (ES_WRAP);
      aWBCH.addCell (StringHelper.getImploded ("\n", aEntity.websiteURIs ()));
      aWBCH.addCellStyle (ES_WRAP);
      aWBCH.addCell (StringHelper.getImplodedMapped ("\n", aEntity.contacts (), PDStoredContact::getType));
      aWBCH.addCellStyle (ES_WRAP);
      aWBCH.addCell (StringHelper.getImplodedMapped ("\n", aEntity.contacts (), PDStoredContact::getName));
      aWBCH.addCellStyle (ES_WRAP);
      aWBCH.addCell (StringHelper.getImplodedMapped ("\n", aEntity.contacts (), PDStoredContact::getPhone));
      aWBCH.addCellStyle (ES_WRAP);
      aWBCH.addCell (StringHelper.getImplodedMapped ("\n", aEntity.contacts (), PDStoredContact::getEmail));
      aWBCH.addCellStyle (ES_WRAP);
      aWBCH.addCell (aEntity.getAdditionalInformation ());
      aWBCH.addCellStyle (ES_WRAP);
      aWBCH.addCell (aEntity.getRegistrationDate ());
      aWBCH.addCellStyle (ES_DATE);
      if (bIncludeDocTypes)
      {
        aWBCH.addCell (StringHelper.getImplodedMapped ("\n",
                                                       aEntity.documentTypeIDs (),
                                                       IDocumentTypeIdentifier::getURIEncoded));
        aWBCH.addCellStyle (ES_WRAP);
      }
    };
    // Query all and group by participant ID
    PDMetaManager.getStorageMgr ().searchAllDocuments (aQuery, -1, aConsumer);
    aWBCH.autoSizeAllColumns ();
    aWBCH.autoFilterAllColumns ();

    return aWBCH;
  }

  @Nonnull
  private static File _getInternalFileExcel ()
  {
    return WebFileIO.getDataIO ().getFile (INTERNAL_EXPORT_ALL_BUSINESSCARDS_XLSX);
  }

  @Nonnull
  static ESuccess writeFileExcel (@Nonnull final Function <File, ESuccess> aFileWriter)
  {
    final File f = _getInternalFileExcel ();

    // Do it in a write lock!
    s_aRWLock.writeLock ().lock ();
    try
    {
      if (aFileWriter.apply (f).isFailure ())
      {
        if (LOGGER.isErrorEnabled ())
          LOGGER.error ("Failed to export all BCs as XLSX to " + f.getAbsolutePath ());
        return ESuccess.FAILURE;
      }
      LOGGER.info ("Successfully exported all BCs as XLSX to " + f.getAbsolutePath ());
    }
    finally
    {
      s_aRWLock.writeLock ().unlock ();
    }

    return ESuccess.SUCCESS;
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
      final File f = _getInternalFileExcel ();
      // setContent(IReadableResource) is lazy
      aUR.setContent (new FileSystemResource (f));
    }
    finally
    {
      s_aRWLock.readLock ().unlock ();
    }
  }

  public static void queryAllContainedBusinessCardsAsCSV (@Nonnull final EQueryMode eQueryMode,
                                                          @Nonnull @WillNotClose final CSVWriter aCSVWriter) throws IOException
  {
    aCSVWriter.setSeparatorChar (';');

    final Query aQuery = eQueryMode.getEffectiveQuery (new MatchAllDocsQuery ());

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
      aCSVWriter.writeNext (aEntity.getParticipantID ().getURIEncoded (),
                            StringHelper.getImplodedMapped ("\n",
                                                            aEntity.names (),
                                                            PDStoredMLName::getNameAndLanguageCode),
                            aEntity.getCountryCode (),
                            aEntity.getGeoInfo (),
                            StringHelper.getImplodedMapped ("\n",
                                                            aEntity.identifiers (),
                                                            PDStoredIdentifier::getScheme),
                            StringHelper.getImplodedMapped ("\n", aEntity.identifiers (), PDStoredIdentifier::getValue),
                            StringHelper.getImploded ("\n", aEntity.websiteURIs ()),
                            StringHelper.getImplodedMapped ("\n", aEntity.contacts (), PDStoredContact::getType),
                            StringHelper.getImplodedMapped ("\n", aEntity.contacts (), PDStoredContact::getName),
                            StringHelper.getImplodedMapped ("\n", aEntity.contacts (), PDStoredContact::getPhone),
                            StringHelper.getImplodedMapped ("\n", aEntity.contacts (), PDStoredContact::getEmail),
                            aEntity.getAdditionalInformation (),
                            aEntity.getRegistrationDate () == null ? "" : aEntity.getRegistrationDate ().toString (),
                            StringHelper.getImplodedMapped ("\n",
                                                            aEntity.documentTypeIDs (),
                                                            IDocumentTypeIdentifier::getURIEncoded));
    };
    PDMetaManager.getStorageMgr ().searchAllDocuments (aQuery, -1, aConsumer);
    aCSVWriter.flush ();
  }

  @Nonnull
  private static File _getInternalFileCSV ()
  {
    return WebFileIO.getDataIO ().getFile (INTERNAL_EXPORT_ALL_BUSINESSCARDS_CSV);
  }

  @Nonnull
  static ESuccess writeFileCSV (@Nonnull final EQueryMode eQueryMode)
  {
    final File f = _getInternalFileCSV ();

    // Do it in a write lock!
    s_aRWLock.writeLock ().lock ();
    try (final CSVWriter aCSVWriter = new CSVWriter (FileHelper.getBufferedWriter (f, StandardCharsets.ISO_8859_1)))
    {
      ExportAllManager.queryAllContainedBusinessCardsAsCSV (eQueryMode, aCSVWriter);
      LOGGER.info ("Successfully exported all BCs as CSV to " + f.getAbsolutePath ());
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Failed to export all BCs as CSV to " + f.getAbsolutePath (), ex);
    }
    finally
    {
      s_aRWLock.writeLock ().unlock ();
    }

    return ESuccess.SUCCESS;
  }

  /**
   * Stream the stored CSV file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void streamFileCSVTo (@Nonnull final UnifiedResponse aUR)
  {
    // Do it in a read lock!
    s_aRWLock.readLock ().lock ();
    try
    {
      final File f = _getInternalFileCSV ();
      // setContent(IReadableResource) is lazy
      aUR.setContent (new FileSystemResource (f));
    }
    finally
    {
      s_aRWLock.readLock ().unlock ();
    }
  }
}
