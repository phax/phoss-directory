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
package com.helger.pd.publisher.aws;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.string.StringHelper;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public final class AwsHelper
{
  // We run everything in EU West 1 (=Ireland)
  private static final Region AWS_REGION = Region.EU_WEST_1;

  private static final Logger LOGGER = LoggerFactory.getLogger (AwsHelper.class);

  private static final AwsCredentialsProvider CP;
  static
  {
    CP = null;
  }

  public static final S3Client S3 = S3Client.builder ().region (AWS_REGION).credentialsProvider (CP).build ();

  private AwsHelper ()
  {}

  @Nullable
  private static ResponseInputStream <GetObjectResponse> _getS3Object (final String sBucketName, final String sKey)
  {
    try
    {
      final String sRealKey = StringHelper.trimStartAndEnd (sKey, '/');

      LOGGER.info ("Reading from S3 '" + sBucketName + "' / '" + sRealKey + "'");

      return S3.getObject (GetObjectRequest.builder ().bucket (sBucketName).key (sRealKey).build ());
    }
    catch (final RuntimeException ex)
    {
      throw new IllegalStateException ("Failed to read content of '" + sKey + "' in S3 bucket '" + sBucketName + "'",
                                       ex);
    }
  }

  @Nullable
  public static String getS3ObjectContentAsString (final String sBucketName, final String sKey)
  {
    final ResponseInputStream <GetObjectResponse> s3object = _getS3Object (sBucketName, sKey);
    // The assumption of UTF-8 was explicitly discussed
    return StreamHelper.getAllBytesAsString (s3object, StandardCharsets.UTF_8);
  }

  @Nullable
  public static byte [] getS3ObjectContentAsBytes (final String sBucketName, final String sKey)
  {
    final ResponseInputStream <GetObjectResponse> s3object = _getS3Object (sBucketName, sKey);
    return StreamHelper.getAllBytes (s3object);
  }

  private static void _writeToS3 (@Nonnull @Nonempty final String sTargetBucket,
                                  @Nonnull @Nonempty final String sTargetKey,
                                  @Nonnull final IMimeType aMimeType,
                                  @Nonnull final RequestBody aRequestBody,
                                  @Nonnull final String sDebugWhatIsIt) throws IllegalStateException
  {
    final String sLogLocation = "S3 '" + sTargetBucket + "' / '" + sTargetKey + "'";
    LOGGER.info ("Writing " + sDebugWhatIsIt + " to " + sLogLocation);
    final PutObjectResponse aPutResponse = AwsHelper.S3.putObject (PutObjectRequest.builder ()
                                                                                    .bucket (sTargetBucket)
                                                                                    .key (sTargetKey)
                                                                                    .contentType (aMimeType.getAsString ())
                                                                                    .build (), aRequestBody);
    if (!aPutResponse.sdkHttpResponse ().isSuccessful ())
      throw new IllegalStateException ("Failed to putObject " + sDebugWhatIsIt + " to " + sLogLocation);

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Successfully putObject " + sDebugWhatIsIt + " to " + sLogLocation);
  }

  public static void writeToS3Xml (@Nonnull @Nonempty final String sTargetBucket,
                                   @Nonnull @Nonempty final String sTargetKey,
                                   @Nonnull final byte [] aBytes,
                                   @Nonnull final String sDebugWhatIsIt) throws IllegalStateException
  {
    _writeToS3 (sTargetBucket, sTargetKey, CMimeType.APPLICATION_XML, RequestBody.fromBytes (aBytes), sDebugWhatIsIt);
  }

  public static void writeToS3Xml (@Nonnull @Nonempty final String sTargetBucket,
                                   @Nonnull @Nonempty final String sTargetKey,
                                   @Nonnull final String sPayload,
                                   @Nonnull final String sDebugWhatIsIt) throws IllegalStateException
  {
    _writeToS3 (sTargetBucket,
                sTargetKey,
                CMimeType.APPLICATION_XML,
                RequestBody.fromString (sPayload),
                sDebugWhatIsIt);
  }

  public static void writeToS3OctetStream (@Nonnull @Nonempty final String sTargetBucket,
                                           @Nonnull @Nonempty final String sTargetKey,
                                           @Nonnull final byte [] aBytes,
                                           @Nonnull final String sDebugWhatIsIt) throws IllegalStateException
  {
    _writeToS3 (sTargetBucket,
                sTargetKey,
                CMimeType.APPLICATION_OCTET_STREAM,
                RequestBody.fromBytes (aBytes),
                sDebugWhatIsIt);
  }
}
