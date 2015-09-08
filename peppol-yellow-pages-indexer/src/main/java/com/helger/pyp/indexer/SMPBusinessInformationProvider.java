package com.helger.pyp.indexer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.busdox.servicemetadata.publishing._1.ExtensionType;
import org.busdox.servicemetadata.publishing._1.ServiceGroupType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.IMicroNode;
import com.helger.commons.microdom.serialize.MicroWriter;
import com.helger.commons.microdom.util.MicroHelper;
import com.helger.commons.xml.XMLDebug;
import com.helger.peppol.identifier.participant.IPeppolParticipantIdentifier;
import com.helger.peppol.smpclient.SMPClientReadOnly;
import com.helger.peppol.smpclient.exception.SMPClientException;
import com.helger.pyp.businessinformation.BusinessInformationType;
import com.helger.pyp.businessinformation.IPYPBusinessInformationProvider;
import com.helger.pyp.businessinformation.PYPBusinessInformationMarshaller;
import com.helger.pyp.settings.PYPSettings;

/**
 * The SMP based {@link IPYPBusinessInformationProvider} implementation. An SMP
 * lookup of the ServiceGroup is performed, and the <code>Extension</code>
 * element is parsed for the elements as specified in the PYP specification.
 *
 * @author Philip Helger
 */
public class SMPBusinessInformationProvider implements IPYPBusinessInformationProvider
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (SMPBusinessInformationProvider.class);

  @Nullable
  public static BusinessInformationType extractBusinessInformation (@Nullable final ExtensionType aExtension)
  {
    if (aExtension != null && aExtension.getAny () != null)
    {
      final IMicroNode aExtensionContainer = MicroHelper.convertToMicroNode (aExtension.getAny ());
      if (aExtensionContainer instanceof IMicroElement)
      {
        final IMicroElement eExtensionContainer = (IMicroElement) aExtensionContainer;
        if ("ExtensionContainer".equals (eExtensionContainer.getTagName ()))
        {
          for (final IMicroElement eExtensionElement : eExtensionContainer.getAllChildElements ("ExtensionElement"))
            if ("business information".equals (eExtensionElement.getAttributeValue ("type")))
            {
              final IMicroElement eBussinessInfo = eExtensionElement.getFirstChildElement ("BusinessInformation");
              if (eBussinessInfo != null)
              {
                final String sBusinessInfo = MicroWriter.getXMLString (eBussinessInfo);
                final BusinessInformationType aBI = new PYPBusinessInformationMarshaller ().read (sBusinessInfo);
                if (aBI != null)
                {
                  // Finally we're done
                  return aBI;
                }
                s_aLogger.warn ("Failed to parse business information data:\n" + sBusinessInfo);
              }
              else
                s_aLogger.warn ("The 'ExtensionElement' for business information does not contain a 'BusinessInformation' child element");
              break;
            }
          s_aLogger.warn ("'ExtensionContainer' does not contain an 'ExtensionElement' with @type 'business information'");
        }
        else
        {
          s_aLogger.warn ("Extension content is expected to be an 'ExtensionContainer' but it is a '" +
                          eExtensionContainer.getTagName () +
                          "'");
        }
      }
      else
      {
        s_aLogger.warn ("Extension content is not an element but a " +
                        XMLDebug.getNodeTypeAsString (aExtension.getAny ().getNodeType ()));
      }
    }

    return null;
  }

  @Nullable
  public BusinessInformationType getBusinessInformation (@Nonnull final IPeppolParticipantIdentifier aParticipantID)
  {
    // Fetch data
    final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (aParticipantID, PYPSettings.getSMLToUse ());
    ServiceGroupType aServiceGroup;
    try
    {
      aServiceGroup = aSMPClient.getServiceGroup (aParticipantID);
    }
    catch (final SMPClientException ex)
    {
      s_aLogger.error ("Error querying SMP", ex);
      return null;
    }

    final BusinessInformationType aBI = extractBusinessInformation (aServiceGroup.getExtension ());
    if (aBI == null)
    {
      // No extension present - no need to try again
      s_aLogger.warn ("Failed to get SMP BusinessInformation from Extension of " + aParticipantID.getURIEncoded ());
      return null;
    }

    return aBI;
  }
}
