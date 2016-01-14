/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.regex.RegExHelper;
import com.helger.pd.indexer.lucene.ILuceneAnalyzerProvider;

/**
 * PYP Lucene Query manager
 *
 * @author Philip Helger
 */
@Immutable
public final class PDQueryManager
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PDQueryManager.class);

  private PDQueryManager ()
  {}

  /**
   * Surround the provided {@link Query} with a clause that forbids deleted
   * documents to be returned
   *
   * @param aQuery
   *        Source Query
   * @return {@link BooleanQuery} contained the "MUST_NOT" on the "deleted"
   *         field.
   */
  @Nonnull
  public static BooleanQuery andNotDeleted (@Nonnull final Query aQuery)
  {
    return new BooleanQuery.Builder ().add (aQuery, Occur.MUST)
                                      .add (new TermQuery (new Term (CPDStorage.FIELD_DELETED)), Occur.MUST_NOT)
                                      .build ();
  }

  /**
   * Split a user provided query string into the terms relevant for querying
   * using the provided Lucene Analyzer. This will e.g. remove ":" from a word
   * etc.
   *
   * @param aAnalyzerProvider
   *        Analyzer provider. E.g. instance of
   *        {@link com.helger.pd.indexer.lucene.PDLucene}.
   * @param sQueryString
   *        The user provided query string. Must neither be <code>null</code>
   *        nor empty.
   * @return The non-<code>null</code> list of all terms.
   */
  @Nonnull
  public static List <String> getSplitIntoTerms (@Nonnull final ILuceneAnalyzerProvider aAnalyzerProvider,
                                                 @Nonnull @Nonempty final String sQueryString)
  {
    // Use the default analyzer to split the query string into fields
    try (final TokenStream aTokenStream = aAnalyzerProvider.getAnalyzer ().tokenStream (CPDStorage.FIELD_ALL_FIELDS,
                                                                                        sQueryString))
    {
      final List <String> ret = new ArrayList <> ();
      final CharTermAttribute aCharTermAttribute = aTokenStream.addAttribute (CharTermAttribute.class);
      aTokenStream.reset ();
      while (aTokenStream.incrementToken ())
      {
        final String sTerm = aCharTermAttribute.toString ();
        ret.add (sTerm);
      }
      aTokenStream.end ();
      return ret;
    }
    catch (final IOException ex)
    {
      s_aLogger.warn ("Failed to split user query '" +
                      sQueryString +
                      "' into terms. Defaulting to regEx splitting",
                      ex);
      // Fall-back
      return RegExHelper.getSplitToList (sQueryString, "\\s+");
    }
  }

  private static Query _createSimpleAllFieldsQuery (@Nonnull final String sText)
  {
    return new WildcardQuery (new Term (CPDStorage.FIELD_ALL_FIELDS, "*" + sText + "*"));
  }

  /**
   * Convert a query string as entered by the used into a Lucene query.
   *
   * @param aAnalyzerProvider
   *        Lucene Analyzer provider
   * @param sQueryString
   *        The query string. May not be <code>null</code> and not be empty and
   *        may not be whitespace only.
   * @return The created Lucene {@link Query}
   */
  @Nonnull
  public static Query convertQueryStringToLuceneQuery (@Nonnull final ILuceneAnalyzerProvider aAnalyzerProvider,
                                                       @Nonnull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");

    // Split into terms
    final List <String> aParts = getSplitIntoTerms (aAnalyzerProvider, sQueryString);
    assert !aParts.isEmpty ();

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Split query string: '" + sQueryString + "' ==> " + aParts);

    Query aQuery;
    if (aParts.size () == 1)
    {
      // Single term - simple query
      aQuery = _createSimpleAllFieldsQuery (aParts.get (0));
    }
    else
    {
      // All parts must be matched
      final BooleanQuery.Builder aBuilder = new BooleanQuery.Builder ();
      for (final String sPart : aParts)
        aBuilder.add (_createSimpleAllFieldsQuery (sPart), Occur.MUST);
      aQuery = aBuilder.build ();
    }

    // Alter the query so that only not-deleted documents are returned
    return andNotDeleted (aQuery);
  }
}
