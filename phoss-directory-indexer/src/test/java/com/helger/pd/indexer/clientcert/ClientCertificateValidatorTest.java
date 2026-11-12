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
package com.helger.pd.indexer.clientcert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.junit.Test;

/**
 * Test class for class {@link ClientCertificateValidator}.
 *
 * @author Philip Helger
 */
public final class ClientCertificateValidatorTest
{
  // Subject: CN=POP000739,O=Billberry O\u00dc,C=EE
  private static final String CERT_NON_ASCII = "-----BEGIN CERTIFICATE-----\n" +
                                         "MIIBjDCCATGgAwIBAgIIFf8eNXUR1b0wCgYIKoZIzj0EAwMwOTELMAkGA1UEBhMC\n" +
                                         "RUUxFjAUBgNVBAoMDUJpbGxiZXJyeSBPw5wxEjAQBgNVBAMTCVBPUDAwMDczOTAg\n" +
                                         "Fw0yNjExMTIxMzUwMTdaGA8yMTI2MTAxOTEzNTAxN1owOTELMAkGA1UEBhMCRUUx\n" +
                                         "FjAUBgNVBAoMDUJpbGxiZXJyeSBPw5wxEjAQBgNVBAMTCVBPUDAwMDczOTBZMBMG\n" +
                                         "ByqGSM49AgEGCCqGSM49AwEHA0IABJ0pRKKo9qolh0jGXfs9PYlty573PYj3Jqzx\n" +
                                         "Tv7fvO7JZtkY85TxAe+aMHoI41RHsAgF/pjXqmHt8p5fVxS6fjmjITAfMB0GA1Ud\n" +
                                         "DgQWBBTPxI2gaEtrgNFdAKn627Gsm5V4szAKBggqhkjOPQQDAwNJADBGAiEAkzWt\n" +
                                         "m5x0GoUSjMAGuz5JEaQRlewPmfCb31AeEAWAjocCIQD+6H1Bvw2YFb+1mDBAdWhG\n" +
                                         "6faGjRCidDYkyOQHQ+mXVQ==\n" +
                                         "-----END CERTIFICATE-----\n";

  // Subject: CN=POP000740,O=Foo\, Inc,C=EE
  private static final String CERT_COMMA = "-----BEGIN CERTIFICATE-----\n" +
                                         "MIIBgjCCASigAwIBAgIJAOcpnAyYqb5bMAoGCCqGSM49BAMDMDQxCzAJBgNVBAYT\n" +
                                         "AkVFMREwDwYDVQQKEwhGb28sIEluYzESMBAGA1UEAxMJUE9QMDAwNzQwMCAXDTI2\n" +
                                         "MTExMjEzNTAxN1oYDzIxMjYxMDE5MTM1MDE3WjA0MQswCQYDVQQGEwJFRTERMA8G\n" +
                                         "A1UEChMIRm9vLCBJbmMxEjAQBgNVBAMTCVBPUDAwMDc0MDBZMBMGByqGSM49AgEG\n" +
                                         "CCqGSM49AwEHA0IABBc3rzeGMwcMaPrfrohShaeI0BGBA2aAb4uWwMo4Qru7OPqi\n" +
                                         "B+JJhQq/I9V9P7QUSw95zz5UNVSFlV4NJSPtDPmjITAfMB0GA1UdDgQWBBS7Kd/m\n" +
                                         "X8pyXJ6ImCJOlTi/7UM6kzAKBggqhkjOPQQDAwNIADBFAiASsjkJmXMIJgwoA8Hg\n" +
                                         "0lZ2yiS8n0VIFrE1nj/gvmN6VQIhAJVcKi++ZaZTtvjyNQk3rJYt9qMXWshdH1p0\n" +
                                         "5fVzmtXY\n" +
                                         "-----END CERTIFICATE-----\n";

