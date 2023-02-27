package org.wordpress.android.fluxc.network.rest.wpcom.site

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type
import java.util.Locale

data class DomainsResponse(val domains: List<Domain>)

data class Domain(
    @SerializedName("a_records_required_for_mapping")
    val aRecordsRequiredForMapping: List<String>? = null,
    @SerializedName("auto_renewal_date")
    val autoRenewalDate: String? = null,
    @SerializedName("auto_renewing")
    @JsonAdapter(BooleanTypeAdapter::class)
    val autoRenewing: Boolean = false,
    @SerializedName("blog_id")
    val blogId: Long = 0,
    @SerializedName("bundled_plan_subscription_id")
    val bundledPlanSubscriptionId: String? = null,
    @SerializedName("can_set_as_primary")
    val canSetAsPrimary: Boolean = false,
    @SerializedName("connection_mode")
    val connectionMode: String? = null,
    @SerializedName("contact_info_disclosed")
    val contactInfoDisclosed: Boolean = false,
    @SerializedName("contact_info_disclosure_available")
    val contactInfoDisclosureAvailable: Boolean = false,
    @SerializedName("current_user_can_add_email")
    val currentUserCanAddEmail: Boolean = false,
    @SerializedName("current_user_can_create_site_from_domain_only")
    val currentUserCanCreateSiteFromDomainOnly: Boolean = false,
    @SerializedName("current_user_can_manage")
    val currentUserCanManage: Boolean = false,
    @SerializedName("current_user_cannot_add_email_reason")
    val currentUserCannotAddEmailReason: JsonElement? = null,
    @SerializedName("domain")
    val domain: String? = null,
    @SerializedName("domain_locking_available")
    val domainLockingAvailable: Boolean = false,
    @SerializedName("domain_registration_agreement_url")
    val domainRegistrationAgreementUrl: String? = null,
    @SerializedName("email_forwards_count")
    val emailForwardsCount: Int = 0,
    @SerializedName("expired")
    val expired: Boolean = false,
    @SerializedName("expiry")
    val expiry: String? = null,
    @SerializedName("expiry_soon")
    val expirySoon: Boolean = false,
    @SerializedName("google_apps_subscription")
    val googleAppsSubscription: GoogleAppsSubscription? = null,
    @SerializedName("has_private_registration")
    val hasPrivateRegistration: Boolean = false,
    @SerializedName("has_registration")
    val hasRegistration: Boolean = false,
    @SerializedName("has_wpcom_nameservers")
    val hasWpcomNameservers: Boolean = false,
    @SerializedName("has_zone")
    val hasZone: Boolean = false,
    @SerializedName("is_eligible_for_inbound_transfer")
    val isEligibleForInboundTransfer: Boolean = false,
    @SerializedName("is_locked")
    val isLocked: Boolean = false,
    @SerializedName("is_pending_icann_verification")
    val isPendingIcannVerification: Boolean = false,
    @SerializedName("is_premium")
    val isPremium: Boolean = false,
    @SerializedName("is_redeemable")
    val isRedeemable: Boolean = false,
    @SerializedName("is_renewable")
    val isRenewable: Boolean = false,
    @SerializedName("is_subdomain")
    val isSubdomain: Boolean = false,
    @SerializedName("is_whois_editable")
    val isWhoisEditable: Boolean = false,
    @SerializedName("is_wpcom_staging_domain")
    val isWpcomStagingDomain: Boolean = false,
    @SerializedName("manual_transfer_required")
    val manualTransferRequired: Boolean = false,
    @SerializedName("new_registration")
    val newRegistration: Boolean = false,
    @SerializedName("owner")
    val owner: String? = null,
    @SerializedName("partner_domain")
    val partnerDomain: Boolean = false,
    @SerializedName("pending_registration")
    val pendingRegistration: Boolean = false,
    @SerializedName("pending_registration_time")
    val pendingRegistrationTime: String? = null,
    @SerializedName("pending_transfer")
    val pendingTransfer: Boolean = false,
    @SerializedName("pending_whois_update")
    val pendingWhoisUpdate: Boolean = false,
    @SerializedName("points_to_wpcom")
    val pointsToWpcom: Boolean = false,
    @SerializedName("primary_domain")
    val primaryDomain: Boolean = false,
    @SerializedName("privacy_available")
    val privacyAvailable: Boolean = false,
    @SerializedName("private_domain")
    val privateDomain: Boolean = false,
    @SerializedName("product_slug")
    val productSlug: String? = null,
    @SerializedName("redeemable_until")
    val redeemableUntil: String? = null,
    @SerializedName("registrar")
    val registrar: String? = null,
    @SerializedName("registration_date")
    val registrationDate: String? = null,
    @SerializedName("renewable_until")
    val renewableUntil: String? = null,
    @SerializedName("ssl_status")
    val sslStatus: String? = null,
    @SerializedName("subdomain_part")
    val subdomainPart: String? = null,
    @SerializedName("subscription_id")
    val subscriptionId: String? = null,
    @SerializedName("supports_domain_connect")
    val supportsDomainConnect: Boolean = false,
    @SerializedName("supports_gdpr_consent_management")
    val supportsGdprConsentManagement: Boolean = false,
    @SerializedName("supports_transfer_approval")
    val supportsTransferApproval: Boolean = false,
    @SerializedName("titan_mail_subscription")
    val titanMailSubscription: TitanMailSubscription? = null,
    @SerializedName("tld_maintenance_end_time")
    val tldMaintenanceEndTime: Int = 0,
    @SerializedName("transfer_away_eligible_at")
    val transferAwayEligibleAt: String? = null,
    @SerializedName("transfer_lock_on_whois_update_optional")
    val transferLockOnWhoisUpdateOptional: Boolean = false,
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("whois_update_unmodifiable_fields")
    val whoisUpdateUnmodifiableFields: List<String>? = null,
    @SerializedName("wpcom_domain")
    val wpcomDomain: Boolean = false
)

data class GoogleAppsSubscription(
    @SerializedName("status")
    val status: String? = null
)

data class TitanMailSubscription(
    @SerializedName("is_eligible_for_introductory_offer")
    val isEligibleForIntroductoryOffer: Boolean = false,
    @SerializedName("status")
    val status: String? = null
)

internal class BooleanTypeAdapter : JsonDeserializer<Boolean?> {
    @Suppress("VariableNaming") private val TRUE_STRINGS: Set<String> = HashSet(listOf("true", "1", "yes"))

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Boolean {
        val jsonPrimitive = json.asJsonPrimitive
        return when {
            jsonPrimitive.isBoolean -> jsonPrimitive.asBoolean
            jsonPrimitive.isNumber -> jsonPrimitive.asNumber.toInt() == 1
            jsonPrimitive.isString -> TRUE_STRINGS.contains(jsonPrimitive.asString.toLowerCase(Locale.getDefault()))
            else -> false
        }
    }
}
