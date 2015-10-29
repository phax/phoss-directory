package com.helger.pyp.publisher.ui;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.joda.time.LocalDateTime;
import org.joda.time.Period;

import com.helger.commons.locale.country.CountryCache;
import com.helger.datetime.PDTFactory;
import com.helger.datetime.format.PDTToString;
import com.helger.datetime.format.PeriodFormatMultilingual;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.ext.HCExtHelper;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.tabular.HCCol;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.html.textlevel.HCStrong;
import com.helger.html.hc.html.textlevel.HCWBR;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.peppol.identifier.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppol.identifier.doctype.IPeppolDocumentTypeIdentifier;
import com.helger.peppol.identifier.doctype.IPeppolDocumentTypeIdentifierParts;
import com.helger.peppol.identifier.process.EPredefinedProcessIdentifier;
import com.helger.peppol.identifier.process.IPeppolProcessIdentifier;
import com.helger.photon.bootstrap3.form.BootstrapFormGroup;
import com.helger.photon.bootstrap3.form.BootstrapViewForm;
import com.helger.photon.bootstrap3.label.BootstrapLabel;
import com.helger.photon.bootstrap3.label.EBootstrapLabelType;
import com.helger.photon.bootstrap3.table.BootstrapTable;
import com.helger.photon.core.app.html.PhotonCSS;
import com.helger.photon.uictrls.EUICtrlsCSSPathProvider;
import com.helger.photon.uictrls.famfam.EFamFamFlagIcon;
import com.helger.pyp.indexer.storage.PYPStoredDocument;
import com.helger.pyp.indexer.storage.PYPStoredIdentifier;

/**
 * Common UI ctrls for business information display
 *
 * @author Philip Helger
 */
@Immutable
public final class PYPCommonUI
{
  private PYPCommonUI ()
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
  public static BootstrapViewForm showBusinessInfoDetails (@Nonnull final PYPStoredDocument aStoredDoc,
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
    if (aStoredDoc.hasName ())
      aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Entity Name").setCtrl (aStoredDoc.getName ()));
    if (aStoredDoc.hasGeoInfo ())
      aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Geographical information")
                                                       .setCtrl (HCExtHelper.nl2divList (aStoredDoc.getGeoInfo ())));
    if (aStoredDoc.hasAnyIdentifier ())
    {
      final BootstrapTable aIDTable = new BootstrapTable (HCCol.star (), HCCol.star ()).setStriped (true)
                                                                                       .setBordered (true);
      aIDTable.addHeaderRow ().addCells ("Type", "Value");
      for (final PYPStoredIdentifier aStoredID : aStoredDoc.getAllIdentifiers ())
        aIDTable.addBodyRow ().addCells (aStoredID.getType (), aStoredID.getValue ());
      aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Additional identifiers").setCtrl (aIDTable));
    }
    if (aStoredDoc.hasFreeText ())
      aViewForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Free text")
                                                       .setCtrl (HCExtHelper.nl2divList (aStoredDoc.getFreeText ())));
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
  public static IHCNode getDocumentTypeID (@Nonnull final IPeppolDocumentTypeIdentifier aDocTypeID)
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
  public static IHCNode getProcessID (@Nonnull final IPeppolProcessIdentifier aDocTypeID)
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
    aUL.addItem ().addChild ("Transaction ID: ").addChild (new HCCode ().addChild (aParts.getTransactionID ()));
    final HCUL aExtensions = new HCUL ();
    final List <String> aExtensionIDs = aParts.getExtensionIDs ();
    for (final String sExtension : aExtensionIDs)
      aExtensions.addItem (new HCCode ().addChild (sExtension));
    aUL.addItem ().addChild ("Extension IDs:").addChild (aExtensions);
    aUL.addItem ()
       .addChild ("Customization ID (transaction + extensions): ")
       .addChild (new HCCode ().addChild (aParts.getAsUBLCustomizationID ()));
    aUL.addItem ().addChild ("Version: ").addChild (new HCCode ().addChild (aParts.getVersion ()));
    return aUL;
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
                                                                               PeriodFormatMultilingual.getFormatterLong (aDisplayLocale)
                                                                                                       .print (new Period (aNowLDT,
                                                                                                                           aNotAfter))));

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
