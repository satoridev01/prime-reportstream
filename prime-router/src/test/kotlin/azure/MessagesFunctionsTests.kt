package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.InvalidCodeMessage
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.CovidResultMetadata
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.messageTracker.MessageActionLog
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.AuthenticationType
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test

class MessagesFunctionsTests {
    val id: Long = 6

    private fun buildMessagesFunction(
        mockDbAccess: DatabaseAccess? = null
    ): MessagesFunctions {
        val dbAccess = mockDbAccess ?: mockk()
        return MessagesFunctions(dbAccess)
    }

    private fun buildCovidResultMetadata(
        reportId: UUID? = null,
        messageId: String? = null,
        id: Long? = 0
    ): CovidResultMetadata {
        return CovidResultMetadata(
            id,
            reportId,
            1,
            messageId,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            LocalDateTime.now(),
            null,
            "simple_report.default",
            null,
            null,
            null,
            null,
            null,
            null
        )
    }

    private fun buildReportFile(reportId: UUID): ReportFile {
        return ReportFile(
            reportId,
            1,
            TaskAction.send,
            OffsetDateTime.now().minusWeeks(1),
            "test",
            "test-sender",
            "test",
            "test-receiver",
            "",
            "",
            "covid-19",
            "covid-19",
            "https://localhost/blob",
            "",
            "CSV",
            byteArrayOf(),
            1,
            null,
            OffsetDateTime.now().minusWeeks(1),
            "",
            null
        )
    }

    private fun buildActionLogs(): List<MessageActionLog> {
        val actionLogDetail = InvalidCodeMessage("", "Specimen_type_code (specimen_type)", null)

        return listOf(
            MessageActionLog(
                "trackingId",
                actionLogDetail
            )
        )
    }

    private fun buildItemLineagesByParentReportIdAndTrackingId(parentReportId: ReportId, trackingId: String):
        List<ItemLineage> {
        return listOf(
            ItemLineage(
                1,
                parentReportId,
                14,
                UUID.randomUUID(),
                1,
                trackingId,
                null,
                OffsetDateTime.now().minusWeeks(1),
                ""
            )
        )
    }

    private fun buildReportFileByIds(reportId: ReportId): List<ReportFile> {
        return listOf(
            ReportFile(
                reportId,
                1,
                null,
                null,
                null,
                null,
                "md-phd",
                "elr",
                null,
                "Success: sftp upload of covid-19-123.hl7 to SFTPTransportType",
                "covid-19",
                "covid-19",
                null,
                "covid-19-c74ddaa2-4a8b-4a6a-ba04-9635d8ed7432-20220928195607.hl7",
                "HL7_BATCH",
                null,
                2,
                null,
                OffsetDateTime.now().minusWeeks(1),
                null,
                null
            )
        )
    }

