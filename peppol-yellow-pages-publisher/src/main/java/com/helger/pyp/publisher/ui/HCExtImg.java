/**
 * Copyright (C) 2006-2015 BRZ GmbH
 * http://www.brz.gv.at
 *
 * All rights reserved
 */
package com.helger.pyp.publisher.ui;

import javax.annotation.Nonnull;

import com.helger.commons.gfx.ImageDataManager;
import com.helger.commons.url.ISimpleURL;
import com.helger.commons.url.SimpleURL;
import com.helger.html.hc.html.embedded.AbstractHCImg;
import com.helger.photon.basic.app.io.WebFileIO;
import com.helger.web.servlet.request.RequestHelper;

public class HCExtImg extends AbstractHCImg <HCExtImg>
{
  public HCExtImg (@Nonnull final ISimpleURL aSrc)
  {
    setSrc (aSrc);

    // Remove the session ID (if any)
    final String sPureSrc = new SimpleURL (RequestHelper.getWithoutSessionID (aSrc.getPath ()),
                                           aSrc.getAllParams (),
                                           aSrc.getAnchor ()).getAsString ();
    setExtent (ImageDataManager.getInstance ().getImageSize (WebFileIO.getServletContextIO ().getResource (sPureSrc)));
  }
}
