/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.xml.validation.Validator;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.array.ArrayHelper;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsEnumMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.web.PDTWebDateHelper;
import com.helger.diagnostics.error.IError;
import com.helger.http.CHttp;
import com.helger.http.CHttpHeader;
import com.helger.io.resource.ClassPathResource;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.mime.CMimeType;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.storage.PDStorageManagerLucene;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.pd.publisher.app.AppCommonUI;
import com.helger.pd.publisher.search.EPDOutputFormat;
import com.helger.pd.publisher.search.EPDSearchField;
import com.helger.pd.publisher.search.SearchRateLimit;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.servlet.request.RequestHelper;
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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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

    ESearchVersion (@Nonnull @Nonempty final String sVersion)
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

  public PublicSearchXServletHandler ()
  {}

  public void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                             @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final BiConsumer <UnifiedResponse, String> applyError = (ur, msg) -> ur.setContentAndCharset (msg,
                                                                                                  StandardCharsets.UTF_8)
                                                                           .setMimeType (CMimeType.TEXT_PLAIN);

    if (SearchRateLimit.INSTANCE.rateLimiter () != null)
    {
      final String sRateLimitKey = "ip:" + aRequestScope.getRemoteAddr ();
      final boolean bOverLimit = SearchRateLimit.INSTANCE.rateLimiter ().overLimitWhenIncremented (sRateLimitKey);
      if (bOverLimit)
      {
        // Too Many Requests
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("REST search rate limit exceeded for " + sRateLimitKey);

        aUnifiedResponse.setStatus (CHttp.HTTP_TOO_MANY_REQUESTS);
        applyError.accept (aUnifiedResponse, "REST search rate limit exceeded");
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
      final List <String> aParts = StringHelper.getExploded ('/', sPathInfo.substring (1));
      final String sFormat = aParts.size () >= 2 ? aParts.get (1) : null;
      final EPDOutputFormat eOutputFormat = EPDOutputFormat.getFromIDCaseInsensitiveOrDefault (sFormat,
                                                                                               EPDOutputFormat.XML);
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Using REST query API 1.0 with output format " +
                      eOutputFormat +
                      " (" +
                      sPathInfo +
                      ") from '" +
                      RequestHelper.getHttpUserAgentStringFromRequest (aRequestScope.getRequest ()) +
                      "'");

      // Determine result offset and count
      final int nResultPageIndex = aParams.getAsInt (PARAM_RESULT_PAGE_INDEX,
                                                     aParams.getAsInt ("rpi", DEFAULT_RESULT_PAGE_INDEX));
      if (nResultPageIndex < 0)
      {
        final String sErrorMsg = "ResultPageIndex " + nResultPageIndex + " is invalid. It must be >= 0.";
        LOGGER.error (sErrorMsg);
        aUnifiedResponse.setStatus (CHttp.HTTP_BAD_REQUEST);
        applyError.accept (aUnifiedResponse, sErrorMsg);
        return;
      }
      final int nResultPageCount = aParams.getAsInt (PARAM_RESULT_PAGE_COUNT,
                                                     aParams.getAsInt ("rpc", DEFAULT_RESULT_PAGE_COUNT));
      if (nResultPageCount <= 0)
      {
        final String sErrorMsg = "ResultPageCount " + nResultPageCount + " is invalid. It must be > 0.";
        LOGGER.error (sErrorMsg);
        aUnifiedResponse.setStatus (CHttp.HTTP_BAD_REQUEST);
        applyError.accept (aUnifiedResponse, sErrorMsg);
        return;
      }
      final int nFirstResultIndex = nResultPageIndex * nResultPageCount;
      final int nLastResultIndex = (nResultPageIndex + 1) * nResultPageCount - 1;
      if (nFirstResultIndex > MAX_RESULTS)
      {
        final String sErrorMsg = "The first result index " +
                                 nFirstResultIndex +
                                 " is invalid. It must be <= " +
                                 MAX_RESULTS +
                                 ".";
        LOGGER.error (sErrorMsg);
        aUnifiedResponse.setStatus (CHttp.HTTP_BAD_REQUEST);
        applyError.accept (aUnifiedResponse, sErrorMsg);
        return;
      }
      if (nLastResultIndex > MAX_RESULTS)
      {
        final String sErrorMsg = "The last result index " +
                                 nLastResultIndex +
                                 " is invalid. It must be <= " +
                                 MAX_RESULTS +
                                 ".";
        LOGGER.error (sErrorMsg);
        aUnifiedResponse.setStatus (CHttp.HTTP_BAD_REQUEST);
        applyError.accept (aUnifiedResponse, sErrorMsg);
        return;
      }
      // Format output?
      final boolean bBeautify = aParams.getAsBoolean (PARAM_BEAUTIFY, false);

      // Determine query terms
      final StringBuilder aSBQueryString = new StringBuilder ();
      final ICommonsMap <EPDSearchField, ICommonsList <String>> aQueryValues = new CommonsEnumMap <> (EPDSearchField.class);
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
        final String sErrorMsg = "No valid query term provided!";
        LOGGER.error (sErrorMsg);
        aUnifiedResponse.setStatus (CHttp.HTTP_BAD_REQUEST);
        applyError.accept (aUnifiedResponse, sErrorMsg);
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
          {
            LOGGER.error ("Failed to create query '" + sQuery + "' of field " + eField + " - ignoring term!");
          }
        }
      }
      if (aQueries.isEmpty ())
      {
        final String sErrorMsg = "No valid queries could be created!";
        LOGGER.error (sErrorMsg);
        aUnifiedResponse.setStatus (CHttp.HTTP_BAD_REQUEST);
        applyError.accept (aUnifiedResponse, sErrorMsg);
        return;
      }
      // Build final query term
      final Query aLuceneQuery;
      if (aQueries.size () == 1)
      {
        aLuceneQuery = aQueries.getFirstOrNull ();
      }
      else
      {
        // Connect all with "AND"
        final BooleanQuery.Builder aBuilder = new BooleanQuery.Builder ();
        for (final Query aQuery : aQueries)
          aBuilder.add (aQuery, Occur.MUST);
        aLuceneQuery = aBuilder.build ();
      }
      // How many results to deliver at most
      final int nMaxResults = nLastResultIndex + 1;

      // Search all documents
      final PDStorageManagerLucene aStorageMgr = PDMetaManager.getStorageMgr ();
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
      final List <PDStoredBusinessEntity> aResultView = nFirstResultIndex >= aResultDocs.size () ? Collections
                                                                                                              .emptyList ()
                                                                                                 : aResultDocs.subList (nFirstResultIndex,
                                                                                                                        nEffectiveLastIndex +
                                                                                                                                           1);

      // Group results by participant ID
      final ICommonsMap <IParticipantIdentifier, ICommonsList <PDStoredBusinessEntity>> aGroupedDocs = PDStorageManagerLucene.getGroupedByParticipantID (aResultView);
      final ZonedDateTime aNow = PDTFactory.getCurrentZonedDateTimeUTC ();

      // See Directory issue #68
      aUnifiedResponse.addCustomResponseHeader (CHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
      aUnifiedResponse.disableCaching ();
      aUnifiedResponse.setMimeType (eOutputFormat.getMimeType ());

      // build result
      switch (eOutputFormat)
      {
        case XML:
        {
          final XMLWriterSettings aXWS = new XMLWriterSettings ().setIndent (bBeautify ? EXMLSerializeIndent.INDENT_AND_ALIGN
                                                                                       : EXMLSerializeIndent.NONE);
          final IMicroDocument aDoc = new MicroDocument ();
          final IMicroElement eRoot = aDoc.addElement ("resultlist");
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
            eRoot.addChild (eItem);
          }
          if (false)
          {
            // Demo validation
            final CollectingSAXErrorHandler aErrHdl = new CollectingSAXErrorHandler ();
            // Will not work anymore - moved to searchapi submodule
            final Validator v = new XMLSchemaCache (aErrHdl).getValidator (new ClassPathResource ("/schema/directory-search-result-list-v1.xsd"));
            v.validate (TransformSourceFactory.create (MicroWriter.getNodeAsBytes (aDoc, aXWS)));
            for (final IError aError : aErrHdl.getErrorList ())
              LOGGER.error (aError.getAsString (AppCommonUI.DEFAULT_LOCALE));
          }

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
          aDoc.add ("matches", aMatches);

          aUnifiedResponse.setContentAndCharset (aDoc.getAsJsonString (aJWS), StandardCharsets.UTF_8);
          break;
        default:
          // Happens only on programming error
          throw new IllegalStateException ("Unsupported output format: " + eOutputFormat);
      }
    }
    else
    {
      final String sErrorMsg = "Unsupported search version API provided (" + sPathInfo + ")";
      LOGGER.error (sErrorMsg);
      aUnifiedResponse.setStatus (CHttp.HTTP_NOT_FOUND);
      applyError.accept (aUnifiedResponse, sErrorMsg);
    }
  }
}
