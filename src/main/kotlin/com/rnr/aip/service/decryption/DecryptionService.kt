package com.rnr.aip.service.decryption

import com.rnr.aip.exception.DecryptionException
import com.rnr.aip.exception.FileProtectionCheckException
import com.rnr.aip.utils.FileProtectionChecker
import com.rnr.aip.utils.TempFileManager
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.util.concurrent.ExecutionException

@Service
class DecryptionService(
    private val fileValidator: FileValidator,
    private val tempFileManager: TempFileManager,
    private val fileProtectionChecker: FileProtectionChecker,
    private val mipFileHandler: MipFileHandler
) {
    private val logger: Logger = LogManager.getLogger()

    fun decrypt(inputStream: InputStream, mipToken: String): ByteArray {
        fileValidator.validateMipToken(mipToken)
        fileValidator.validateInputStream(inputStream)

        var tempFile: File? = null
        try {
            logger.debug("Starting decryption process")

            tempFile = tempFileManager.createTempFile(inputStream, "decrypt-")

            if (!fileProtectionChecker.isFileProtected(tempFile.absolutePath)) {
                throw DecryptionException("File is not protected with Microsoft Information Protection")
            }

            val decryptedBytes = mipFileHandler.decryptFile(tempFile.absolutePath, mipToken)

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
        fileValidator.validateInputStream(inputStream)

        inputStream.use { stream ->
            if (stream.markSupported()) {
                stream.mark(Int.MAX_VALUE)
            }

            var tempFile: File? = null
            return try {
                tempFile = tempFileManager.createTempFile(stream, "isprotected-")
                val result = fileProtectionChecker.isFileProtected(tempFile.absolutePath)

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
}