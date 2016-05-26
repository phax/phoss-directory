package com.helger.pd.indexer.lucene;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParser.Operator;
import org.apache.lucene.search.Query;
import org.junit.Test;

public class QueryParserFuncTest
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
