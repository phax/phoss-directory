package com.helger.pd.indexer.storage;

import java.util.function.Function;

import org.apache.lucene.document.Field;

import com.helger.pd.indexer.storage.PDStringField.EPDTokenize;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;

public class PDField
{
  public static final PDStringField <IParticipantIdentifier> PARTICIPANT_ID = new PDStringField<> ("participantid",
                                                                                                   x -> x.getURIEncoded (),
                                                                                                   Field.Store.YES,
                                                                                                   EPDTokenize.NO_TOKENIZE);
  public static final PDStringField <IDocumentTypeIdentifier> DOCTYPE_ID = new PDStringField<> ("doctypeid",
                                                                                                x -> x.getURIEncoded (),
                                                                                                Field.Store.YES,
                                                                                                EPDTokenize.NO_TOKENIZE);
  public static final PDStringField <String> REGISTRATION_DATE = new PDStringField<> ("registrationdate",
                                                                                      Function.identity (),
                                                                                      Field.Store.YES,
                                                                                      EPDTokenize.NO_TOKENIZE);
  public static final PDStringField <String> NAME = new PDStringField<> ("name",
                                                                         Function.identity (),
                                                                         Field.Store.YES,
                                                                         EPDTokenize.TOKENIZE);
  public static final PDStringField <String> COUNTRY_CODE = new PDStringField<> ("country",
                                                                                 Function.identity (),
                                                                                 Field.Store.YES,
                                                                                 EPDTokenize.NO_TOKENIZE);
  public static final PDStringField <String> GEO_INFO = new PDStringField<> ("geoinfo",
                                                                             Function.identity (),
                                                                             Field.Store.YES,
                                                                             EPDTokenize.TOKENIZE);
  public static final PDStringField <String> IDENTIFIER_SCHEME = new PDStringField<> ("identifiertype",
                                                                                      Function.identity (),
                                                                                      Field.Store.YES,
                                                                                      EPDTokenize.TOKENIZE);
  public static final PDStringField <String> IDENTIFIER_VALUE = new PDStringField<> ("identifier",
                                                                                     Function.identity (),
                                                                                     Field.Store.YES,
                                                                                     EPDTokenize.TOKENIZE);
  public static final PDStringField <String> WEBSITE_URI = new PDStringField<> ("website",
                                                                                Function.identity (),
                                                                                Field.Store.YES,
                                                                                EPDTokenize.TOKENIZE);
  public static final PDStringField <String> CONTACT_TYPE = new PDStringField<> ("bc-description",
                                                                                 Function.identity (),
                                                                                 Field.Store.YES,
                                                                                 EPDTokenize.TOKENIZE);
  public static final PDStringField <String> CONTACT_NAME = new PDStringField<> ("bc-name",
                                                                                 Function.identity (),
                                                                                 Field.Store.YES,
                                                                                 EPDTokenize.TOKENIZE);
  public static final PDStringField <String> CONTACT_PHONE = new PDStringField<> ("bc-phone",
                                                                                  Function.identity (),
                                                                                  Field.Store.YES,
                                                                                  EPDTokenize.TOKENIZE);
  public static final PDStringField <String> CONTACT_EMAIL = new PDStringField<> ("bc-email",
                                                                                  Function.identity (),
                                                                                  Field.Store.YES,
                                                                                  EPDTokenize.TOKENIZE);
  public static final PDStringField <String> ADDITIONAL_INFO = new PDStringField<> ("freetext",
                                                                                    Function.identity (),
                                                                                    Field.Store.YES,
                                                                                    EPDTokenize.TOKENIZE);

  public static final PDStringField <String> METADATA_OWNERID = new PDStringField<> ("md-ownerid",
                                                                                     Function.identity (),
                                                                                     Field.Store.YES,
                                                                                     EPDTokenize.NO_TOKENIZE);
  public static final PDStringField <String> METADATA_REQUESTING_HOST = new PDStringField<> ("md-requestinghost",
                                                                                             Function.identity (),
                                                                                             Field.Store.YES,
                                                                                             EPDTokenize.NO_TOKENIZE);

  private PDField ()
  {}
}
