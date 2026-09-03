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

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.mime.IMimeType;

/**
 * Abstract base implementation of {@link IExportAllHandler} that takes care of the static
 * properties of an export format.
 *
 * @author Philip Helger
 */
public abstract class AbstractExportAllHandler implements IExportAllHandler
{
  private final String m_sDisplayName;
  private final String m_sS3Filename;
  private final IMimeType m_aContentType;

  protected AbstractExportAllHandler (@NonNull @Nonempty final String sDisplayName,
                                      @NonNull @Nonempty final String sS3Filename,
                                      @NonNull final IMimeType aContentType)
  {
    m_sDisplayName = ValueEnforcer.notEmpty (sDisplayName, "DisplayName");
    m_sS3Filename = ValueEnforcer.notEmpty (sS3Filename, "S3Filename");
    m_aContentType = ValueEnforcer.notNull (aContentType, "ContentType");
  }

  @NonNull
  @Nonempty
  public final String getDisplayName ()
  {
    return m_sDisplayName;
  }

  @NonNull
  @Nonempty
  public final String getS3Filename ()
  {
    return m_sS3Filename;
  }

  @NonNull
  public final IMimeType getContentType ()
  {
    return m_aContentType;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("DisplayName", m_sDisplayName)
                                       .append ("S3Filename", m_sS3Filename)
                                       .append ("ContentType", m_aContentType)
                                       .getToString ();
  }
}
