/**
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.servlet;

import com.helger.commons.http.EHttpMethod;
import com.helger.xservlet.AbstractXServlet;

/**
 * The participant quick lookup servlet (issue #30). Handles only GET requests.
 *
 * @author Philip Helger
 */
public final class PublicParticipantServlet extends AbstractXServlet
{
  public PublicParticipantServlet ()
  {
    handlerRegistry ().registerHandler (EHttpMethod.GET, new PublicParticipantXServletHandler ());
  }
}
