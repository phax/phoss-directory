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
package com.helger.pd.publisher.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.grouping.HCBlockQuote;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCP;
import com.helger.html.hc.html.grouping.HCPre;
import com.helger.html.hc.html.sections.HCH1;
import com.helger.html.hc.html.sections.HCH2;
import com.helger.html.hc.html.sections.HCH3;
import com.helger.html.hc.html.sections.HCH4;
import com.helger.html.hc.html.sections.HCH5;
import com.helger.html.hc.html.sections.HCH6;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.html.textlevel.HCEM;
import com.helger.html.hc.html.textlevel.HCSmall;
import com.helger.html.hc.html.textlevel.HCSpan;
import com.helger.html.hc.html.textlevel.HCStrong;
import com.helger.html.hc.html.textlevel.HCSub;
import com.helger.html.hc.html.textlevel.HCSup;

/**
 * Traits interface to add simpler UI codes.
 *
 * @author Philip Helger
 */
public interface IHCTrait
{
  @Nonnull
  default HCBlockQuote blockquote ()
  {
    return new HCBlockQuote ();
  }

  @Nonnull
  default HCBlockQuote blockquote (@Nullable final IHCNode aNode)
  {
    return new HCBlockQuote ().addChild (aNode);
  }

  @Nonnull
  default HCBlockQuote blockquote (@Nullable final String s)
  {
    return new HCBlockQuote ().addChild (s);
  }

  @Nonnull
  default HCBlockQuote blockquote (@Nullable final Iterable <? extends IHCNode> aNodes)
  {
    return new HCBlockQuote ().addChildren (aNodes);
  }

  @Nonnull
  default HCCode code ()
  {
    return new HCCode ();
  }

  @Nonnull
  default HCCode code (@Nullable final IHCNode aNode)
  {
    return new HCCode ().addChild (aNode);
  }

  @Nonnull
  default HCCode code (@Nullable final String s)
  {
    return new HCCode ().addChild (s);
  }

  @Nonnull
  default HCCode code (@Nullable final Iterable <? extends IHCNode> aNodes)
  {
    return new HCCode ().addChildren (aNodes);
  }

  @Nonnull
  default HCDiv div ()
  {
    return new HCDiv ();
  }

  @Nonnull
  default HCDiv div (@Nullable final IHCNode aNode)
  {
    return new HCDiv ().addChild (aNode);
  }

  @Nonnull
  default HCDiv div (@Nullable final String s)
  {
    return new HCDiv ().addChild (s);
  }

  @Nonnull
  default HCDiv div (@Nullable final Iterable <? extends IHCNode> aNodes)
  {
    return new HCDiv ().addChildren (aNodes);
  }

  @Nonnull
  default HCEM em ()
  {
    return new HCEM ();
  }

  @Nonnull
  default HCEM em (@Nullable final IHCNode aNode)
  {
    return new HCEM ().addChild (aNode);
  }

  @Nonnull
  default HCEM em (@Nullable final String s)
  {
    return new HCEM ().addChild (s);
  }

  @Nonnull
  default HCEM em (@Nullable final Iterable <? extends IHCNode> aNodes)
  {
    return new HCEM ().addChildren (aNodes);
  }

  @Nonnull
  default HCH1 h1 ()
  {
    return new HCH1 ();
  }

  @Nonnull
  default HCH1 h1 (@Nullable final IHCNode aNode)
  {
    return new HCH1 ().addChild (aNode);
  }

  @Nonnull
  default HCH1 h1 (@Nullable final String s)
  {
    return new HCH1 ().addChild (s);
  }

  @Nonnull
  default HCH1 h1 (@Nullable final Iterable <? extends IHCNode> aNodes)
  {
    return new HCH1 ().addChildren (aNodes);
  }

  @Nonnull
  default HCH2 h2 ()
  {
    return new HCH2 ();
  }

  @Nonnull
  default HCH2 h2 (@Nullable final IHCNode aNode)
  {
    return new HCH2 ().addChild (aNode);
  }

  @Nonnull
  default HCH2 h2 (@Nullable final String s)
  {
    return new HCH2 ().addChild (s);
  }

  @Nonnull
  default HCH2 h2 (@Nullable final Iterable <? extends IHCNode> aNodes)
  {
    return new HCH2 ().addChildren (aNodes);
  }

  @Nonnull
  default HCH3 h3 ()
  {
    return new HCH3 ();
  }

  @Nonnull
  default HCH3 h3 (@Nullable final IHCNode aNode)
  {
    return new HCH3 ().addChild (aNode);
  }

  @Nonnull
  default HCH3 h3 (@Nullable final String s)
  {
    return new HCH3 ().addChild (s);
  }

  @Nonnull
  default HCH3 h3 (@Nullable final Iterable <? extends IHCNode> aNodes)
  {
    return new HCH3 ().addChildren (aNodes);
  }

  @Nonnull
  default HCH4 h4 ()
  {
    return new HCH4 ();
  }

  @Nonnull
  default HCH4 h4 (@Nullable final IHCNode aNode)
  {
    return new HCH4 ().addChild (aNode);
  }

