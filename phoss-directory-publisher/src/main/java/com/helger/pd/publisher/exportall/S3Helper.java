package com.helger.pd.publisher.exportall;

import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.WillNotClose;
import com.helger.base.state.ESuccess;
import com.helger.mime.IMimeType;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
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
  private static final Region AWS_REGION = true ? Region.US_EAST_1 : Region.EU_WEST_1;

  public static final S3Client S3;
  public static final S3AsyncClient S3_ASYNC;

  static
  {
    S3 = S3Client.builder ()
                 .region (AWS_REGION)
                 .endpointOverride (URI.create ("http://localhost:4566"))
                 .credentialsProvider (StaticCredentialsProvider.create (AwsBasicCredentials.create ("test", "test")))
                 .serviceConfiguration (S3Configuration.builder ().pathStyleAccessEnabled (Boolean.TRUE).build ())
                 .build ();
    S3_ASYNC = S3AsyncClient.crtBuilder ()
                            .region (AWS_REGION)
                            .endpointOverride (URI.create ("http://localhost:4566"))
                            .credentialsProvider (StaticCredentialsProvider.create (AwsBasicCredentials.create ("test",
                                                                                                                "test")))
                            .build ();
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

  private static void _putS3Object (@NonNull @Nonempty final String sBucketName,
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
  public static PutObjectResponse putObjectFromStreamCrt (@NonNull @Nonempty final String sBucketName,
                                                          @NonNull @Nonempty final String sKey,
                                                          @NonNull final IMimeType aMimeType,
                                                          @NonNull @WillNotClose final InputStream aIS)
  {
    LOGGER.info ("Writing async S3 '" + sBucketName + "' / '" + sKey + "' as '" + aMimeType.getAsString () + "'");

    try
    {
      // Executor required to handle reading from the InputStream on a separate thread so the main
      // upload is not blocked.
      final ExecutorService aExecutor = Executors.newSingleThreadExecutor ();
      try
      {
        // Specify "null" for the content length as the length is unknown
        final AsyncRequestBody aBody = AsyncRequestBody.fromInputStream (aIS, null, aExecutor);

        final CompletableFuture <PutObjectResponse> aFuture = S3_ASYNC.putObject (putObjReq -> putObjReq.bucket (sBucketName)
                                                                                                        .key (sKey)
                                                                                                        .contentType (aMimeType.getAsString ()),
                                                                                  aBody);

        // Wait for the response.
        final PutObjectResponse aResponse = aFuture.join ();

        if (!aResponse.sdkHttpResponse ().isSuccessful ())
          throw new IllegalStateException ("Failed to put async S3 '" +
                                           sBucketName +
                                           "' / '" +
                                           sKey +
                                           "': " +
                                           aResponse.sdkHttpResponse ());

        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Successfully wrote async S3 '" + sBucketName + "' / '" + sKey + "'");

        return aResponse;
      }
      finally
      {
        aExecutor.shutdown ();
      }
    }
    catch (final IllegalStateException ex)
    {
      throw ex;
    }
    catch (final RuntimeException ex)
    {
      throw new IllegalStateException ("Failed to write async S3 '" + sBucketName + "' / '" + sKey + "'", ex);
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
