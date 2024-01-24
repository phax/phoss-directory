/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.pd.client;

import javax.annotation.Nonnull;

import com.helger.commons.callback.ICallback;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * Special exception callback interface for the Peppol Directory client.
 *
 * @author Philip Helger
 * @since 0.5.1
 */
@FunctionalInterface
public interface IPDClientExceptionCallback extends ICallback
{
  /**
   * Called for every exception in HTTP calls.
   *
   * @param aParticipantID
   *        The participant for which the PD should be invoked.
   * @param sContext
   *        The context in which the exception occurred. May neither be
   *        <code>null</code> nor empty.
   * @param aException
   *        The exception that occurred. May not be <code>null</code>.
   */
  void onException (@Nonnull IParticipantIdentifier aParticipantID, @Nonnull String sContext, @Nonnull Throwable aException);
}
