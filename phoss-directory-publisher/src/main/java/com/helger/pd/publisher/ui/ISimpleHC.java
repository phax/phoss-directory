package com.helger.pd.publisher.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.sections.HCH1;
import com.helger.html.hc.html.sections.HCH2;
import com.helger.html.hc.html.sections.HCH3;
import com.helger.html.hc.html.sections.HCH4;
import com.helger.html.hc.html.sections.HCH5;
import com.helger.html.hc.html.sections.HCH6;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.html.textlevel.HCSpan;
import com.helger.photon.bootstrap4.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap4.alert.BootstrapInfoBox;
import com.helger.photon.bootstrap4.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap4.alert.BootstrapWarnBox;

/**
 * Traits interface to add simpler UI codes.
 *
 * @author Philip Helger
 */
public interface ISimpleHC
{
  @Nonnull
  default HCCode code (@Nullable final String s)
  {
    return new HCCode ().addChild (s);
  }

  @Nonnull
  default HCDiv div (@Nullable final String s)
  {
    return new HCDiv ().addChild (s);
  }

  @Nonnull
  default HCH1 h1 (@Nullable final String s)
  {
    return new HCH1 ().addChild (s);
  }

  @Nonnull
  default HCH2 h2 (@Nullable final String s)
  {
    return new HCH2 ().addChild (s);
  }

  @Nonnull
  default HCH3 h3 (@Nullable final String s)
  {
    return new HCH3 ().addChild (s);
  }

  @Nonnull
  default HCH4 h4 (@Nullable final String s)
  {
    return new HCH4 ().addChild (s);
  }

  @Nonnull
  default HCH5 h5 (@Nullable final String s)
  {
    return new HCH5 ().addChild (s);
  }

  @Nonnull
  default HCH6 h6 (@Nullable final String s)
  {
    return new HCH6 ().addChild (s);
  }

  @Nonnull
  default HCSpan span (@Nullable final String s)
  {
    return new HCSpan ().addChild (s);
  }

  @Nonnull
  default BootstrapErrorBox error (@Nullable final String s)
  {
    return new BootstrapErrorBox ().addChild (s);
  }

  @Nonnull
  default BootstrapInfoBox info (@Nullable final String s)
  {
    return new BootstrapInfoBox ().addChild (s);
  }

  @Nonnull
  default BootstrapSuccessBox success (@Nullable final String s)
  {
    return new BootstrapSuccessBox ().addChild (s);
  }

  @Nonnull
  default BootstrapWarnBox warn (@Nullable final String s)
  {
    return new BootstrapWarnBox ().addChild (s);
  }
}
