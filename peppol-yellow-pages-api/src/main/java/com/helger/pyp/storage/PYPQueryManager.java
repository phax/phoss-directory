package com.helger.pyp.storage;

import javax.annotation.Nonnull;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.regex.RegExHelper;

/**
 * PYP Lucene Query manager
 *
 * @author Philip Helger
 */
public final class PYPQueryManager
{
  private PYPQueryManager ()
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
                                      .add (new TermQuery (new Term (CPYPStorage.FIELD_DELETED)), Occur.MUST_NOT)
                                      .build ();
  }

  /**
   * Convert a query string as entered by the used into a Lucene query.
   *
   * @param sQueryString
   *        The query string. May not be <code>null</code> and not be empty and
   *        may not be whitespace only.
   * @return The created Lucene {@link Query}
   */
  @Nonnull
  public static Query convertQueryStringToLuceneQuery (@Nonnull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");

    final String [] aParts = RegExHelper.getSplitToArray (sQueryString, "\\s+");
    assert aParts.length > 0;
    Query aQuery;
    if (aParts.length == 1)
    {
      // Single term - simple query
      aQuery = new TermQuery (new Term (CPYPStorage.FIELD_ALL_FIELDS, aParts[0]));
    }
    else
    {
      // All parts must be matched
      final BooleanQuery.Builder aBuilder = new BooleanQuery.Builder ();
      for (final String sPart : aParts)
        aBuilder.add (new TermQuery (new Term (CPYPStorage.FIELD_ALL_FIELDS, sPart)), Occur.MUST);
      aQuery = aBuilder.build ();
    }

    // Alter the query so that only not-deleted documents are returned
    return andNotDeleted (aQuery);
  }
}
