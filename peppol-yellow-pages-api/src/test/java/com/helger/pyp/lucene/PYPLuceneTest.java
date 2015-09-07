package com.helger.pyp.lucene;

import static org.junit.Assert.assertNotNull;

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
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.photon.basic.mock.PhotonBasicTestRule;

/**
 * Test class for class {@link PYPLucene}.
 *
 * @author Philip Helger
 */
public final class PYPLuceneTest
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
    PYPLucene.getInstance ().getWriter ().updateDocument (new Term ("id", "Apache Lucene 5.0.0"), doc);
    PYPLucene.getInstance ().getWriter ().commit ();
  }

  @Nullable
  private static Document _searchBest (final Query aQuery) throws IOException
  {
    // Find top 5 hits
    final TopDocs results = PYPLucene.getInstance ().getSearcher ().search (aQuery, 5);

    // Get results
    final ScoreDoc [] aHits = results.scoreDocs;
    if (aHits.length == 0)
      return null;

    final int numTotalHits = results.totalHits;
    System.out.println (numTotalHits + " total matching documents");

    /*
     * Matching score for the first document
     */
    System.out.println ("Matching score for first document: " + aHits[0].score);

    final Document doc = PYPLucene.getInstance ().getReader ().document (aHits[0].doc);
    System.out.println ("Id of the document: " + doc.get ("id"));
    return doc;
  }

  @Test
  public void testBasic () throws IOException, ParseException
  {
    _doIndex ();
    final Document aDoc = _searchBest (new TermQuery (new Term ("id", "Apache Lucene 5.0.0")));
    assertNotNull (aDoc);
  }
}
