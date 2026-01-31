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
package com.helger.pd.publisher.nicename;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.string.StringHelper;
import com.helger.io.resource.ClassPathResource;
import com.helger.io.resource.FileSystemResource;
import com.helger.io.resource.IReadableResource;
import com.helger.pd.indexer.settings.PDServerConfiguration;
import com.helger.peppol.ui.types.nicename.NiceNameManager;

public final class NiceNameHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (NiceNameHandler.class);

  @NonNull
  private static ClassLoader _getCL ()
  {
    return NiceNameHandler.class.getClassLoader ();
  }

  private NiceNameHandler ()
  {}

  public static void loadNiceNames ()
  {
    // Doc types
    {
      IReadableResource aDocTypeIDRes = null;
      final String sDocTypePath = PDServerConfiguration.getConfig ().getAsString ("webapp.nicename.doctypes.path");
      if (StringHelper.isNotEmpty (sDocTypePath))
      {
        aDocTypeIDRes = new FileSystemResource (sDocTypePath);
        if (!aDocTypeIDRes.exists ())
        {
          LOGGER.warn ("The configured document type nice name file '" + sDocTypePath + "' does not exist");
          // Enforce defaults
          aDocTypeIDRes = null;
        }
      }

      // Use defaults
      if (aDocTypeIDRes == null)
        aDocTypeIDRes = new ClassPathResource ("codelists/directory/doctypeid-mapping.xml", _getCL ());
      NiceNameManager.loadDocTypeNames (aDocTypeIDRes);
    }

    // Processes
    {
      IReadableResource aProcessIDRes = null;
      final String sProcessPath = PDServerConfiguration.getConfig ().getAsString ("webapp.nicename.processes.path");
      if (StringHelper.isNotEmpty (sProcessPath))
      {
        aProcessIDRes = new FileSystemResource (sProcessPath);
        if (!aProcessIDRes.exists ())
        {
          LOGGER.warn ("The configured process nice name file '" + sProcessPath + "' does not exist");
          // Enforce defaults
          aProcessIDRes = null;
        }
      }

      // Use defaults
      if (aProcessIDRes == null)
        aProcessIDRes = new ClassPathResource ("codelists/directory/processid-mapping.xml", _getCL ());

      NiceNameManager.loadProcessNames (aProcessIDRes);
    }
  }
}
