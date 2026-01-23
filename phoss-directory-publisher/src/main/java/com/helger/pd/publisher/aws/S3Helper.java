package com.helger.pd.publisher.aws;

import java.io.File;
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
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedCopy;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.Copy;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;

public final class S3Helper
{
  private static final Logger LOGGER = LoggerFactory.getLogger (S3Helper.class);

  // We run everything in EU West 1 (=Ireland)
  // Should be provided via EnvVar

  public static final S3Client S3_SYNC;
  public static final S3AsyncClient S3_ASYNC;
  public static final String S3_PUBLIC_URL;

  static
  {
    final String sBucketName = PDServerConfiguration.getS3BucketName ();

    final S3ClientBuilder aS3Builder;
    S3AsyncClientBuilder aS3AsyncBuilder;

    if ("true".equals (SystemProperties.getPropertyValue ("pd.aws.localstack")))
    {
      LOGGER.info ("Configuring S3 for LocalStack mode");
      final URI aEndpointOverride = URI.create ("http://localhost:4566");
      aS3Builder = S3Client.builder ()
                           .region (Region.US_EAST_1)
                           .credentialsProvider (StaticCredentialsProvider.create (AwsBasicCredentials.create ("test",
                                                                                                               "test")))
                           .endpointOverride (aEndpointOverride);
      aS3AsyncBuilder = S3AsyncClient.builder ()
                                     .region (Region.US_EAST_1)
                                     .credentialsProvider (StaticCredentialsProvider.create (AwsBasicCredentials.create ("test",
                                                                                                                         "test")))
                                     .endpointOverride (aEndpointOverride);
      // Constant URL for local stuff
      S3_PUBLIC_URL = "http://" + sBucketName + ".s3-website.localhost.localstack.cloud:4566/";
    }
    else
    {
      LOGGER.info ("Configuring S3 for AWS mode");
      aS3Builder = S3Client.builder ().region (Region.EU_WEST_1);
      aS3AsyncBuilder = S3AsyncClient.builder ().region (Region.EU_WEST_1);
      // Constant URL for local stuff
      S3_PUBLIC_URL = PDServerConfiguration.getS3WebsiteURLWithTrailingSlash ();
    }

    S3_SYNC = aS3Builder.serviceConfiguration (S3Configuration.builder ()
                                                              .pathStyleAccessEnabled (Boolean.TRUE)
                                                              .build ()).build ();
    S3_ASYNC = aS3AsyncBuilder.serviceConfiguration (S3Configuration.builder ()
                                                                    .pathStyleAccessEnabled (Boolean.TRUE)
                                                                    .build ()).build ();
  }

  @Nullable
  public static ResponseInputStream <GetObjectResponse> getS3Object (@NonNull @Nonempty final String sBucketName,
                                                                     @NonNull @Nonempty final String sKey)
  {
    try
    {
      LOGGER.info ("Reading from S3 '" + sBucketName + "' / '" + sKey + "'");

      return S3_SYNC.getObject (GetObjectRequest.builder ().bucket (sBucketName).key (sKey).build ());
    }
    catch (final RuntimeException ex)
    {
      throw new IllegalStateException ("Failed to read content of S3 '" + sBucketName + "' / '" + sKey + "'", ex);
    }
  }

  public static void putS3Object (@NonNull @Nonempty final String sBucketName,
                                  @NonNull @Nonempty final String sKey,
                                  @NonNull final File aFileToUpload,
                                  @NonNull final IMimeType aMimeType,
                                  @NonNull final String sContentDisposition)
  {
    LOGGER.info ("Writing to S3 '" + sBucketName + "' / '" + sKey + "' as '" + aMimeType.getAsString () + "'");

    try
    {
      final S3TransferManager aTransferMgr = S3TransferManager.builder ().s3Client (S3_ASYNC).build ();

      // main upload; TransferManager picks strategy based on size
      // Hmm: the checksum256 is not stored in LocalStack
      final PutObjectRequest aPutReq = PutObjectRequest.builder ()
                                                       .bucket (sBucketName)
                                                       .key (sKey)
                                                       .contentType (aMimeType.getAsString ())
                                                       .contentDisposition (sContentDisposition)
                                                       // Cache for 6h
                                                       .cacheControl ("max-age=21600, public")
                                                       .build ();

      final FileUpload aUpload = aTransferMgr.uploadFile (x -> x.putObjectRequest (aPutReq)
                                                                .source (aFileToUpload)
                                                                .addTransferListener (LoggingTransferListener.create ()));

      // wait for completion
      final CompletedFileUpload aCompletedUpload = aUpload.completionFuture ().join ();
      if (!aCompletedUpload.response ().sdkHttpResponse ().isSuccessful ())
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
      final DeleteObjectResponse aResponse = S3_SYNC.deleteObject (DeleteObjectRequest.builder ()
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
                                       @NonNull @Nonempty final String sNewKey,
                                       @NonNull final IMimeType aMimeType,
                                       @NonNull final String sContentDisposition)
  {
    LOGGER.info ("Copying on S3 '" + sBucketName + "' / '" + sOldKey + "' to '" + sNewKey + "'");

    final S3TransferManager aTransferMgr = S3TransferManager.builder ().s3Client (S3_ASYNC).build ();

    // multipart copy; TransferManager picks strategy based on size
    final CopyObjectRequest aCopyReq = CopyObjectRequest.builder ()
                                                        .sourceBucket (sBucketName)
                                                        .sourceKey (sOldKey)
                                                        .destinationBucket (sBucketName)
                                                        .destinationKey (sNewKey)
                                                        .metadataDirective (MetadataDirective.REPLACE)
                                                        .contentType (aMimeType.getAsString ())
                                                        .contentDisposition (sContentDisposition)
                                                        .build ();

    final Copy aCopy = aTransferMgr.copy (x -> x.copyObjectRequest (aCopyReq)
                                                .addTransferListener (LoggingTransferListener.create ()));
    // wait for completion
    final CompletedCopy aCompletedCopy = aCopy.completionFuture ().join ();
    if (!aCompletedCopy.response ().sdkHttpResponse ().isSuccessful ())
      return ESuccess.FAILURE;

    if (false)
    {
      final HeadObjectResponse aHead = S3_SYNC.headObject (x -> x.bucket (sBucketName).key (sNewKey));
      LOGGER.info ("HeadResponse: " + aHead);
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Successfully copied S3 '" + sBucketName + "' / '" + sOldKey + "' to '" + sNewKey + "'");
    return ESuccess.SUCCESS;
  }
}
