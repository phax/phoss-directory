package com.helger.pd.publisher.search;

import java.io.Serializable;

@FunctionalInterface
public interface IPDStringMatcher extends Serializable
{
  boolean matches (String s1, String s2);
}
