# Project Structure

Maintaining a clean project structure and organized folder setup simplifies navigation through code files and enhances maintainability.

## Files & folders

```bash
.
├── .gradle
├── config
│   ├── application.properties
│   └── application.properties.example
├── docker
├── docs                                                # Contain detail information of the README.md file
│   ├── DEPLOYMENT.md
│   ├── PROJ_STRUCTURE.md
│   └── WORKFLOW.md
├── gradle
│   └── wrapper
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── libs
│   ├── java-sdk-wrapper.jar
│   ├── mip_java.dll
│   ├── mip_java.exp
│   └── mip_java.lib
├── scripts
│   ├── ci
│   │   ├── blackduck.sh
│   │   ├── blackduckUpdateVersion.sh
│   │   └── generateMavenMirrorSettings.sh
│   ├── minio
│   │   ├── allAccessPolicy.json
│   │   └── minioInitialSetup.sh
│   └── generateBuildLabel.sh
├── src
│   ├── main
│   │   ├── kotlin
│   │   │   └── com.rnr.aip
│   │   │       ├── controller
│   │   │       │   └── DecryptionController.kt
│   │   │       ├── service
│   │   │       │   └── decryption
│   │   │       │       ├── AuthDelegate.kt
│   │   │       │       ├── ConsentDelegate.kt
│   │   │       │       └── DecryptionService.kt
│   │   │       ├── objectstorage
│   │   │       │   ├── S3ClientFactory.kt
│   │   │       │   └── S3LifecycleConfig.kt
│   │   │       └── AipApplication.kt
│   │   └── resources
│   │       └── application.properties              # Application configuration file
│   └── test                                  		# Contain unit tests
│       └── kotlin
│           └── com.rnr.aip
│               ├── AipApplicationTests.kt
│               └── service
│                   └── DecryptionServiceTest.kt
├── .gitignore                                  		# Git ignore file
├── build.gradle                            		    # Gradle build file of the whole project
├── Dockerfile
├── gradle.properties                                   # Configuration file used in Gradle
├── gradle.properties.example
├── gradlew
├── gradlew.bat
├── README.md                            		        # Document that provides important information about the project
└── settings.gradle                                     # Gradle settings file of the whole project

```

