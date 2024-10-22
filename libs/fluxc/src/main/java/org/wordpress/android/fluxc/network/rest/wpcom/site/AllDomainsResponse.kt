package org.wordpress.android.fluxc.network.rest.wpcom.site

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.wordpress.android.util.DateTimeUtils
import java.lang.reflect.Type
import java.util.Date

data class AllDomainsResponse(val domains: List<AllDomainsDomain>)

data class AllDomainsDomain(
    @SerializedName("domain")
    val domain: String? = null,
    @SerializedName("blog_id")
    val blogId: Long = 0,
    @SerializedName("blog_name")
    val blogName: String? = null,
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("is_domain_only_site")
    @JsonAdapter(BooleanTypeAdapter::class)
    val isDomainOnlySite: Boolean = false,
    @SerializedName("is_wpcom_staging_domain")
    @JsonAdapter(BooleanTypeAdapter::class)
    val isWpcomStagingDomain: Boolean = false,
    @SerializedName("has_registration")
    @JsonAdapter(BooleanTypeAdapter::class)
    val hasRegistration: Boolean = false,
    @SerializedName("registration_date")
    @JsonAdapter(AllDomainsDateAdapter::class)
    val registrationDate: Date? = null,
    @SerializedName("expiry")
    @JsonAdapter(AllDomainsDateAdapter::class)
    val expiry: Date? = null,
    @SerializedName("wpcom_domain")
    @JsonAdapter(BooleanTypeAdapter::class)
    val wpcomDomain: Boolean = false,
    @SerializedName("current_user_is_owner")
    @JsonAdapter(BooleanTypeAdapter::class)
    val currentUserIsOwner: Boolean = false,
    @SerializedName("site_slug")
    val siteSlug: String? = null,
    @SerializedName("domain_status")
    val domainStatus: DomainStatus? = null,
)

data class DomainStatus(
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("status_type")
    @JsonAdapter(StatusTypeAdapter::class)
    val statusType: StatusType? = null,
    @SerializedName("status_weight")
    val statusWeight: Long? = 0,
    @SerializedName("action_required")
    @JsonAdapter(BooleanTypeAdapter::class)
    val actionRequired: Boolean? = false,
)

enum class StatusType(private val stringValue: String) {
    SUCCESS("success"),
    NEUTRAL("neutral"),
    ALERT("alert"),
    WARNING("warning"),
    ERROR("error"),
    UNKNOWN("unknown");

    override fun toString() = stringValue

    companion object {
        fun fromString(string: String): StatusType {
            for (item in values()) {
                if (item.stringValue == string) {
                    return item
                }
            }
            return UNKNOWN
        }
    }
}

internal class StatusTypeAdapter : JsonDeserializer<StatusType> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): StatusType {
        val jsonPrimitive = json.asJsonPrimitive
        return when {
            jsonPrimitive.isString -> StatusType.fromString(jsonPrimitive.asString)
            else -> StatusType.UNKNOWN
        }
    }
}

internal class AllDomainsDateAdapter : JsonDeserializer<Date?> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Date? {
        val jsonPrimitive = json.asJsonPrimitive
        return when {
            jsonPrimitive.isString -> DateTimeUtils.dateUTCFromIso8601(jsonPrimitive.asString)
            else -> null
        }
    }
}
