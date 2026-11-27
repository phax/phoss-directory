/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.cache.regex.RegExHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.datetime.web.PDTWebDateHelper;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.searchindex.IPDIndex;
import com.helger.pd.indexer.searchindex.query.EPDIndexQueryOccur;
import com.helger.pd.indexer.searchindex.query.IPDIndexQuery;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryBool;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryContains;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryPrefix;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryTerm;
import com.helger.pd.indexer.storage.field.PDField;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;

import jakarta.annotation.Nullable;

/**
 * Peppol Directory search index query manager
 *
 * @author Philip Helger
 */
@Immutable
public final class PDQueryManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDQueryManager.class);

  /**
   * Country codes that are used synonymously to each other, because a Business Card may use a
   * country code that differs from the ISO 3166-1 alpha-2 code of the respective code list. Each
   * contained country code is mapped onto all the country codes of its group - including itself.
   */
  private static final ICommonsMap <String, ICommonsList <String>> COUNTRY_CODE_SYNONYMS = new CommonsHashMap <> ();

  static
  {
    // "UK" is frequently used instead of the ISO 3166-1 alpha-2 code "GB" of the United Kingdom
    final ICommonsList <String> aUnitedKingdom = new CommonsArrayList <> ("GB", "UK");
    COUNTRY_CODE_SYNONYMS.put ("GB", aUnitedKingdom);
    COUNTRY_CODE_SYNONYMS.put ("UK", aUnitedKingdom);

    // "EL" is the EU VAT prefix of Greece, whose ISO 3166-1 alpha-2 code is "GR"
    final ICommonsList <String> aGreece = new CommonsArrayList <> ("GR", "EL");
    COUNTRY_CODE_SYNONYMS.put ("GR", aGreece);
    COUNTRY_CODE_SYNONYMS.put ("EL", aGreece);
  }

  private PDQueryManager ()
  {}

  /**
   * Split a user provided query string into the terms relevant for querying using the rules of the
   * provided index. This will e.g. remove ":" from a word etc.
   *
   * @param aIndex
   *        The search index to be used. May not be <code>null</code>.
   * @param sFieldName
   *        Index field name to get split into terms.
   * @param sQueryString
   *        The user provided query string. Must neither be <code>null</code> nor empty.
   * @return The non-<code>null</code> list of all terms.
   */
  @NonNull
  public static ICommonsList <String> getSplitIntoTerms (@NonNull final IPDIndex aIndex,
                                                         @NonNull @Nonempty final String sFieldName,
                                                         @NonNull @Nonempty final String sQueryString)
  {
    try
    {
      return aIndex.getSplitIntoTerms (sFieldName, sQueryString);
    }
    catch (final IOException ex)
    {
      LOGGER.warn ("Failed to split user query '" + sQueryString + "' into terms. Defaulting to regEx splitting", ex);
      // Fall-back
      return RegExHelper.getSplitToList (sQueryString.trim (), "\\s+");
    }
  }

  @NonNull
  private static IPDIndexQuery _createSimpleAllFieldsQuery (@NonNull final String sFieldName,
                                                            @NonNull final String sQueryText)
  {
    if (false)
      return new PDIndexQueryTerm (sFieldName, sQueryText);

    // This works -> text ==> *text*
    return new PDIndexQueryContains (sFieldName, sQueryText);
  }

  /**
   * Create the query that determines which documents match, based on the already split terms of the
   * user query. All terms must be matched (like an AND query).
   *
   * @param sFieldName
   *        The field name to query. May neither be <code>null</code> nor empty.
   * @param aParts
   *        The terms of the user query. May not be <code>null</code>.
   * @return The created {@link IPDIndexQuery}
   */
  @NonNull
  private static IPDIndexQuery _createContainsQuery (@NonNull @Nonempty final String sFieldName,
                                                     @NonNull final List <String> aParts)
  {
    if (aParts.size () == 1)
    {
      // Single term - simple query
      return _createSimpleAllFieldsQuery (sFieldName, aParts.get (0));
    }

    // All parts must be matched
    final PDIndexQueryBool.Builder aBuilder = new PDIndexQueryBool.Builder ();
    for (final String sPart : aParts)
      aBuilder.add (_createSimpleAllFieldsQuery (sFieldName, sPart), EPDIndexQueryOccur.FILTER);
    return aBuilder.build ();
  }

  /**
   * Convert a query string as entered by the used into an index query. This methods uses
   * {@link #getSplitIntoTerms(IPDIndex, String, String)} to split the provided string into pieces
   * and returns a boolean query that includes all terms (like an AND query).
   *
   * @param aIndex
   *        The search index to be used. May not be <code>null</code>.
   * @param sFieldName
   *        The field name to query. May neither be <code>null</code> nor empty.
   * @param sQueryString
   *        The query string. May not be <code>null</code> and not be empty and may not be
   *        whitespace only.
   * @return The created {@link IPDIndexQuery}
   */
  @NonNull
  public static IPDIndexQuery convertQueryStringToQuery (@NonNull final IPDIndex aIndex,
                                                         @NonNull final String sFieldName,
                                                         @NonNull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");

    // Split into terms
    final ICommonsList <String> aParts = getSplitIntoTerms (aIndex, sFieldName, sQueryString);
    assert aParts.isNotEmpty ();

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Split query string: '" + sQueryString + "' for field '" + sFieldName + "' ==> " + aParts);

    return _createContainsQuery (sFieldName, aParts);
  }

  @NonNull
  private static String _lowerCase (@NonNull final String s)
  {
    return s.toLowerCase (Locale.US);
  }

  @NonNull
  private static String _upperCase (@NonNull final String s)
  {
    return s.toUpperCase (Locale.US);
  }

  @Nullable
  public static IPDIndexQuery getParticipantIDQuery (@NonNull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");

    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();
    final IParticipantIdentifier aPI = aIdentifierFactory.parseParticipantIdentifier (_lowerCase (sQueryString));
    if (aPI == null)
    {
      LOGGER.warn ("Failed to convert '" + sQueryString + "' to participant ID!");
      return null;
    }
    return PDField.PARTICIPANT_ID.getExactMatchQuery (aPI);
  }

  /**
   * Create the queries that improve the relevance ordering of a search. They never change the set
   * of matching documents, because they are only added as optional clauses next to a mandatory
   * clause. A field in which a word is exactly the search term is ranked higher than a field in
   * which a word only starts with the search term, which in turn is ranked higher than a field that
   * merely contains the search term somewhere inside a word. See
   * https://github.com/phax/phoss-directory/issues/49
   *
   * @param sFieldName
   *        The field name to query. May neither be <code>null</code> nor empty.
   * @param aParts
   *        The terms of the user query. May not be <code>null</code>.
   * @return A list with two queries per provided term. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  private static ICommonsList <IPDIndexQuery> _createRelevanceQueries (@NonNull @Nonempty final String sFieldName,
                                                                       @NonNull final List <String> aParts)
  {
    final ICommonsList <IPDIndexQuery> ret = new CommonsArrayList <> ();
    for (final String sPart : aParts)
    {
      // A word of the field is exactly the search term
      ret.add (new PDIndexQueryTerm (sFieldName, sPart));
      // A word of the field starts with the search term
      ret.add (new PDIndexQueryPrefix (sFieldName, sPart));
    }
    return ret;
  }

  /**
   * A participant identifier following POLICY 1 of the "Peppol Policy for use of Identifiers":
   * <ul>
   * <li>Optionally prefixed with the only participant identifier scheme
   * <code>iso6523-actorid-upis</code> and the <code>::</code> separator (case insensitive)</li>
   * <li>A numeric identifier scheme (ICD) of exactly 4 digits, followed by a colon</li>
   * <li>The identifier itself, 1 to 130 characters, only letters (a-z, A-Z), digits (0-9), minus
   * (-), period (.), underscore (_) and tilde (~)</li>
   * </ul>
   * Examples: <code>0208:0123456789</code> and <code>iso6523-actorid-upis::0208:0123456789</code>.
   * Only a generic query string that completely matches this expression is looked up as a
   * participant identifier.
   */
  public static final String REGEX_PARTICIPANT_ID = "^(?:(?i:iso6523-actorid-upis)::)?([0-9]{4}):([A-Za-z0-9\\-._~]{1,130})$";
  private static final Pattern PATTERN_PARTICIPANT_ID = Pattern.compile (REGEX_PARTICIPANT_ID);

  /**
   * Create the query of the generic search field for a query string that is a complete participant
   * identifier. Without this, such a query string is executed as a "contains" query on all fields,
   * which is a leading wildcard search over the whole index and therefore very expensive, while the
   * user clearly looks for one specific participant.
   * <p>
   * The returned query matches the participant with exactly this identifier, as well as all
   * business entities that list the identifier scheme and value as one of their identifiers. Its
   * results are therefore always a subset of the results of the "contains" query. It does not
   * match entries that merely contain the value inside a longer value (e.g. a participant
   * <code>9925:be0123456789</code> for the query <code>0208:0123456789</code>).
   *
   * @param aIdentifierFactory
   *        The identifier factory to use. May not be <code>null</code>.
   * @param sQueryString
   *        The query string. May not be <code>null</code>.
   * @return <code>null</code> if the query string does not match {@link #REGEX_PARTICIPANT_ID} or
   *         is rejected by the identifier factory, so that the regular generic query should be
   *         used.
   */
  @Nullable
  public static IPDIndexQuery getParticipantIDGenericQuery (@NonNull final IIdentifierFactory aIdentifierFactory,
                                                            @NonNull final String sQueryString)
  {
    ValueEnforcer.notNull (aIdentifierFactory, "IdentifierFactory");
    ValueEnforcer.notNull (sQueryString, "QueryString");

    final Matcher aMatcher = PATTERN_PARTICIPANT_ID.matcher (sQueryString.trim ());
    if (!aMatcher.matches ())
      return null;

    final String sICD = aMatcher.group (1);
    final String sValue = _lowerCase (aMatcher.group (2));

    // The expression only accepts the iso6523-actorid-upis scheme
    final IParticipantIdentifier aPI = aIdentifierFactory.createParticipantIdentifier (PeppolIdentifierHelper.PARTICIPANT_SCHEME_ISO6523_ACTORID_UPIS,
                                                                                       sICD + ':' + sValue);
    if (aPI == null)
      return null;

    // The business entity declares the identifier: scheme and value must both match
    final PDIndexQueryBool.Builder aIdentifierBuilder = new PDIndexQueryBool.Builder ();
    aIdentifierBuilder.add (PDField.IDENTIFIER_SCHEME.getExactMatchQuery (sICD), EPDIndexQueryOccur.FILTER);
    aIdentifierBuilder.add (PDField.IDENTIFIER_VALUE.getExactMatchQuery (sValue), EPDIndexQueryOccur.FILTER);

    // Either the participant itself, or a business entity declaring the identifier
    final PDIndexQueryBool.Builder aBuilder = new PDIndexQueryBool.Builder ();
    aBuilder.add (PDField.PARTICIPANT_ID.getExactMatchQuery (aPI), EPDIndexQueryOccur.SHOULD);
    aBuilder.add (aIdentifierBuilder.build (), EPDIndexQueryOccur.SHOULD);
    return aBuilder.build ();
  }

  /**
   * Create the query of the generic search field, that queries the "all fields" index field. The
   * matching itself is unchanged, but a match of a whole word is ranked higher than a match inside
   * a word, and a match in the name of the business entity is ranked higher than a match in any
   * other field.
   *
   * @param aIndex
   *        The search index to be used. May not be <code>null</code>.
   * @param sQueryString
   *        The query string. May not be <code>null</code> and not be empty and may not be
   *        whitespace only.
   * @return The created {@link IPDIndexQuery}
   */
  @NonNull
  public static IPDIndexQuery getGenericQuery (@NonNull final IPDIndex aIndex,
                                               @NonNull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");

    // A complete participant identifier is looked up exactly instead of with a leading wildcard
    final IPDIndexQuery aParticipantIDQuery = getParticipantIDGenericQuery (PDMetaManager.getIdentifierFactory (),
                                                                            sQueryString);
    if (aParticipantIDQuery != null)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Generic query string '" + sQueryString + "' is a participant identifier - using an exact match");
      return aParticipantIDQuery;
    }

    // Split into terms - once per field, because the splitting is field based by definition
    final ICommonsList <String> aAllParts = getSplitIntoTerms (aIndex, CPDStorage.FIELD_ALL_FIELDS, sQueryString);
    final ICommonsList <String> aNameParts = getSplitIntoTerms (aIndex, PDField.NAME.getFieldName (), sQueryString);
    final ICommonsList <String> aMLNameParts = getSplitIntoTerms (aIndex,
                                                                  PDField.ML_NAME.getFieldName (),
                                                                  sQueryString);

    final PDIndexQueryBool.Builder aBuilder = new PDIndexQueryBool.Builder ();
    // This clause decides what matches, but it deliberately does not contribute to the score
    aBuilder.add (_createContainsQuery (CPDStorage.FIELD_ALL_FIELDS, aAllParts), EPDIndexQueryOccur.FILTER);
    // Because of the mandatory clause above, the following clauses are optional and only improve
    // the relevance ordering of the results
    for (final IPDIndexQuery aQuery : _createRelevanceQueries (CPDStorage.FIELD_ALL_FIELDS, aAllParts))
      aBuilder.add (aQuery, EPDIndexQueryOccur.SHOULD);
    // A match in the name of the business entity is more relevant than a match in any other field
    for (final IPDIndexQuery aQuery : _createRelevanceQueries (PDField.NAME.getFieldName (), aNameParts))
      aBuilder.add (aQuery, EPDIndexQueryOccur.SHOULD);
    for (final IPDIndexQuery aQuery : _createRelevanceQueries (PDField.ML_NAME.getFieldName (), aMLNameParts))
      aBuilder.add (aQuery, EPDIndexQueryOccur.SHOULD);
    return aBuilder.build ();
  }

  @Nullable
  public static IPDIndexQuery getNameQuery (@NonNull final IPDIndex aIndex,
                                            @NonNull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");
    if (sQueryString.length () < 3)
    {
      LOGGER.warn ("Name query string '" + sQueryString + "' is too short!");
      return null;
    }

    // Split into terms - once per field, because the splitting is field based by definition
    final ICommonsList <String> aNameParts = getSplitIntoTerms (aIndex, PDField.NAME.getFieldName (), sQueryString);
    final ICommonsList <String> aMLNameParts = getSplitIntoTerms (aIndex,
                                                                  PDField.ML_NAME.getFieldName (),
                                                                  sQueryString);

    // Query both fields in parallel - one of both must match
    final PDIndexQueryBool.Builder aMatchBuilder = new PDIndexQueryBool.Builder ();
    aMatchBuilder.add (_createContainsQuery (PDField.NAME.getFieldName (), aNameParts), EPDIndexQueryOccur.SHOULD);
    aMatchBuilder.add (_createContainsQuery (PDField.ML_NAME.getFieldName (), aMLNameParts), EPDIndexQueryOccur.SHOULD);

    final PDIndexQueryBool.Builder aBuilder = new PDIndexQueryBool.Builder ();
    // This clause decides what matches, but it deliberately does not contribute to the score
    aBuilder.add (aMatchBuilder.build (), EPDIndexQueryOccur.FILTER);
    // Because of the mandatory clause above, the following clauses are optional and only improve
    // the relevance ordering of the results
    for (final IPDIndexQuery aQuery : _createRelevanceQueries (PDField.NAME.getFieldName (), aNameParts))
      aBuilder.add (aQuery, EPDIndexQueryOccur.SHOULD);
    for (final IPDIndexQuery aQuery : _createRelevanceQueries (PDField.ML_NAME.getFieldName (), aMLNameParts))
      aBuilder.add (aQuery, EPDIndexQueryOccur.SHOULD);
    return aBuilder.build ();
  }

  /**
   * Create the query of the country code search field. If the provided country code has synonyms,
   * the entries of all the synonymous country codes are matched, because a Business Card may use
   * any of them for the same country.
   *
   * @param sQueryString
   *        The query string. May not be <code>null</code> and not be empty and may not be
   *        whitespace only.
   * @return The created {@link IPDIndexQuery}
   */
  @Nullable
  public static IPDIndexQuery getCountryCodeQuery (@NonNull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");

    final String sCountryCode = _upperCase (sQueryString);
    final ICommonsList <String> aSynonyms = COUNTRY_CODE_SYNONYMS.get (sCountryCode);
    if (aSynonyms == null)
    {
      // The regular case - the country code has no synonyms
      return PDField.COUNTRY_CODE.getExactMatchQuery (sCountryCode);
    }

    // Any of the synonymous country codes may match
    final PDIndexQueryBool.Builder aBuilder = new PDIndexQueryBool.Builder ();
    for (final String sSynonym : aSynonyms)
      aBuilder.add (PDField.COUNTRY_CODE.getExactMatchQuery (sSynonym), EPDIndexQueryOccur.SHOULD);
    return aBuilder.build ();
  }

  @Nullable
  public static IPDIndexQuery getGeoInfoQuery (@NonNull final IPDIndex aIndex,
                                               @NonNull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");
    if (sQueryString.length () < 3)
    {
      LOGGER.warn ("GeoInfo query string '" + sQueryString + "' is too short!");
      return null;
    }

    // Split into pieces
    return convertQueryStringToQuery (aIndex, PDField.GEO_INFO.getFieldName (), sQueryString);
  }

  @Nullable
  public static IPDIndexQuery getIdentifierSchemeQuery (@NonNull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");

    return PDField.IDENTIFIER_SCHEME.getExactMatchQuery (_lowerCase (sQueryString));
  }

  @Nullable
  public static IPDIndexQuery getIdentifierValueQuery (@NonNull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");

    return PDField.IDENTIFIER_VALUE.getExactMatchQuery (_lowerCase (sQueryString));
  }

  @Nullable
  public static IPDIndexQuery getWebsiteQuery (@NonNull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");
    if (sQueryString.length () < 3)
    {
      LOGGER.warn ("Website query string '" + sQueryString + "' is too short!");
      return null;
    }
    return PDField.WEBSITE_URI.getContainsQuery (_lowerCase (sQueryString));
  }

  @Nullable
  public static IPDIndexQuery getContactQuery (@NonNull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");
    if (sQueryString.length () < 3)
    {
      LOGGER.warn ("Contact query string '" + sQueryString + "' is too short!");
      return null;
    }

    final IPDIndexQuery aQuery1 = PDField.CONTACT_TYPE.getContainsQuery (_lowerCase (sQueryString));
    final IPDIndexQuery aQuery2 = PDField.CONTACT_NAME.getContainsQuery (_lowerCase (sQueryString));
    final IPDIndexQuery aQuery3 = PDField.CONTACT_PHONE.getContainsQuery (_lowerCase (sQueryString));
    final IPDIndexQuery aQuery4 = PDField.CONTACT_EMAIL.getContainsQuery (_lowerCase (sQueryString));
    return new PDIndexQueryBool.Builder ().add (aQuery1, EPDIndexQueryOccur.SHOULD)
                                          .add (aQuery2, EPDIndexQueryOccur.SHOULD)
                                          .add (aQuery3, EPDIndexQueryOccur.SHOULD)
                                          .add (aQuery4, EPDIndexQueryOccur.SHOULD)
                                          .build ();
  }

  @Nullable
  public static IPDIndexQuery getAdditionalInformationQuery (@NonNull final IPDIndex aIndex,
                                                             @NonNull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");
    if (sQueryString.length () < 3)
    {
      LOGGER.warn ("AdditionalInformation query string '" + sQueryString + "' is too short!");
      return null;
    }
    // Split into pieces
    return convertQueryStringToQuery (aIndex, PDField.ADDITIONAL_INFO.getFieldName (), sQueryString);
  }

  @Nullable
  public static IPDIndexQuery getRegistrationDateQuery (@NonNull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");

    final LocalDate aLD = PDTWebDateHelper.getLocalDateFromXSD (sQueryString);
    if (aLD == null)
    {
      LOGGER.warn ("Registration date '" + sQueryString + "' is invalid!");
      return null;
    }

    return PDField.REGISTRATION_DATE.getExactMatchQuery (sQueryString);
  }

  @Nullable
  public static IPDIndexQuery getDocumentTypeIDQuery (@NonNull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");

    final IIdentifierFactory aIdentifierFactory = PDMetaManager.getIdentifierFactory ();
    // No casing here!
    final IDocumentTypeIdentifier aDTI = aIdentifierFactory.parseDocumentTypeIdentifier (sQueryString);
    if (aDTI == null)
    {
      LOGGER.warn ("Failed to convert '" + sQueryString + "' to document type ID!");
      return null;
    }

    return PDField.DOCTYPE_ID.getExactMatchQuery (aDTI);
  }
}
