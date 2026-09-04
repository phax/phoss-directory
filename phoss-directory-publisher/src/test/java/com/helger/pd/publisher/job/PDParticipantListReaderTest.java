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
package com.helger.pd.publisher.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.jspecify.annotations.NonNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.pd.publisher.job.PDParticipantListReader.EFormat;
import com.helger.pd.publisher.job.PDParticipantListReader.ReadResult;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;

/**
 * Test class for {@link PDParticipantListReader}.
 *
 * @author Philip Helger
 */
public final class PDParticipantListReaderTest
{
  @Rule
  public final TemporaryFolder m_aFolder = new TemporaryFolder ();

  @NonNull
  private static ICommonsList <String> _getAllURIEncoded (@NonNull final ReadResult aResult)
  {
    final ICommonsList <String> ret = new CommonsArrayList <> ();
    for (final IParticipantIdentifier aPI : aResult.participantIDs ())
      ret.add (aPI.getURIEncoded ());
    return ret;
  }

  @NonNull
  private ReadResult _read (@NonNull final String sContent) throws IOException
  {
    final File aFile = m_aFolder.newFile ();
    Files.write (aFile.toPath (), sContent.getBytes (StandardCharsets.UTF_8));
    return PDParticipantListReader.readParticipantList (aFile, PeppolIdentifierFactory.INSTANCE);
  }

  @Test
  public void testReadXML () throws IOException
  {
    final ReadResult aResult = _read ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                      "<root>\n" +
                                      "  <participant scheme=\"iso6523-actorid-upis\" value=\"9915:test1\" />\n" +
                                      "  <participant scheme=\"iso6523-actorid-upis\" value=\"9915:test2\" />\n" +
                                      "</root>\n");
    assertEquals (EFormat.XML, aResult.getFormat ());
    assertTrue (aResult.getSuccess ().isSuccess ());
    assertEquals (new CommonsArrayList <> ("iso6523-actorid-upis::9915:test1", "iso6523-actorid-upis::9915:test2"),
                  _getAllURIEncoded (aResult));
    assertEquals (0, aResult.getDuplicateCount ());
    assertTrue (aResult.getAllInvalidEntries ().isEmpty ());
  }

  @Test
  public void testReadXMLWithNamespaceAndNesting () throws IOException
  {
    // This is the layout of the full Business Card export
    final ReadResult aResult = _read ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                      "<root xmlns=\"http://www.peppol.eu/schema/pd/businesscard-generic/201907/\">\n" +
                                      "  <businesscard>\n" +
                                      "    <participant scheme=\"iso6523-actorid-upis\" value=\"9915:nested\" />\n" +
                                      "  </businesscard>\n" +
                                      "</root>\n");
    assertEquals (EFormat.XML, aResult.getFormat ());
    assertEquals (new CommonsArrayList <> ("iso6523-actorid-upis::9915:nested"), _getAllURIEncoded (aResult));
  }

  @Test
  public void testReadXMLWithLeadingWhitespace () throws IOException
  {
    final ReadResult aResult = _read ("\n\n  \t<root><participant scheme=\"iso6523-actorid-upis\" value=\"9915:ws\" /></root>");
    assertEquals (EFormat.XML, aResult.getFormat ());
    assertEquals (new CommonsArrayList <> ("iso6523-actorid-upis::9915:ws"), _getAllURIEncoded (aResult));
  }

  @Test
  public void testReadXMLWithUTF8BOM () throws IOException
  {
    final ReadResult aResult = _read ("﻿<root><participant scheme=\"iso6523-actorid-upis\" value=\"9915:bom\" /></root>");
    assertEquals (EFormat.XML, aResult.getFormat ());
    assertEquals (new CommonsArrayList <> ("iso6523-actorid-upis::9915:bom"), _getAllURIEncoded (aResult));
  }

  @Test
  public void testReadText () throws IOException
  {
    final ReadResult aResult = _read ("iso6523-actorid-upis::9915:text1\niso6523-actorid-upis::9915:text2\n");
    assertEquals (EFormat.TEXT, aResult.getFormat ());
    assertTrue (aResult.getSuccess ().isSuccess ());
    assertEquals (new CommonsArrayList <> ("iso6523-actorid-upis::9915:text1", "iso6523-actorid-upis::9915:text2"),
                  _getAllURIEncoded (aResult));
    assertTrue (aResult.getAllInvalidEntries ().isEmpty ());
  }

  @Test
  public void testReadTextIgnoresBlankLinesAndComments () throws IOException
  {
    final ReadResult aResult = _read ("# this is a comment\n" +
                                      "\n" +
                                      "   \t  \n" +
                                      "  iso6523-actorid-upis::9915:trimmed  \n" +
                                      "\t#another comment is only a comment after trimming\n" +
                                      "iso6523-actorid-upis::9915:second\n" +
                                      "\n");
    assertEquals (EFormat.TEXT, aResult.getFormat ());
    assertEquals (new CommonsArrayList <> ("iso6523-actorid-upis::9915:trimmed", "iso6523-actorid-upis::9915:second"),
                  _getAllURIEncoded (aResult));
    assertTrue (aResult.getAllInvalidEntries ().isEmpty ());
  }

  @Test
  public void testReadTextWithDuplicatesAndInvalidEntries () throws IOException
  {
    final ReadResult aResult = _read ("iso6523-actorid-upis::9915:dup\n" +
                                      "iso6523-actorid-upis::9915:dup\n" +
                                      "iso6523-actorid-upis::9915:dup\n" +
                                      "this-is-not-an-identifier\n" +
                                      "iso6523-actorid-upis::not-a-valid-value\n");
    assertEquals (EFormat.TEXT, aResult.getFormat ());
    assertEquals (new CommonsArrayList <> ("iso6523-actorid-upis::9915:dup"), _getAllURIEncoded (aResult));
    assertEquals (2, aResult.getDuplicateCount ());
    assertEquals (new CommonsArrayList <> ("this-is-not-an-identifier", "iso6523-actorid-upis::not-a-valid-value"),
                  aResult.getAllInvalidEntries ());
  }

  @Test
  public void testReadTextIsCaseInsensitive () throws IOException
  {
    // Peppol participant IDs are case insensitive - the upper case one is a duplicate
    final ReadResult aResult = _read ("iso6523-actorid-upis::9915:Case\niso6523-actorid-upis::9915:case\n");
    assertEquals (new CommonsArrayList <> ("iso6523-actorid-upis::9915:case"), _getAllURIEncoded (aResult));
    assertEquals (1, aResult.getDuplicateCount ());
  }

  @Test
  public void testReadEmptyFile () throws IOException
  {
    final ReadResult aResult = _read ("");
    assertEquals (EFormat.TEXT, aResult.getFormat ());
    assertTrue (aResult.getSuccess ().isSuccess ());
    assertTrue (aResult.participantIDs ().isEmpty ());
  }

  @Test
  public void testReadBrokenXMLKeepsWhatWasRead () throws IOException
  {
    final ReadResult aResult = _read ("<root>\n" +
                                      "  <participant scheme=\"iso6523-actorid-upis\" value=\"9915:before\" />\n" +
                                      "  <unclosed>\n");
    assertEquals (EFormat.XML, aResult.getFormat ());
    assertTrue (aResult.getSuccess ().isFailure ());
    assertTrue (aResult.getAllErrors ().isNotEmpty ());
    assertEquals (new CommonsArrayList <> ("iso6523-actorid-upis::9915:before"), _getAllURIEncoded (aResult));
  }
}
