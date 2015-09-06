package com.helger.pyp.indexer;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.microdom.IMicroDocument;
import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.MicroDocument;
import com.helger.commons.microdom.convert.MicroTypeConverter;
import com.helger.commons.microdom.serialize.MicroReader;
import com.helger.commons.microdom.serialize.MicroWriter;
import com.helger.commons.scope.IScope;
import com.helger.commons.scope.singleton.AbstractGlobalSingleton;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ESuccess;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.photon.basic.app.io.WebFileIO;

/**
 * The global indexer manager that takes an item for queuing and maintains the
 * uniqueness of the items to queue.
 *
 * @author Philip Helger
 */
public final class IndexerManager extends AbstractGlobalSingleton
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (IndexerManager.class);
  private static final String ELEMENT_ROOT = "root";
  private static final String ELEMENT_ITEM = "item";

  private final Set <IndexerWorkItem> m_aUniqueItems = new HashSet <> ();
  private final ReIndexWorkQueue m_aReIndexList = new ReIndexWorkQueue ();
  private final IndexerWorkQueue m_aIndexerWorkQueue = new IndexerWorkQueue (this::_fetchParticipantData);

  @Deprecated
  @UsedViaReflection
  public IndexerManager ()
  {
    // Read an eventually existing serialized element
    final IMicroDocument aDoc = MicroReader.readMicroXML (_getFile ());
    if (aDoc != null)
      for (final IMicroElement eItem : aDoc.getDocumentElement ().getAllChildElements (ELEMENT_ITEM))
      {
        final IndexerWorkItem aItem = MicroTypeConverter.convertToNative (eItem, IndexerWorkItem.class);
        m_aUniqueItems.add (aItem);
        m_aIndexerWorkQueue.queueObject (aItem);
      }
  }

  @Nonnull
  public static IndexerManager getInstance ()
  {
    return getGlobalSingleton (IndexerManager.class);
  }

  @Nonnull
  private static File _getFile ()
  {
    return WebFileIO.getDataIO ().getFile ("indexer-work-queue.xml");
  }

  private static void _write (@Nonnull final List <IndexerWorkItem> aItems)
  {
    if (!aItems.isEmpty ())
    {
      s_aLogger.info ("Persisting " + aItems.size () + " items");
      final IMicroDocument aDoc = new MicroDocument ();
      final IMicroElement eRoot = aDoc.appendElement (ELEMENT_ROOT);
      for (final IndexerWorkItem aItem : aItems)
        eRoot.appendChild (MicroTypeConverter.convertToMicroElement (aItem, ELEMENT_ITEM));
      if (MicroWriter.writeToFile (aDoc, _getFile ()).isFailure ())
        throw new IllegalStateException ("Failed to write IndexerWorkItems to " + _getFile ());
    }
  }

  @Override
  protected void onBeforeDestroy (@Nonnull final IScope aScopeToBeDestroyed)
  {
    // Get all remaining objects and save them for late reuse
    final List <IndexerWorkItem> aRemainingItems = m_aIndexerWorkQueue.stop ();
    _write (aRemainingItems);
  }

  @Nonnull
  public EChange queueObject (@Nonnull final IParticipantIdentifier aParticipantID,
                              @Nonnull final EIndexerWorkItemType eType)
  {
    final IndexerWorkItem aItem = new IndexerWorkItem (aParticipantID, eType);

    m_aRWLock.writeLock ().lock ();
    try
    {
      if (!m_aUniqueItems.add (aItem))
      {
        s_aLogger.info ("Ignoring item " + aItem + " because it is already in the queue!");
        return EChange.UNCHANGED;
      }
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    m_aIndexerWorkQueue.queueObject (aItem);
    return EChange.CHANGED;
  }

  /**
   * This is the main method to perform the operation on the SMP.
   *
   * @param aItem
   *        The item to be fetched. Never <code>null</code>.
   * @return {@link ESuccess}.
   */
  @Nonnull
  private ESuccess _fetchParticipantData (@Nonnull final IndexerWorkItem aItem)
  {
    final boolean bSuccess = false;
    // TODO Perform SMP queries etc.

    if (bSuccess)
      return ESuccess.SUCCESS;

    // Failed to fetch participant data - add to re-index queue
    m_aReIndexList.addItem (new ReIndexWorkItem (aItem));
    return ESuccess.FAILURE;
  }

  public void expireOldEntries ()
  {
    m_aRWLock.writeLock ().lock ();
    try
    {
      // Expire old entries and remove them from the overall list
      m_aReIndexList.expireOldEntries ().stream ().forEach (aItem -> m_aUniqueItems.remove (aItem.getWorkItem ()));
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
  }
}
