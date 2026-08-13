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
package com.helger.pd.indexer.storage.field;

import java.time.LocalDateTime;

import com.helger.datetime.helper.PDTFactory;
import com.helger.pd.indexer.searchindex.EPDIndexFieldStore;
import com.helger.pd.indexer.searchindex.EPDIndexFieldTokenize;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * This class contains all the PD index fields
 *
 * @author Philip Helger
 */
public final class PDField
{
  public static final PDStringField <IParticipantIdentifier> PARTICIPANT_ID = PDStringField.createParticipantIdentifier ("participantid",
                                                                                                                         EPDIndexFieldStore.YES,
                                                                                                                         EPDIndexFieldTokenize.NO_TOKENIZE);
  public static final PDStringField <IDocumentTypeIdentifier> DOCTYPE_ID = PDStringField.createDocumentTypeIdentifier ("doctypeid",
                                                                                                                       EPDIndexFieldStore.YES,
                                                                                                                       EPDIndexFieldTokenize.NO_TOKENIZE);
  public static final PDStringField <String> REGISTRATION_DATE = PDStringField.createString ("registrationdate",
                                                                                             EPDIndexFieldStore.YES,
                                                                                             EPDIndexFieldTokenize.NO_TOKENIZE);
  public static final PDStringField <String> NAME = PDStringField.createString ("name",
                                                                                EPDIndexFieldStore.YES,
                                                                                EPDIndexFieldTokenize.TOKENIZE);
  public static final PDStringField <String> ML_NAME = PDStringField.createString ("ml-name",
                                                                                   EPDIndexFieldStore.YES,
                                                                                   EPDIndexFieldTokenize.TOKENIZE);
  public static final PDStringField <String> ML_LANGUAGE = PDStringField.createString ("ml-language",
                                                                                       EPDIndexFieldStore.YES,
                                                                                       EPDIndexFieldTokenize.NO_TOKENIZE);
  public static final PDStringField <String> COUNTRY_CODE = PDStringField.createString ("country",
                                                                                        EPDIndexFieldStore.YES,
                                                                                        EPDIndexFieldTokenize.NO_TOKENIZE);
  public static final PDStringField <String> GEO_INFO = PDStringField.createString ("geoinfo",
                                                                                    EPDIndexFieldStore.YES,
                                                                                    EPDIndexFieldTokenize.TOKENIZE);
  // Legacy name
  public static final PDStringField <String> IDENTIFIER_SCHEME = PDStringField.createString ("identifiertype",
                                                                                             EPDIndexFieldStore.YES,
                                                                                             EPDIndexFieldTokenize.TOKENIZE);
  // Legacy name
  public static final PDStringField <String> IDENTIFIER_VALUE = PDStringField.createString ("identifier",
                                                                                            EPDIndexFieldStore.YES,
                                                                                            EPDIndexFieldTokenize.TOKENIZE);
  public static final PDStringField <String> WEBSITE_URI = PDStringField.createString ("website",
                                                                                       EPDIndexFieldStore.YES,
                                                                                       EPDIndexFieldTokenize.TOKENIZE);
  public static final PDStringField <String> CONTACT_TYPE = PDStringField.createString ("bc-description",
                                                                                        EPDIndexFieldStore.YES,
                                                                                        EPDIndexFieldTokenize.TOKENIZE);
  public static final PDStringField <String> CONTACT_NAME = PDStringField.createString ("bc-name",
                                                                                        EPDIndexFieldStore.YES,
                                                                                        EPDIndexFieldTokenize.TOKENIZE);
  public static final PDStringField <String> CONTACT_PHONE = PDStringField.createString ("bc-phone",
                                                                                         EPDIndexFieldStore.YES,
                                                                                         EPDIndexFieldTokenize.TOKENIZE);
  public static final PDStringField <String> CONTACT_EMAIL = PDStringField.createString ("bc-email",
                                                                                         EPDIndexFieldStore.YES,
                                                                                         EPDIndexFieldTokenize.TOKENIZE);
  // Legacy name
  public static final PDStringField <String> ADDITIONAL_INFO = PDStringField.createString ("freetext",
                                                                                           EPDIndexFieldStore.YES,
                                                                                           EPDIndexFieldTokenize.TOKENIZE);

  public static final PDNumericField <LocalDateTime> METADATA_CREATIONDT = new PDNumericField <> ("md-creationdt",
                                                                                                  x -> Long.valueOf (PDTFactory.getMillis (x)),
                                                                                                  x -> PDTFactory.createLocalDateTime (x.longValue ()),
                                                                                                  EPDIndexFieldStore.YES);
  public static final PDStringField <String> METADATA_OWNERID = PDStringField.createString ("md-ownerid",
                                                                                            EPDIndexFieldStore.YES,
                                                                                            EPDIndexFieldTokenize.NO_TOKENIZE);
  public static final PDStringField <String> METADATA_REQUESTING_HOST = PDStringField.createString ("md-requestinghost",
                                                                                                    EPDIndexFieldStore.YES,
                                                                                                    EPDIndexFieldTokenize.NO_TOKENIZE);

  private PDField ()
  {}
}
