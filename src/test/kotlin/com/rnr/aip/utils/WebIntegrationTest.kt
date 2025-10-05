package com.rnr.aip.utils

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.lang.annotation.Inherited

/**
 * This annotation class provides Spring configuration for web tests.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@SpringBootTest(
    properties = [
        "app.aip.client-id=test-client-id",
        "app.aip.app-name=test-app",
        "app.aip.app-version=1.0"
    ]
)
@AutoConfigureMockMvc
@ActiveProfiles("test")

annotation class WebIntegrationTest
