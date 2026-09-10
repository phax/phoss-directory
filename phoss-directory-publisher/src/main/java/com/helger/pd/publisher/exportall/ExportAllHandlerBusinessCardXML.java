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

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.WillNotClose;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.web.PDTWebDateHelper;
import com.helger.mime.CMimeType;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.xml.serialize.write.XMLWriterSettings;

/**
 * Export all Business Cards as XML.
 *
 * @author Philip Helger
 */
public class ExportAllHandlerBusinessCardXML extends AbstractExportAllHandler
{
  private final boolean m_bIncludeDocTypes;
  private XMLStreamWriter m_aXmlWriter;

  public ExportAllHandlerBusinessCardXML (@NonNull @Nonempty final String sDisplayName,
                                          @NonNull @Nonempty final String sS3Filename,
                                          final boolean bIncludeDocTypes)
  {
    super (sDisplayName, sS3Filename, CMimeType.APPLICATION_XML);
    m_bIncludeDocTypes = bIncludeDocTypes;
  }

  public boolean isBusinessEntityDataNeeded ()
  {
    return true;
  }

  public void onStart (@NonNull @WillNotClose final OutputStream aOS,
                       @Nonnegative final int nParticipantCount) throws Exception
  {
    m_aXmlWriter = XMLOutputFactory.newInstance ().createXMLStreamWriter (aOS);
    m_aXmlWriter.setDefaultNamespace (ExportHelper.XML_EXPORT_NS_URI_V3);

    // XML root
    m_aXmlWriter.writeStartDocument (XMLWriterSettings.DEFAULT_XML_CHARSET, "1.0");

    m_aXmlWriter.writeStartElement (ExportHelper.XML_EXPORT_NS_URI_V3, "root");
    m_aXmlWriter.writeAttribute ("xmlns", ExportHelper.XML_EXPORT_NS_URI_V3);
    m_aXmlWriter.writeAttribute ("version", "3");
    m_aXmlWriter.writeAttribute ("creationdt",
                                 PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentZonedDateTimeUTC ()));
    m_aXmlWriter.writeAttribute ("codeListSupported", EPredefinedDocumentTypeIdentifier.CODE_LIST_VERSION);
  }

  public void onParticipant (@NonNull @Nonempty final String sParticipantID,
                             @NonNull final IParticipantIdentifier aParticipantID,
                             @NonNull final ICommonsList <PDStoredBusinessEntity> aEntities) throws Exception
  {
    // If the participant was deleted in the meantime, it is not exported
    if (aEntities.isNotEmpty ())
      ExportHelper.exportSingleBusinessCard (aParticipantID, aEntities, m_bIncludeDocTypes, m_aXmlWriter);
  }

  public void onEnd () throws Exception
  {
    // root
    m_aXmlWriter.writeEndElement ();
    m_aXmlWriter.writeEndDocument ();
    m_aXmlWriter.flush ();
  }
}
