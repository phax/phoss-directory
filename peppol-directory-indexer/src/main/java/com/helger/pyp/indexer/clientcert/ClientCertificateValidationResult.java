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
package com.helger.pyp.indexer.clientcert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.state.ISuccessIndicator;

/**
 * This class contains the result of a single client certificate validation.
 *
 * @author Philip Helger
 */
@Immutable
public final class ClientCertificateValidationResult implements ISuccessIndicator
{
  private final boolean m_bSuccess;
  private final String m_sClientID;

  private ClientCertificateValidationResult (final boolean bSuccess, @Nullable final String sClientID)
  {
    m_bSuccess = bSuccess;
    m_sClientID = sClientID;
  }

  public boolean isSuccess ()
  {
    return m_bSuccess;
  }

  public boolean isFailure ()
  {
    return !m_bSuccess;
  }

  /**
   * @return The ID of the client that triggered the request. Must be
   *         <code>null</code> on failure and must not be <code>null</code> on
   *         success.
   */
  @Nullable
  public String getClientID ()
  {
    return m_sClientID;
  }

  @Nonnull
  public static ClientCertificateValidationResult createSuccess (@Nonnull @Nonempty final String sClientID)
  {
    ValueEnforcer.notEmpty (sClientID, "ClientID");
    return new ClientCertificateValidationResult (true, sClientID);
  }

  @Nonnull
  public static ClientCertificateValidationResult createFailure ()
  {
    return new ClientCertificateValidationResult (false, null);
  }
}
