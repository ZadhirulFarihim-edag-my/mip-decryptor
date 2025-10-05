package com.rnr.aip.service.decryption

import com.microsoft.informationprotection.CacheStorageType
import com.microsoft.informationprotection.MIP
import com.microsoft.informationprotection.file.*
import com.microsoft.informationprotection.internal.callback.FileHandlerObserver
import com.microsoft.informationprotection.protection.IProtectionHandler
import com.rnr.aip.exception.DecryptionException
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.stereotype.Component
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

@Component
class MipFileHandler(private val mipSdkInitializer: MipSdkInitializer) {
    private val logger: Logger = LogManager.getLogger()

    fun decryptFile(filePath: String, token: String): ByteArray {
        var fileProfile: IFileProfile? = null
        var fileEngine: IFileEngine? = null
        var fileHandler: IFileHandler? = null
        var decryptedTempFile: File? = null

        try {
            logger.debug("MIP SDK decryption for file: $filePath")

            fileProfile = createFileProfile(token)
            logger.debug("FileProfile created successfully")

            fileEngine = createFileEngine(fileProfile, token)
            logger.debug("FileEngine created successfully")

            fileHandler = createFileHandler(fileEngine, filePath)
            logger.debug("FileHandler created successfully")

            val protectionHandler = fileHandler?.protection
                ?: throw DecryptionException("File is not protected")

            validateUserRights(protectionHandler)

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

    private fun validateUserRights(protectionHandler: IProtectionHandler) {
        val hasViewRights = protectionHandler.getAccessCheck("VIEW")
        val hasExtractRights = protectionHandler.getAccessCheck("EXTRACT")

        if (!hasViewRights && !hasExtractRights) {
            throw DecryptionException("User does not have rights to decrypt this file")
        }

        logger.debug("User has permission to decrypt the file")
    }

    private fun createFileProfile(token: String): IFileProfile {
        val profileSettings = FileProfileSettings(
            mipSdkInitializer.mipContext,
            CacheStorageType.ON_DISK_ENCRYPTED,
            ConsentDelegate()
        )

        return MIP.loadFileProfileAsync(profileSettings).get()
    }

    private fun createFileEngine(fileProfile: IFileProfile, token: String): IFileEngine {
        val authDelegate = AuthDelegate(token)

        val engineSettings = FileEngineSettings(
            "",
            authDelegate,
            "",
            "en-US"
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