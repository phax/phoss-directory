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
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.cache.regex.RegExHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.web.PDTWebDateHelper;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.searchindex.IPDIndex;
import com.helger.pd.indexer.searchindex.query.EPDIndexQueryOccur;
import com.helger.pd.indexer.searchindex.query.IPDIndexQuery;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryBool;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryContains;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryTerm;
import com.helger.pd.indexer.storage.field.PDField;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;

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

  private PDQueryManager ()
  {}

  /**
   * Split a user provided query string into the terms relevant for querying
   * using the rules of the provided index. This will e.g. remove ":" from a
   * word etc.
   *
   * @param aIndex
   *        The search index to be used. May not be <code>null</code>.
   * @param sFieldName
   *        Index field name to get split into terms.
   * @param sQueryString
   *        The user provided query string. Must neither be <code>null</code>
   *        nor empty.
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
   * Convert a query string as entered by the used into an index query. This
   * methods uses {@link #getSplitIntoTerms(IPDIndex, String, String)} to split
   * the provided string into pieces and returns a boolean query that includes
   * all terms (like an AND query).
   *
   * @param aIndex
   *        The search index to be used. May not be <code>null</code>.
   * @param sFieldName
   *        The field name to query. May neither be <code>null</code> nor empty.
   * @param sQueryString
   *        The query string. May not be <code>null</code> and not be empty and
   *        may not be whitespace only.
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

    final IPDIndexQuery aQuery;
    if (aParts.size () == 1)
    {
      // Single term - simple query
      aQuery = _createSimpleAllFieldsQuery (sFieldName, aParts.get (0));
    }
    else
    {
      // All parts must be matched
      final PDIndexQueryBool.Builder aBuilder = new PDIndexQueryBool.Builder ();
      for (final String sPart : aParts)
        aBuilder.add (_createSimpleAllFieldsQuery (sFieldName, sPart), EPDIndexQueryOccur.FILTER);
      aQuery = aBuilder.build ();
    }
    return aQuery;
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
    
    // Query both fields in parallel
    final IPDIndexQuery q1 = convertQueryStringToQuery (aIndex, PDField.NAME.getFieldName (), sQueryString);
    final IPDIndexQuery q2 = convertQueryStringToQuery (aIndex, PDField.ML_NAME.getFieldName (), sQueryString);

    // One of both must match
    final PDIndexQueryBool.Builder aBuilder = new PDIndexQueryBool.Builder ();
    aBuilder.add (q1, EPDIndexQueryOccur.SHOULD);
    aBuilder.add (q2, EPDIndexQueryOccur.SHOULD);
    return aBuilder.build ();
  }

  @Nullable
  public static IPDIndexQuery getCountryCodeQuery (@NonNull @Nonempty final String sQueryString)
  {
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    ValueEnforcer.notEmpty (sQueryString.trim (), "QueryString trimmed");

    return PDField.COUNTRY_CODE.getExactMatchQuery (_upperCase (sQueryString));
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
