package com.rnr.aip.service.decryption

import com.microsoft.informationprotection.IAuthDelegate
import com.microsoft.informationprotection.Identity
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * Authentication delegate for MIP SDK token acquisition.
 */
class AuthDelegate(private val token: String) : IAuthDelegate {

    override fun acquireToken(identity: Identity, authority: String, resource: String, claims: String): String {
        logger.debug("MIP SDK requested authentication token for resource: $resource")
        return token
    }

    companion object {
        private val logger: Logger = LogManager.getLogger()
    }
}