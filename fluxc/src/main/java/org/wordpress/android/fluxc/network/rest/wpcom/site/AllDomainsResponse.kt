package org.wordpress.android.fluxc.network.rest.wpcom.site

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

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
    val registrationDate: String? = null,
    @SerializedName("expiry")
    val expiry: String? = null,
    @SerializedName("wpcom_domain")
    @JsonAdapter(BooleanTypeAdapter::class)
    val wpcomDomain: Boolean = false,
    @SerializedName("current_user_is_owner")
    @JsonAdapter(BooleanTypeAdapter::class)
    val currentUserIsOwner: Boolean = false,
    @SerializedName("site_slug")
    val siteSlug: String? = null,
)
