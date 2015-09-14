package com.helger.pyp.businessinformation;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.peppol.identifier.IDocumentTypeIdentifier;

/**
 * This class encapsulates all the data to be added to the Lucene index. It
 * consists of the main {@link BusinessInformationType} object as retrieved from
 * the SMP plus a list of all document types supported by the respective service
 * group.
 *
 * @author Philip Helger
 */
public class PYPExtendedBusinessInformation
{
  private final BusinessInformationType m_aBusinessInfo;
  private final List <IDocumentTypeIdentifier> m_aDocumentTypeIDs = new ArrayList <> ();

  public PYPExtendedBusinessInformation (@Nonnull final BusinessInformationType aBusinessInfo,
                                         @Nullable final List <IDocumentTypeIdentifier> aDocumentTypeIDs)
  {
    m_aBusinessInfo = ValueEnforcer.notNull (aBusinessInfo, "BusinessInfo");
    if (aDocumentTypeIDs != null)
      for (final IDocumentTypeIdentifier aDocTypeID : aDocumentTypeIDs)
        if (aDocTypeID != null)
          m_aDocumentTypeIDs.add (aDocTypeID);
  }

  @Nonnull
  public BusinessInformationType getBusinessInformation ()
  {
    return m_aBusinessInfo;
  }

  @Nonnull
  @ReturnsMutableCopy
  public List <IDocumentTypeIdentifier> getAllDocumentTypeIDs ()
  {
    return CollectionHelper.newList (m_aDocumentTypeIDs);
  }
}
