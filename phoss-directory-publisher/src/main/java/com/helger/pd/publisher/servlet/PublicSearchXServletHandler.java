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
package com.helger.pd.publisher.servlet;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.validation.Validator;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTWebDateHelper;
import com.helger.commons.error.IError;
import com.helger.commons.http.CHttp;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.string.StringHelper;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.settings.PDServerConfiguration;
import com.helger.pd.indexer.storage.PDQueryManager;
import com.helger.pd.indexer.storage.PDStorageManager;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.pd.publisher.app.AppCommonUI;
import com.helger.pd.publisher.search.EPDOutputFormat;
import com.helger.pd.publisher.search.EPDSearchField;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestParamContainer;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.sax.CollectingSAXErrorHandler;
import com.helger.xml.schema.XMLSchemaCache;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.XMLWriterSettings;
import com.helger.xml.transform.TransformSourceFactory;
import com.helger.xservlet.handler.simple.IXServletSimpleHandler;

import es.moki.ratelimitj.core.limiter.request.RequestLimitRule;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;
import es.moki.ratelimitj.inmemory.request.InMemorySlidingWindowRequestRateLimiter;

/**
 * The REST search servlet. Handles only GET requests.
 *
 * @author Philip Helger
 */
public final class PublicSearchXServletHandler implements IXServletSimpleHandler
{
  public static final String PARAM_RESULT_PAGE_INDEX = "resultPageIndex";
  public static final String PARAM_RESULT_PAGE_COUNT = "resultPageCount";
  public static final String PARAM_BEAUTIFY = "beautify";
  public static final int DEFAULT_RESULT_PAGE_INDEX = 0;
  public static final int DEFAULT_RESULT_PAGE_COUNT = 20;
  public static final int MAX_RESULTS = 1_000;

  private static final String RESPONSE_VERSION = "version";
  private static final String RESPONSE_TOTAL_RESULT_COUNT = "total-result-count";
  private static final String RESPONSE_USED_RESULT_COUNT = "used-result-count";
  private static final String RESPONSE_RESULT_PAGE_INDEX = "result-page-index";
  private static final String RESPONSE_RESULT_PAGE_COUNT = "result-page-count";
  private static final String RESPONSE_FIRST_RESULT_INDEX = "first-result-index";
  private static final String RESPONSE_LAST_RESULT_INDEX = "last-result-index";
  private static final String RESPONSE_QUERY_TERMS = "query-terms";
  private static final String RESPONSE_CREATION_DT = "creation-dt";

  private static final Logger LOGGER = LoggerFactory.getLogger (PublicSearchXServletHandler.class);

  public enum ESearchVersion
  {
    V1 ("1.0");

    private final String m_sVersion;
    private final String m_sPathPrefix;

    private ESearchVersion (@Nonnull @Nonempty final String sVersion)
    {
      m_sVersion = sVersion;
      m_sPathPrefix = "/" + sVersion;
    }

    @Nonnull
    @Nonempty
    public String getVersion ()
    {
      return m_sVersion;
    }

    @Nonnull
    @Nonempty
    public String getPathPrefix ()
    {
      return m_sPathPrefix;
    }

    @Nullable
    public static ESearchVersion getFromPathInfoOrNull (@Nonnull final String sPathInfo)
    {
      return ArrayHelper.findFirst (values (), x -> sPathInfo.startsWith (x.m_sPathPrefix));
    }
  }

  private final RequestRateLimiter m_aRequestRateLimiter;

  public PublicSearchXServletHandler ()
  {
    final long nRequestsPerSec = PDServerConfiguration.getRESTAPIMaxRequestsPerSecond ();
    if (nRequestsPerSec > 0)
    {
      // 2 request per second, per key
      // Note: duration must be > 1 second
      m_aRequestRateLimiter = new InMemorySlidingWindowRequestRateLimiter (RequestLimitRule.of (Duration.ofSeconds (2),
                                                                                                nRequestsPerSec * 2));
      LOGGER.info ("Installed REST search rate limiter with a maximum of " + nRequestsPerSec + " requests per second");
    }
    else
    {
      m_aRequestRateLimiter = null;
      LOGGER.info ("REST search API runs without limit");
    }
  }

