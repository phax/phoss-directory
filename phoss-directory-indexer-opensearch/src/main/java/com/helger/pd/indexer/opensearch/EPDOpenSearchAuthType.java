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
package com.helger.pd.indexer.opensearch;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;

import jakarta.annotation.Nullable;

/**
 * The way the Peppol Directory authenticates against the OpenSearch endpoint.
 *
 * @author Philip Helger
 * @since 0.16.0
 */
public enum EPDOpenSearchAuthType implements IHasID <String>
{
  /** No authentication at all - only suitable for a local test installation */
  NONE ("none"),
  /**
   * AWS Signature Version 4 request signing, using the default AWS credentials provider chain. This
   * is the authentication of a managed AWS OpenSearch Service domain or an AWS OpenSearch
   * Serverless collection.
   */
  AWS_SIGV4 ("aws");

  private final String m_sID;

  EPDOpenSearchAuthType (@NonNull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nullable
  public static EPDOpenSearchAuthType getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDCaseInsensitiveOrNull (EPDOpenSearchAuthType.class, sID);
  }

  @Nullable
  public static EPDOpenSearchAuthType getFromIDOrDefault (@Nullable final String sID,
                                                          @Nullable final EPDOpenSearchAuthType eDefault)
  {
    return EnumHelper.getFromIDCaseInsensitiveOrDefault (EPDOpenSearchAuthType.class, sID, eDefault);
  }
}
