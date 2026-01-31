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
package com.helger.pd.publisher.app;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.dao.DAOException;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.sml.SMLInfo;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.io.dao.AbstractPhotonMapBasedWALDAO;

import jakarta.annotation.Nullable;

public final class SMLInfoManager extends AbstractPhotonMapBasedWALDAO <ISMLInfo, SMLInfo> implements ISMLInfoManager
{
  public SMLInfoManager (@NonNull @Nonempty final String sFilename) throws DAOException
  {
    super (SMLInfo.class, sFilename);
  }

  @Override
  @NonNull
  protected EChange onInit ()
  {
    // Add the default transport profiles
    for (final ESML e : ESML.values ())
      internalCreateItem (SMLInfo.builder (e).build ());
    return EChange.CHANGED;
  }

  @NonNull
  public ISMLInfo createSMLInfo (@NonNull @Nonempty final String sDisplayName,
                                 @NonNull @Nonempty final String sDNSZone,
                                 @NonNull @Nonempty final String sManagementServiceURL,
                                 final boolean bClientCertificateRequired)
  {
    final SMLInfo aSMLInfo = SMLInfo.builder ()
                                    .idNewPersistent ()
                                    .displayName (sDisplayName)
                                    .dnsZone (sDNSZone)
                                    .managementServiceURL (sManagementServiceURL)
                                    .clientCertificateRequired (bClientCertificateRequired)
                                    .build ();

    m_aRWLock.writeLocked ( () -> { internalCreateItem (aSMLInfo); });
    AuditHelper.onAuditCreateSuccess (SMLInfo.OT,
                                      aSMLInfo.getID (),
                                      sDisplayName,
                                      sDNSZone,
                                      sManagementServiceURL,
                                      Boolean.valueOf (bClientCertificateRequired));
    return aSMLInfo;
  }

  @NonNull
  public EChange updateSMLInfo (@Nullable final String sSMLInfoID,
                                @NonNull @Nonempty final String sDisplayName,
                                @NonNull @Nonempty final String sDNSZone,
                                @NonNull @Nonempty final String sManagementServiceURL,
                                final boolean bClientCertificateRequired)
  {
    final SMLInfo aSMLInfo = getOfID (sSMLInfoID);
    if (aSMLInfo == null)
    {
      AuditHelper.onAuditModifyFailure (SMLInfo.OT, sSMLInfoID, "no-such-id");
      return EChange.UNCHANGED;
    }

    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMLInfo aNewSMLInfo = SMLInfo.builder (aSMLInfo)
                                         .displayName (sDisplayName)
                                         .dnsZone (sDNSZone)
                                         .managementServiceURL (sManagementServiceURL)
                                         .clientCertificateRequired (bClientCertificateRequired)
                                         .build ();
      if (aSMLInfo.equals (aNewSMLInfo))
        return EChange.UNCHANGED;

      internalUpdateItem (aNewSMLInfo);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditModifySuccess (SMLInfo.OT,
                                      "all",
                                      sSMLInfoID,
                                      sDisplayName,
                                      sDNSZone,
                                      sManagementServiceURL,
                                      Boolean.valueOf (bClientCertificateRequired));
    return EChange.CHANGED;
  }

  @Nullable
  public EChange removeSMLInfo (@Nullable final String sSMLInfoID)
  {
    if (StringHelper.isEmpty (sSMLInfoID))
      return EChange.UNCHANGED;

    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMLInfo aSMLInfo = internalDeleteItem (sSMLInfoID);
      if (aSMLInfo == null)
      {
        AuditHelper.onAuditDeleteFailure (SMLInfo.OT, "no-such-id", sSMLInfoID);
        return EChange.UNCHANGED;
      }
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditDeleteSuccess (SMLInfo.OT, sSMLInfoID);
    return EChange.CHANGED;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMLInfo> getAllSorted ()
  {
    return getAll ().getSortedInline ( (c1, c2) -> {
      int ret = c1.getDNSZone ().length () - c2.getDNSZone ().length ();
      if (ret == 0)
        ret = c1.getDNSZone ().compareTo (c2.getDNSZone ());
      return ret;
    });
  }

  @Nullable
  public ISMLInfo getSMLInfoOfID (@Nullable final String sID)
  {
    return getOfID (sID);
  }

  public boolean containsSMLInfoWithID (@Nullable final String sID)
  {
    return containsWithID (sID);
  }
}
