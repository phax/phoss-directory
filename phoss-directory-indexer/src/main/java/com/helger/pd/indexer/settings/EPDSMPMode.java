/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.settings;

/**
 * The SMP mode to be used.
 *
 * @author Philip Helger
 */
public enum EPDSMPMode
{
  /** Peppol mode */
  PEPPOL,
  /** OASIS SMP v1 mode */
  OASIS_BDXR_V1,
  /** OASIS SMP v2 mode */
  OASIS_BDXR_V2;
}
