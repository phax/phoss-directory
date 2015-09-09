package com.helger.pyp.indexer.domain;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joda.time.LocalDateTime;

import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.MicroElement;
import com.helger.commons.microdom.convert.IMicroTypeConverter;
import com.helger.commons.microdom.convert.MicroTypeConverter;
import com.helger.commons.string.StringParser;

public final class ReIndexWorkItemMicroTypeConverter implements IMicroTypeConverter
{
  private static final String ATTR_ID = "id";
  private static final String ELEMENT_WORK_ITEM = "workitem";
  private static final String ATTR_MAX_RETRY_DT = "maxretrydt";
  private static final String ATTR_RETRY_COUNT = "retries";
  private static final String ATTR_PREVIOUS_RETRY_DT = "prevretrydt";
  private static final String ATTR_NEXT_RETRY_DT = "nextretrydt";

  @Nullable
  public IMicroElement convertToMicroElement (@Nonnull final Object aObject,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final ReIndexWorkItem aValue = (ReIndexWorkItem) aObject;
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.setAttribute (ATTR_ID, aValue.getID ());
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
    final String sID = aElement.getAttributeValue (ATTR_ID);

    final IndexerWorkItem aWorkItem = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_WORK_ITEM),
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

    return new ReIndexWorkItem (sID, aWorkItem, aMaxRetryDT, nRetryCount, aPreviousRetryDT, aNextRetryDT);
  }

}
