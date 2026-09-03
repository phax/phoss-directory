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
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.WillNotClose;
import com.helger.base.io.stream.NonClosingOutputStream;
import com.helger.base.io.stream.StreamHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.web.PDTWebDateHelper;
import com.helger.mime.CMimeType;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.peppolid.IParticipantIdentifier;

import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;

/**
 * Export all participant IDs as JSON.
 *
 * @author Philip Helger
 */
public class ExportAllHandlerParticipantJSON extends AbstractExportAllHandler
{
  private Writer m_aWriter;
  private JsonGenerator m_aJsonGen;

  public ExportAllHandlerParticipantJSON (@NonNull @Nonempty final String sDisplayName,
                                          @NonNull @Nonempty final String sS3Filename)
  {
    super (sDisplayName, sS3Filename, CMimeType.APPLICATION_JSON);
  }

  public boolean isBusinessEntityDataNeeded ()
  {
    return false;
  }

  public void onStart (@NonNull @WillNotClose final OutputStream aOS, @Nonnegative final int nParticipantCount)
                                                                                                                throws Exception
  {
    m_aWriter = StreamHelper.createWriter (new NonClosingOutputStream (aOS), StandardCharsets.UTF_8);
    m_aJsonGen = Json.createGenerator (m_aWriter);

    // JSON root
    m_aJsonGen.writeStartObject ()
              .write ("version", 1)
              .write ("creationdt", PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentZonedDateTimeUTC ()))
              .write ("count", nParticipantCount)
              .writeStartArray ("participants");
  }

  public void onParticipant (@NonNull @Nonempty final String sParticipantID,
                             @NonNull final IParticipantIdentifier aParticipantID,
                             @NonNull final ICommonsList <PDStoredBusinessEntity> aEntities) throws Exception
  {
    m_aJsonGen.write (sParticipantID);
  }

  public void onEnd () throws Exception
  {
    m_aJsonGen.writeEnd ().writeEnd ();
    m_aJsonGen.close ();
    m_aWriter.close ();
  }
}
