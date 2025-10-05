# R&R (Risk & Response) - MIP Decryptor Application

This is a Spring Boot application that provides a REST API to decrypt Microsoft Information Protection (MIP) protected Excel files. It uses the MIP SDK for Java to handle the decryption process.

This application expects the frontend to handle user authentication with Azure AD and pass a valid access token in the `Authorization` header of the request.

## Tech Stack

- Spring Boot 3.5.x
- Kotlin (Java) 1.9.x
- Gradle 8.14.x
- SwaggerUI
- Jacoco / SonarQube
- MinIO (S3) Object Storage
- JUnit 5
- Mockito
- Spring Security
- Azure AD
- MIP SDK

## Prerequisites

- Java 11 or higher (developed with Java 21)
- An Azure Active Directory (Azure AD) application registration.
- The Microsoft Information Protection (MIP) SDK binaries.
- MinIO

## Gradle Configuration

We use VW Artifactory as a mirror for all gradle dependencies. Write your devstack credentials into `gradle.properties` next to this readme file:

```properties
artifactoryUsername=xxx
artifactoryPassword=use-your-generated-token-here
```

**Note:** Basic authentication has been disabled in devstack. You must generate an identity token from the JFrog Artifactory at:

[https://jfrog.devstack.vwgroup.com](https://jfrog.devstack.vwgroup.com)

Go to: **Edit Profile > Generate Identity Token**

## Project Structure

Description of how project directory is structured and files are sorted can be found in [PROJ_STRUCTURE.md](./docs/PROJ_STRUCTURE.md).

## GIT Workflow

For documentation on our GIT workflow and commit guidelines, please refer to [WORKFLOW.md](./docs/WORKFLOW.md).

## Local Execution

Override `application.properties` for local settings (see `config/application.properties.example`).

## Deployment

For documentation on deployment, please refer to [DEPLOYMENT.md](./docs/DEPLOYMENT.md).

## Setting up Local MinIO Server

MinIO is used in this application as an alternative to Amazon S3 for local development.
Further details are available at [MinIO Documentation](https://min.io/docs/minio/windows/index.html).

1. **Install the MinIO Server**
   
   Download the MinIO executable from the following URL:
   
   ```
   https://dl.min.io/server/minio/release/windows-amd64/minio.exe
   ```

2. **Launch the MinIO Server**
   
   In PowerShell or the Command Prompt, navigate to the location of the downloaded executable. The minio server process prints its output to the system console.
   
   ```powershell
   .\minio.exe server C:\minio --console-address :9001
   ```

3. **Connect your Browser to the MinIO Server**
   
   Access the MinIO Web Console by going to a browser and going to `http://localhost:9001`
   
   While port 9000 is used for connecting to the API.

### User, Bucket Creation in MinIO

You can create users and buckets in MinIO using either the mc client CLI or the MinIO web console UI.

#### Using the mc Client CLI

1. **Install the MinIO Client (mc)**
   
   The MinIO Client (mc) command-line tool allows you to perform administrative tasks on your MinIO deployments. You can download it from the following link:
   
   ```
   https://dl.min.io/client/mc/release/windows-amd64/mc.exe
   ```

2. **Launch the mc Command Line**
   
   Open a PowerShell or Linux-based shell terminal and run the following script that is located in `\scripts\minio\`:
   
   ```bash
   .\minioInitialSetup.sh
   ```

3. **Configure application.properties**
   
   ```properties
   ### AIP S3 config ###
   app.aip.s3.endpoint=http://localhost:9000
   app.aip.s3.region=eu-central-1
   app.aip.s3.access-key=rnr
   app.aip.s3.secret-key=rnr-secret-key
   app.aip.s3.upload.bucket=rnr-dev-aip
   app.aip.s3.expiration-in-days=10
   app.aip.s3.noncurrentversion-expiration-in-days=10
   ```

## Azure AD App Registration

Before running the application, you need to register an application in Azure AD.

## MIP SDK Setup

The MIP SDK is not available in a public Maven or Gradle repository and must be downloaded and set up manually.

### Important: Architecture Compatibility

**The MIP SDK native libraries must match your JVM architecture (32-bit or 64-bit).**

- If you're running a **64-bit JVM** (most common), you need the 64-bit native libraries and should rename `mip_java.dll` to `mip_java_x64.dll`.
- If you're running a **32-bit JVM**, you need the 32-bit native libraries (keep the name as `mip_java.dll`).

To check your JVM architecture, run:
```bash
java -version
```
Look for "64-Bit" or "32-Bit" in the output.

### Setup Steps

1.  **Download the SDK:** Download the latest version of the MIP SDK for Java from the [Microsoft Download Center](https://www.microsoft.com/en-us/download/details.aspx?id=108216).
    *   For 64-bit JVM: Download `mip_sdk_java_wrappers_win64_*.zip`
    *   For 32-bit JVM: Download `mip_sdk_java_wrappers_win32_*.zip`
2.  **Extract the Archive:** Unzip the downloaded file.
3.  **Create `libs` Directory:** In the root of this project, create a directory named `libs`.
4.  **Copy JAR file:**
    *   Navigate into the extracted SDK folder.
    *   Find the `mip-sdk-protection-*.jar` file (e.g., `mip-sdk-protection-1.14.88.jar`).
    *   Copy this JAR file into the `libs` directory you created.
    *   **Important:** Make sure the version number in the `build.gradle` file matches the version of the JAR you downloaded.
5.  **Copy Native Libraries:**
    *   From the extracted SDK folder, navigate to the `protection/bins/release/amd64` directory (or `debug` if you prefer).
    *   Copy the native library files into the `libs` directory.
    *   **For 64-bit JVM:** Rename `mip_java.dll` to `mip_java_x64.dll` in the `libs` directory.
    *   **For 32-bit JVM:** Keep the name as `mip_java.dll`.

After this step, your `libs` directory should contain the MIP SDK JAR and the required native libraries with the correct naming convention for your JVM architecture.

## Application Configuration

You need to provide the Azure AD application Client ID to the Spring Boot application.

1.  Open the `src/main/resources/application.properties` file.
2.  Update the following property with your Azure AD Client ID:

    ```properties
    # Azure AD Application Details
    app.aip.client-id=
    app.aip.app-name=mip_sdk_decryption_service
    app.aip.app-version=1.0
    ```

## Building and Running the Application

Once the setup and configuration are complete, you can run the application using the Gradle wrapper. You must also provide the path to the native libraries using the `java.library.path` system property.

```bash
# On Linux/macOS
./gradlew bootRun --args='-Djava.library.path=libs'

# On Windows
./gradlew.bat bootRun --args='-Djava.library.path=libs'
```

The application will start on port 8080. To avoid conflicts with RNR main applications, you can change the port in `application.properties`.

## Using the API

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