    private fun buildActionLogsByReportIdAndFilterType():
        List<MessageActionLog> {
        val actionLogDetail1 = InvalidCodeMessage("", "Specimen_type_code (specimen_type)", null)
        val actionLogDetail2 = InvalidCodeMessage("", "Specimen_type_code (specimen_type)", null)

        return listOf(
            MessageActionLog(
                "trackingId1",
                actionLogDetail1
            ),
            MessageActionLog(
                "trackingId2",
                actionLogDetail2
            )
        )
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `test processSearchRequest function`() {
        val reportId: UUID = UUID.randomUUID()
        val messageId: String = UUID.randomUUID().toString()
        val mockDbAccess = mockk<DatabaseAccess>()
        val messagesFunctions = spyk(buildMessagesFunction(mockDbAccess))

        // Happy path
        val mockRequestWithMessageId = MockHttpRequestMessage()
        mockRequestWithMessageId.parameters["messageId"] = messageId
        every { mockDbAccess.fetchCovidResultMetadatasByMessageId(any(), any()) } returns listOf(
            buildCovidResultMetadata(reportId, messageId)
        )
        val response = messagesFunctions.processSearchRequest(mockRequestWithMessageId)

        val jsonResponse = JSONArray(response.body.toString())
        val messageObject = jsonResponse.first() as JSONObject
        val messageObjectId = messageObject.get("messageId")
        assert(messageObjectId.equals(messageId))
        assert(response.status.equals(HttpStatus.OK))

        // Missing message id param
        val mockRequestMissingMessageIdParam = MockHttpRequestMessage()
        val missingMessageIdParamsResponse = messagesFunctions.processSearchRequest(mockRequestMissingMessageIdParam)
        assert(missingMessageIdParamsResponse.status.equals(HttpStatus.BAD_REQUEST))

        // empty message id
        val mockRequestEmptyMessageId = MockHttpRequestMessage()
        mockRequestEmptyMessageId.parameters += mapOf(
            "messageId" to ""
        )
        val emptyMessageIdResponse = messagesFunctions.processSearchRequest(mockRequestEmptyMessageId)
        assert(emptyMessageIdResponse.status.equals(HttpStatus.BAD_REQUEST))

        // whitespace message id
        val mockRequestWhitespaceMessageId = MockHttpRequestMessage()
        mockRequestWhitespaceMessageId.parameters += mapOf(
            "messageId" to " "
        )
        val whitespaceMessageIdResponse = messagesFunctions.processSearchRequest(mockRequestWhitespaceMessageId)
        assert(whitespaceMessageIdResponse.status.equals(HttpStatus.BAD_REQUEST))

        // Exception in the database
        val mockRequestMissingMessageIdValue = MockHttpRequestMessage()
        mockRequestMissingMessageIdValue.parameters["messageId"] = "message-id"
        every {
            mockDbAccess.fetchCovidResultMetadatasByMessageId(
                any(),
                any()
            )
        }.throws(Exception("missing a message id"))
        val missingMessageIdValueResponse = messagesFunctions.processSearchRequest(mockRequestMissingMessageIdValue)
        assert(missingMessageIdValueResponse.status.equals(HttpStatus.BAD_REQUEST))
    }

    @Test
    fun `test search function`() {
        val messagesFunctions = spyk(MessagesFunctions())

        // Happy path
        val req = MockHttpRequestMessage()
        val resp = HttpUtilities.okResponse(req, "fakeOkay")

        every { messagesFunctions.processSearchRequest(any()) } returns resp

        val res = messagesFunctions.messageSearch(req)
        assert(res.status.equals(HttpStatus.OK))

        // unauthorized - no claims
        val unAuthReq = MockHttpRequestMessage()
        unAuthReq.httpHeaders += mapOf(
            "authorization" to "x.y.z"
        )

        val unAuthRes = messagesFunctions.messageSearch(unAuthReq)
        assert(unAuthRes.status.equals(HttpStatus.UNAUTHORIZED))

        // unauthorized - not an admin
        val jwt = mapOf("organization" to listOf("DHSender_simple_report"), "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

        mockkObject(AuthenticatedClaims)
        every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

        val unAuthReq2 = MockHttpRequestMessage()
        unAuthReq2.httpHeaders += mapOf(
            "authorization" to "x.y.z"
        )

        val unAuthRes2 = messagesFunctions.messageSearch(unAuthReq)
        assert(unAuthRes2.status.equals(HttpStatus.UNAUTHORIZED))
    }

    @Test
    fun `test search function internal server error`() {
        // throw exception
        val internalServerErrorReq = MockHttpRequestMessage()
        val messagesFunctions = spyk(buildMessagesFunction())

        every { messagesFunctions.processSearchRequest(any()) }.throws(Exception("something went wrong somewhere"))

        var internalErrorRes = messagesFunctions.messageSearch(internalServerErrorReq)

        assert(internalErrorRes.status.equals(HttpStatus.INTERNAL_SERVER_ERROR))

        every { messagesFunctions.processSearchRequest(any()) }.throws(Exception())

        internalErrorRes = messagesFunctions.messageSearch(internalServerErrorReq)
        assert(internalErrorRes.status.equals(HttpStatus.INTERNAL_SERVER_ERROR))
    }

    @Test
    fun `test processMessageDetailRequest function`() {
        val reportId: UUID = UUID.randomUUID()
        val messageId: String = UUID.randomUUID().toString()
        val mockDbAccess = mockk<DatabaseAccess>()
        val messagesFunctions = spyk(buildMessagesFunction(mockDbAccess))

        // Happy path
        val mockRequestWithMessageId = MockHttpRequestMessage()
        every { mockDbAccess.fetchSingleMetadataById(any(), any()) } returns buildCovidResultMetadata(
            reportId,
            messageId,
            id
        )
        every { mockDbAccess.fetchReportFile(any(), any(), any()) } returns buildReportFile(reportId)
        every {
            mockDbAccess.fetchActionLogsByReportIdAndTrackingIdAndType(
                any(),
                any(),
                any(),
                any()
            )
        } returns buildActionLogs()
        every {
            mockDbAccess.fetchItemLineagesByParentReportIdAndTrackingId(
                any(),
                any(),
                any()
            )
        } returns buildItemLineagesByParentReportIdAndTrackingId(reportId, messageId)
        every { mockDbAccess.fetchReportFileByIds(any()) } returns buildReportFileByIds(reportId)
        every {
            mockDbAccess.fetchActionLogsByReportIdAndFilterType(
                any(),
                any()
            )
        } returns buildActionLogsByReportIdAndFilterType()

        val response = messagesFunctions.processMessageDetailRequest(mockRequestWithMessageId, id)

        val jsonResponse = JSONObject(response.body.toString())
        val messageDetailObjectId = jsonResponse.get("id") as Int
        assert(messageDetailObjectId.toLong() == id)
        assert(response.status.equals(HttpStatus.OK))

        // Exception in the database
        val mockRequestInvalidReportIdValue = MockHttpRequestMessage()
        mockRequestInvalidReportIdValue.parameters["reportId"] = "report-id"
        every {
            mockDbAccess.fetchReportFile(any())
        }.throws(Exception("missing a report id"))
        val missingReportIdValueResponse = messagesFunctions.processMessageDetailRequest(
            mockRequestInvalidReportIdValue,
            id
        )
        assert(missingReportIdValueResponse.status.equals(HttpStatus.BAD_REQUEST))
    }

    @Test
    fun `test processMessageDetailRequest function no id found`() {
        val mockDbAccess = mockk<DatabaseAccess>()
        val messagesFunctions = spyk(buildMessagesFunction(mockDbAccess))

        // No id in the database
        val mockRequestNoIdFound = MockHttpRequestMessage()
        every { mockDbAccess.fetchSingleMetadataById(any(), any()) } returns null

        val response = messagesFunctions.processMessageDetailRequest(mockRequestNoIdFound, id)

        val responseBody = JSONObject(response.body.toString())
        val messageObject = responseBody.get("message")
        assert(messageObject.equals("No message found."))
        assert(response.status.equals(HttpStatus.BAD_REQUEST))
    }

    @Test
    fun `test messageDetails function`() {
        val messagesFunctions = spyk(MessagesFunctions())

        // Happy path
        val req = MockHttpRequestMessage()
        val resp = HttpUtilities.okResponse(req, "fakeOkay")

        every { messagesFunctions.messageDetails(any(), any()) } returns resp

        val res = messagesFunctions.messageDetails(req, id)
        assert(res.status.equals(HttpStatus.OK))

        clearAllMocks()

        // unauthorized - no claims
        val unAuthReq = MockHttpRequestMessage()
        unAuthReq.httpHeaders += mapOf(
            "authorization" to "x.y.z"
        )

        val unAuthRes = messagesFunctions.messageDetails(unAuthReq, id)
        assert(unAuthRes.status.equals(HttpStatus.UNAUTHORIZED))

        // unauthorized - not an admin
        val jwt = mapOf("organization" to listOf("DHSender_simple_report"), "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

        mockkObject(AuthenticatedClaims)
        every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

        val unAuthReq2 = MockHttpRequestMessage()
        unAuthReq2.httpHeaders += mapOf(
            "authorization" to "x.y.z"
        )

        val unAuthRes2 = messagesFunctions.messageDetails(unAuthReq, id)
        assert(unAuthRes2.status.equals(HttpStatus.UNAUTHORIZED))
    }

    @Test
    fun `test messageDetails function internal server error`() {
        // throw exception
        val internalServerErrorReq = MockHttpRequestMessage()
        val messagesFunctions = spyk(buildMessagesFunction())

        every { messagesFunctions.processMessageDetailRequest(any(), id) }.throws(
            Exception("something went wrong somewhere")
        )

        var internalErrorRes = messagesFunctions.messageDetails(internalServerErrorReq, id)

        assert(internalErrorRes.status.equals(HttpStatus.INTERNAL_SERVER_ERROR))

        every { messagesFunctions.processMessageDetailRequest(any(), id) }.throws(Exception())

        internalErrorRes = messagesFunctions.messageDetails(internalServerErrorReq, id)
        assert(internalErrorRes.status.equals(HttpStatus.INTERNAL_SERVER_ERROR))
    }
}