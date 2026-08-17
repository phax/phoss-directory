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
package com.helger.pd.indexer.conformance;

import java.time.Month;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.pd.indexer.businesscard.PDExtendedBusinessCard;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.searchindex.EPDIndexFieldStore;
import com.helger.pd.indexer.searchindex.EPDIndexFieldTokenize;
import com.helger.pd.indexer.searchindex.PDIndexDocument;
import com.helger.pd.indexer.searchindex.PDIndexField;
import com.helger.pd.indexer.storage.CPDStorage;
import com.helger.pd.indexer.storage.PDStoredMetaData;
import com.helger.pd.indexer.storage.field.PDField;
import com.helger.peppol.businesscard.generic.PDBusinessCard;
import com.helger.peppol.businesscard.generic.PDBusinessEntity;
import com.helger.peppol.businesscard.generic.PDContact;
import com.helger.peppol.businesscard.generic.PDIdentifier;
import com.helger.peppol.businesscard.generic.PDName;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;

/**
 * The test data shared by all the conformance tests of the Peppol Directory search index.
 *
 * @author Philip Helger
 * @since 0.16.0
 */
@Immutable
public final class PDConformanceTestData
{
  /** The owner ID used in all conformance test documents */
  public static final String OWNER_ID = "CN=SMP_TEST,O=Test,C=AT:1234567890";
  /** The part of {@link #OWNER_ID} that a prefix query is executed with */
  public static final String OWNER_ID_PREFIX = "CN=SMP_TEST,O=Test,C=AT";

  private PDConformanceTestData ()
  {}

  /**
   * @return The participant identifier used in all conformance test documents. It is created with
   *         the configured identifier factory, so that an identifier read back from the search
   *         index is equal to it.
   */
  @NonNull
  public static IParticipantIdentifier createParticipantID ()
  {
    return PDMetaManager.getIdentifierFactory ()
                        .createParticipantIdentifier ("iso6523-actorid-upis", "9915:testcompany");
  }

  @NonNull
  public static PDStoredMetaData createMockMetaData ()
  {
    return new PDStoredMetaData (PDTFactory.getCurrentLocalDateTime (), "junittest", "localhost");
  }

  /**
   * Create a single index document that uses every field type of the Peppol Directory - a String
   * field that is tokenized, a String field that is not tokenized, a numeric field and the "all
   * fields" field that is indexed but not stored.
   *
   * @param aParticipantID
   *        The participant ID to use. May not be <code>null</code>.
   * @param sName
   *        The multilingual name of the business entity. May neither be <code>null</code> nor
   *        empty.
   * @param sLanguage
   *        The language of the name. May neither be <code>null</code> nor empty.
   * @return The created document. Never <code>null</code>.
   */
  @NonNull
  public static PDIndexDocument createMockIndexDocument (@NonNull final IParticipantIdentifier aParticipantID,
                                                         @NonNull @Nonempty final String sName,
                                                         @NonNull @Nonempty final String sLanguage)
  {
    final PDIndexDocument ret = new PDIndexDocument ();
    ret.add (PDField.PARTICIPANT_ID.getAsField (aParticipantID));
    ret.add (PDField.DOCTYPE_ID.getAsField (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getAsDocumentTypeIdentifier ()));
    ret.add (PDField.ML_NAME.getAsField (sName));
    ret.add (PDField.ML_LANGUAGE.getAsField (sLanguage));
    ret.add (PDField.COUNTRY_CODE.getAsField ("AT"));
    ret.add (PDField.GEO_INFO.getAsField ("Vienna Austria"));
    ret.add (PDField.IDENTIFIER_SCHEME.getAsField ("vat"));
    ret.add (PDField.IDENTIFIER_VALUE.getAsField ("atu12345678"));
    ret.add (PDField.WEBSITE_URI.getAsField ("https://www.example.org"));
    ret.add (PDField.CONTACT_TYPE.getAsField ("support"));
    ret.add (PDField.CONTACT_NAME.getAsField ("john doe"));
    ret.add (PDField.CONTACT_PHONE.getAsField ("+43 1 234567"));
    ret.add (PDField.CONTACT_EMAIL.getAsField ("support@example.org"));
    ret.add (PDField.ADDITIONAL_INFO.getAsField ("Some additional information"));
    ret.add (PDField.REGISTRATION_DATE.getAsField ("2026-08-17"));
    ret.add (PDIndexField.createString (CPDStorage.FIELD_ALL_FIELDS,
                                        sName + " AT Vienna Austria",
                                        EPDIndexFieldStore.NO,
                                        EPDIndexFieldTokenize.TOKENIZE));
    ret.add (PDField.METADATA_CREATIONDT.getAsField (PDTFactory.getCurrentLocalDateTime ()));
    ret.add (PDField.METADATA_OWNERID.getAsField (OWNER_ID));
    ret.add (PDField.METADATA_REQUESTING_HOST.getAsField ("127.0.0.1"));
    return ret;
  }

  /**
   * Create the two index documents of the participant of {@link #createParticipantID()}.
   *
   * @param aParticipantID
   *        The participant ID to use. May not be <code>null</code>.
   * @return A list with exactly 2 documents. Never <code>null</code>.
   */
  @NonNull
  public static ICommonsList <PDIndexDocument> createMockIndexDocuments (@NonNull final IParticipantIdentifier aParticipantID)
  {
    return new CommonsArrayList <> (createMockIndexDocument (aParticipantID, "Test Company GmbH", "de"),
                                    createMockIndexDocument (aParticipantID, "Test Company Ltd", "en"));
  }

  /**
   * Create a business card with two business entities - the first one with a single name without a
   * language, the second one with three multilingual names.
   *
   * @param aParticipantID
   *        The participant ID to use. May not be <code>null</code>.
   * @return The created business card. Never <code>null</code>.
   */
  @NonNull
  public static PDExtendedBusinessCard createMockBusinessCard (@NonNull final IParticipantIdentifier aParticipantID)
  {
    final PDBusinessCard aBI = new PDBusinessCard ();
    aBI.setParticipantIdentifier (new PDIdentifier (aParticipantID.getScheme (), aParticipantID.getValue ()));
    {
      final PDBusinessEntity aEntity = new PDBusinessEntity ();
      aEntity.setCountryCode ("AT");
      aEntity.setRegistrationDate (PDTFactory.createLocalDate (2015, Month.JULY, 6));
      aEntity.names ().add (new PDName ("Philip's mock Peppol receiver"));
      aEntity.setGeoInfo ("Vienna");

      for (int i = 0; i < 10; ++i)
        aEntity.identifiers ().add (new PDIdentifier ("scheme" + i, "value" + i));
      aEntity.websiteURIs ().add ("https://peppol.org");
      aEntity.contacts ().add (new PDContact ("support", "BC name", "12345", "test@example.org"));

      aEntity.setAdditionalInfo ("This is a mock entry for testing purposes only");
      aBI.businessEntities ().add (aEntity);
    }
    {
      final PDBusinessEntity aEntity = new PDBusinessEntity ();
      aEntity.setCountryCode ("NO");
      aEntity.names ().add (new PDName ("Entity2a", "no"));
      aEntity.names ().add (new PDName ("Entity2b", "de"));
      aEntity.names ().add (new PDName ("Entity2c", "en"));

      aEntity.setAdditionalInfo ("Mock");
      aBI.businessEntities ().add (aEntity);
    }
    return new PDExtendedBusinessCard (aBI,
                                       new CommonsArrayList <> (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30));
  }
}
