/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.storage;

import javax.annotation.concurrent.Immutable;

/**
 * Constants Lucene field names
 *
 * @author Philip Helger
 */
@Immutable
public final class CPDStorage
{
  public static final String FIELD_PARTICIPANTID = "participantid";
  public static final String FIELD_DOCUMENT_TYPE_ID = "doctypeid";
  public static final String FIELD_COUNTRY_CODE = "country";
  public static final String FIELD_REGISTRATION_DATE = "registrationdate";
  public static final String FIELD_NAME = "name";
  public static final String FIELD_GEOGRAPHICAL_INFORMATION = "geoinfo";
  public static final String FIELD_IDENTIFIER_SCHEME = "identifiertype";
  public static final String FIELD_IDENTIFIER = "identifier";
  public static final String FIELD_WEBSITEURI = "website";
  public static final String FIELD_CONTACT_TYPE = "bc-description";
  public static final String FIELD_CONTACT_NAME = "bc-name";
  public static final String FIELD_CONTACT_PHONE = "bc-phone";
  public static final String FIELD_CONTACT_EMAIL = "bc-email";
  public static final String FIELD_ADDITIONAL_INFORMATION = "freetext";
  public static final String FIELD_METADATA_CREATIONDT = "md-creationdt";
  public static final String FIELD_METADATA_OWNERID = "md-ownerid";
  public static final String FIELD_METADATA_REQUESTING_HOST = "md-requestinghost";
  public static final String FIELD_ALL_FIELDS = "allfields";
  public static final String FIELD_DELETED = "deleted";
  public static final String FIELD_GROUP_END = "groupend";

  private CPDStorage ()
  {}
}
