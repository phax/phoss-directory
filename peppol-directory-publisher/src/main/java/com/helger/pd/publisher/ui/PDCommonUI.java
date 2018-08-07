/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.CGlobal;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTToString;
import com.helger.commons.locale.LocaleHelper;
import com.helger.commons.locale.country.CountryCache;
import com.helger.commons.locale.language.LanguageCache;
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
import com.helger.html.hc.html.textlevel.HCWBR;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.pd.indexer.storage.PDStoredContact;
import com.helger.pd.indexer.storage.PDStoredIdentifier;
import com.helger.pd.indexer.storage.PDStoredMLName;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
import com.helger.peppol.identifier.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppol.identifier.peppol.doctype.IPeppolDocumentTypeIdentifierParts;
import com.helger.peppol.identifier.peppol.process.EPredefinedProcessIdentifier;
import com.helger.photon.bootstrap3.form.BootstrapFormGroup;
import com.helger.photon.bootstrap3.form.BootstrapViewForm;
import com.helger.photon.bootstrap3.label.BootstrapLabel;
import com.helger.photon.bootstrap3.label.EBootstrapLabelType;
import com.helger.photon.bootstrap3.table.BootstrapTable;
import com.helger.photon.core.app.html.PhotonCSS;
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
    ICommonsList <PDStoredMLName> ret = CommonsArrayList.createFiltered (aNames,
                                                                         x -> x.hasLanguageCode (sDisplayLanguage));
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
  private static IHCNode _getWBRList (@Nonnull final String s)
  {
    final HCNodeList ret = new HCNodeList ();
    String sRest = s;
    final int nChars = 30;
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
  public static IHCNode getDocumentTypeID (@Nonnull final IDocumentTypeIdentifier aDocTypeID)
  {
    final HCNodeList ret = new HCNodeList ();

    final EPredefinedDocumentTypeIdentifier ePredefined = EPredefinedDocumentTypeIdentifier.getFromDocumentTypeIdentifierOrNull (aDocTypeID);
    if (ePredefined != null)
      ret.addChild (new HCDiv ().addChild (new BootstrapLabel (EBootstrapLabelType.SUCCESS).addChild ("Predefined document type"))
                                .addChild (" " + ePredefined.getCommonName ()));
    else
      ret.addChild (new HCDiv ().addChild (new BootstrapLabel (EBootstrapLabelType.WARNING).addChild ("Non standard document type")));
    ret.addChild (new HCCode ().addChild (_getWBRList (aDocTypeID.getURIEncoded ())));
    return ret;
  }

  @Nonnull
  public static IHCNode getProcessID (@Nonnull final IProcessIdentifier aDocTypeID)
  {
    EPredefinedProcessIdentifier ePredefined = null;
    for (final EPredefinedProcessIdentifier e : EPredefinedProcessIdentifier.values ())
      if (e.getAsProcessIdentifier ().equals (aDocTypeID))
      {
        ePredefined = e;
        break;
      }

    if (ePredefined != null)
      return new HCTextNode (ePredefined.getValue () + " [predefined]");

    return _getWBRList (aDocTypeID.getURIEncoded ());
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

  private static String _getPeriodText (@Nonnull final LocalDateTime aNowLDT, @Nonnull final LocalDateTime aNotAfter)
  {
    final Period aPeriod = Period.between (aNowLDT.toLocalDate (), aNotAfter.toLocalDate ());
    final Duration aDuration = Duration.between (aNowLDT.toLocalTime (),
                                                 aNotAfter.plus (1, ChronoUnit.DAYS).toLocalTime ());

    long nSecs = aDuration.getSeconds ();
    final long nHours = nSecs / CGlobal.SECONDS_PER_HOUR;
    nSecs -= nHours * CGlobal.SECONDS_PER_HOUR;
    final long nMinutes = nSecs / CGlobal.SECONDS_PER_MINUTE;
    nSecs -= nMinutes * CGlobal.SECONDS_PER_MINUTE;
    return aPeriod.getYears () +
           " years, " +
           aPeriod.getMonths () +
           " months, " +
           aPeriod.getDays () +
           " days, " +
           nHours +
           " hours, " +
           nMinutes +
           " minutes and " +
           nSecs +
           " seconds";
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
                                                                               _getPeriodText (aNowLDT, aNotAfter)));

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
