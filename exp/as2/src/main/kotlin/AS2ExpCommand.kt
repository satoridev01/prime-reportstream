package gov.cdc.prime.as2exp

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.helger.as2lib.client.AS2Client
import com.helger.as2lib.client.AS2ClientRequest
import com.helger.as2lib.client.AS2ClientSettings
import com.helger.security.keystore.EKeyStoreType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Example program to send a message
 *
 * URL https://onehealthport-as2.axwaycloud.com/exchange/ZZOHP
 */

// AS2 endpoints
const val PROD_AS2_URL = "https://onehealthport-as2.axwaycloud.com/exchange/ZZOHP"
const val TEST_AS2_URL = "https://uat-onehealthport-as2.axwaycloud.com/exchange/ZZOHPUAT"
const val LOCAL_AS2_URL = "http://localhost:8080/pyas2/as2receive"

// AS2 Receiver IDS
const val PROD_AS2ID = "ZZOHP"
const val TEST_AS2ID = "ZZOHPUAT"
const val LOCAL_AS2ID = "ZZOHP"

// PRIME Sender IDS
const val PROD_PRIME_AS2ID = "CDCPRIME"
const val TEST_PRIME_AS2ID = "CDCPRIMETEST"
const val LOCAL_PRIME_AS2ID = "CDCPRIME"

// PRIME sender email
const val PRIME_SENDER_EMAIL = "prime@cdc.gov"
const val HL7_MIME_TYPE = "text/x-hl7-ft"
const val CONTENT_DESCRIPTION = "COVID-19 Electronic Lab Results"

// JKS key alias
const val PRIME_KEY_ALIAS = "cdcprime"
const val OHP_KEY_ALIAS = "as2ohp"

class AS2ExpCommand: CliktCommand() {

    val payload by option("--payload", help="Payload file").file(mustExist = true).required()
    val keystore by option("--keystore", help="Keystore(.jks) file").file(mustExist = true).required()
    val keypass by option("--keypass", help="Keystore password").required()

    override fun run() {
        val client = AS2Client()
        val settings = AS2ClientSettings()
        settings.setKeyStore(EKeyStoreType.JKS, keystore, keypass)
        settings.setSenderData (LOCAL_PRIME_AS2ID, PRIME_SENDER_EMAIL, PRIME_KEY_ALIAS)
        settings.setReceiverData(LOCAL_AS2ID, OHP_KEY_ALIAS, LOCAL_AS2_URL)

        // Lot's options for a response. We will likely ignore, so don't request one
        settings.isMDNRequested = false

        // Retry once
        settings.retryCount = 1
        settings.connectTimeoutMS = 10_000
        settings.readTimeoutMS = 10_000

        // Insert report filename here for the subject
        val reportName = "wa-covid-19" +
                "-${DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now())}" +
                "-${UUID.randomUUID()}"

        val request = AS2ClientRequest(reportName)
        request.contentType = HL7_MIME_TYPE
        request.setContentDescription(CONTENT_DESCRIPTION)
        request.setData(payload, Charsets.UTF_8)

        val response = client.sendSynchronous(settings, request)
        echo("${response.asString}")
    }
}

fun main(args: Array<String>) = AS2ExpCommand().main(args)