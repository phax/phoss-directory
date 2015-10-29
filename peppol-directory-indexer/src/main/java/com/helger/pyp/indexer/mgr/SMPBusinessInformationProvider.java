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
package com.helger.pyp.indexer.mgr;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.XMLConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.charset.CCharset;
import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.IMicroNode;
import com.helger.commons.microdom.MicroCDATA;
import com.helger.commons.microdom.MicroComment;
import com.helger.commons.microdom.MicroDocument;
import com.helger.commons.microdom.MicroDocumentType;
import com.helger.commons.microdom.MicroElement;
import com.helger.commons.microdom.MicroEntityReference;
import com.helger.commons.microdom.MicroProcessingInstruction;
import com.helger.commons.microdom.MicroText;
import com.helger.commons.microdom.serialize.MicroWriter;
import com.helger.commons.url.SimpleURL;
import com.helger.commons.url.URLHelper;
import com.helger.commons.xml.XMLDebug;
import com.helger.pd.businessinformation.IPDBusinessInformationProvider;
import com.helger.pd.businessinformation.PDBusinessInformationMarshaller;
import com.helger.pd.businessinformation.PDExtendedBusinessInformation;
import com.helger.pd.settings.PDSettings;
import com.helger.peppol.identifier.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.IdentifierHelper;
import com.helger.peppol.identifier.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.identifier.participant.IPeppolParticipantIdentifier;
import com.helger.peppol.smp.ExtensionType;
import com.helger.peppol.smp.ServiceGroupType;
import com.helger.peppol.smp.ServiceMetadataReferenceType;
import com.helger.peppol.smpclient.SMPClientReadOnly;
import com.helger.peppol.smpclient.exception.SMPClientException;
import com.helger.pyp.businessinformation.BusinessInformationType;

/**
 * The SMP based {@link IPDBusinessInformationProvider} implementation. An SMP
 * lookup of the ServiceGroup is performed, and the <code>Extension</code>
 * element is parsed for the elements as specified in the PYP specification.
 *
 * @author Philip Helger
 */
public final class SMPBusinessInformationProvider implements IPDBusinessInformationProvider
{
  private static final String URL_PART_SERVICES = "/services/";
  private static final Logger s_aLogger = LoggerFactory.getLogger (SMPBusinessInformationProvider.class);

  // FIXME replace with MicroHelper version in ph-commons >= 6.2.1
  @Nonnull
  public static IMicroNode convertToMicroNode (@Nonnull final Node aNode)
  {
    ValueEnforcer.notNull (aNode, "Node");

    IMicroNode ret;
    final short nNodeType = aNode.getNodeType ();
    switch (nNodeType)
    {
      case Node.DOCUMENT_NODE:
      {
        ret = new MicroDocument ();
        break;
      }
      case Node.DOCUMENT_TYPE_NODE:
      {
        final DocumentType aDT = (DocumentType) aNode;
        // inline DTDs are not supported yet
        // aDT.getEntities ();
        ret = new MicroDocumentType (aDT.getName (), aDT.getPublicId (), aDT.getSystemId ());
        break;
      }
      case Node.ELEMENT_NODE:
      {
        final Element aElement = (Element) aNode;
        final String sNamespaceURI = aElement.getNamespaceURI ();
        final IMicroElement eElement = sNamespaceURI != null ? new MicroElement (sNamespaceURI,
                                                                                 aElement.getLocalName ())
                                                             : new MicroElement (aElement.getTagName ());
        final NamedNodeMap aAttrs = aNode.getAttributes ();
        if (aAttrs != null)
        {
          final int nAttrCount = aAttrs.getLength ();
          for (int i = 0; i < nAttrCount; ++i)
          {
            final Attr aAttr = (Attr) aAttrs.item (i);
            final String sAttrNamespaceURI = aAttr.getNamespaceURI ();
            if (sAttrNamespaceURI != null)
            {
              // Ignore all "xmlns" attributes (special namespace URI!)
              if (!XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals (sAttrNamespaceURI))
                eElement.setAttribute (sAttrNamespaceURI, aAttr.getLocalName (), aAttr.getValue ());
            }
            else
              eElement.setAttribute (aAttr.getName (), aAttr.getValue ());
          }
        }
        ret = eElement;
        break;
      }
      case Node.CDATA_SECTION_NODE:
        ret = new MicroCDATA (aNode.getNodeValue ());
        break;
      case Node.TEXT_NODE:
        ret = new MicroText (aNode.getNodeValue ());
        break;
      case Node.COMMENT_NODE:
        ret = new MicroComment (aNode.getNodeValue ());
        break;
      case Node.ENTITY_REFERENCE_NODE:
        ret = new MicroEntityReference (aNode.getNodeValue ());
        break;
      case Node.PROCESSING_INSTRUCTION_NODE:
        final ProcessingInstruction aPI = (ProcessingInstruction) aNode;
        ret = new MicroProcessingInstruction (aPI.getTarget (), aPI.getData ());
        break;
      case Node.ATTRIBUTE_NODE:
        throw new IllegalArgumentException ("Unknown/unsupported node type: ATTRIBUTE_NODE");
      case Node.ENTITY_NODE:
        throw new IllegalArgumentException ("Unknown/unsupported node type: ENTITY_NODE");
      case Node.DOCUMENT_FRAGMENT_NODE:
        throw new IllegalArgumentException ("Unknown/unsupported node type: DOCUMENT_FRAGMENT_NODE");
      case Node.NOTATION_NODE:
        throw new IllegalArgumentException ("Unknown/unsupported node type: NOTATION_NODE");
      default:
        throw new IllegalArgumentException ("Unknown/unsupported node type: " + nNodeType);
    }

    // handle children recursively (works for different node types)
    final NodeList aChildren = aNode.getChildNodes ();
    if (aChildren != null)
    {
      final int nChildCount = aChildren.getLength ();
      for (int i = 0; i < nChildCount; ++i)
      {
        final Node aChildNode = aChildren.item (i);
        ret.appendChild (convertToMicroNode (aChildNode));
      }
    }

    return ret;
  }

