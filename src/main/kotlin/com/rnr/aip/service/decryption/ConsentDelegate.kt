package com.rnr.aip.service.decryption

import com.microsoft.informationprotection.Consent
import com.microsoft.informationprotection.IConsentDelegate
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * Consent delegate for MIP SDK operations.
 */
class ConsentDelegate : IConsentDelegate {
    private val logger: Logger = LogManager.getLogger()

    override fun getUserConsent(url: String): Consent {
        logger.info("MIP SDK requested consent for URL: $url")
        return Consent.ACCEPT_ALWAYS
    }

}