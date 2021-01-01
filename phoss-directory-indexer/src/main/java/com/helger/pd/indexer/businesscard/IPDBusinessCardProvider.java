/**
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.businesscard;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.peppolid.IParticipantIdentifier;

/**
 * Abstract interface to retrieve the {@link PDExtendedBusinessCard} from a
 * provided Peppol participant ID. By default an SMP <code>/businesscard</code>
 * API is queried. Nevertheless for testing purposes it may be possible to
 * provide mock data.
 *
 * @author Philip Helger
 */
@FunctionalInterface
public interface IPDBusinessCardProvider
{
  /**
   * Get the {@link PDExtendedBusinessCard} for the given participant ID.
   *
   * @param aParticipantID
   *        Peppol participant ID. May not be <code>null</code>.
   * @return <code>null</code> if no business card exists for the provided
   *         participant ID.
   */
  @Nullable
  PDExtendedBusinessCard getBusinessCard (@Nonnull IParticipantIdentifier aParticipantID);
}
