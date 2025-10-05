package com.rnr.aip.objectstorage


import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.multipart.MultipartConfiguration
import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.transfer.s3.SizeConstant.MB
import java.net.URI

@Profile("!test")
@Configuration
class S3ClientFactory {
    private val logger = LogManager.getLogger()

    /**
     * S3 client configuration for RNR AIP
     */
    @Bean
    fun aipS3Client(
        @Value("\${app.aip.s3.endpoint}") endpoint: String,
        @Value("\${app.aip.s3.region}") region: String,
        @Value("\${app.aip.s3.access-key}") accessKey: String,
        @Value("\${app.aip.s3.secret-key}") secretKey: String,
    ) = createS3Client(endpoint, region, accessKey, secretKey)


    private fun createS3Client(
        endpoint: String,
        region: String,
        accessKey: String,
        secretKey: String,
    ): S3Client {
        logger.info("Connecting to S3 endpoint: $endpoint")
        return S3Client.builder()
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
            .region(Region.of(region))
            .endpointOverride(URI.create(endpoint))
            .forcePathStyle(true)
            .build()
    }


    @Bean
    fun aipS3TransferManager(
        @Value("\${app.aip.s3.endpoint}") endpoint: String,
        @Value("\${app.aip.s3.region}") region: String,
        @Value("\${app.aip.s3.access-key}") accessKey: String,
        @Value("\${app.aip.s3.secret-key}") secretKey: String,
        @Value("\${app.aip.s3.upload-part-size:2500}") partSize: Int
    ): S3TransferManager {
        val aipAsyncS3Client = aipAsyncS3Client(endpoint, region, accessKey, secretKey, partSize)

        return S3TransferManager.builder()
            .s3Client(aipAsyncS3Client)
            .build()
    }


    private fun aipAsyncS3Client(
        endpoint: String,
        region: String,
        accessKey: String,
        secretKey: String,
        partSize: Int
    ) = S3AsyncClient.builder()
        .endpointOverride(URI.create(endpoint))
        .forcePathStyle(true)
        .multipartEnabled(true)
        .asyncConfiguration(ClientAsyncConfiguration.builder().build())
        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        .region(Region.of(region))
        .multipartConfiguration(MultipartConfiguration.builder().minimumPartSizeInBytes(partSize * MB).build())
        .build()
        .also {
            logger.info("Creating async S3 client for rnr-aip with endpoint: $endpoint and multipart size: $partSize MB")
        }
}

