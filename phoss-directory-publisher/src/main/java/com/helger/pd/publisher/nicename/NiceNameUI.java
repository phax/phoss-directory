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
package com.helger.pd.publisher.nicename;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.string.StringHelper;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.AbstractHCElementWithChildren;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.html.textlevel.HCSmall;
import com.helger.html.hc.html.textlevel.HCSpan;
import com.helger.html.hc.html.textlevel.HCWBR;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.photon.bootstrap4.badge.BootstrapBadge;
import com.helger.photon.bootstrap4.badge.EBootstrapBadgeType;

/**
 * Common UI for nice names
 *
 * @author Philip Helger
 */
@Immutable
public final class NiceNameUI
{
  private NiceNameUI ()
  {}

  @Nonnull
  private static IHCNode _getWBRList (@Nonnull final String s)
  {
    final HCNodeList ret = new HCNodeList ();
    String sRest = s;
    final int nChars = 10;
    while (sRest.length () > nChars)
    {
      ret.addChild (sRest.substring (0, nChars)).addChild (new HCWBR ());
      sRest = sRest.substring (nChars);
    }
    if (sRest.length () > 0)
      ret.addChild (sRest);
    return ret;
  }

  @Nonnull
  private static IHCNode _createFormattedID (@Nonnull final String sID,
                                             @Nullable final String sName,
                                             @Nullable final EBootstrapBadgeType eNameBadgeType,
                                             final boolean bIsDeprecated,
                                             @Nullable final String sSpecialLabel,
                                             @Nullable final EBootstrapBadgeType eSpecialLabelBadgeType,
                                             final boolean bInDetails)
  {
    if (sName == null)
    {
      // No nice name present
      final AbstractHCElementWithChildren <?> ret = bInDetails ? new HCCode () : new HCSpan ();
      ret.addChild (sID);
      if (bInDetails)
        ret.addChild (" ").addChild (new BootstrapBadge (EBootstrapBadgeType.WARNING).addChild ("Unknown ID"));
      return ret;
    }

    final HCNodeList ret = new HCNodeList ();
    final BootstrapBadge aNameBadge = ret.addAndReturnChild (new BootstrapBadge (eNameBadgeType).addChild (sName));
    if (bIsDeprecated)
    {
      ret.addChild (" ")
         .addChild (new BootstrapBadge (EBootstrapBadgeType.WARNING).addChild ("Identifier is deprecated"));
    }
    if (StringHelper.hasText (sSpecialLabel))
    {
      ret.addChild (" ").addChild (new BootstrapBadge (eSpecialLabelBadgeType).addChild (sSpecialLabel));
    }
    if (bInDetails)
    {
      // Print ID in smaller font
      ret.addChild (new HCSmall ().addChild (" (").addChild (new HCCode ().addChild (sID)).addChild (")"));
    }
    else
    {
      // Add ID as mouse over
      aNameBadge.setTitle (sID);
    }
    return ret;
  }

  @Nonnull
  private static IHCNode _createID (@Nonnull final String sID,
                                    @Nullable final NiceNameEntry aNiceName,
                                    final boolean bInDetails)
  {
    if (aNiceName == null)
      return _createFormattedID (sID, null, null, false, null, null, bInDetails);
    return _createFormattedID (sID,
                               aNiceName.getName (),
                               EBootstrapBadgeType.SUCCESS,
                               aNiceName.isDeprecated (),
                               aNiceName.getSpecialLabel (),
                               EBootstrapBadgeType.INFO,
                               bInDetails);
  }

  @Nonnull
  public static IHCNode getDocumentTypeID (@Nonnull final IDocumentTypeIdentifier aDocTypeID, final boolean bInDetails)
  {
    final String sURI = aDocTypeID.getURIEncoded ();
    return _createID (sURI, NiceNameHandler.getDocTypeNiceName (sURI), bInDetails);
  }

  @Nonnull
  public static IHCNode getProcessID (@Nonnull final IDocumentTypeIdentifier aDocTypeID,
                                      @Nonnull final IProcessIdentifier aProcessID,
                                      final boolean bInDetails)
  {
    final String sURI = aProcessID.getURIEncoded ();

    // Check direct match first
    NiceNameEntry aNN = NiceNameHandler.getProcessNiceName (sURI);
    if (aNN != null)
      return _createID (sURI, aNN, bInDetails);

    aNN = NiceNameHandler.getDocTypeNiceName (aDocTypeID.getURIEncoded ());
    if (aNN != null)
    {
      if (aNN.containsProcessID (aProcessID))
        return _createFormattedID (sURI,
                                   "Matching Process Identifier",
                                   EBootstrapBadgeType.SUCCESS,
                                   false,
                                   null,
                                   null,
                                   bInDetails);
      return _createFormattedID (sURI,
                                 "Unexpected Process Identifier",
                                 EBootstrapBadgeType.WARNING,
                                 false,
                                 null,
                                 null,
                                 bInDetails);
    }
    return _createFormattedID (sURI, null, null, false, null, null, bInDetails);
  }
}
