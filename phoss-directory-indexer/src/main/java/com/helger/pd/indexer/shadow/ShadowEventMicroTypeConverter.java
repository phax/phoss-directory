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
package com.helger.pd.indexer.shadow;

import java.time.LocalDateTime;

import com.helger.pd.indexer.index.EIndexerWorkItemType;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.IMicroTypeConverter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * {@link IMicroTypeConverter} implementation for {@link ShadowEvent}
 *
 * @author Mikael Aksamit
 */
public final class ShadowEventMicroTypeConverter implements IMicroTypeConverter <ShadowEvent>
{
  private static final String ATTR_EVENT_ID = "eventid";
  private static final String ATTR_CREATED_AT = "createdat";
  private static final String ATTR_OPERATION = "operation";
  private static final String ATTR_PARTICIPANT_ID = "participantid";
  private static final String ATTR_REQUESTING_HOST = "requestinghost";
  private static final String ATTR_CERT_SHA256 = "certsha256";
  private static final String ATTR_CERT_SUBJECT_DN = "certsubjectdn";
  private static final String ATTR_CERT_ISSUER_DN = "certissuerdn";

  public IMicroElement convertToMicroElement (@Nonnull final ShadowEvent aValue,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.setAttribute (ATTR_EVENT_ID, aValue.getEventID ());
    aElement.setAttributeWithConversion (ATTR_CREATED_AT, aValue.getCreatedAt ());
    aElement.setAttribute (ATTR_OPERATION, aValue.getOperation ().getID ());
    aElement.setAttribute (ATTR_PARTICIPANT_ID, aValue.getParticipantID ());
    aElement.setAttribute (ATTR_REQUESTING_HOST, aValue.getRequestingHost ());
    aElement.setAttribute (ATTR_CERT_SHA256, aValue.getCertSHA256Fingerprint ());
    aElement.setAttribute (ATTR_CERT_SUBJECT_DN, aValue.getCertSubjectDN ());
    aElement.setAttribute (ATTR_CERT_ISSUER_DN, aValue.getCertIssuerDN ());
    return aElement;
  }

  public ShadowEvent convertToNative (@Nonnull final IMicroElement aElement)
  {
    final String sEventID = aElement.getAttributeValue (ATTR_EVENT_ID);
    final LocalDateTime aCreatedAt = aElement.getAttributeValueWithConversion (ATTR_CREATED_AT, LocalDateTime.class);
    final String sOperationID = aElement.getAttributeValue (ATTR_OPERATION);
    final EIndexerWorkItemType eOperation = EIndexerWorkItemType.getFromIDOrNull (sOperationID);
    if (eOperation == null)
      throw new IllegalStateException ("Failed to parse operation type ID '" + sOperationID + "'");

    final String sParticipantID = aElement.getAttributeValue (ATTR_PARTICIPANT_ID);
    final String sRequestingHost = aElement.getAttributeValue (ATTR_REQUESTING_HOST);
    final String sCertSHA256 = aElement.getAttributeValue (ATTR_CERT_SHA256);
    final String sCertSubjectDN = aElement.getAttributeValue (ATTR_CERT_SUBJECT_DN);
    final String sCertIssuerDN = aElement.getAttributeValue (ATTR_CERT_ISSUER_DN);

    return new ShadowEvent (sEventID,
                            aCreatedAt,
                            eOperation,
                            sParticipantID,
                            sRequestingHost,
                            sCertSHA256,
                            sCertSubjectDN,
                            sCertIssuerDN);
  }
}
