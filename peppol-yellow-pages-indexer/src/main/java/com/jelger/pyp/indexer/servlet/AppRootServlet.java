package com.jelger.pyp.indexer.servlet;

import javax.annotation.Nonnull;

import com.helger.commons.string.StringHelper;
import com.helger.photon.core.servlet.AbstractPublicApplicationServlet;
import com.helger.photon.core.servlet.AbstractUnifiedResponseServlet;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.web.servlet.response.UnifiedResponse;

public class AppRootServlet extends AbstractUnifiedResponseServlet
{
  @Override
  protected void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    String sRedirectURL = aRequestScope.getContextPath () + AbstractPublicApplicationServlet.SERVLET_DEFAULT_PATH;
    final String sQueryString = aRequestScope.getQueryString ();
    if (StringHelper.hasText (sQueryString))
      sRedirectURL += "?" + sQueryString;
    aUnifiedResponse.setRedirect (sRedirectURL);
  }
}
