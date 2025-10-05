package com.rnr.aip.service.decryption

import com.microsoft.informationprotection.*
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Paths

@Component
class MipSdkInitializer(
    @param:Value("\${app.aip.client-id}") private val clientId: String,
    @param:Value("\${app.aip.app-name}") private val appName: String,
    @param:Value("\${app.aip.app-version}") private val appVersion: String
) {
    private val logger: Logger = LogManager.getLogger()
    private val mipDataPath = Paths.get(System.getProperty("java.io.tmpdir"), "mip_sdk_data").toString()

    lateinit var mipContext: MipContext

    @PostConstruct
    fun initialize() {
        logger.info("Initializing MIP SDK...")

        try {
            val platformInfo = PlatformDetector.detectPlatform()
            logPlatformInfo(platformInfo)

            val nativeLibraryPath = resolveNativeLibraryPath(platformInfo)
            validateLibraryExists(nativeLibraryPath, platformInfo)

            if (platformInfo.isWindows) {
                updateWindowsPath(platformInfo)
            }

            loadNativeLibrary(nativeLibraryPath)
            initializeMipContext()

            logger.info("MIP SDK initialized successfully.")
        } catch (e: Exception) {
            logger.error("Failed to initialize MIP SDK", e)
            throw RuntimeException("MIP SDK initialization failed: ${e.message}", e)
        }
    }

    @PreDestroy
    fun shutdown() {
        logger.info("Shutting down MIP SDK.")
        try {
            if (::mipContext.isInitialized) {
                logger.info("MIP SDK context will be released on application shutdown.")
            }
        } catch (e: Exception) {
            logger.error("Error during MIP SDK shutdown", e)
        }
    }

    private fun logPlatformInfo(platform: PlatformInfo) {
        logger.info("Platform Detection:")
        logger.info("  OS: ${platform.osName}")
        logger.info("  Architecture: ${platform.architecture}")
        logger.info("  Bitness: ${if (platform.is64Bit) "64-bit" else "32-bit"}")
        logger.info("  Java library path: ${System.getProperty("java.library.path")}")
    }

    private fun resolveNativeLibraryPath(platform: PlatformInfo): String {
        val projectRoot = System.getProperty("user.dir")

        val archSubdir = when (platform.architecture) {
            Architecture.ARM64 -> "arm64"
            Architecture.AMD64 -> "amd64"
            Architecture.X86 -> "x86"
            Architecture.UNKNOWN -> "amd64"
        }

        val libraryName = when {
            platform.isWindows -> "mip_java.dll"
            platform.isMac -> "mip_java.dylib"
            else -> "mip_java.so"
        }

        val libraryPath = Paths.get(projectRoot, "libs", archSubdir, libraryName)
            .toAbsolutePath()
            .toString()

        logger.info("Resolved native library path: $libraryPath")
        return libraryPath
    }

    private fun validateLibraryExists(libraryPath: String, platform: PlatformInfo) {
        val libFile = File(libraryPath)

        if (!libFile.exists()) {
            val errorMessage = buildErrorMessage(libraryPath, platform)
            throw RuntimeException(errorMessage)
        }

        logger.info("Native library file found: $libraryPath")
    }

    private fun buildErrorMessage(libraryPath: String, platform: PlatformInfo): String {
        return buildString {
            appendLine("═".repeat(80))
            appendLine("NATIVE LIBRARY NOT FOUND")
            appendLine("═".repeat(80))
            appendLine()
            appendLine("Expected location: $libraryPath")
            appendLine()
            appendLine("Platform Information:")
            appendLine("  OS: ${platform.osName}")
            appendLine("  Architecture: ${platform.architecture}")
            appendLine("  Bitness: ${if (platform.is64Bit) "64-bit" else "32-bit"}")
            appendLine()
            appendLine("Required library structure:")
            appendLine("  libs/")
            appendLine("    ├── amd64/    (for x86_64/AMD64 processors)")
            appendLine("    ├── arm64/    (for ARM64 processors)")
            appendLine("    └── x86/      (for 32-bit x86 processors)")
            appendLine()

            if (platform.isWindows && platform.is64Bit) {
                appendLine("⚠️  ARCHITECTURE MISMATCH DETECTED")
                appendLine("Your JVM is 64-bit ${platform.architecture}, but the library may not be available.")
                appendLine()
                appendLine("Solutions:")
                appendLine("  1. Place the correct mip_java.dll in libs/${platform.architecture.name.lowercase()}/")
                appendLine("  2. Ensure you have the ${platform.architecture} version of the library")
                appendLine("  3. If using ARM64, verify ARM64 compatibility")
            } else {
                appendLine("Solution:")
                appendLine("  Place the native library in the correct directory based on your architecture.")
            }
            appendLine()
            appendLine("═".repeat(80))
        }
    }

    private fun updateWindowsPath(platform: PlatformInfo) {
        try {
            val projectRoot = System.getProperty("user.dir")
            val archSubdir = when (platform.architecture) {
                Architecture.ARM64 -> "arm64"
                Architecture.AMD64 -> "amd64"
                else -> "amd64"
            }

            val libsDir = Paths.get(projectRoot, "libs", archSubdir).toAbsolutePath().toString()
            val currentPath = System.getenv("PATH") ?: ""
            val newPath = "$libsDir;$currentPath"

            val envClass = System.getenv().javaClass
            val field = envClass.getDeclaredField("m")
            field.isAccessible = true

            @Suppress("UNCHECKED_CAST")
            val map = field.get(System.getenv()) as MutableMap<String, String>
            map["PATH"] = newPath

            logger.info("Updated PATH to include: $libsDir")
        } catch (e: Exception) {
            logger.warn("Could not update PATH environment variable: ${e.message}")
            logger.warn("Ensure all dependent DLLs are in the same directory as the main library")
        }
    }

    private fun loadNativeLibrary(libraryPath: String) {
        System.load(libraryPath)
        logger.info("Native library loaded successfully: $libraryPath")
    }

    private fun initializeMipContext() {
        val appInfo = ApplicationInfo().apply {
            applicationId = clientId
            applicationName = appName
            applicationVersion = appVersion
        }

        val config = MipConfiguration(appInfo, mipDataPath, LogLevel.TRACE, false)
        mipContext = MIP.createMipContext(config)

        logger.info("MIP context created successfully")
    }
}
