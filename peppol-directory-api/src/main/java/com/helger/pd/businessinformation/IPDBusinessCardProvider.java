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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.peppol.identifier.participant.IPeppolParticipantIdentifier;

/**
 * Abstract interface to retrieve the {@link PDExtendedBusinessCard}
 * from a provided PEPPOL participant ID. By default the SMP must be queried and
 * the <code>Extension</code> element queried. Nevertheless for testing purposes
 * it may be possible to provide mock data.
 *
 * @author Philip Helger
 */
public interface IPDBusinessCardProvider
{
  /**
   * Get the {@link PDExtendedBusinessCard} for the given participant
   * ID.
   *
   * @param aParticipantID
   *        PEPPOL participant ID. May not be <code>null</code>.
   * @return <code>null</code> if lookup fails
   */
  @Nullable
  PDExtendedBusinessCard getBusinessCard (@Nonnull IPeppolParticipantIdentifier aParticipantID);
}
