package com.rnr.aip.utils

import com.rnr.aip.exception.DecryptionException
import org.apache.commons.io.IOUtils
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@Component
class TempFileManager {

    fun createTempFile(inputStream: InputStream, prefix: String): File {
        return try {
            val tempFile = File.createTempFile(prefix, ".tmp")
            FileOutputStream(tempFile).use { IOUtils.copy(inputStream, it) }
            tempFile
        } catch (e: Exception) {
            throw DecryptionException("Failed to create temporary file", e)
        }
    }
}