  public void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                             @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    if (m_aRequestRateLimiter != null)
    {
      final String sRateLimitKey = "ip:" + aRequestScope.getRemoteAddr ();
      final boolean bOverLimit = m_aRequestRateLimiter.overLimitWhenIncremented (sRateLimitKey);
      if (bOverLimit)
      {
        // Too Many Requests
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("REST search rate limit exceeded for " + sRateLimitKey);

        aUnifiedResponse.setStatus (CHttp.HTTP_TOO_MANY_REQUESTS);
        return;
      }
    }

    final IRequestParamContainer aParams = aRequestScope.params ();

    // http://127.0.0.1:8080/search -> null
    // http://127.0.0.1:8080/search/ -> "/"
    // http://127.0.0.1:8080/search/x -> "/x"
    final String sPathInfo = StringHelper.getNotNull (aRequestScope.getPathInfo (), "");
    final ESearchVersion eSearchVersion = ESearchVersion.getFromPathInfoOrNull (sPathInfo);

    if (eSearchVersion == ESearchVersion.V1)
    {
      // Version 1.0

      // Determine output format
      final ICommonsList <String> aParts = StringHelper.getExploded ('/', sPathInfo.substring (1));
      final String sFormat = aParts.getAtIndex (1);
      final EPDOutputFormat eOutputFormat = EPDOutputFormat.getFromIDCaseInsensitiveOrDefault (sFormat, EPDOutputFormat.XML);
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Using REST query API 1.0 with output format " +
                      eOutputFormat +
                      " (" +
                      sPathInfo +
                      ") from '" +
                      aRequestScope.getUserAgent ().getAsString () +
                      "'");

      // Determine result offset and count
      final int nResultPageIndex = aParams.getAsInt (PARAM_RESULT_PAGE_INDEX, aParams.getAsInt ("rpi", DEFAULT_RESULT_PAGE_INDEX));
      if (nResultPageIndex < 0)
      {
        LOGGER.error ("ResultPageIndex " + nResultPageIndex + " is invalid. It must be >= 0.");
        aUnifiedResponse.setStatus (CHttp.HTTP_BAD_REQUEST);
        return;
      }
      final int nResultPageCount = aParams.getAsInt (PARAM_RESULT_PAGE_COUNT, aParams.getAsInt ("rpc", DEFAULT_RESULT_PAGE_COUNT));
      if (nResultPageCount <= 0)
      {
        LOGGER.error ("ResultPageCount " + nResultPageCount + " is invalid. It must be > 0.");
        aUnifiedResponse.setStatus (CHttp.HTTP_BAD_REQUEST);
        return;
      }
      final int nFirstResultIndex = nResultPageIndex * nResultPageCount;
      final int nLastResultIndex = (nResultPageIndex + 1) * nResultPageCount - 1;
      if (nFirstResultIndex > MAX_RESULTS)
      {
        LOGGER.error ("The first result index " + nFirstResultIndex + " is invalid. It must be <= " + MAX_RESULTS + ".");
        aUnifiedResponse.setStatus (CHttp.HTTP_BAD_REQUEST);
        return;
      }
      if (nLastResultIndex > MAX_RESULTS)
      {
        LOGGER.error ("The last result index " + nLastResultIndex + " is invalid. It must be <= " + MAX_RESULTS + ".");
        aUnifiedResponse.setStatus (CHttp.HTTP_BAD_REQUEST);
        return;
      }

      // Format output?
      final boolean bBeautify = aParams.getAsBoolean (PARAM_BEAUTIFY, false);

