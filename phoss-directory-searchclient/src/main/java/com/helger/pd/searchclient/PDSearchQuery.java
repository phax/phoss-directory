/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.pd.searchclient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.pd.searchapi.CPDSearchAPI;
import com.helger.pd.searchapi.EPDSearchAPIField;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * A single query against the PD search REST API. Collects the query terms per search field plus the
 * paging parameters, and is turned into a URL query string by {@link #getAsQueryString()}.<br>
 * At least one query term is required - the server rejects a query without one with HTTP 400. Use
 * {@link #hasQueryTerms()} to check before sending.
 *
 * @author Philip Helger
 * @since 0.19.1
 */
@NotThreadSafe
public class PDSearchQuery
{
  private final ICommonsOrderedMap <EPDSearchAPIField, ICommonsList <String>> m_aQueryTerms = new CommonsLinkedHashMap <> ();
  private int m_nResultPageIndex = CPDSearchAPI.DEFAULT_RESULT_PAGE_INDEX;
  private int m_nResultPageCount = CPDSearchAPI.DEFAULT_RESULT_PAGE_COUNT;

  public PDSearchQuery ()
  {}

  /**
   * Add a query term for a specific search field. A field may carry more than one value, in which
   * case all of them are sent as repeated query parameters.
   *
   * @param eField
   *        The search field to query. May not be <code>null</code>.
   * @param sValue
   *        The value to search for. May neither be <code>null</code> nor empty.
   * @return this for chaining
   */
  @NonNull
  public PDSearchQuery addQueryTerm (@NonNull final EPDSearchAPIField eField, @NonNull @Nonempty final String sValue)
  {
    ValueEnforcer.notNull (eField, "Field");
    ValueEnforcer.notEmpty (sValue, "Value");

    m_aQueryTerms.computeIfAbsent (eField, k -> new CommonsArrayList <> ()).add (sValue);
    return this;
  }

  /**
   * Add a generic full text query term.
   *
   * @param sValue
   *        The value to search for. May neither be <code>null</code> nor empty.
   * @return this for chaining
   */
  @NonNull
  public PDSearchQuery generic (@NonNull @Nonempty final String sValue)
  {
    return addQueryTerm (EPDSearchAPIField.GENERIC, sValue);
  }

  /**
   * Add a participant identifier query term.
   *
   * @param aParticipantID
   *        The participant identifier to search for. May not be <code>null</code>.
   * @return this for chaining
   */
  @NonNull
  public PDSearchQuery participantID (@NonNull final IParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    return addQueryTerm (EPDSearchAPIField.PARTICIPANT_ID, aParticipantID.getURIEncoded ());
  }

  /**
   * Add a document type identifier query term.
   *
   * @param aDocTypeID
   *        The document type identifier to search for. May not be <code>null</code>.
   * @return this for chaining
   */
  @NonNull
  public PDSearchQuery documentTypeID (@NonNull final IDocumentTypeIdentifier aDocTypeID)
  {
    ValueEnforcer.notNull (aDocTypeID, "DocTypeID");
    return addQueryTerm (EPDSearchAPIField.DOCUMENT_TYPE, aDocTypeID.getURIEncoded ());
  }

  /**
   * Add a business entity name query term.
   *
   * @param sName
   *        The name to search for. May neither be <code>null</code> nor empty.
   * @return this for chaining
   */
  @NonNull
  public PDSearchQuery name (@NonNull @Nonempty final String sName)
  {
    return addQueryTerm (EPDSearchAPIField.NAME, sName);
  }

  /**
   * Add a country code query term.
   *
   * @param sCountryCode
   *        The country code to search for. May neither be <code>null</code> nor empty.
   * @return this for chaining
   */
  @NonNull
  public PDSearchQuery countryCode (@NonNull @Nonempty final String sCountryCode)
  {
    return addQueryTerm (EPDSearchAPIField.COUNTRY, sCountryCode);
  }

  /**
   * @return A copy of all query terms per search field. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsOrderedMap <EPDSearchAPIField, ICommonsList <String>> getAllQueryTerms ()
  {
    final ICommonsOrderedMap <EPDSearchAPIField, ICommonsList <String>> ret = new CommonsLinkedHashMap <> ();
    for (final Map.Entry <EPDSearchAPIField, ICommonsList <String>> aEntry : m_aQueryTerms.entrySet ())
      ret.put (aEntry.getKey (), aEntry.getValue ().getClone ());
    return ret;
  }

  /**
   * @return <code>true</code> if at least one query term is present, <code>false</code> otherwise.
   *         The server rejects a query without any term with HTTP 400.
   */
  public boolean hasQueryTerms ()
  {
    return m_aQueryTerms.isNotEmpty ();
  }

  /**
   * @return The 0-based index of the result page to be retrieved. Always &ge; 0.
   */
  @Nonnegative
  public int getResultPageIndex ()
  {
    return m_nResultPageIndex;
  }

  /**
   * Set the 0-based index of the result page to be retrieved.
   *
   * @param nResultPageIndex
   *        The page index. Must be &ge; 0.
   * @return this for chaining
   */
  @NonNull
  public PDSearchQuery setResultPageIndex (@Nonnegative final int nResultPageIndex)
  {
    ValueEnforcer.isGE0 (nResultPageIndex, "ResultPageIndex");
    m_nResultPageIndex = nResultPageIndex;
    return this;
  }

  /**
   * @return The number of results per page. Always &gt; 0.
   */
  @Nonnegative
  public int getResultPageCount ()
  {
    return m_nResultPageCount;
  }

  /**
   * Set the number of results per page.
   *
   * @param nResultPageCount
   *        The number of results per page. Must be &gt; 0.
   * @return this for chaining
   */
  @NonNull
  public PDSearchQuery setResultPageCount (@Nonnegative final int nResultPageCount)
  {
    ValueEnforcer.isGT0 (nResultPageCount, "ResultPageCount");
    m_nResultPageCount = nResultPageCount;
    return this;
  }

  /**
   * @return The index of the first result this query asks for. Always &ge; 0.
   */
  @Nonnegative
  public int getFirstResultIndex ()
  {
    return m_nResultPageIndex * m_nResultPageCount;
  }

  /**
   * @return <code>true</code> if the requested page lies beyond {@link CPDSearchAPI#MAX_RESULTS}, in
   *         which case the server answers with HTTP 400.
   */
  public boolean isBeyondMaxResults ()
  {
    return getFirstResultIndex () > CPDSearchAPI.MAX_RESULTS ||
           (m_nResultPageIndex + 1) * m_nResultPageCount - 1 > CPDSearchAPI.MAX_RESULTS;
  }

  /**
   * @return The URL query string for this query, without a leading "?". All values are URL encoded.
   *         Never <code>null</code>.
   */
  @NonNull
  public String getAsQueryString ()
  {
    final StringBuilder aSB = new StringBuilder ();
    for (final Map.Entry <EPDSearchAPIField, ICommonsList <String>> aEntry : m_aQueryTerms.entrySet ())
      for (final String sValue : aEntry.getValue ())
      {
        if (aSB.length () > 0)
          aSB.append ('&');
        aSB.append (aEntry.getKey ().getFieldName ())
           .append ('=')
           .append (URLEncoder.encode (sValue, StandardCharsets.UTF_8));
      }

    if (m_nResultPageIndex != CPDSearchAPI.DEFAULT_RESULT_PAGE_INDEX)
    {
      if (aSB.length () > 0)
        aSB.append ('&');
      aSB.append (CPDSearchAPI.QUERY_PARAM_RESULT_PAGE_INDEX).append ('=').append (m_nResultPageIndex);
    }

    if (m_nResultPageCount != CPDSearchAPI.DEFAULT_RESULT_PAGE_COUNT)
    {
      if (aSB.length () > 0)
        aSB.append ('&');
      aSB.append (CPDSearchAPI.QUERY_PARAM_RESULT_PAGE_COUNT).append ('=').append (m_nResultPageCount);
    }

    return aSB.toString ();
  }

  /**
   * @return A copy of this query. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public PDSearchQuery getClone ()
  {
    final PDSearchQuery ret = new PDSearchQuery ();
    for (final Map.Entry <EPDSearchAPIField, ICommonsList <String>> aEntry : m_aQueryTerms.entrySet ())
      ret.m_aQueryTerms.put (aEntry.getKey (), aEntry.getValue ().getClone ());
    ret.m_nResultPageIndex = m_nResultPageIndex;
    ret.m_nResultPageCount = m_nResultPageCount;
    return ret;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("QueryTerms", m_aQueryTerms)
                                       .append ("ResultPageIndex", m_nResultPageIndex)
                                       .append ("ResultPageCount", m_nResultPageCount)
                                       .getToString ();
  }

  /**
   * Create a query for a generic full text search.
   *
   * @param sValue
   *        The value to search for. May neither be <code>null</code> nor empty.
   * @return A new query. Never <code>null</code>.
   */
  @NonNull
  public static PDSearchQuery createGeneric (@NonNull @Nonempty final String sValue)
  {
    return new PDSearchQuery ().generic (sValue);
  }

  /**
   * Create a query for a single participant identifier.
   *
   * @param aParticipantID
   *        The participant identifier to search for. May not be <code>null</code>.
   * @return A new query. Never <code>null</code>.
   */
  @NonNull
  public static PDSearchQuery createForParticipant (@NonNull final IParticipantIdentifier aParticipantID)
  {
    return new PDSearchQuery ().participantID (aParticipantID);
  }

  /**
   * Parse a query string of the form "field=value&amp;field=value", as it is returned in the
   * "query-terms" attribute of a result list. Unknown field names are ignored.
   *
   * @param sQueryTerms
   *        The query terms string to parse. May be <code>null</code>.
   * @return A new query. Never <code>null</code>, but may carry no query term at all.
   */
  @NonNull
  public static PDSearchQuery parseQueryTerms (@Nullable final String sQueryTerms)
  {
    final PDSearchQuery ret = new PDSearchQuery ();
    if (StringHelper.isNotEmpty (sQueryTerms))
      for (final String sPair : StringHelper.getExploded ('&', sQueryTerms))
      {
        final int nIndex = sPair.indexOf ('=');
        if (nIndex > 0)
        {
          final EPDSearchAPIField eField = EPDSearchAPIField.getFromIDOrNull (sPair.substring (0, nIndex));
          final String sValue = sPair.substring (nIndex + 1);
          if (eField != null && StringHelper.isNotEmpty (sValue))
            ret.addQueryTerm (eField, sValue);
        }
      }
    return ret;
  }
}
