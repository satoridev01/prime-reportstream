package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

// Schemas used
const val HL7_SCHEMA = "covid-19"
const val REDOX_SCHEMA = "covid-19-redox"

// Common set of properties for all translators
interface TranslatorProperties {
    val format: Report.Format
    val schemaName: String
    val defaults: Map<String, String>
}

// Base JSON Type
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(Hl7Configuration::class, name = "HL7"),
    JsonSubTypes.Type(RedoxConfiguration::class, name = "REDOX"),
    JsonSubTypes.Type(CustomConfiguration::class, name = "CUSTOM"),
)
abstract class TranslatorConfiguration(val type: String) : TranslatorProperties

/**
 * Standard HL7 report configuration
 */
data class Hl7Configuration
@JsonCreator constructor(
    val useTestProcessingMode: Boolean = false,
    val useBatchHeaders: Boolean = true,
    val receivingApplicationName: String?,
    val receivingApplicationOID: String?,
    val receivingFacilityName: String?,
    val receivingFacilityOID: String?,
    val messageProfileId: String?,
) : TranslatorConfiguration("HL7") {
    @get:JsonIgnore
    override val format: Report.Format get() = if (useBatchHeaders) Report.Format.HL7_BATCH else Report.Format.HL7

    @get:JsonIgnore
    override val schemaName: String get() = HL7_SCHEMA

    @get:JsonIgnore
    override val defaults: Map<String, String> get() {
        val receivingApplication = when {
            receivingApplicationName != null && receivingApplicationOID != null ->
                "$receivingApplicationName^$receivingApplicationOID^ISO"
            receivingApplicationName != null && receivingApplicationOID == null -> receivingApplicationName
            else -> ""
        }
        val receivingFacility = when {
            receivingFacilityName != null && receivingFacilityOID != null ->
                "$receivingFacilityName^$receivingFacilityOID^ISO"
            receivingFacilityName != null && receivingFacilityOID == null -> receivingFacilityName
            else -> ""
        }
        return mapOf(
            "processing_mode_code" to (if (useTestProcessingMode) "T" else "P"),
            "receiving_application" to receivingApplication,
            "receiving_facility" to receivingFacility,
            "message_profile_id" to (messageProfileId ?: ""),
        )
    }
}

/**
 * Standard Redox report configuration
 */
data class RedoxConfiguration
@JsonCreator constructor(
    val useTestProcessingMode: Boolean = false,
    val destinationId: String,
    val destinationName: String,
    val sourceId: String,
    val sourceName: String,
) : TranslatorConfiguration("REDOX") {
    @get:JsonIgnore
    override val format: Report.Format get() = Report.Format.REDOX

    @get:JsonIgnore
    override val schemaName: String get() = REDOX_SCHEMA

    @get:JsonIgnore
    override val defaults: Map<String, String> get() {
        return mapOf(
            "processing_mode_code" to (if (useTestProcessingMode) "T" else "P"),
            "redox_destination_id" to destinationId,
            "redox_destination_name" to destinationName,
            "redox_source_id" to sourceId,
            "redox_source_name" to sourceName,
        )
    }
}

/**
 * Custom report configuration
 */
data class CustomConfiguration
@JsonCreator constructor(
    override val schemaName: String,
    override val format: Report.Format,
    override val defaults: Map<String, String> = emptyMap()
) : TranslatorConfiguration("CUSTOM")