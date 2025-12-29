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
package com.helger.pd.indexer.lucene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.io.file.FileOperationManager;
import com.helger.io.file.SimpleFileIO;
import com.helger.photon.app.mock.PhotonAppTestRule;

import jakarta.annotation.Nullable;

/**
 * Test class for class {@link PDLucene}.
 *
 * @author Philip Helger
 */
public final class PDLuceneTest
{
  @Rule
  public final TestRule m_aRule = new PhotonAppTestRule ();

  private static void _doIndex () throws IOException
  {
    FileOperationManager.INSTANCE.deleteDirRecursiveIfExisting (PDLucene.getLuceneIndexDir ());

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
    doc.add (new LongPoint ("num", 12345));

    // The version info content should be searchable also be tokens,
    // this is why we use a TextField; as we use a reader, the content is
    // not stored!
    doc.add (new TextField ("pom",
                            SimpleFileIO.getFileAsString (new File ("pom.xml"), StandardCharsets.UTF_8),
                            Field.Store.NO));

    // Next
    doc.add (new StringField ("participantid", "iso6523-actorid-upis::9915:testluc", Field.Store.YES));

    // Stored but not indexed
    doc.add (new StoredField ("stored", "value"));

    // Existing index
    try (final PDLucene aLucene = new PDLucene ())
    {
      // create or update document
      aLucene.updateDocument (new Term (idField.name (), idField.stringValue ()), doc);

      // Update field
      doc.removeFields (idField.name ());
      final StringField idField2 = new StringField (idField.name (), "Apache Lucene 7.0.0", Field.Store.YES);
      doc.add (idField2);

      // create or update document (with old ID!)
      aLucene.updateDocument (new Term (idField.name (), idField.stringValue ()), doc);

      // Update field
      doc.removeFields (idField.name ());
      final StringField idField3 = new StringField (idField.name (), "Apache Lucene 7.5.0", Field.Store.YES);
      doc.add (idField3);

      // create or update document (with old ID!)
      aLucene.updateDocument (new Term (idField2.name (), idField2.stringValue ()), doc);
    }
  }

  @Nullable
  private static Document _search (final Query aQuery) throws IOException
  {
    try (final PDLucene aLucene = new PDLucene ())
    {
      // Find top 5 hits
      final TopDocs results = aLucene.getSearcher ().search (aQuery, 5);

      // Get results
      final ScoreDoc [] aHits = results.scoreDocs;
      if (aHits.length == 0)
        return null;

      // Lucene 8
      final long numTotalHits = results.totalHits.value;
      assertEquals (1, numTotalHits);

      /*
       * Matching score for the first document
       */
      assertTrue (aHits[0].score > 0);

      final Document doc = aLucene.getDocument (aHits[0].doc);
      return doc;
    }
  }

  @Nullable
  private static Document _searchBest (final String sField, final String sExpectedValue) throws IOException
  {
    final Document aDoc = _search (new TermQuery (new Term (sField, sExpectedValue)));
    if (aDoc != null)
      assertEquals (sExpectedValue, aDoc.get (sField));
    return aDoc;
  }

  @Nullable
  private static Document _searchPrefix (final String sField, final String sExpectedValue) throws IOException
  {
    final Document aDoc = _search (new PrefixQuery (new Term (sField, sExpectedValue)));
    if (aDoc != null)
      assertTrue (aDoc.get (sField).startsWith (sExpectedValue));
    return aDoc;
  }

  @Test
  public void testBasic () throws IOException
  {
    _doIndex ();

    // Full ID
    Document aDoc = _searchBest ("id", "Apache Lucene 7.5.0");
    assertNotNull (aDoc);
    assertEquals ("Apache Lucene 7.5.0", aDoc.get ("id"));

    // Full identifier
    aDoc = _searchBest ("participantid", "iso6523-actorid-upis::9915:testluc");
    assertNotNull (aDoc);
    assertEquals ("iso6523-actorid-upis::9915:testluc", aDoc.get ("participantid"));

    // Prefix identifier
    aDoc = _searchPrefix ("participantid", "iso6523-actorid-upis::9915:test");
    assertNotNull (aDoc);
    assertEquals ("iso6523-actorid-upis::9915:testluc", aDoc.get ("participantid"));

    // part of the identifier
    aDoc = _searchBest ("participantid", "9915:testluc");
    assertNull (aDoc);

    // Stored not indexed
    aDoc = _searchBest ("stored", "value");
    assertNull (aDoc);

    // LongPoint - indexed but not stored
    aDoc = _search (LongPoint.newExactQuery ("num", 12345));
    assertNotNull (aDoc);
    assertNull (aDoc.getField ("num"));
  }
}