  @Nonnull
  default HCH4 h4 (@Nullable final String s)
  {
    return new HCH4 ().addChild (s);
  }

  @Nonnull
  default HCH4 h4 (@Nullable final Iterable <? extends IHCNode> aNodes)
  {
    return new HCH4 ().addChildren (aNodes);
  }

  @Nonnull
  default HCH5 h5 ()
  {
    return new HCH5 ();
  }

  @Nonnull
  default HCH5 h5 (@Nullable final IHCNode aNode)
  {
    return new HCH5 ().addChild (aNode);
  }

  @Nonnull
  default HCH5 h5 (@Nullable final String s)
  {
    return new HCH5 ().addChild (s);
  }

  @Nonnull
  default HCH5 h5 (@Nullable final Iterable <? extends IHCNode> aNodes)
  {
    return new HCH5 ().addChildren (aNodes);
  }

  @Nonnull
  default HCH6 h6 ()
  {
    return new HCH6 ();
  }

  @Nonnull
  default HCH6 h6 (@Nullable final IHCNode aNode)
  {
    return new HCH6 ().addChild (aNode);
  }

  @Nonnull
  default HCH6 h6 (@Nullable final String s)
  {
    return new HCH6 ().addChild (s);
  }

  @Nonnull
  default HCH6 h6 (@Nullable final Iterable <? extends IHCNode> aNodes)
  {
    return new HCH6 ().addChildren (aNodes);
  }

  @Nonnull
  default HCP p ()
  {
    return new HCP ();
  }

  @Nonnull
  default HCP p (@Nullable final IHCNode aNode)
  {
    return new HCP ().addChild (aNode);
  }

  @Nonnull
  default HCP p (@Nullable final String s)
  {
    return new HCP ().addChild (s);
  }

  @Nonnull
  default HCP p (@Nullable final Iterable <? extends IHCNode> aNodes)
  {
    return new HCP ().addChildren (aNodes);
  }

  @Nonnull
  default HCPre pre ()
  {
    return new HCPre ();
  }

  @Nonnull
  default HCPre pre (@Nullable final IHCNode aNode)
  {
    return new HCPre ().addChild (aNode);
  }

  @Nonnull
  default HCPre pre (@Nullable final String s)
  {
    return new HCPre ().addChild (s);
  }

  @Nonnull
  default HCPre pre (@Nullable final Iterable <? extends IHCNode> aNodes)
  {
    return new HCPre ().addChildren (aNodes);
  }

  @Nonnull
  default HCSmall small ()
  {
    return new HCSmall ();
  }

  @Nonnull
  default HCSmall small (@Nullable final IHCNode aNode)
  {
    return new HCSmall ().addChild (aNode);
  }

  @Nonnull
  default HCSmall small (@Nullable final String s)
  {
    return new HCSmall ().addChild (s);
  }

  @Nonnull
  default HCSmall small (@Nullable final Iterable <? extends IHCNode> aNodes)
  {
    return new HCSmall ().addChildren (aNodes);
  }

  @Nonnull
  default HCSpan span ()
  {
    return new HCSpan ();
  }

  @Nonnull
  default HCSpan span (@Nullable final IHCNode aNode)
  {
    return new HCSpan ().addChild (aNode);
  }

  @Nonnull
  default HCSpan span (@Nullable final String s)
  {
    return new HCSpan ().addChild (s);
  }

  @Nonnull
  default HCSpan span (@Nullable final Iterable <? extends IHCNode> aNodes)
  {
    return new HCSpan ().addChildren (aNodes);
  }

  @Nonnull
  default HCStrong strong ()
  {
    return new HCStrong ();
  }

  @Nonnull
  default HCStrong strong (@Nullable final IHCNode aNode)
  {
    return new HCStrong ().addChild (aNode);
  }

  @Nonnull
  default HCStrong strong (@Nullable final String s)
  {
    return new HCStrong ().addChild (s);
  }

  @Nonnull
  default HCStrong strong (@Nullable final Iterable <? extends IHCNode> aNodes)
  {
    return new HCStrong ().addChildren (aNodes);
  }

  @Nonnull
  default HCSub sub ()
  {
    return new HCSub ();
  }

  @Nonnull
  default HCSub sub (@Nullable final IHCNode aNode)
  {
    return new HCSub ().addChild (aNode);
  }

  @Nonnull
  default HCSub sub (@Nullable final String s)
  {
    return new HCSub ().addChild (s);
  }

  @Nonnull
  default HCSub sub (@Nullable final Iterable <? extends IHCNode> aNodes)
  {
    return new HCSub ().addChildren (aNodes);
  }

  @Nonnull
  default HCSup sup ()
  {
    return new HCSup ();
  }

  @Nonnull
  default HCSup sup (@Nullable final IHCNode aNode)
  {
    return new HCSup ().addChild (aNode);
  }

  @Nonnull
  default HCSup sup (@Nullable final String s)
  {
    return new HCSup ().addChild (s);
  }

  @Nonnull
  default HCSup sup (@Nullable final Iterable <? extends IHCNode> aNodes)
  {
    return new HCSup ().addChildren (aNodes);
  }
}
