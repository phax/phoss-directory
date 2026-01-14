package com.helger.pd.publisher.exportall;

import java.net.URI;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.state.ESuccess;
import com.helger.base.system.SystemProperties;
import com.helger.mime.IMimeType;
import com.helger.pd.indexer.settings.PDServerConfiguration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedCopy;
import software.amazon.awssdk.transfer.s3.model.Copy;

public final class S3Helper
{
  private static final Logger LOGGER = LoggerFactory.getLogger (S3Helper.class);

  // We run everything in EU West 1 (=Ireland)
  // Should be provided via EnvVar

  public static final S3Client S3;
  public static final S3AsyncClient S3_ASYNC;
  public static final String S3_PUBLIC_URL;

  static
  {
    final String sBucketName = PDServerConfiguration.getS3BucketName ();
    if ("true".equals (SystemProperties.getPropertyValue ("pd.aws.localstack")))
    {
      LOGGER.info ("Configuring S3 for LocalStack mode");
      final URI aEndpointOverride = URI.create ("http://localhost:4566");
      S3 = S3Client.builder ()
                   .region (Region.US_EAST_1)
                   .credentialsProvider (StaticCredentialsProvider.create (AwsBasicCredentials.create ("test", "test")))
                   .endpointOverride (aEndpointOverride)
                   .serviceConfiguration (S3Configuration.builder ().pathStyleAccessEnabled (Boolean.TRUE).build ())
                   .build ();
      S3_ASYNC = S3AsyncClient.builder ()
                              .region (Region.US_EAST_1)
                              .credentialsProvider (StaticCredentialsProvider.create (AwsBasicCredentials.create ("test",
                                                                                                                  "test")))
                              .endpointOverride (aEndpointOverride)
                              .serviceConfiguration (S3Configuration.builder ()
                                                                    .pathStyleAccessEnabled (Boolean.TRUE)
                                                                    .build ())
                              .build ();
      // Constant URL for local stuff
      S3_PUBLIC_URL = "http://" + sBucketName + ".s3-website.localhost.localstack.cloud:4566/";
    }
    else
    {
      LOGGER.info ("Configuring S3 for AWS mode");
      S3 = S3Client.builder ()
                   .region (Region.US_WEST_1)
                   .serviceConfiguration (S3Configuration.builder ().pathStyleAccessEnabled (Boolean.TRUE).build ())
                   .build ();
      S3_ASYNC = S3AsyncClient.builder ()
                              .region (Region.US_WEST_1)
                              .serviceConfiguration (S3Configuration.builder ()
                                                                    .pathStyleAccessEnabled (Boolean.TRUE)
                                                                    .build ())
                              .build ();
      // Constant URL for local stuff
      S3_PUBLIC_URL = PDServerConfiguration.getS3WebsiteURLWithTrailingSlash ();
    }
  }

  @Nullable
  public static ResponseInputStream <GetObjectResponse> getS3Object (@NonNull @Nonempty final String sBucketName,
                                                                     @NonNull @Nonempty final String sKey)
  {
    try
    {
      LOGGER.info ("Reading from S3 '" + sBucketName + "' / '" + sKey + "'");

      return S3.getObject (GetObjectRequest.builder ().bucket (sBucketName).key (sKey).build ());
    }
    catch (final RuntimeException ex)
    {
      throw new IllegalStateException ("Failed to read content of S3 '" + sBucketName + "' / '" + sKey + "'", ex);
    }
  }

  public static void putS3Object (@NonNull @Nonempty final String sBucketName,
                                  @NonNull @Nonempty final String sKey,
                                  @NonNull final IMimeType aMimeType,
                                  @NonNull final RequestBody aRequestBody)
  {
    LOGGER.info ("Writing to S3 '" + sBucketName + "' / '" + sKey + "' as '" + aMimeType.getAsString () + "'");

    try
    {
      final PutObjectResponse aResponse = S3.putObject (PutObjectRequest.builder ()
                                                                        .bucket (sBucketName)
                                                                        .key (sKey)
                                                                        .contentType (aMimeType.getAsString ())
                                                                        .build (), aRequestBody);
      if (!aResponse.sdkHttpResponse ().isSuccessful ())
        throw new IllegalStateException ("Failed to put S3 object '" + sBucketName + "' / '" + sKey + "'");

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Successfully wrote to S3 '" + sBucketName + "' / '" + sKey + "'");
    }
    catch (final IllegalStateException ex)
    {
      throw ex;
    }
    catch (final RuntimeException ex)
    {
      throw new IllegalStateException ("Failed to write to S3 '" + sBucketName + "' / '" + sKey + "'", ex);
    }
  }

  @NonNull
  public static ESuccess deleteS3Object (@NonNull @Nonempty final String sBucketName,
                                         @NonNull @Nonempty final String sKey)
  {
    LOGGER.info ("Deleting from S3 '" + sBucketName + "' / '" + sKey + "'");

    try
    {
      final DeleteObjectResponse aResponse = S3.deleteObject (DeleteObjectRequest.builder ()
                                                                                 .bucket (sBucketName)
                                                                                 .key (sKey)
                                                                                 .build ());
      if (!aResponse.sdkHttpResponse ().isSuccessful ())
        return ESuccess.FAILURE;

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Successfully deleted from S3 '" + sBucketName + "' / '" + sKey + "'");
      return ESuccess.SUCCESS;
    }
    catch (final RuntimeException ex)
    {
      throw new IllegalStateException ("Failed to delete from S3 '" + sBucketName + "' / '" + sKey + "'", ex);
    }
  }

  @NonNull
  public static ESuccess copyS3Object (@NonNull @Nonempty final String sBucketName,
                                       @NonNull @Nonempty final String sOldKey,
                                       @NonNull @Nonempty final String sNewKey)
  {
    LOGGER.info ("Copying on S3 '" + sBucketName + "' / '" + sOldKey + "' to '" + sNewKey + "'");

    final S3TransferManager aTransferMgr = S3TransferManager.builder ().s3Client (S3_ASYNC).build ();

    // multipart copy; TransferManager picks strategy based on size
    final CopyObjectRequest aCopyReq = CopyObjectRequest.builder ()
                                                        .sourceBucket (sBucketName)
                                                        .sourceKey (sOldKey)
                                                        .destinationBucket (sBucketName)
                                                        .destinationKey (sNewKey)
                                                        .build ();

    final Copy aCopy = aTransferMgr.copy (c -> c.copyObjectRequest (aCopyReq));
    // wait for completion
    final CompletedCopy aCompletedCopy = aCopy.completionFuture ().join ();
    if (!aCompletedCopy.response ().sdkHttpResponse ().isSuccessful ())
      return ESuccess.FAILURE;

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Successfully copied S3 '" + sBucketName + "' / '" + sOldKey + "' to '" + sNewKey + "'");
    return ESuccess.SUCCESS;
  }
}
