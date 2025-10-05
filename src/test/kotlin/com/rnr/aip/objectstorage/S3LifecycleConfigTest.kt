package com.rnr.aip.objectstorage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationRequest
import software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationResponse

@ExtendWith(MockitoExtension::class)
class S3LifecycleConfigTest {

    @Mock
    private lateinit var aipS3Client: S3Client

    @Captor
    private lateinit var bucketNameCaptor: ArgumentCaptor<PutBucketLifecycleConfigurationRequest>

    private lateinit var cut: S3LifecycleConfig

    private val bucketName = "test-bucket"
    private val expirationDays = 30
    private val noncurrentVersionExpirationDays = 90

    @BeforeEach
    fun setup() {
        cut = S3LifecycleConfig(aipS3Client, bucketName, expirationDays, noncurrentVersionExpirationDays)
    }

    @Test
    fun `should configure lifecycle policies`() {
        whenever(aipS3Client.putBucketLifecycleConfiguration(any(PutBucketLifecycleConfigurationRequest::class.java)))
            .thenReturn(PutBucketLifecycleConfigurationResponse.builder().build())

        cut.configureLifecyclePolicies()

        verify(aipS3Client, times(1))
            .putBucketLifecycleConfiguration(bucketNameCaptor.capture())

        val capturedRequest = bucketNameCaptor.value
        assertEquals(bucketName, capturedRequest.bucket())

        val rules = capturedRequest.lifecycleConfiguration().rules()
        assertEquals(1, rules.size)

        val tempFileRule = rules.find { it.id() == "Delete temp files" }

        assertNotNull(tempFileRule)
    }
}
