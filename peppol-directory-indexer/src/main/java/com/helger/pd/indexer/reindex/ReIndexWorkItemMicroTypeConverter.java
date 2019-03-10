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
package com.helger.pd.indexer.reindex;

import java.time.LocalDateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.string.StringParser;
import com.helger.pd.indexer.index.IIndexerWorkItem;
import com.helger.pd.indexer.index.IndexerWorkItem;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.IMicroTypeConverter;
import com.helger.xml.microdom.convert.MicroTypeConverter;

/**
 * Micro type converter for {@link ReIndexWorkItem}
 * 
 * @author Philip Helger
 */
public final class ReIndexWorkItemMicroTypeConverter implements IMicroTypeConverter <ReIndexWorkItem>
{
  private static final String ELEMENT_WORK_ITEM = "workitem";
  private static final String ATTR_MAX_RETRY_DT = "maxretrydt";
  private static final String ATTR_RETRY_COUNT = "retries";
  private static final String ATTR_PREVIOUS_RETRY_DT = "prevretrydt";
  private static final String ATTR_NEXT_RETRY_DT = "nextretrydt";

  @Nullable
  public IMicroElement convertToMicroElement (@Nonnull final ReIndexWorkItem aValue,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getWorkItem (),
                                                                    sNamespaceURI,
                                                                    ELEMENT_WORK_ITEM));
    aElement.setAttributeWithConversion (ATTR_MAX_RETRY_DT, aValue.getMaxRetryDT ());
    aElement.setAttribute (ATTR_RETRY_COUNT, aValue.getRetryCount ());
    aElement.setAttributeWithConversion (ATTR_PREVIOUS_RETRY_DT, aValue.getPreviousRetryDT ());
    aElement.setAttributeWithConversion (ATTR_NEXT_RETRY_DT, aValue.getNextRetryDT ());
    return aElement;
  }

  @Nullable
  public ReIndexWorkItem convertToNative (@Nonnull final IMicroElement aElement)
  {
    final IIndexerWorkItem aWorkItem = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_WORK_ITEM),
                                                                           IndexerWorkItem.class);

    final LocalDateTime aMaxRetryDT = aElement.getAttributeValueWithConversion (ATTR_MAX_RETRY_DT, LocalDateTime.class);

    final String sRetryCount = aElement.getAttributeValue (ATTR_RETRY_COUNT);
    final int nRetryCount = StringParser.parseInt (sRetryCount, -1);
    if (nRetryCount < 0)
      throw new IllegalStateException ("Invalid retry count '" + sRetryCount + "'");

    final LocalDateTime aPreviousRetryDT = aElement.getAttributeValueWithConversion (ATTR_PREVIOUS_RETRY_DT,
                                                                                     LocalDateTime.class);

    final LocalDateTime aNextRetryDT = aElement.getAttributeValueWithConversion (ATTR_NEXT_RETRY_DT,
                                                                                 LocalDateTime.class);

    return new ReIndexWorkItem (aWorkItem, aMaxRetryDT, nRetryCount, aPreviousRetryDT, aNextRetryDT);
  }

}
