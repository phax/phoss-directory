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
package com.helger.pd.publisher.ui;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTToString;
import com.helger.commons.locale.LocaleHelper;
import com.helger.commons.locale.country.CountryCache;
import com.helger.commons.locale.language.LanguageCache;
import com.helger.commons.math.MathHelper;
import com.helger.datetime.util.PDTDisplayHelper;
import com.helger.html.css.DefaultCSSClassProvider;
import com.helger.html.css.ICSSClassProvider;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.ext.HCExtHelper;
import com.helger.html.hc.html.HC_Target;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCOL;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.tabular.HCCol;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.html.textlevel.HCStrong;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.pd.indexer.storage.PDStoredContact;
import com.helger.pd.indexer.storage.PDStoredIdentifier;
import com.helger.pd.indexer.storage.PDStoredMLName;
import com.helger.peppolid.peppol.doctype.IPeppolDocumentTypeIdentifierParts;
import com.helger.photon.app.html.PhotonCSS;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.form.BootstrapViewForm;
import com.helger.photon.bootstrap4.table.BootstrapTable;
import com.helger.photon.uictrls.EUICtrlsCSSPathProvider;
import com.helger.photon.uictrls.famfam.EFamFamFlagIcon;

/**
 * Common UI ctrls for business information display
 *
 * @author Philip Helger
 */
@Immutable
public final class PDCommonUI
{
  public static final ICSSClassProvider CSS_CLASS_VERTICAL_PADDED_TEXT = DefaultCSSClassProvider.create ("vertical-padded-text");

  private PDCommonUI ()
  {}

  @Nullable
  public static IHCNode getFlagNode (@Nullable final String sCountryCode)
  {
    final EFamFamFlagIcon eFlagIcon = EFamFamFlagIcon.getFromIDOrNull (sCountryCode);
    if (eFlagIcon == null)
      return null;
    PhotonCSS.registerCSSIncludeForThisRequest (EUICtrlsCSSPathProvider.FAMFAM_FLAGS);
    return eFlagIcon.getAsNode ();
  }

  @Nonnull
  public static IHCNode getMLNameNode (@Nonnull final PDStoredMLName aName, @Nonnull final Locale aDisplayLocale)
  {
    String sName = aName.getName ();
    if (aName.hasLanguageCode ())
    {
      final Locale aLanguage = LanguageCache.getInstance ().getLanguage (aName.getLanguageCode ());
      if (aLanguage != null)
      {
        // Show language in current display locale
        sName += " (" + aLanguage.getDisplayLanguage (aDisplayLocale) + ")";
      }
    }
    return new HCTextNode (sName);
  }

  @Nonnull
  public static ICommonsList <PDStoredMLName> getUIFilteredNames (@Nonnull final ICommonsList <PDStoredMLName> aNames,
                                                                  @Nonnull final Locale aDisplayLocale)
  {
    final String sDisplayLanguage = LocaleHelper.getValidLanguageCode (aDisplayLocale.getLanguage ());
    ICommonsList <PDStoredMLName> ret = CommonsArrayList.createFiltered (aNames, x -> x.hasLanguageCode (sDisplayLanguage));
    if (ret.isEmpty ())
    {
      // Filter matched no entry - take all names
      ret = aNames.getClone ();
    }
    return ret;
  }

  @Nonnull
  public static BootstrapViewForm showBusinessInfoDetails (@Nonnull final PDStoredBusinessEntity aStoredDoc,
                                                           @Nonnull final Locale aDisplayLocale)
  {
    final BootstrapViewForm aViewForm = new BootstrapViewForm ();
    if (aStoredDoc.hasCountryCode ())
    {
      final HCNodeList aCountryCtrl = new HCNodeList ();
      final String sCountryCode = aStoredDoc.getCountryCode ();
      aCountryCtrl.addChild (getFlagNode (sCountryCode));

      final Locale aCountry = CountryCache.getInstance ().getCountry (sCountryCode);
      if (aCountry != null)
        aCountryCtrl.addChild (" " + aCountry.getDisplayCountry (aDisplayLocale) + " [" + sCountryCode + "]");
      else
        aCountryCtrl.addChild (" " + sCountryCode);

      aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Country").setCtrl (aCountryCtrl));
    }

    if (aStoredDoc.names ().isNotEmpty ())
    {
      final ICommonsList <PDStoredMLName> aNames = getUIFilteredNames (aStoredDoc.names (), aDisplayLocale);

      IHCNode aNameCtrl;
      if (aNames.size () == 1)
        aNameCtrl = getMLNameNode (aNames.getFirst (), aDisplayLocale);
      else
      {
        final HCUL aNameUL = new HCUL ();
        aNames.forEach (x -> aNameUL.addItem (getMLNameNode (x, aDisplayLocale)));
        aNameCtrl = aNameUL;
      }

      aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Entity Name").setCtrl (aNameCtrl));
    }

    if (aStoredDoc.hasGeoInfo ())
      aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Geographical information")
                                                       .setCtrl (HCExtHelper.nl2divList (aStoredDoc.getGeoInfo ())));

