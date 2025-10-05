package com.rnr.aip.utils

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.stereotype.Component
import java.io.File

@Component
class FileProtectionChecker {
    private val logger: Logger = LogManager.getLogger()

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
}