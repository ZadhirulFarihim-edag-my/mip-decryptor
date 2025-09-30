package rnr.mip.service

import com.microsoft.informationprotection.*
import com.microsoft.informationprotection.protection.ProtectionEngine
import com.microsoft.informationprotection.protection.ProtectionHandler
import com.microsoft.informationprotection.protection.PublishingLicenseInfo
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Paths

@Service
class MipDecryptionService {

    @Value("\${azure.activedirectory.client-id}")
    private lateinit var clientId: String

    private val appName = "mip_sdk_decryption_service"
    private val appVersion = "1.0"

    fun decrypt(inputStream: InputStream, token: String): ByteArray {
        val tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "mip_sdk_data").toString()
        val mipDataDir = File(tempDir, "mip_data")
        mipDataDir.mkdirs()

        val mipContext = createMipContext(mipDataDir.absolutePath)
        val protectionProfile = createProtectionProfile(mipContext, token)
        val protectionEngine = createProtectionEngine(protectionProfile)
        val protectionHandler = createProtectionHandler(inputStream, protectionEngine)

        val decryptedBytes = decryptData(protectionHandler)

        // Shutdown MIP SDK
        protectionProfile.close()
        mipContext.shutDown()

        return decryptedBytes
    }

    fun isProtected(inputStream: InputStream): Boolean {
        val tempFile = File.createTempFile("isprotected-", ".tmp")
        return try {
            FileOutputStream(tempFile).use { IOUtils.copy(inputStream, it) }
            // This method throws a BadInputException if the file is not protected.
            PublishingLicenseInfo.getPublishingLicenseInfo(tempFile.absolutePath)
            true
        } catch (e: com.microsoft.informationprotection.exceptions.BadInputException) {
            false
        } finally {
            tempFile.delete()
        }
    }

    private fun createMipContext(mipDataPath: String): MipContext {
        val appInfo = ApplicationInfo(clientId, appName, appVersion)
        return MipContext.create(appInfo, mipDataPath, LogLevel.TRACE, null, null)
    }

    private fun createProtectionProfile(mipContext: MipContext, token: String): ProtectionEngine.Profile {
        val authDelegate = AuthDelegate(token)
        val consentDelegate = object : ConsentDelegate {
            override fun getUserConsent(url: String): Consent = Consent.ACCEPT
        }
        val profileSettings = ProtectionEngine.Settings(authDelegate, consentDelegate, "")
        val future = mipContext.createProtectionEngineAsync(profileSettings)
        return future.get()
    }

    private fun createProtectionEngine(profile: ProtectionEngine.Profile): ProtectionEngine {
        val engineSettings = ProtectionEngine.Settings("", "")
        val future = profile.addEngineAsync(engineSettings)
        return future.get()
    }

    private fun createProtectionHandler(inputStream: InputStream, engine: ProtectionEngine): ProtectionHandler {
        val tempFile = File.createTempFile("protected-", ".tmp")
        FileOutputStream(tempFile).use { IOUtils.copy(inputStream, it) }

        val publishingLicenseInfo = PublishingLicenseInfo.getPublishingLicenseInfo(tempFile.absolutePath)
        val future = engine.createProtectionHandlerFromPublishingLicenseAsync(publishingLicenseInfo)
        return future.get()
    }

    private fun decryptData(handler: ProtectionHandler): ByteArray {
        val buffer = handler.getProtectedContent()
        val decryptedBuffer = ByteArray(handler.getUnprotectedContentSize(buffer.size).toInt())
        val bytesWritten = handler.decryptBuffer(0, buffer, 0, buffer.size, decryptedBuffer, 0, decryptedBuffer.size)
        return decryptedBuffer.copyOf(bytesWritten)
    }
}

class AuthDelegate(private val token: String) : IAuthDelegate {
    override fun acquireToken(identity: Identity, authority: String, resource: String, claims: String): String {
        return token
    }
}