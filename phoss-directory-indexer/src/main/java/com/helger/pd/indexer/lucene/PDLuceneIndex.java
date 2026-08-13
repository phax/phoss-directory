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
package com.helger.pd.indexer.lucene;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.search.WildcardQuery;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.CheckForSigned;
import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.pd.indexer.searchindex.IPDIndex;
import com.helger.pd.indexer.searchindex.PDIndexDocument;
import com.helger.pd.indexer.searchindex.PDIndexField;
import com.helger.pd.indexer.searchindex.query.EPDIndexQueryOccur;
import com.helger.pd.indexer.searchindex.query.IPDIndexQuery;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryBool;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryContains;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryMatchAll;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryPrefix;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryTerm;

import jakarta.annotation.Nullable;

/**
 * The Apache Lucene based implementation of {@link IPDIndex}. It translates the search engine
 * independent documents and queries into their Lucene counterparts and performs all index access
 * via {@link PDLucene}.
 *
 * @author Philip Helger
 */
public class PDLuceneIndex implements IPDIndex
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDLuceneIndex.class);
  private static final String FIELD_GROUP_END = "groupend";
  private static final FieldType TYPE_GROUP_END = new FieldType ();
  private static final String VALUE_GROUP_END = "x";

  static
  {
    TYPE_GROUP_END.setStored (false);
    TYPE_GROUP_END.setIndexOptions (IndexOptions.DOCS);
    TYPE_GROUP_END.setOmitNorms (true);
    TYPE_GROUP_END.freeze ();
  }

  private final PDLucene m_aLucene;

  /**
   * Default constructor creating a new {@link PDLucene} instance.
   *
   * @throws IOException
   *         On IO error
   */
  public PDLuceneIndex () throws IOException
  {
    this (new PDLucene ());
  }

  /**
   * Constructor with an existing Lucene index.
   *
   * @param aLucene
   *        The Lucene index to be used. May not be <code>null</code>.
   */
  public PDLuceneIndex (@NonNull final PDLucene aLucene)
  {
    m_aLucene = ValueEnforcer.notNull (aLucene, "Lucene");
  }

  public void close () throws IOException
  {
    m_aLucene.close ();
  }

  public boolean isClosing ()
  {
    return m_aLucene.isClosing ();
  }

  @NonNull
  public ICommonsOrderedMap <String, String> getIndexInformation () throws IOException
  {
    final ICommonsOrderedMap <String, String> ret = new CommonsLinkedHashMap <> ();
    ret.put ("Lucene index directory", PDLucene.getLuceneIndexDir ().getAbsolutePath ());

    final DirectoryReader aReader = m_aLucene.getDirectoryReader ();
    if (aReader != null)
      ret.put ("Directory information", aReader.toString ());
    return ret;
  }

  @NonNull
  public ICommonsList <String> getSplitIntoTerms (@NonNull @Nonempty final String sFieldName,
                                                  @NonNull @Nonempty final String sQueryString) throws IOException
  {
    // Use the analyzer of the index to split the query string into fields
    try (final TokenStream aTokenStream = m_aLucene.getAnalyzer ().tokenStream (sFieldName, sQueryString))
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
  }

  public void updateDocuments (@Nullable final PDIndexQueryTerm aDeleteQuery,
                               @NonNull final List <PDIndexDocument> aDocs) throws IOException
  {
    ValueEnforcer.notNull (aDocs, "Docs");

    final ICommonsList <Document> aLuceneDocs = new CommonsArrayList <> (aDocs, PDLuceneIndex::_toLuceneDocument);
    if (aLuceneDocs.isNotEmpty ())
    {
      // Add "group end" marker, so that all documents of a block can be
      // identified as belonging together
      aLuceneDocs.getLastOrNull ().add (new Field (FIELD_GROUP_END, VALUE_GROUP_END, TYPE_GROUP_END));
    }
    m_aLucene.updateDocuments (aDeleteQuery == null ? null : _toLuceneTerm (aDeleteQuery), aLuceneDocs);
  }

  public void deleteDocuments (@NonNull final IPDIndexQuery aQuery) throws IOException
  {
    ValueEnforcer.notNull (aQuery, "Query");

    m_aLucene.deleteDocuments (_toLuceneQuery (aQuery));
  }

  @CheckForSigned
  public int getCount (@NonNull final IPDIndexQuery aQuery) throws IOException
  {
    ValueEnforcer.notNull (aQuery, "Query");

    final TotalHitCountCollector aCollector = new TotalHitCountCollector ();
    _searchAtomic (_toLuceneQuery (aQuery), aCollector);
    return aCollector.getTotalHits ();
  }

  public void searchAll (@NonNull final IPDIndexQuery aQuery,
                         @CheckForSigned final int nMaxResultCount,
                         @NonNull final Consumer <? super PDIndexDocument> aConsumer) throws IOException
  {
    ValueEnforcer.notNull (aQuery, "Query");
    ValueEnforcer.notNull (aConsumer, "Consumer");

    final Query aLuceneQuery = _toLuceneQuery (aQuery);
    if (nMaxResultCount <= 0)
    {
      // Search all
      final ObjIntConsumer <Document> aConverter = (aDoc, nDocID) -> aConsumer.accept (_toIndexDocument (aDoc));
      final Collector aCollector = new AllDocumentsCollector (m_aLucene, aConverter);
      _searchAtomic (aLuceneQuery, aCollector);
    }
    else
    {
      // Search top docs only
      // Lucene 8
      final TopScoreDocCollector aCollector = TopScoreDocCollector.create (nMaxResultCount, Integer.MAX_VALUE);
      _searchAtomic (aLuceneQuery, aCollector);
      for (final ScoreDoc aScoreDoc : aCollector.topDocs ().scoreDocs)
      {
        final Document aDoc = m_aLucene.getDocument (aScoreDoc.doc);
        if (aDoc == null)
          throw new IllegalStateException ("Failed to resolve Lucene Document with ID " + aScoreDoc.doc);
        // Pass to Consumer
        aConsumer.accept (_toIndexDocument (aDoc));
      }
    }
  }

  /**
   * Search all documents matching the passed query and pass the result on to the provided Lucene
   * {@link Collector}.
   *
   * @param aQuery
   *        Lucene query to execute. May not be <code>null</code>.
   * @param aCollector
   *        The Lucene collector to be used. May not be <code>null</code>.
   * @throws IOException
   *         On Lucene error
   */
  private void _searchAtomic (@NonNull final Query aQuery, @NonNull final Collector aCollector) throws IOException
  {
    final IndexSearcher aSearcher = m_aLucene.getSearcher ();
    if (aSearcher != null)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Searching Lucene: " + aQuery);

      // Search all documents, collect them
      aSearcher.search (aQuery, aCollector);
    }
    else
      LOGGER.error ("Failed to obtain IndexSearcher for " + aQuery);
  }

  @NonNull
  private static Term _toLuceneTerm (@NonNull final PDIndexQueryTerm aQuery)
  {
    return new Term (aQuery.getFieldName (), aQuery.getValue ());
  }

  @NonNull
  private static Occur _toLuceneOccur (@NonNull final EPDIndexQueryOccur eOccur)
  {
    return switch (eOccur)
    {
      case MUST -> Occur.MUST;
      case SHOULD -> Occur.SHOULD;
      case FILTER -> Occur.FILTER;
      default -> throw new IllegalArgumentException ("Unsupported occurrence " + eOccur);
    };
  }

  @NonNull
  private static Query _toLuceneQuery (@NonNull final IPDIndexQuery aQuery)
  {
    if (aQuery instanceof PDIndexQueryMatchAll)
      return new MatchAllDocsQuery ();

    if (aQuery instanceof PDIndexQueryTerm)
      return new TermQuery (_toLuceneTerm ((PDIndexQueryTerm) aQuery));

    if (aQuery instanceof final PDIndexQueryPrefix aPrefixQuery)
    {
      // Note: PrefixQuery is supposed to work with the exact term, without a trailing "*"
      return new PrefixQuery (new Term (aPrefixQuery.getFieldName (), aPrefixQuery.getValue ()));
    }

    if (aQuery instanceof final PDIndexQueryContains aContainsQuery)
    {
      // This works -> text ==> *text*
      return new WildcardQuery (new Term (aContainsQuery.getFieldName (), "*" + aContainsQuery.getValue () + "*"));
    }

    if (aQuery instanceof PDIndexQueryBool)
    {
      final BooleanQuery.Builder aBuilder = new BooleanQuery.Builder ();
      for (final PDIndexQueryBool.Clause aClause : ((PDIndexQueryBool) aQuery).getAllClauses ())
        aBuilder.add (_toLuceneQuery (aClause.getQuery ()), _toLuceneOccur (aClause.getOccur ()));
      return aBuilder.build ();
    }

    throw new IllegalArgumentException ("Unsupported query type " + aQuery.getClass ().getName ());
  }

  @NonNull
  private static Document _toLuceneDocument (@NonNull final PDIndexDocument aDoc)
  {
    final Document ret = new Document ();
    for (final PDIndexField aField : aDoc.fields ())
    {
      if (aField.isNumeric ())
      {
        // Lucene 6: cannot yet use a LongPoint because it has no way to create
        // a stored one
        ret.add (new StoredField (aField.getName (), aField.getNumericValue ().longValue ()));
      }
      else
      {
        final Field.Store eStore = aField.getStore ().isStored () ? Field.Store.YES : Field.Store.NO;
        if (aField.getTokenize ().isTokenize ())
          ret.add (new TextField (aField.getName (), aField.getStringValue (), eStore));
        else
          ret.add (new StringField (aField.getName (), aField.getStringValue (), eStore));
      }
    }
    return ret;
  }

  @NonNull
  private static PDIndexDocument _toIndexDocument (@NonNull final Document aDoc)
  {
    final PDIndexDocument ret = new PDIndexDocument ();
    for (final IndexableField aField : aDoc)
    {
      // A Document read from the index contains the stored fields only
      final Number aNumericValue = aField.numericValue ();
      if (aNumericValue != null)
        ret.add (PDIndexField.createNumeric (aField.name (), aNumericValue));
      else
      {
        final String sStringValue = aField.stringValue ();
        if (sStringValue != null)
          ret.add (PDIndexField.createStoredString (aField.name (), sStringValue));
        else
          LOGGER.warn ("Ignoring the value of Lucene field '" +
                       aField.name () +
                       "' because it is neither a String nor a Number");
      }
    }
    return ret;
  }
}
