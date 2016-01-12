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
package com.helger.pd.businessinformation;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.pd.businesscard.PDBusinessCardType;
import com.helger.peppol.identifier.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.doctype.SimpleDocumentTypeIdentifier;

/**
 * This class encapsulates all the data to be added to the Lucene index. It
 * consists of the main {@link PDBusinessCardType} object as retrieved from the
 * SMP plus a list of all document types supported by the respective service
 * group.
 *
 * @author Philip Helger
 */
@Immutable
public class PDExtendedBusinessInformation
{
  private final PDBusinessCardType m_aBusinessInfo;
  private final List <IDocumentTypeIdentifier> m_aDocumentTypeIDs = new ArrayList <> ();

  public PDExtendedBusinessInformation (@Nonnull final PDBusinessCardType aBusinessInfo,
                                        @Nullable final List <IDocumentTypeIdentifier> aDocumentTypeIDs)
  {
    m_aBusinessInfo = ValueEnforcer.notNull (aBusinessInfo, "BusinessInfo");
    if (aDocumentTypeIDs != null)
      for (final IDocumentTypeIdentifier aDocTypeID : aDocumentTypeIDs)
        if (aDocTypeID != null)
          m_aDocumentTypeIDs.add (new SimpleDocumentTypeIdentifier (aDocTypeID));
  }

  /**
   * @return The mutable {@link PDBusinessCardType} object as provided in the
   *         constructor. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableObject ("design")
  public PDBusinessCardType getBusinessInformation ()
  {
    return m_aBusinessInfo;
  }

  /**
   * @return A copy of the list of all contained document type IDs. Never
   *         <code>null</code> but maybe empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  public List <IDocumentTypeIdentifier> getAllDocumentTypeIDs ()
  {
    return CollectionHelper.newList (m_aDocumentTypeIDs);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("BusinessInfo", m_aBusinessInfo)
                                       .append ("DocTypeIDs", m_aDocumentTypeIDs)
                                       .toString ();
  }
}
