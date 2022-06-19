/*
 * Copyright (C) 2015-2022 Philip Helger (www.helger.com)
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
import java.time.LocalDate;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.datetime.PDTWebDateHelper;
import com.helger.commons.regex.RegExHelper;
import com.helger.pd.indexer.lucene.ILuceneAnalyzerProvider;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.storage.field.PDField;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;

/**
 * Peppol Directory Lucene Query manager
 *
 * @author Philip Helger
 */
@Immutable
public final class PDQueryManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDQueryManager.class);

  private PDQueryManager ()
  {}

  /**
   * Split a user provided query string into the terms relevant for querying
   * using the provided Lucene Analyzer. This will e.g. remove ":" from a word
   * etc.
   *
   * @param aAnalyzerProvider
   *        Analyzer provider. E.g. instance of
   *        {@link com.helger.pd.indexer.lucene.PDLucene}.
   * @param sFieldName
   *        Lucene field name to get split into terms.
   * @param sQueryString
   *        The user provided query string. Must neither be <code>null</code>
   *        nor empty.
   * @return The non-<code>null</code> list of all terms.
   */
  @Nonnull
  public static ICommonsList <String> getSplitIntoTerms (@Nonnull final ILuceneAnalyzerProvider aAnalyzerProvider,
                                                         @Nonnull @Nonempty final String sFieldName,
                                                         @Nonnull @Nonempty final String sQueryString)
  {
    // Use the default analyzer to split the query string into fields
    try (final TokenStream aTokenStream = aAnalyzerProvider.getAnalyzer ().tokenStream (sFieldName, sQueryString))
    {
      final ICommonsList <String> ret = new CommonsArrayList <> ();
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
      LOGGER.warn ("Failed to split user query '" + sQueryString + "' into terms. Defaulting to regEx splitting", ex);
      // Fall-back
      return RegExHelper.getSplitToList (sQueryString.trim (), "\\s+");
    }
  }

  @Nonnull
  private static Query _createSimpleAllFieldsQuery (@Nonnull final String sFieldName, @Nonnull final String sQueryText)
  {
    if (false)
      return new TermQuery (new Term (sFieldName, sQueryText));

    // This works -> text ==> *text*
    return new WildcardQuery (new Term (sFieldName, "*" + sQueryText + "*"));
  }

  /**
   * Convert a query string as entered by the used into a Lucene query. This
   * methods uses
   * {@link #getSplitIntoTerms(ILuceneAnalyzerProvider, String, String)} to
   * split the provided string into pieces and returns a boolean query that
   * includes all terms (like an AND query).
   *
   * @param aAnalyzerProvider
   *        Lucene Analyzer provider
   * @param sFieldName
   *        The field name to query. May neither be <code>null</code> nor empty.
   * @param sQueryString
   *        The query string. May not be <code>null</code> and not be empty and
   *        may not be whitespace only.
   * @return The created Lucene {@link Query}
   */
  @Nonnull
  public static Query convertQueryStringToLuceneQuery (@Nonnull final ILuceneAnalyzerProvider aAnalyzerProvider,
                                                       @Nonnull final String sFieldName,
                                                       @Nonnull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");

    // Split into terms
    final ICommonsList <String> aParts = getSplitIntoTerms (aAnalyzerProvider, sFieldName, sQueryString);
    assert aParts.isNotEmpty ();

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Split query string: '" + sQueryString + "' for field '" + sFieldName + "' ==> " + aParts);

    final Query aQuery;
    if (aParts.size () == 1)
    {
      // Single term - simple query
      aQuery = _createSimpleAllFieldsQuery (sFieldName, aParts.get (0));
    }
    else
    {
      // All parts must be matched
      final BooleanQuery.Builder aBuilder = new BooleanQuery.Builder ();
      for (final String sPart : aParts)
        aBuilder.add (_createSimpleAllFieldsQuery (sFieldName, sPart), Occur.FILTER);
      aQuery = aBuilder.build ();
    }

    return aQuery;
  }

  @Nonnull
  private static String _lowerCase (@Nonnull final String s)
  {
    return s.toLowerCase (Locale.US);
  }

  @Nonnull
  private static String _upperCase (@Nonnull final String s)
  {
    return s.toUpperCase (Locale.US);
  }

  @Nullable
  public static Query getParticipantIDLuceneQuery (@Nonnull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");

    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();
    final IParticipantIdentifier aPI = aIdentifierFactory.parseParticipantIdentifier (_lowerCase (sQueryString));
    if (aPI == null)
    {
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn ("Failed to convert '" + sQueryString + "' to participant ID!");
      return null;
    }

    return new TermQuery (PDField.PARTICIPANT_ID.getExactMatchTerm (aPI));
  }

  @Nullable
  public static Query getNameLuceneQuery (@Nonnull final ILuceneAnalyzerProvider aAnalyzerProvider,
                                          @Nonnull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");

    if (sQueryString.length () < 3)
    {
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn ("Name query string '" + sQueryString + "' is too short!");
      return null;
    }

    // Query both fields in parallel
    final Query q1 = convertQueryStringToLuceneQuery (aAnalyzerProvider, PDField.NAME.getFieldName (), sQueryString);
    final Query q2 = convertQueryStringToLuceneQuery (aAnalyzerProvider, PDField.ML_NAME.getFieldName (), sQueryString);

    final BooleanQuery.Builder aBuilder = new BooleanQuery.Builder ();
    aBuilder.add (q1, Occur.FILTER);
    aBuilder.add (q2, Occur.FILTER);
    return aBuilder.build ();
  }

  @Nullable
  public static Query getCountryCodeLuceneQuery (@Nonnull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");

    return new TermQuery (PDField.COUNTRY_CODE.getExactMatchTerm (_upperCase (sQueryString)));
  }

  @Nullable
  public static Query getGeoInfoLuceneQuery (@Nonnull final ILuceneAnalyzerProvider aAnalyzerProvider,
                                             @Nonnull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");

    if (sQueryString.length () < 3)
    {
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn ("GeoInfo query string '" + sQueryString + "' is too short!");
      return null;
    }

    // Split into pieces
    return convertQueryStringToLuceneQuery (aAnalyzerProvider, PDField.GEO_INFO.getFieldName (), sQueryString);
  }

  @Nullable
  public static Query getIdentifierSchemeLuceneQuery (@Nonnull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");

    return new TermQuery (PDField.IDENTIFIER_SCHEME.getExactMatchTerm (_lowerCase (sQueryString)));
  }

  @Nullable
  public static Query getIdentifierValueLuceneQuery (@Nonnull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");

    return new TermQuery (PDField.IDENTIFIER_VALUE.getExactMatchTerm (_lowerCase (sQueryString)));
  }

  @Nullable
  public static Query getWebsiteLuceneQuery (@Nonnull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");

    if (sQueryString.length () < 3)
    {
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn ("Website query string '" + sQueryString + "' is too short!");
      return null;
    }

    return new WildcardQuery (PDField.WEBSITE_URI.getContainsTerm (_lowerCase (sQueryString)));
  }

  @Nullable
  public static Query getContactLuceneQuery (@Nonnull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");

    if (sQueryString.length () < 3)
    {
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn ("Contact query string '" + sQueryString + "' is too short!");
      return null;
    }

    final Query aQuery1 = new WildcardQuery (PDField.CONTACT_TYPE.getContainsTerm (_lowerCase (sQueryString)));
    final Query aQuery2 = new WildcardQuery (PDField.CONTACT_NAME.getContainsTerm (_lowerCase (sQueryString)));
    final Query aQuery3 = new WildcardQuery (PDField.CONTACT_PHONE.getContainsTerm (_lowerCase (sQueryString)));
    final Query aQuery4 = new WildcardQuery (PDField.CONTACT_EMAIL.getContainsTerm (_lowerCase (sQueryString)));
    return new BooleanQuery.Builder ().add (aQuery1, Occur.SHOULD)
                                      .add (aQuery2, Occur.SHOULD)
                                      .add (aQuery3, Occur.SHOULD)
                                      .add (aQuery4, Occur.SHOULD)
                                      .build ();
  }

  @Nullable
  public static Query getAdditionalInformationLuceneQuery (@Nonnull final ILuceneAnalyzerProvider aAnalyzerProvider,
                                                           @Nonnull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");

    if (sQueryString.length () < 3)
    {
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn ("AdditionalInformation query string '" + sQueryString + "' is too short!");
      return null;
    }

    // Split into pieces
    return convertQueryStringToLuceneQuery (aAnalyzerProvider, PDField.ADDITIONAL_INFO.getFieldName (), sQueryString);
  }

  @Nullable
  public static Query getRegistrationDateLuceneQuery (@Nonnull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");

    final LocalDate aLD = PDTWebDateHelper.getLocalDateFromXSD (sQueryString);
    if (aLD == null)
    {
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn ("Registration date '" + sQueryString + "' is invalid!");
      return null;
    }

    return new TermQuery (PDField.REGISTRATION_DATE.getExactMatchTerm (sQueryString));
  }

  @Nullable
  public static Query getDocumentTypeIDLuceneQuery (@Nonnull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");

    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();
    // No casing here!
    final IDocumentTypeIdentifier aDTI = aIdentifierFactory.parseDocumentTypeIdentifier (sQueryString);
    if (aDTI == null)
    {
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn ("Failed to convert '" + sQueryString + "' to document type ID!");
      return null;
    }

    return new TermQuery (PDField.DOCTYPE_ID.getExactMatchTerm (aDTI));
  }
}
