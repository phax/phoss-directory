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
package com.helger.pd.indexer.opensearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.opensearch.client.opensearch._types.mapping.DynamicMapping;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;

import com.helger.pd.indexer.storage.CPDStorage;
import com.helger.pd.indexer.storage.field.PDField;

/**
 * Test class for class {@link PDOpenSearchIndex} that needs no running OpenSearch. It locks the
 * OpenSearch type mapping to the Apache Lucene field configuration - if the two ever diverge, the
 * queries of <code>PDQueryManager</code> return different results per search engine.
 *
 * @author Philip Helger
 */
public final class PDOpenSearchIndexTest
{
  private static void _assertText (final Map <String, Property> aProps, final String sFieldName)
  {
    final Property aProperty = aProps.get (sFieldName);
    assertNotNull ("Field '" + sFieldName + "' is not mapped", aProperty);
    assertTrue ("Field '" + sFieldName + "' must be tokenized", aProperty.isText ());
    assertEquals ("Field '" + sFieldName + "' must use the analyzer that matches the Lucene StandardAnalyzer",
                  PDOpenSearchIndex.ANALYZER_STANDARD,
                  aProperty.text ().analyzer ());
  }

  private static void _assertKeyword (final Map <String, Property> aProps, final String sFieldName)
  {
    final Property aProperty = aProps.get (sFieldName);
    assertNotNull ("Field '" + sFieldName + "' is not mapped", aProperty);
    assertTrue ("Field '" + sFieldName + "' must not be tokenized", aProperty.isKeyword ());
  }

  @Test
  public void testTypeMappingMatchesTheLuceneFieldConfiguration ()
  {
    final TypeMapping aMapping = PDOpenSearchIndex.createTypeMapping ();
    final Map <String, Property> aProps = aMapping.properties ();

    // Not tokenized in Lucene -> keyword in OpenSearch.
    // Note: the keyword fields are mapped explicitly and therefore have no "ignore_above" limit -
    // the Peppol document type IDs are way longer than the 256 characters a dynamically mapped
    // keyword field would be limited to.
    _assertKeyword (aProps, PDField.PARTICIPANT_ID.getFieldName ());
    _assertKeyword (aProps, PDField.DOCTYPE_ID.getFieldName ());
    _assertKeyword (aProps, PDField.REGISTRATION_DATE.getFieldName ());
    _assertKeyword (aProps, PDField.ML_LANGUAGE.getFieldName ());
    _assertKeyword (aProps, PDField.COUNTRY_CODE.getFieldName ());
    _assertKeyword (aProps, PDField.METADATA_OWNERID.getFieldName ());
    _assertKeyword (aProps, PDField.METADATA_REQUESTING_HOST.getFieldName ());

    // Tokenized in Lucene -> text in OpenSearch
    _assertText (aProps, PDField.NAME.getFieldName ());
    _assertText (aProps, PDField.ML_NAME.getFieldName ());
    _assertText (aProps, PDField.GEO_INFO.getFieldName ());
    _assertText (aProps, PDField.IDENTIFIER_SCHEME.getFieldName ());
    _assertText (aProps, PDField.IDENTIFIER_VALUE.getFieldName ());
    _assertText (aProps, PDField.WEBSITE_URI.getFieldName ());
    _assertText (aProps, PDField.CONTACT_TYPE.getFieldName ());
    _assertText (aProps, PDField.CONTACT_NAME.getFieldName ());
    _assertText (aProps, PDField.CONTACT_PHONE.getFieldName ());
    _assertText (aProps, PDField.CONTACT_EMAIL.getFieldName ());
    _assertText (aProps, PDField.ADDITIONAL_INFO.getFieldName ());
    _assertText (aProps, CPDStorage.FIELD_ALL_FIELDS);

    // The single numeric field is stored but not indexed
    final Property aCreationDT = aProps.get (PDField.METADATA_CREATIONDT.getFieldName ());
    assertNotNull (aCreationDT);
    assertTrue (aCreationDT.isLong ());
    assertEquals (Boolean.FALSE, aCreationDT.long_ ().index ());

    // Every mapped field must be listed above
    assertEquals (20, aProps.size ());

    // Unknown fields must not be indexed silently
    assertEquals (DynamicMapping.False, aMapping.dynamic ());

    // The "all fields" field is indexed but not stored
    assertNotNull (aMapping.source ());
    assertEquals (1, aMapping.source ().excludes ().size ());
    assertEquals (CPDStorage.FIELD_ALL_FIELDS, aMapping.source ().excludes ().get (0));
  }
}
