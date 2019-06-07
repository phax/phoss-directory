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
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.collection.multimap.MultiLinkedHashMapArrayListBased;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTWebDateHelper;
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.pd.businesscard.generic.PDBusinessCard;
import com.helger.pd.businesscard.generic.PDBusinessEntity;
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
  private static final String EXPORT_ALL_BUSINESSCARDS_XML = "export-all-businesscards.xml";
  private static final String EXPORT_ALL_BUSINESSCARDS_XLSX = "export-all-businesscards.xlsx";
  private static final Logger LOGGER = LoggerFactory.getLogger (ExportAllManager.class);

  private static final SimpleReadWriteLock s_aRWLock = new SimpleReadWriteLock ();

  private ExportAllManager ()
  {}

  @Nonnull
  public static IMicroDocument getAllContainedBusinessCardsAsXML (@Nonnull final EQueryMode eQueryMode) throws IOException
  {
    final Query aQuery = eQueryMode.getEffectiveQuery (new MatchAllDocsQuery ());

    // Query all and group by participant ID
    final MultiLinkedHashMapArrayListBased <IParticipantIdentifier, PDBusinessEntity> aMap = new MultiLinkedHashMapArrayListBased <> ();
    PDMetaManager.getStorageMgr ()
                 .searchAllDocuments (aQuery,
                                      -1,
                                      x -> aMap.putSingle (x.getParticipantID (), x.getAsBusinessEntity ()));

    // XML root
    final IMicroDocument aDoc = new MicroDocument ();
    final String sNamespaceURI = "http://www.peppol.eu/schema/pd/businesscard-generic/201806/";
    final IMicroElement aRoot = aDoc.appendElement (sNamespaceURI, "root");
    aRoot.setAttribute ("version", "1");
    aRoot.setAttribute ("creationdt", PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentZonedDateTimeUTC ()));

    // For all BCs
    for (final Map.Entry <IParticipantIdentifier, ICommonsList <PDBusinessEntity>> aEntry : aMap.entrySet ())
    {
      final IParticipantIdentifier aParticipantID = aEntry.getKey ();
      final PDBusinessCard aBC = new PDBusinessCard ();
      aBC.setParticipantIdentifier (new PDIdentifier (aParticipantID.getScheme (), aParticipantID.getValue ()));
      aBC.businessEntities ().addAll (aEntry.getValue ());
      aRoot.appendChild (aBC.getAsMicroXML (sNamespaceURI, "businesscard"));
    }

    return aDoc;
  }

  @Nonnull
  private static File _getFileXML ()
  {
    return WebFileIO.getDataIO ().getFile (EXPORT_ALL_BUSINESSCARDS_XML);
  }

  static void writeFileXML (@Nonnull final IMicroDocument aDoc)
  {
    // Do it in a write lock!
    s_aRWLock.writeLock ().lock ();
    try
    {
      final File f = _getFileXML ();
      if (MicroWriter.writeToFile (aDoc, f).isFailure ())
        if (LOGGER.isErrorEnabled ())
          LOGGER.error ("Failed to export all BCs as XML to " + f.getAbsolutePath ());
    }
    finally
    {
      s_aRWLock.writeLock ().unlock ();
    }
  }

  /**
   * Stream the stored XML file to the provided HTTP response
   *
   * @param aUR
   *        The response to stream to. May not be <code>null</code>.
   */
  public static void streamFileXMLTo (@Nonnull final UnifiedResponse aUR)
  {
    // Do it in a read lock!
    s_aRWLock.readLock ().lock ();
    try
    {
      final File f = _getFileXML ();
      // setContent(IReadableResource) is lazy
      aUR.setContent (new FileSystemResource (f));
    }
    finally
    {
      s_aRWLock.readLock ().unlock ();
    }
  }

  @Nonnull
  public static WorkbookCreationHelper getAllContainedBusinessCardsAsExcel (@Nonnull final EQueryMode eQueryMode) throws IOException
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
      aWBCH.addCell (StringHelper.getImplodedMapped ("\n",
                                                     aEntity.documentTypeIDs (),
                                                     IDocumentTypeIdentifier::getURIEncoded));
      aWBCH.addCellStyle (ES_WRAP);
    };
    // Query all and group by participant ID
    PDMetaManager.getStorageMgr ().searchAllDocuments (aQuery, -1, aConsumer);
    aWBCH.autoSizeAllColumns ();
    aWBCH.autoFilterAllColumns ();

    return aWBCH;
  }

  @Nonnull
  private static File _getFileExcel ()
  {
    return WebFileIO.getDataIO ().getFile (EXPORT_ALL_BUSINESSCARDS_XLSX);
  }

  static void writeFileExcel (@Nonnull final Function <File, ESuccess> aFileWriter)
  {
    // Do it in a write lock!
    s_aRWLock.writeLock ().lock ();
    try
    {
      final File f = _getFileExcel ();

      if (aFileWriter.apply (f).isFailure ())
        if (LOGGER.isErrorEnabled ())
          LOGGER.error ("Failed to export all BCs as XLSX to " + f.getAbsolutePath ());
    }
    finally
    {
      s_aRWLock.writeLock ().unlock ();
    }
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
      final File f = _getFileExcel ();
      // setContent(IReadableResource) is lazy
      aUR.setContent (new FileSystemResource (f));
    }
    finally
    {
      s_aRWLock.readLock ().unlock ();
    }
  }
}
