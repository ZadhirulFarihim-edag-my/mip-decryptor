import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile

@ExtendWith(MockitoExtension::class)
class DecryptionControllerTest {

    @Mock
    private lateinit var mipDecryptionService: MipDecryptionService

    @InjectMocks
    private lateinit var decryptionController: DecryptionController

    private val mockToken = "Bearer mock-token"
    private val mockTokenValue = "mock-token"

    @Test
    fun `when a valid file is uploaded, then decryption is successful`() {
        // Given
        val mockFile = MockMultipartFile(
            "file",
            "protected.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "test data".toByteArray()
        )
        val decryptedData = "decrypted data".toByteArray()

        `when`(mipDecryptionService.decrypt(mockFile.inputStream, mockTokenValue)).thenReturn(decryptedData)

        // When
        val response = decryptionController.decryptFile(mockFile, mockToken)

        // Then
        assert(response.statusCode == HttpStatus.OK)
        assert(response.body.contentEquals(decryptedData))
    }

    @Test
    fun `when an empty file is uploaded for decryption, then a bad request response is returned`() {
        // Given
        val mockFile = MockMultipartFile(
            "file",
            "empty.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            ByteArray(0)
        )

        // When
        val response = decryptionController.decryptFile(mockFile, mockToken)

        // Then
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `when decryption fails, then an internal server error response is returned`() {
        // Given
        val mockFile = MockMultipartFile(
            "file",
            "protected.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "test data".toByteArray()
        )

        `when`(mipDecryptionService.decrypt(mockFile.inputStream, mockTokenValue)).thenThrow(RuntimeException("Decryption failed"))

        // When
        val response = decryptionController.decryptFile(mockFile, mockToken)

        // Then
        assert(response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR)
    }

    // --- Is-Protected Endpoint Tests ---

    @Test
    fun `when a protected file is checked, then it should return true`() {
        // Given
        val mockFile = MockMultipartFile(
            "file",
            "protected.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "test data".toByteArray()
        )

        `when`(mipDecryptionService.isProtected(mockFile.inputStream)).thenReturn(true)

        // When
        val response = decryptionController.isFileProtected(mockFile)

        // Then
        assert(response.statusCode == HttpStatus.OK)
        assert(response.body == true)
    }

    @Test
    fun `when an unprotected file is checked, then it should return false`() {
        // Given
        val mockFile = MockMultipartFile(
            "file",
            "unprotected.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "test data".toByteArray()
        )

        `when`(mipDecryptionService.isProtected(mockFile.inputStream)).thenReturn(false)

        // When
        val response = decryptionController.isFileProtected(mockFile)

        // Then
        assert(response.statusCode == HttpStatus.OK)
        assert(response.body == false)
    }

    @Test
    fun `when an empty file is checked for protection, then a bad request response is returned`() {
        // Given
        val mockFile = MockMultipartFile(
            "file",
            "empty.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            ByteArray(0)
        )

        // When
        val response = decryptionController.isFileProtected(mockFile)

        // Then
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `when protection check fails, then an internal server error response is returned`() {
        // Given
        val mockFile = MockMultipartFile(
            "file",
            "error.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "test data".toByteArray()
        )

        `when`(mipDecryptionService.isProtected(mockFile.inputStream)).thenThrow(RuntimeException("Protection check failed"))

        // When
        val response = decryptionController.isFileProtected(mockFile)

        // Then
        assert(response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR)
    }
}