      // Determine query terms
      final StringBuilder aSBQueryString = new StringBuilder ();
      final ICommonsMap <EPDSearchField, ICommonsList <String>> aQueryValues = new CommonsHashMap <> ();
      for (final EPDSearchField eSF : EPDSearchField.values ())
      {
        final String sFieldName = eSF.getFieldName ();
        // Check if one or more request parameters are present for the current
        // search field
        final ICommonsList <String> aValues = aParams.getAsStringList (sFieldName);
        if (aValues != null && aValues.isNotEmpty ())
        {
          aQueryValues.put (eSF, aValues);
          for (final String sValue : aValues)
          {
            if (aSBQueryString.length () > 0)
              aSBQueryString.append ('&');
            aSBQueryString.append (sFieldName).append ('=').append (sValue);
          }
        }
      }
      if (aQueryValues.isEmpty ())
      {
        LOGGER.error ("No valid query term provided!");
        aUnifiedResponse.setStatus (CHttp.HTTP_BAD_REQUEST);
        return;
      }
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Using the following query terms: " + aQueryValues);

      final ICommonsList <Query> aQueries = new CommonsArrayList <> ();
      for (final Map.Entry <EPDSearchField, ICommonsList <String>> aEntry : aQueryValues.entrySet ())
      {
        final EPDSearchField eField = aEntry.getKey ();
        for (final String sQuery : aEntry.getValue ())
        {
          final Query aQuery = eField.getQuery (sQuery);
          if (aQuery != null)
            aQueries.add (aQuery);
          else
            LOGGER.error ("Failed to create query '" + sQuery + "' of field " + eField + " - ignoring term!");
        }
      }
      if (aQueries.isEmpty ())
      {
        LOGGER.error ("No valid queries could be created!");
        aUnifiedResponse.setStatus (CHttp.HTTP_BAD_REQUEST);
        return;
      }

      // Build final query term
      Query aLuceneQuery;
      if (aQueries.size () == 1)
      {
        aLuceneQuery = aQueries.getFirst ();
      }
      else
      {
        // Connect all with "AND"
        final BooleanQuery.Builder aBuilder = new BooleanQuery.Builder ();
        for (final Query aQuery : aQueries)
          aBuilder.add (aQuery, Occur.MUST);
        aLuceneQuery = aBuilder.build ();
      }

      // Only-non deleted
      aLuceneQuery = PDQueryManager.andNotDeleted (aLuceneQuery);

      // How many results to deliver at most
      final int nMaxResults = nLastResultIndex + 1;

      // Search all documents
      final PDStorageManager aStorageMgr = PDMetaManager.getStorageMgr ();
      final ICommonsList <PDStoredBusinessEntity> aResultDocs = aStorageMgr.getAllDocuments (aLuceneQuery, nMaxResults);

      // Also get the total hit count for UI display. May be < 0 in case of
      // error
      final int nTotalBEs = aStorageMgr.getCount (aLuceneQuery);

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("  Result for <" +
                      aLuceneQuery +
                      "> (max=" +
                      nMaxResults +
                      ") " +
                      (nTotalBEs == 1 ? "is 1 document" : "are " + nTotalBEs + " documents"));

      // Filter by index/count
      final int nEffectiveLastIndex = Math.min (nLastResultIndex, aResultDocs.size () - 1);
      final List <PDStoredBusinessEntity> aResultView = nFirstResultIndex >= aResultDocs.size () ? Collections.emptyList ()
                                                                                                 : aResultDocs.subList (nFirstResultIndex,
                                                                                                                        nEffectiveLastIndex +
                                                                                                                                           1);

      // Group results by participant ID
      final ICommonsMap <IParticipantIdentifier, ICommonsList <PDStoredBusinessEntity>> aGroupedDocs = PDStorageManager.getGroupedByParticipantID (aResultView);
      final ZonedDateTime aNow = PDTFactory.getCurrentZonedDateTimeUTC ();

