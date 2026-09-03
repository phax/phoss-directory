/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.exportall;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.NonNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.WillNotClose;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsTreeSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSortedSet;
import com.helger.mime.CMimeType;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.pd.publisher.PDPublisherTestRule;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;

/**
 * Test class for class {@link ExportAllManager}.
 *
 * @author Philip Helger
 */
public final class ExportAllManagerTest
{
  private static final class MockExportAllHandler extends AbstractExportAllHandler
  {
    private final boolean m_bFailOnParticipant;
    private OutputStream m_aOS;
    private int m_nParticipants = 0;
    private boolean m_bEnded = false;

    MockExportAllHandler (@NonNull @Nonempty final String sDisplayName, final boolean bFailOnParticipant)
    {
      super (sDisplayName, "export1/unittest-" + sDisplayName + ".txt", CMimeType.TEXT_PLAIN);
      m_bFailOnParticipant = bFailOnParticipant;
    }

    public boolean isBusinessEntityDataNeeded ()
    {
      // Don't rely on the content of the search index
      return false;
    }

    public void onStart (@NonNull @WillNotClose final OutputStream aOS,
                         @Nonnegative final int nParticipantCount) throws Exception
    {
      m_aOS = aOS;
      m_aOS.write (("Count: " + nParticipantCount + "\n").getBytes (StandardCharsets.UTF_8));
    }

    public void onParticipant (@NonNull @Nonempty final String sParticipantID,
                               @NonNull final IParticipantIdentifier aParticipantID,
                               @NonNull final ICommonsList <PDStoredBusinessEntity> aEntities) throws Exception
    {
      if (m_bFailOnParticipant)
        throw new IllegalStateException ("Mock failure");

      m_nParticipants++;
      m_aOS.write ((sParticipantID + "\n").getBytes (StandardCharsets.UTF_8));
    }

    public void onEnd () throws Exception
    {
      m_bEnded = true;
      m_aOS.flush ();
    }
  }

  @Rule
  public final TestRule m_aRule = new PDPublisherTestRule ();

  @Test
  public void testExportAllContinuesAfterAFailingFormat () throws IOException
  {
    final ICommonsSortedSet <String> aAllParticipantIDs = new CommonsTreeSet <> ();
    for (int i = 0; i < 3; ++i)
    {
      aAllParticipantIDs.add (PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:unittest" +
                                                                                                             i)
                                                              .getURIEncoded ());
    }

    final MockExportAllHandler aSuccess = new MockExportAllHandler ("success", false);
    final MockExportAllHandler aFailure = new MockExportAllHandler ("failure", true);
    final ICommonsList <IExportAllHandler> aHandlers = new CommonsArrayList <> (aFailure, aSuccess);

    final ICommonsList <String> aFailedNames = ExportAllManager.exportAll (aAllParticipantIDs, aHandlers, x -> {});

    // Only the failing format is reported as failure - and it is not uploaded
    assertEquals (new CommonsArrayList <> ("failure"), aFailedNames);

    // The failing format must not affect the successful one
    assertEquals (aAllParticipantIDs.size (), aSuccess.m_nParticipants);
    assertTrue (aSuccess.m_bEnded);

    // The failing format is not called again after the first error
    assertEquals (0, aFailure.m_nParticipants);
    assertFalse (aFailure.m_bEnded);
  }
}
