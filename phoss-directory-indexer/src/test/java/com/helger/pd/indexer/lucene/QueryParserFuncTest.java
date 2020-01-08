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
package com.helger.pd.indexer.lucene;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParser.Operator;
import org.apache.lucene.search.Query;
import org.junit.Test;

public final class QueryParserFuncTest
{
  @Test
  public void test () throws ParseException
  {
    final QueryParser aQP = new QueryParser ("", PDLucene.createAnalyzer ());
    aQP.setDefaultOperator (Operator.AND);
    aQP.setAllowLeadingWildcard (true);
    final Query aQuery = aQP.parse ("(allfields:*9905* AND allfields:*leckma*) AND NOT deleted:(*)");
    System.out.println (aQuery.getClass () + " -- " + aQuery);
  }
}
