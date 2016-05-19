package com.helger.pd.indexer.storage;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.datetime.PDTFactory;

public final class PDDocumentMetaDataTest
{
  @Test
  public void testMillis ()
  {
    final long nMillis = System.currentTimeMillis ();
    final long nMillis2 = new PDDocumentMetaData (PDTFactory.getCurrentLocalDateTime (),
                                                  "me",
                                                  "localhost").getCreationDTMillis ();
    // Less than a second difference
    assertTrue (nMillis + " vs. " + nMillis2, Math.abs (nMillis2 - nMillis) <= 1000);
  }
}