  @Nullable
  public static BusinessInformationType extractBusinessInformation (@Nullable final ExtensionType aExtension)
  {
    if (aExtension != null && aExtension.getAny () != null)
    {
      final IMicroNode aExtensionContainer = convertToMicroNode (aExtension.getAny ());
      if (aExtensionContainer instanceof IMicroElement)
      {
        final IMicroElement eExtensionContainer = (IMicroElement) aExtensionContainer;
        if ("ExtensionContainer".equals (eExtensionContainer.getLocalName ()))
        {
          for (final IMicroElement eExtensionElement : eExtensionContainer.getAllChildElements ("ExtensionElement"))
            if ("business information".equals (eExtensionElement.getAttributeValue ("type")))
            {
              final IMicroElement eBussinessInfo = eExtensionElement.getFirstChildElement ("BusinessInformation");
              if (eBussinessInfo != null)
              {
                final String sBusinessInfo = MicroWriter.getXMLString (eBussinessInfo);
                final BusinessInformationType aBI = new PDBusinessInformationMarshaller ().read (sBusinessInfo);
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
                          eExtensionContainer.getLocalName () +
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
  public PDExtendedBusinessInformation getBusinessInformation (@Nonnull final IPeppolParticipantIdentifier aParticipantID)
  {
    // Fetch data
    final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (aParticipantID, PDSettings.getSMLToUse ());
    ServiceGroupType aServiceGroup;
    try
    {
      aServiceGroup = aSMPClient.getServiceGroup (aParticipantID);
    }
    catch (final SMPClientException ex)
    {
      s_aLogger.error ("Error querying SMP for service group '" + aParticipantID.getURIEncoded () + "'", ex);
      return null;
    }

    final BusinessInformationType aBI = extractBusinessInformation (aServiceGroup.getExtension ());
    if (aBI == null)
    {
      // No extension present - no need to try again
      s_aLogger.warn ("Failed to get SMP BusinessInformation from Extension of service group " +
                      aParticipantID.getURIEncoded ());
      return null;
    }

    final List <IDocumentTypeIdentifier> aDocumentTypeIDs = new ArrayList <> ();
    for (final ServiceMetadataReferenceType aRef : aServiceGroup.getServiceMetadataReferenceCollection ()
                                                                .getServiceMetadataReference ())
    {
      // Extract the path in case there are parameters or anchors attached
      final String sHref = new SimpleURL (aRef.getHref ()).getPath ();
      final int nIndex = sHref.indexOf (URL_PART_SERVICES);
      if (nIndex < 0)
      {
        s_aLogger.error ("Invalid href when querying service group '" +
                         aParticipantID.getURIEncoded () +
                         "': '" +
                         sHref +
                         "'");
      }
      else
      {
        // URL decode because of encoded '#' and ':' characters
        final String sDocumentTypeID = URLHelper.urlDecode (sHref.substring (nIndex + URL_PART_SERVICES.length ()),
                                                            CCharset.CHARSET_UTF_8_OBJ);
        final SimpleDocumentTypeIdentifier aDocTypeID = IdentifierHelper.createDocumentTypeIdentifierFromURIPartOrNull (sDocumentTypeID);
        if (aDocTypeID == null)
        {
          s_aLogger.error ("Invalid document type when querying service group '" +
                           aParticipantID.getURIEncoded () +
                           "': '" +
                           sDocumentTypeID +
                           "'");
        }
        else
        {
          // Success
          aDocumentTypeIDs.add (aDocTypeID);
        }
      }
    }

    return new PDExtendedBusinessInformation (aBI, aDocumentTypeIDs);
  }
}
