/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.ui;

import javax.annotation.Nonnull;

import com.helger.commons.url.ISimpleURL;
import com.helger.commons.url.SimpleURL;
import com.helger.html.hc.html.embedded.AbstractHCImg;
import com.helger.lesscommons.gfx.ImageDataManager;
import com.helger.photon.basic.app.io.WebFileIO;
import com.helger.servlet.request.RequestHelper;

public class HCExtImg extends AbstractHCImg <HCExtImg>
{
  public HCExtImg (@Nonnull final ISimpleURL aSrc)
  {
    setSrc (aSrc);

    // Remove the session ID (if any)
    final String sPureSrc = new SimpleURL (RequestHelper.getWithoutSessionID (aSrc.getPath ()),
                                           aSrc.params (),
                                           aSrc.getAnchor ()).getAsStringWithEncodedParameters ();
    setExtent (ImageDataManager.getInstance ().getImageSize (WebFileIO.getServletContextIO ().getResource (sPureSrc)));
  }
}