    if (aStoredDoc.identifiers ().isNotEmpty ())
    {
      final BootstrapTable aIDTable = new BootstrapTable (HCCol.star (), HCCol.star ()).setStriped (true)
                                                                                       .setBordered (true)
                                                                                       .setCondensed (true);
      aIDTable.addHeaderRow ().addCells ("Scheme", "Value");
      for (final PDStoredIdentifier aStoredID : aStoredDoc.identifiers ())
        aIDTable.addBodyRow ().addCells (aStoredID.getScheme (), aStoredID.getValue ());
      aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Additional identifiers").setCtrl (aIDTable));
    }

    if (aStoredDoc.websiteURIs ().isNotEmpty ())
    {
      final HCOL aOL = new HCOL ();
      for (final String sWebsiteURI : aStoredDoc.websiteURIs ())
        aOL.addItem (HCA.createLinkedWebsite (sWebsiteURI, HC_Target.BLANK));
      aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Website URIs").setCtrl (aOL));
    }

    if (aStoredDoc.contacts ().isNotEmpty ())
    {
      final BootstrapTable aContactTable = new BootstrapTable (HCCol.star (),
                                                               HCCol.star (),
                                                               HCCol.star (),
                                                               HCCol.star ()).setStriped (true).setBordered (true);
      aContactTable.addHeaderRow ().addCells ("Type", "Name", "Phone Number", "Email");
      for (final PDStoredContact aStoredContact : aStoredDoc.contacts ())
        aContactTable.addBodyRow ()
                     .addCells (aStoredContact.getType (),
                                aStoredContact.getName (),
                                aStoredContact.getPhone (),
                                aStoredContact.getEmail ());
      aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Contacts").setCtrl (aContactTable));
    }
    if (aStoredDoc.hasAdditionalInformation ())
      aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Additional information")
                                                       .setCtrl (HCExtHelper.nl2divList (aStoredDoc.getAdditionalInformation ())));
    if (aStoredDoc.hasRegistrationDate ())
      aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Registration date")
                                                       .setCtrl (PDTToString.getAsString (aStoredDoc.getRegistrationDate (),
                                                                                          aDisplayLocale)));
    return aViewForm;
  }

  @Nonnull
  public static HCUL getDocumentTypeIDDetails (@Nonnull final IPeppolDocumentTypeIdentifierParts aParts)
  {
    final HCUL aUL = new HCUL ();
    aUL.addItem ().addChild ("Root namespace: ").addChild (new HCCode ().addChild (aParts.getRootNS ()));
    aUL.addItem ().addChild ("Local name: ").addChild (new HCCode ().addChild (aParts.getLocalName ()));
    aUL.addItem ().addChild ("Customization ID: ").addChild (new HCCode ().addChild (aParts.getCustomizationID ()));
    aUL.addItem ().addChild ("Version: ").addChild (new HCCode ().addChild (aParts.getVersion ()));
    return aUL;
  }

  // Based on PeriodFuncTest code
  @Nonnull
  @Nonempty
  private static String _getPeriodString (final int nYears,
                                          final int nMonths,
                                          final int nDays,
                                          final long nHours,
                                          final long nMinutes,
                                          final long nSeconds)
  {
    // Use "abs" to ensure it is "1 year" and "-1 year"
    final String sYear = MathHelper.abs (nYears) == 1 ? nYears + " year" : nYears + " years";
    final String sMonth = MathHelper.abs (nMonths) == 1 ? nMonths + " month" : nMonths + " months";
    final String sDay = MathHelper.abs (nDays) == 1 ? nDays + " day" : nDays + " days";
    final String sHour = MathHelper.abs (nHours) == 1 ? nHours + " hour" : nHours + " hours";
    final String sMinute = MathHelper.abs (nMinutes) == 1 ? nMinutes + " minute" : nMinutes + " minutes";
    final String sSecond = MathHelper.abs (nSeconds) == 1 ? nSeconds + " second" : nSeconds + " seconds";

    // Skip all "leading 0" parts
    final ICommonsList <String> aParts = new CommonsArrayList <> (6);
    if (nYears != 0)
      aParts.add (sYear);
    if (nMonths != 0 || aParts.isNotEmpty ())
      aParts.add (sMonth);
    if (nDays != 0 || aParts.isNotEmpty ())
      aParts.add (sDay);
    if (nHours != 0 || aParts.isNotEmpty ())
      aParts.add (sHour);
    if (nMinutes != 0 || aParts.isNotEmpty ())
      aParts.add (sMinute);
    aParts.add (sSecond);

    final int nParts = aParts.size ();
    if (nParts == 1)
      return aParts.get (0);
    if (nParts == 2)
      return aParts.get (0) + " and " + aParts.get (1);
    final StringBuilder aSB = new StringBuilder ();
    for (int i = 0; i < nParts - 1; ++i)
    {
      if (aSB.length () > 0)
        aSB.append (", ");
      aSB.append (aParts.get (i));
    }
    return aSB.append (" and ").append (aParts.getLast ()).toString ();
  }

  @Nonnull
  public static BootstrapTable createCertificateDetailsTable (@Nonnull final X509Certificate aX509Cert,
                                                              @Nonnull final LocalDateTime aNowLDT,
                                                              @Nonnull final Locale aDisplayLocale)
  {
    final LocalDateTime aNotBefore = PDTFactory.createLocalDateTime (aX509Cert.getNotBefore ());
    final LocalDateTime aNotAfter = PDTFactory.createLocalDateTime (aX509Cert.getNotAfter ());
    final PublicKey aPublicKey = aX509Cert.getPublicKey ();

    final BootstrapTable aCertDetails = new BootstrapTable (HCCol.star (), HCCol.star ());
    aCertDetails.addBodyRow ().addCell ("Version:").addCell (Integer.toString (aX509Cert.getVersion ()));
    aCertDetails.addBodyRow ().addCell ("Subject:").addCell (aX509Cert.getSubjectX500Principal ().getName ());
    aCertDetails.addBodyRow ().addCell ("Issuer:").addCell (aX509Cert.getIssuerX500Principal ().getName ());
    aCertDetails.addBodyRow ().addCell ("Serial number:").addCell (aX509Cert.getSerialNumber ().toString (16));
    aCertDetails.addBodyRow ()
                .addCell ("Valid from:")
                .addCell (new HCTextNode (PDTToString.getAsString (aNotBefore, aDisplayLocale)),
                          aNowLDT.isBefore (aNotBefore) ? new HCStrong ().addChild (" !!!NOT YET VALID!!!") : null);
    aCertDetails.addBodyRow ()
                .addCell ("Valid to:")
                .addCell (new HCTextNode (PDTToString.getAsString (aNotAfter, aDisplayLocale)),
                          aNowLDT.isAfter (aNotAfter) ? new HCStrong ().addChild (" !!!NO LONGER VALID!!!")
                                                      : new HCDiv ().addChild ("Valid for: " +
                                                                               PDTDisplayHelper.getPeriodTextEN (aNowLDT, aNotAfter)));

    if (aPublicKey instanceof RSAPublicKey)
    {
      // Special handling for RSA
      aCertDetails.addBodyRow ()
                  .addCell ("Public key:")
                  .addCell (aX509Cert.getPublicKey ().getAlgorithm () +
                            " (" +
                            ((RSAPublicKey) aPublicKey).getModulus ().bitLength () +
                            " bits)");
    }
    else
    {
      // Usually EC or DSA key
      aCertDetails.addBodyRow ().addCell ("Public key:").addCell (aX509Cert.getPublicKey ().getAlgorithm ());
    }
    aCertDetails.addBodyRow ()
                .addCell ("Signature algorithm:")
                .addCell (aX509Cert.getSigAlgName () + " (" + aX509Cert.getSigAlgOID () + ")");
    return aCertDetails;
  }
}
