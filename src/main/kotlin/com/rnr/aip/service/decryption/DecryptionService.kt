package com.rnr.aip.service.decryption

import com.microsoft.informationprotection.*
import com.microsoft.informationprotection.file.*
import com.microsoft.informationprotection.internal.callback.FileHandlerObserver
import com.rnr.aip.exception.DecryptionException
import com.rnr.aip.exception.EmptyFileException
import com.rnr.aip.exception.FileProtectionCheckException
import com.rnr.aip.exception.InvalidMipTokenException
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.apache.commons.io.IOUtils
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Paths
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

@Service
class DecryptionService(
    @param:Value("\${app.aip.client-id}")
    private val clientId: String,
    @param:Value("\${app.aip.app-name}")
    private val appName: String,
    @param:Value("\${app.aip.app-version}")
    private val appVersion: String
) {
    private val logger: Logger = LogManager.getLogger()

    private lateinit var mipContext: MipContext
    private val mipDataPath = Paths.get(System.getProperty("java.io.tmpdir"), "mip_sdk_data").toString()

    @PostConstruct
    fun initialize() {
        logger.info("Initializing MIP SDK...")
        try {
            val libPath = System.getProperty("java.library.path")
            logger.info("Java library path: $libPath")

            val appInfo = ApplicationInfo().apply {
                applicationId = clientId
                applicationName = appName
                applicationVersion = appVersion
            }

            val config = MipConfiguration(appInfo, mipDataPath, LogLevel.TRACE, false)
            mipContext = MIP.createMipContext(config)

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

    fun decrypt(inputStream: InputStream, mipToken: String): ByteArray {
        validateMipToken(mipToken)
        validateInputStream(inputStream)

        var tempFile: File? = null
        try {
            logger.debug("Starting decryption process")

            tempFile = createTempFile(inputStream, "decrypt-")

            if (!isFileProtected(tempFile.absolutePath)) {
                throw DecryptionException("File is not protected with Microsoft Information Protection")
            }

            val decryptedBytes = performMipDecryption(tempFile.absolutePath, mipToken)

            logger.debug("Decryption completed successfully")
            return decryptedBytes

        } catch (e: DecryptionException) {
            throw e
        } catch (e: ExecutionException) {
            val cause = e.cause ?: e
            throw DecryptionException("Decryption failed: ${cause.message}", cause)
        } catch (e: Exception) {
            throw DecryptionException("Unexpected error during decryption: ${e.message}", e)
        } finally {
            tempFile?.delete()
        }
    }

    fun isProtected(inputStream: InputStream): Boolean {
        validateInputStream(inputStream)

        inputStream.use { stream ->
            if (stream.markSupported()) {
                stream.mark(Int.MAX_VALUE)
            }

            var tempFile: File? = null
            return try {
                tempFile = createTempFile(stream, "isprotected-")
                val result = isFileProtected(tempFile.absolutePath)

                if (stream.markSupported()) {
                    try {
                        stream.reset()
                    } catch (e: Exception) {
                        logger.warn("Failed to reset input stream", e)
                    }
                }

                result
            } catch (e: Exception) {
                throw FileProtectionCheckException("Failed to check file protection status", e)
            } finally {
                tempFile?.delete()
            }
        }
    }

    private fun validateMipToken(token: String) {
        if (token.isBlank()) {
            throw InvalidMipTokenException("MIP token cannot be empty")
        }
    }

    private fun validateInputStream(inputStream: InputStream) {
        try {
            if (inputStream.available() == 0) {
                throw EmptyFileException("File is empty")
            }
        } catch (e: EmptyFileException) {
            throw e
        } catch (e: Exception) {
            throw FileProtectionCheckException("Failed to read input stream", e)
        }
    }

    fun createTempFile(inputStream: InputStream, prefix: String): File {
        return try {
            val tempFile = File.createTempFile(prefix, ".tmp")
            FileOutputStream(tempFile).use { IOUtils.copy(inputStream, it) }
            tempFile
        } catch (e: Exception) {
            throw DecryptionException("Failed to create temporary file", e)
        }
    }

    fun isFileProtected(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                logger.debug("File does not exist: $filePath")
                return false
            }

            val content = file.readText(Charsets.ISO_8859_1)
            val hasMicrosoftMarker = content.contains("Microsoft", ignoreCase = true)
            val hasProtectionMarker = content.contains("Protection", ignoreCase = true) ||
                    content.contains("ProtectionTemplate", ignoreCase = true) ||
                    content.contains("RightsTemplate", ignoreCase = true)

            hasMicrosoftMarker && hasProtectionMarker
        } catch (e: Exception) {
            logger.debug("Error checking file protection: ${e.message}")
            false
        }
    }

    fun performMipDecryption(filePath: String, token: String): ByteArray {
        var fileProfile: IFileProfile?
        var fileEngine: IFileEngine?
        var fileHandler: IFileHandler?
        var decryptedTempFile: File? = null

        try {
            logger.debug("MIP SDK decryption for file: $filePath")

            fileProfile = createFileProfile(token)
            logger.debug("FileProfile created successfully")

            fileEngine = createFileEngine(fileProfile, token)
            logger.debug("FileEngine created successfully")

            fileHandler = createFileHandler(fileEngine, filePath)
            logger.debug("FileHandler created successfully")

            val protectionHandler = fileHandler?.protection ?: throw DecryptionException("File is not protected")

            val hasViewRights = protectionHandler.getAccessCheck("VIEW")
            val hasExtractRights = protectionHandler.getAccessCheck("EXTRACT")

            if (!hasViewRights && !hasExtractRights) {
                throw DecryptionException("User does not have rights to decrypt this file")
            }

            logger.debug("User has permission to decrypt the file")

            val decryptedFilePath = fileHandler.decryptedTemporaryFileAsync.get()
            decryptedTempFile = File(decryptedFilePath)

            if (!decryptedTempFile.exists()) {
                throw DecryptionException("Decrypted temporary file not found")
            }

            val decryptedBytes = decryptedTempFile.readBytes()
            logger.debug("File decrypted successfully, size: ${decryptedBytes.size} bytes")

            return decryptedBytes

        } catch (e: ExecutionException) {
            val cause = e.cause ?: e
            logger.error("MIP SDK decryption failed during async operation", cause)
            throw DecryptionException("MIP SDK decryption failed: ${cause.message}", cause)
        } catch (e: InterruptedException) {
            logger.error("MIP SDK decryption was interrupted", e)
            Thread.currentThread().interrupt()
            throw DecryptionException("MIP SDK decryption was interrupted", e)
        } catch (e: DecryptionException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error during MIP SDK decryption", e)
            throw DecryptionException("MIP SDK decryption failed: ${e.message}", e)
        } finally {
            try {
                decryptedTempFile?.delete()
            } catch (e: Exception) {
                logger.warn("Error deleting temporary file", e)
            }
        }
    }

    private fun createFileProfile(token: String): IFileProfile {
        val profileSettings = FileProfileSettings(
            mipContext,
            CacheStorageType.ON_DISK_ENCRYPTED,
            ConsentDelegate()
        )

        return MIP.loadFileProfileAsync(profileSettings).get()
    }

    private fun createFileEngine(fileProfile: IFileProfile, token: String): IFileEngine {
        val authDelegate = AuthDelegate(token)

        val engineSettings = FileEngineSettings(
            "",  // Engine ID - empty string for unique engine
            authDelegate,
            "",  // Client data - optional
            "en-US"  // Locale
        )

        return fileProfile.addEngineAsync(engineSettings).get()
    }

    private fun createFileHandler(fileEngine: IFileEngine, filePath: String): IFileHandler? {
        val handlerFuture: Future<IFileHandler?> = fileEngine.createFileHandlerAsync(
            filePath,
            filePath,
            true,
            FileHandlerObserver(),
            null
        )
        return handlerFuture.get()
    }
}