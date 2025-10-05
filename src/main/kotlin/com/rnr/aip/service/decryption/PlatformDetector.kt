package com.rnr.aip.service.decryption

import org.apache.logging.log4j.LogManager

enum class Architecture {
    AMD64, ARM64, X86, UNKNOWN
}

data class PlatformInfo(
    val osName: String,
    val is64Bit: Boolean,
    val architecture: Architecture,
    val isWindows: Boolean,
    val isMac: Boolean,
    val isLinux: Boolean
)

object PlatformDetector {
    private val logger = LogManager.getLogger()

    fun detectPlatform(): PlatformInfo {
        val osName = System.getProperty("os.name").lowercase()
        val osArch = System.getProperty("os.arch").lowercase()
        val jvmBitness = System.getProperty("sun.arch.data.model")

        val is64Bit = jvmBitness == "64"

        val architecture = when {
            osArch.contains("aarch64") || osArch.contains("arm64") -> Architecture.ARM64
            osArch.contains("amd64") || osArch.contains("x86_64") -> Architecture.AMD64
            osArch.contains("x86") || osArch.contains("i386") || osArch.contains("i686") -> Architecture.X86
            else -> {
                logger.warn("Unknown architecture: $osArch, defaulting to AMD64")
                Architecture.UNKNOWN
            }
        }

        return PlatformInfo(
            osName = osName,
            is64Bit = is64Bit,
            architecture = architecture,
            isWindows = osName.contains("win"),
            isMac = osName.contains("mac"),
            isLinux = osName.contains("nux") || osName.contains("nix")
        )
    }
}