  // Subject: CN=POP000741,O=Acme,C=EE,OU=Extra
  private static final String CERT_EXTRA_OU = "-----BEGIN CERTIFICATE-----\n" +
                                         "MIIBmjCCAUCgAwIBAgIJAI10SeNdyzp9MAoGCCqGSM49BAMDMEAxDjAMBgNVBAsT\n" +
                                         "BUV4dHJhMQswCQYDVQQGEwJFRTENMAsGA1UEChMEQWNtZTESMBAGA1UEAxMJUE9Q\n" +
                                         "MDAwNzQxMCAXDTI2MTExMjEzNTAxN1oYDzIxMjYxMDE5MTM1MDE3WjBAMQ4wDAYD\n" +
                                         "VQQLEwVFeHRyYTELMAkGA1UEBhMCRUUxDTALBgNVBAoTBEFjbWUxEjAQBgNVBAMT\n" +
                                         "CVBPUDAwMDc0MTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABJ84rZW+K3vt4HFp\n" +
                                         "iQjZz3ww5SCciOkYD0OrSWgdRuWvgQm/YDyF047AXSRDhbriELtl/ylBnodyDtdp\n" +
                                         "UHMbcGOjITAfMB0GA1UdDgQWBBRplUyRQkSRHYfEOI7R3mzfG40uRzAKBggqhkjO\n" +
                                         "PQQDAwNIADBFAiAXmV7mdredlI9XspsPvpWWf1Wv7E8GamWZPuZgqYfOaAIhAPLx\n" +
                                         "P3uPbY4OEzIE5ViUXeGGUa4gOwTFwY/AR4n8/jxN\n" +
                                         "-----END CERTIFICATE-----\n";

  // Subject: CN=POP000742,C=EE
  private static final String CERT_NO_O = "-----BEGIN CERTIFICATE-----\n" +
                                         "MIIBXDCCAQKgAwIBAgIJALp3krEEJetfMAoGCCqGSM49BAMDMCExCzAJBgNVBAYT\n" +
                                         "AkVFMRIwEAYDVQQDEwlQT1AwMDA3NDIwIBcNMjYxMTEyMTM1MDE3WhgPMjEyNjEw\n" +
                                         "MTkxMzUwMTdaMCExCzAJBgNVBAYTAkVFMRIwEAYDVQQDEwlQT1AwMDA3NDIwWTAT\n" +
                                         "BgcqhkjOPQIBBggqhkjOPQMBBwNCAATeu2AFncnbqOBDpEgs0zZhCHXR4lDNd5Qp\n" +
                                         "HBMkaJHhDujqRL43TN+Aq1vp2Z9Sn/fg4JUYkKDnwfYl8r6qU8cioyEwHzAdBgNV\n" +
                                         "HQ4EFgQUr+VBteGbR3RgiRs2aZ3Aw5jpt8wwCgYIKoZIzj0EAwMDSAAwRQIhAILx\n" +
                                         "/DICmhD88OYsImZK4dtD0OdP4467sfYAV3E7dbM/AiAvFXrTo5FIdbUO5kh/onJJ\n" +
                                         "Tz7NCTN/vYbBskGp/qs+dQ==\n" +
                                         "-----END CERTIFICATE-----\n";

  private static X509Certificate _parse (final String sPEM) throws Exception
  {
    final CertificateFactory aCF = CertificateFactory.getInstance ("X.509");
    return (X509Certificate) aCF.generateCertificate (new ByteArrayInputStream (sPEM.getBytes (StandardCharsets.ISO_8859_1)));
  }

  @Test
  public void testGetClientUniqueIDNonASCII () throws Exception
  {
    final X509Certificate aCert = _parse (CERT_NON_ASCII);
    assertEquals ("CN=POP000739,O=Billberry O\u00dc,C=EE", ClientCertificateValidator.getClientUniqueID (aCert));
    // Issuer matching is done via X500Principal equality
    assertEquals (new X500Principal ("CN=POP000739,O=Billberry O\u00dc,C=EE"), aCert.getSubjectX500Principal ());
  }

  @Test
  public void testGetClientUniqueIDEscapedComma () throws Exception
  {
    final X509Certificate aCert = _parse (CERT_COMMA);
    assertEquals ("CN=POP000740,O=Foo\\, Inc,C=EE", ClientCertificateValidator.getClientUniqueID (aCert));
  }

  @Test
  public void testGetClientUniqueIDIgnoresOtherRDNs () throws Exception
  {
    assertEquals ("CN=POP000741,O=Acme,C=EE",
                  ClientCertificateValidator.getClientUniqueID (_parse (CERT_EXTRA_OU)));
  }

  @Test
  public void testGetClientUniqueIDMissingPart () throws Exception
  {
    assertNotNull (_parse (CERT_NO_O));
    assertNull (ClientCertificateValidator.getClientUniqueID (_parse (CERT_NO_O)));
  }
}
