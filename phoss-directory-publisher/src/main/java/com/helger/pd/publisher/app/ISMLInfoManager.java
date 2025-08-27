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
package com.helger.pd.publisher.app;

import java.util.function.Predicate;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.state.EChange;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppol.sml.ISMLInfo;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Base interface for a manager that handles {@link ISMLInfo} objects.
 *
 * @author Philip Helger
 */
public interface ISMLInfoManager
{
  /**
   * Create a new SML information.
   *
   * @param sDisplayName
   *        The "shorthand" display name like "SML" or "SMK". May neither be <code>null</code> nor
   *        empty.
   * @param sDNSZone
   *        The DNS zone on which this SML is operating. May not be <code>null</code>. It must be
   *        ensured that the value consists only of lower case characters for comparability!
   *        Example: <code>sml.peppolcentral.org</code>
   * @param sManagementServiceURL
   *        The service URL where the management application is running on incl. the host name. May
   *        not be <code>null</code>. The difference to the host name is the eventually present
   *        context path.
   * @param bClientCertificateRequired
   *        <code>true</code> if this SML requires a client certificate for access,
   *        <code>false</code> otherwise.<br>
   *        Both Peppol production SML and SMK require a client certificate. Only a locally running
   *        SML software may not require a client certificate.
   * @return Never <code>null</code>.
   */
  @Nonnull
  ISMLInfo createSMLInfo (@Nonnull @Nonempty String sDisplayName,
                          @Nonnull @Nonempty String sDNSZone,
                          @Nonnull @Nonempty String sManagementServiceURL,
                          boolean bClientCertificateRequired);

  /**
   * Update an existing SML information.
   *
   * @param sSMLInfoID
   *        The ID of the SML information to be updated. May be <code>null</code>.
   * @param sDisplayName
   *        The "shorthand" display name like "SML" or "SMK". May neither be <code>null</code> nor
   *        empty.
   * @param sDNSZone
   *        The DNS zone on which this SML is operating. May not be <code>null</code>. It must be
   *        ensured that the value consists only of lower case characters for comparability!
   *        Example: <code>sml.peppolcentral.org</code>
   * @param sManagementServiceURL
   *        The service URL where the management application is running on incl. the host name. May
   *        not be <code>null</code>. The difference to the host name is the eventually present
   *        context path.
   * @param bClientCertificateRequired
   *        <code>true</code> if this SML requires a client certificate for access,
   *        <code>false</code> otherwise.<br>
   *        Both Peppol production SML and SMK require a client certificate. Only a locally running
   *        SML software may not require a client certificate.
   * @return {@link EChange#CHANGED} if something was changed.
   */
  @Nonnull
  EChange updateSMLInfo (@Nullable String sSMLInfoID,
                         @Nonnull @Nonempty String sDisplayName,
                         @Nonnull @Nonempty String sDNSZone,
                         @Nonnull @Nonempty String sManagementServiceURL,
                         boolean bClientCertificateRequired);

  /**
   * Delete an existing SML information.
   *
   * @param sSMLInfoID
   *        The ID of the SML information to be deleted. May be <code>null</code>.
   * @return {@link EChange#CHANGED} if the removal was successful.
   */
  @Nullable
  EChange removeSMLInfo (@Nullable String sSMLInfoID);

  /**
   * @return An unsorted collection of all contained SML information. Never <code>null</code> but
   *         maybe empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <ISMLInfo> getAll ();

  /**
   * Get the SML information with the passed ID.
   *
   * @param sID
   *        The ID to be resolved. May be <code>null</code>.
   * @return <code>null</code> if no such SML information exists.
   */
  @Nullable
  ISMLInfo getSMLInfoOfID (@Nullable String sID);

  /**
   * Find the first SML information that matches the provided predicate.
   *
   * @param aFilter
   *        The predicate to be applied for searching. May not be <code>null</code>.
   * @return <code>null</code> if no such SML information exists.
   */
  @Nullable
  ISMLInfo findFirst (@Nullable Predicate <? super ISMLInfo> aFilter);

  /**
   * Check if a SML information with the passed ID is contained.
   *
   * @param sID
   *        The ID of the SML information to be checked. May be <code>null</code>.
   * @return <code>true</code> if the ID is contained, <code>false</code> otherwise.
   */
  boolean containsSMLInfoWithID (@Nullable String sID);
}
