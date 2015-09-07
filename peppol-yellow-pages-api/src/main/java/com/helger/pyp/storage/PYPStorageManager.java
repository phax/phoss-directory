package com.helger.pyp.storage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.scope.singleton.AbstractGlobalSingleton;
import com.helger.peppol.identifier.participant.IPeppolParticipantIdentifier;
import com.helger.pyp.businessinformation.BusinessInformationType;

/**
 * The global storage manager that wraps the used Lucene index.
 *
 * @author Philip Helger
 */
public final class PYPStorageManager extends AbstractGlobalSingleton
{
  @Deprecated
  @UsedViaReflection
  public PYPStorageManager ()
  {}

  /**
   * @return The one and only instance of this class. Never <code>null</code>.
   */
  @Nonnull
  public static PYPStorageManager getInstance ()
  {
    return getGlobalSingleton (PYPStorageManager.class);
  }

  public boolean containsEntry (@Nullable final IPeppolParticipantIdentifier aParticipantID)
  {
    if (aParticipantID == null)
      return false;

    // TODO contains check
    return true;
  }

  public void deleteEntry (@Nonnull final IPeppolParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    // TODO delete
  }

  public void createOrUpdateEntry (@Nonnull final IPeppolParticipantIdentifier aParticipantID,
                                   @Nonnull final BusinessInformationType aBI)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    // TODO create or update
  }
}
