package com.rnr.aip.objectstorage


import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Profile("!test")
@Configuration
class S3LifecycleConfig(
    @param:Qualifier("etlS3Client") private val etlS3Client: S3Client,
    @param:Value("\${app.aip.s3.upload.bucket}") private val bucketName: String,
    @param:Value("\${app.aip.s3.expiration-in-days}") private val expirationDays: Int,
    @param:Value("\${app.aip.s3.noncurrent-version-expiration-in-days}") private val noncurrentVersionExpirationDays: Int,
) {

    private val logger: Logger = LogManager.getLogger()

    @PostConstruct
    fun configureLifecyclePolicies() {
        logger.info("Configure lifecycle policy to bucket: $bucketName")
        val rawRule = LifecycleRule.builder()
            .id("Delete temp files")
            .filter(LifecycleRuleFilter.builder().prefix("temp/").build())
            .status(ExpirationStatus.ENABLED)
            .expiration(LifecycleExpiration.builder().days(expirationDays).build())
            .noncurrentVersionExpiration(
                NoncurrentVersionExpiration.builder()
                    .noncurrentDays(noncurrentVersionExpirationDays)
                    .newerNoncurrentVersions(noncurrentVersionExpirationDays)
                    .build()
            )
            .build()

        val configuration = BucketLifecycleConfiguration.builder().rules(rawRule).build()

        etlS3Client.putBucketLifecycleConfiguration(
            PutBucketLifecycleConfigurationRequest.builder()
                .bucket(bucketName)
                .lifecycleConfiguration(configuration)
                .build()
        )
        logger.info("Successfully applied lifecycle policy to bucket: $bucketName")
    }
}