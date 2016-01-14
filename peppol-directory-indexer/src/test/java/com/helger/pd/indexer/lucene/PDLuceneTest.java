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
package com.helger.pd.indexer.lucene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.GregorianCalendar;

import javax.annotation.Nullable;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.photon.basic.mock.PhotonBasicTestRule;

/**
 * Test class for class {@link PDLucene}.
 *
 * @author Philip Helger
 */
public final class PDLuceneTest
{
  @Rule
  public final TestRule m_aRule = new PhotonBasicTestRule ();

  private static void _doIndex () throws IOException
  {
    /*
     * 4. add a sample document to the index
     */
    final Document doc = new Document ();

    // We add an id field that is searchable, but doesn't trigger
    // tokenization of the content
    final Field idField = new StringField ("id", "Apache Lucene 5.0.0", Field.Store.YES);
    doc.add (idField);

    // Add the last big lucene version birthday which we don't want to store
    // but to be indexed nevertheless to be filterable
    doc.add (new LongField ("lastVersionBirthday",
                            new GregorianCalendar (2015, 1, 20).getTimeInMillis (),
                            Field.Store.NO));

    // The version info content should be searchable also be tokens,
    // this is why we use a TextField; as we use a reader, the content is
    // not stored!
    doc.add (new TextField ("pom",
                            new BufferedReader (new InputStreamReader (new FileInputStream (new File ("pom.xml")),
                                                                       StandardCharsets.UTF_8))));

    // Existing index
    try (final PDLucene aLucene = new PDLucene ())
    {
      aLucene.updateDocument (new Term ("id", "Apache Lucene 5.0.0"), doc);
    }
  }

  @Nullable
  private static Document _searchBest (final Query aQuery) throws IOException
  {
    try (final PDLucene aLucene = new PDLucene ())
    {
      // Find top 5 hits
      final TopDocs results = aLucene.getSearcher ().search (aQuery, 5);

      // Get results
      final ScoreDoc [] aHits = results.scoreDocs;
      if (aHits.length == 0)
        return null;

      final int numTotalHits = results.totalHits;
      assertEquals (1, numTotalHits);

      /*
       * Matching score for the first document
       */
      assertTrue (aHits[0].score > 0);

      final Document doc = aLucene.getDocument (aHits[0].doc);
      assertEquals ("Apache Lucene 5.0.0", doc.get ("id"));
      return doc;
    }
  }

  @Test
  public void testBasic () throws IOException
  {
    _doIndex ();
    final Document aDoc = _searchBest (new TermQuery (new Term ("id", "Apache Lucene 5.0.0")));
    assertNotNull (aDoc);
  }
}
