# R&R (Risk & Response) - MIP Decryptor Application

## Tech Stack Backend

- Spring Boot 3.5.x
- Kotlin (Java) 1.9.x
- Gradle 8.x
- SwaggerUI
- Jacoco / SonarQube


This is a Spring Boot application that provides a REST API to decrypt Microsoft Information Protection (MIP) protected Excel files. It uses the MIP SDK for Java to handle the decryption process.

This application expects the frontend to handle user authentication with Azure AD and pass a valid access token in the `Authorization` header of the request.

## Prerequisites

- Java 11 or higher (developed with Java 21)
- An Azure Active Directory (Azure AD) application registration.
- The Microsoft Information Protection (MIP) SDK binaries.

## 1. Azure AD App Registration

Before running the application, you need to register an application in Azure AD.

## 2. MIP SDK Setup

The MIP SDK is not available in a public Maven or Gradle repository and must be downloaded and set up manually.

1.  **Download the SDK:** Download the latest version of the MIP SDK for Java from the [Microsoft Download Center](https://www.microsoft.com/en-us/download/details.aspx?id=100423).
2.  **Extract the Archive:** Unzip the downloaded file.
3.  **Create `libs` Directory:** In the root of this project, create a directory named `libs`.
4.  **Copy JAR file:**
    *   Navigate into the extracted SDK folder.
    *   Find the `mip-sdk-protection-*.jar` file (e.g., `mip-sdk-protection-1.14.88.jar`).
    *   Copy this JAR file into the `libs` directory you created.
    *   **Important:** Make sure the version number in the `build.gradle.kts` file matches the version of the JAR you downloaded.
5.  **Copy Native Libraries:**
    *   From the extracted SDK folder, navigate to the `protection/bins/release/amd64` directory (or `debug` if you prefer).
    *   Copy the native library files into the `libs` directory.

After this step, your `libs` directory should contain the MIP SDK JAR and the required native libraries.

## 3. Application Configuration

You need to provide the Azure AD application Client ID to the Spring Boot application.

1.  Open the `src/main/resources/application.properties` file.
2.  Update the following property with the value you copied in Step 1:

    ```properties
    # Azure AD Application Details
    azure.activedirectory.client-id=YOUR_CLIENT_ID
    ```

## 4. Building and Running the Application

Once the setup and configuration are complete, you can run the application using the Gradle wrapper. You must also provide the path to the native libraries using the `java.library.path` system property.

```bash
# On Linux/macOS
./gradlew bootRun --args='-Djava.library.path=libs'

# On Windows
./gradlew.bat bootRun --args='-Djava.library.path=libs'
```

The application will start on port 8080.

## 5. Using the API

The application provides two endpoints to manage protected files.

### Checking if a File is Protected

Before attempting to decrypt a file, you can check if it is protected by sending a `POST` request to the `/api/is-protected` endpoint. This is useful for frontends to determine if they need to acquire an authentication token.

The endpoint will return `true` if the file is protected and `false` otherwise.

#### Example with cURL

```bash
curl --location --request POST 'http://localhost:8080/api/is-protected' \
--form 'file=@"/path/to/your/file.xlsx"'
```
-   Replace `"/path/to/your/file.xlsx"` with the actual path to your file.

### Decrypting a File

To decrypt a protected Excel file, send a `POST` request to the `/api/decrypt` endpoint with the file included as multipart form data and a valid `Authorization` header containing a bearer token obtained from Azure AD.

#### Example with cURL

```bash
curl --location --request POST 'http://localhost:8080/api/decrypt' \
--header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
--form 'file=@"/path/to/your/protected_file.xlsx"' \
--output decrypted_file.xlsx
```

-   Replace `YOUR_ACCESS_TOKEN` with the access token obtained by the frontend.
-   Replace `"/path/to/your/protected_file.xlsx"` with the actual path to your protected file.
-   The decrypted file will be saved as `decrypted_file.xlsx` in your current directory.