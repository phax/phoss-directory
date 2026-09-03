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

import java.io.OutputStream;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.WillNotClose;
import com.helger.base.string.StringImplode;
import com.helger.collection.commons.ICommonsList;
import com.helger.csv.CSVWriter;
import com.helger.mime.CMimeType;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.pd.indexer.storage.PDStoredContact;
import com.helger.pd.indexer.storage.PDStoredIdentifier;
import com.helger.pd.indexer.storage.PDStoredMLName;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * Export all Business Cards as CSV.
 *
 * @author Philip Helger
 */
public class ExportAllHandlerBusinessCardCSV extends AbstractExportAllHandler
{
  private CSVWriter m_aCSVWriter;

  public ExportAllHandlerBusinessCardCSV (@NonNull @Nonempty final String sDisplayName,
                                          @NonNull @Nonempty final String sS3Filename)
  {
    super (sDisplayName, sS3Filename, CMimeType.TEXT_CSV);
  }

  public boolean isBusinessEntityDataNeeded ()
  {
    return true;
  }

  public void onStart (@NonNull @WillNotClose final OutputStream aOS, @Nonnegative final int nParticipantCount)
                                                                                                                throws Exception
  {
    m_aCSVWriter = ExportHelper.createCSVWriter (aOS);
    m_aCSVWriter.writeNext ("Participant ID",
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
  }

  public void onParticipant (@NonNull @Nonempty final String sParticipantID,
                             @NonNull final IParticipantIdentifier aParticipantID,
                             @NonNull final ICommonsList <PDStoredBusinessEntity> aEntities) throws Exception
  {
    // If the participant was deleted in the meantime, no row is written
    for (final PDStoredBusinessEntity aEntity : aEntities)
    {
      if (!aEntity.hasParticipantID ())
        continue;

      m_aCSVWriter.writeNext (aEntity.getParticipantID ().getURIEncoded (),
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
    }
  }

  public void onEnd () throws Exception
  {
    m_aCSVWriter.flush ();
    m_aCSVWriter.close ();
  }
}