      // build result
      switch (eOutputFormat)
      {
        case XML:
        {
          final XMLWriterSettings aXWS = new XMLWriterSettings ().setIndent (bBeautify ? EXMLSerializeIndent.INDENT_AND_ALIGN
                                                                                       : EXMLSerializeIndent.NONE);
          final IMicroDocument aDoc = new MicroDocument ();
          final IMicroElement eRoot = aDoc.appendElement ("resultlist");
          eRoot.setAttribute (RESPONSE_VERSION, eSearchVersion.getVersion ());
          eRoot.setAttribute (RESPONSE_TOTAL_RESULT_COUNT, nTotalBEs);
          eRoot.setAttribute (RESPONSE_USED_RESULT_COUNT, aResultView.size ());
          eRoot.setAttribute (RESPONSE_RESULT_PAGE_INDEX, nResultPageIndex);
          eRoot.setAttribute (RESPONSE_RESULT_PAGE_COUNT, nResultPageCount);
          eRoot.setAttribute (RESPONSE_FIRST_RESULT_INDEX, nFirstResultIndex);
          eRoot.setAttribute (RESPONSE_LAST_RESULT_INDEX, nEffectiveLastIndex);
          eRoot.setAttribute (RESPONSE_QUERY_TERMS, aSBQueryString.toString ());
          eRoot.setAttribute (RESPONSE_CREATION_DT, PDTWebDateHelper.getAsStringXSD (aNow));

          for (final ICommonsList <PDStoredBusinessEntity> aPerParticipant : aGroupedDocs.values ())
          {
            final IMicroElement eItem = PDStoredBusinessEntity.getAsSearchResultMicroElement (aPerParticipant);
            eRoot.appendChild (eItem);
          }

          if (false)
          {
            // Demo validation
            final CollectingSAXErrorHandler aErrHdl = new CollectingSAXErrorHandler ();
            final Validator v = new XMLSchemaCache (aErrHdl).getValidator (new ClassPathResource ("/schema/directory-search-result-list-v1.xsd"));
            v.validate (TransformSourceFactory.create (MicroWriter.getNodeAsBytes (aDoc, aXWS)));
            for (final IError aError : aErrHdl.getErrorList ())
              LOGGER.error (aError.getAsString (AppCommonUI.DEFAULT_LOCALE));
          }

          aUnifiedResponse.disableCaching ();
          aUnifiedResponse.setMimeType (eOutputFormat.getMimeType ());
          aUnifiedResponse.setContent (MicroWriter.getNodeAsBytes (aDoc, aXWS));
          break;
        }
        case JSON:
          final JsonWriterSettings aJWS = new JsonWriterSettings ().setIndentEnabled (bBeautify);
          final IJsonObject aDoc = new JsonObject ();
          aDoc.add (RESPONSE_VERSION, eSearchVersion.getVersion ());
          aDoc.add (RESPONSE_TOTAL_RESULT_COUNT, nTotalBEs);
          aDoc.add (RESPONSE_USED_RESULT_COUNT, aResultView.size ());
          aDoc.add (RESPONSE_RESULT_PAGE_INDEX, nResultPageIndex);
          aDoc.add (RESPONSE_RESULT_PAGE_COUNT, nResultPageCount);
          aDoc.add (RESPONSE_FIRST_RESULT_INDEX, nFirstResultIndex);
          aDoc.add (RESPONSE_LAST_RESULT_INDEX, nEffectiveLastIndex);
          aDoc.add (RESPONSE_QUERY_TERMS, aSBQueryString.toString ());
          aDoc.add (RESPONSE_CREATION_DT, PDTWebDateHelper.getAsStringXSD (aNow));

          final IJsonArray aMatches = new JsonArray ();
          for (final ICommonsList <PDStoredBusinessEntity> aPerParticipant : aGroupedDocs.values ())
          {
            final IJsonObject aItem = PDStoredBusinessEntity.getAsSearchResultJsonObject (aPerParticipant);
            aMatches.add (aItem);
          }
          aDoc.addJson ("matches", aMatches);

          aUnifiedResponse.disableCaching ();
          aUnifiedResponse.setMimeType (eOutputFormat.getMimeType ());
          aUnifiedResponse.setContentAndCharset (aDoc.getAsJsonString (aJWS), StandardCharsets.UTF_8);
          break;
        default:
          throw new IllegalStateException ("Unsupported output format: " + eOutputFormat);
      }

    }
    else
    {
      LOGGER.error ("Unsupported version provided (" + sPathInfo + ")");
      aUnifiedResponse.setStatus (CHttp.HTTP_NOT_FOUND);
    }
  }
}
