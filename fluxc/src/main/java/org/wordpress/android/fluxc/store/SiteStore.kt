package org.wordpress.android.fluxc.store

import android.text.TextUtils
import androidx.annotation.VisibleForTesting
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.ASYNC
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.SiteAction
import org.wordpress.android.fluxc.action.SiteAction.CHECKED_AUTOMATED_TRANSFER_ELIGIBILITY
import org.wordpress.android.fluxc.action.SiteAction.CHECKED_AUTOMATED_TRANSFER_STATUS
import org.wordpress.android.fluxc.action.SiteAction.CHECKED_DOMAIN_AVAILABILITY
import org.wordpress.android.fluxc.action.SiteAction.CHECKED_IS_WPCOM_URL
import org.wordpress.android.fluxc.action.SiteAction.CHECK_AUTOMATED_TRANSFER_ELIGIBILITY
import org.wordpress.android.fluxc.action.SiteAction.CHECK_AUTOMATED_TRANSFER_STATUS
import org.wordpress.android.fluxc.action.SiteAction.CHECK_DOMAIN_AVAILABILITY
import org.wordpress.android.fluxc.action.SiteAction.COMPLETED_QUICK_START
import org.wordpress.android.fluxc.action.SiteAction.COMPLETE_QUICK_START
import org.wordpress.android.fluxc.action.SiteAction.CREATE_NEW_SITE
import org.wordpress.android.fluxc.action.SiteAction.DELETED_SITE
import org.wordpress.android.fluxc.action.SiteAction.DELETE_SITE
import org.wordpress.android.fluxc.action.SiteAction.DESIGNATED_MOBILE_EDITOR_FOR_ALL_SITES
import org.wordpress.android.fluxc.action.SiteAction.DESIGNATED_PRIMARY_DOMAIN
import org.wordpress.android.fluxc.action.SiteAction.DESIGNATE_MOBILE_EDITOR
import org.wordpress.android.fluxc.action.SiteAction.DESIGNATE_MOBILE_EDITOR_FOR_ALL_SITES
import org.wordpress.android.fluxc.action.SiteAction.DESIGNATE_PRIMARY_DOMAIN
import org.wordpress.android.fluxc.action.SiteAction.EXPORTED_SITE
import org.wordpress.android.fluxc.action.SiteAction.EXPORT_SITE
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_BLOCK_LAYOUTS
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_CONNECT_SITE_INFO
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_DOMAIN_SUPPORTED_COUNTRIES
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_DOMAIN_SUPPORTED_STATES
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_JETPACK_CAPABILITIES
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_PLANS
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_PRIVATE_ATOMIC_COOKIE
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_PROFILE_XML_RPC
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_SITE_EDITORS
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_USER_ROLES
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_WPCOM_SITE_BY_URL
import org.wordpress.android.fluxc.action.SiteAction.FETCH_BLOCK_LAYOUTS
import org.wordpress.android.fluxc.action.SiteAction.FETCH_CONNECT_SITE_INFO
import org.wordpress.android.fluxc.action.SiteAction.FETCH_DOMAIN_SUPPORTED_COUNTRIES
import org.wordpress.android.fluxc.action.SiteAction.FETCH_DOMAIN_SUPPORTED_STATES
import org.wordpress.android.fluxc.action.SiteAction.FETCH_JETPACK_CAPABILITIES
import org.wordpress.android.fluxc.action.SiteAction.FETCH_PLANS
import org.wordpress.android.fluxc.action.SiteAction.FETCH_POST_FORMATS
import org.wordpress.android.fluxc.action.SiteAction.FETCH_PRIVATE_ATOMIC_COOKIE
import org.wordpress.android.fluxc.action.SiteAction.FETCH_PROFILE_XML_RPC
import org.wordpress.android.fluxc.action.SiteAction.FETCH_SITE
import org.wordpress.android.fluxc.action.SiteAction.FETCH_SITES
import org.wordpress.android.fluxc.action.SiteAction.FETCH_SITES_XML_RPC
import org.wordpress.android.fluxc.action.SiteAction.FETCH_SITE_EDITORS
import org.wordpress.android.fluxc.action.SiteAction.FETCH_USER_ROLES
import org.wordpress.android.fluxc.action.SiteAction.FETCH_WPCOM_SITE_BY_URL
import org.wordpress.android.fluxc.action.SiteAction.HIDE_SITES
import org.wordpress.android.fluxc.action.SiteAction.INITIATED_AUTOMATED_TRANSFER
import org.wordpress.android.fluxc.action.SiteAction.INITIATE_AUTOMATED_TRANSFER
import org.wordpress.android.fluxc.action.SiteAction.IS_WPCOM_URL
import org.wordpress.android.fluxc.action.SiteAction.REMOVE_ALL_SITES
import org.wordpress.android.fluxc.action.SiteAction.REMOVE_SITE
import org.wordpress.android.fluxc.action.SiteAction.REMOVE_WPCOM_AND_JETPACK_SITES
import org.wordpress.android.fluxc.action.SiteAction.SHOW_SITES
import org.wordpress.android.fluxc.action.SiteAction.SUGGESTED_DOMAINS
import org.wordpress.android.fluxc.action.SiteAction.SUGGEST_DOMAINS
import org.wordpress.android.fluxc.action.SiteAction.UPDATE_SITE
import org.wordpress.android.fluxc.action.SiteAction.UPDATE_SITES
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.JetpackCapability
import org.wordpress.android.fluxc.model.PlanModel
import org.wordpress.android.fluxc.model.PostFormatModel
import org.wordpress.android.fluxc.model.RoleModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.SitesModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordDeletionResult
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsManager
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.site.Domain
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainPriceResponse
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse
import org.wordpress.android.fluxc.network.rest.wpcom.site.GutenbergLayout
import org.wordpress.android.fluxc.network.rest.wpcom.site.GutenbergLayoutCategory
import org.wordpress.android.fluxc.network.rest.wpcom.site.PrivateAtomicCookie
import org.wordpress.android.fluxc.network.rest.wpcom.site.PrivateAtomicCookieResponse
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.DeleteSiteResponsePayload
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.ExportSiteResponsePayload
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.FetchWPComSiteResponsePayload
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.IsWPComResponsePayload
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.NewSiteResponsePayload
import org.wordpress.android.fluxc.network.rest.wpcom.site.SupportedCountryResponse
import org.wordpress.android.fluxc.network.rest.wpcom.site.SupportedStateResponse
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient
import org.wordpress.android.fluxc.persistence.PostSqlUtils
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.SiteSqlUtils.DuplicateSiteException
import org.wordpress.android.fluxc.store.SiteStore.AccessCookieErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.store.SiteStore.AccessCookieErrorType.NON_PRIVATE_AT_SITE
import org.wordpress.android.fluxc.store.SiteStore.AccessCookieErrorType.SITE_MISSING_FROM_STORE
import org.wordpress.android.fluxc.store.SiteStore.DeleteSiteErrorType.INVALID_SITE
import org.wordpress.android.fluxc.store.SiteStore.DomainAvailabilityErrorType.INVALID_DOMAIN_NAME
import org.wordpress.android.fluxc.store.SiteStore.DomainSupportedStatesErrorType.INVALID_COUNTRY_CODE
import org.wordpress.android.fluxc.store.SiteStore.ExportSiteErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.SiteStore.PlansErrorType.NOT_AVAILABLE
import org.wordpress.android.fluxc.store.SiteStore.SelfHostedErrorType.NOT_SET
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.DUPLICATE_SITE
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.UNAUTHORIZED
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.UNKNOWN_SITE
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.SiteErrorUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.Locale
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * SQLite based only. There is no in memory copy of mapped data, everything is queried from the DB.
 *
 * NOTE: This class needs to be open because it's mocked in android tests in the WPAndroid project.
 *       TODO: consider adding https://kotlinlang.org/docs/all-open-plugin.html
 */
@Suppress("LargeClass", "ForbiddenComment")
@Singleton
open class SiteStore @Inject constructor(
    dispatcher: Dispatcher?,
    private val postSqlUtils: PostSqlUtils,
    private val siteRestClient: SiteRestClient,
    private val siteXMLRPCClient: SiteXMLRPCClient,
    private val privateAtomicCookie: PrivateAtomicCookie,
    private val siteSqlUtils: SiteSqlUtils,
    private val coroutineEngine: CoroutineEngine
) : Store(dispatcher) {
    @Inject internal lateinit var applicationPasswordsManagerProvider: Provider<ApplicationPasswordsManager>

    // Payloads
    data class CompleteQuickStartPayload(
        @JvmField val site: SiteModel,
        @JvmField val variant: String
    ) : Payload<BaseNetworkError>()

    data class RefreshSitesXMLRPCPayload(
        @JvmField val username: String = "",
        @JvmField val password: String = "",
        @JvmField val url: String = ""
    ) : Payload<BaseNetworkError>()

    data class FetchSitesPayload @JvmOverloads constructor(
        @JvmField val filters: List<SiteFilter> = ArrayList(),
        @JvmField val filterJetpackConnectedPackageSite: Boolean = false
    ) : Payload<BaseNetworkError>()

    /**
     * Holds the new site parameters for site creation
     *
     * @param siteName The domain of the site
     * @param siteTitle The title of the site
     * @param language The language of the site
     * @param timeZoneId The timezone of the site
     * @param visibility The visibility of the site (public or private)
     * @param segmentId The segment that the site belongs to
     * @param siteDesign The design template of the site
     * @param dryRun If set to true the call only validates the parameters passed
     */
    data class NewSitePayload(
        @JvmField val siteName: String?,
        @JvmField val siteTitle: String?,
        @JvmField val language: String,
        @JvmField val timeZoneId: String?,
        @JvmField val visibility: SiteVisibility,
        @JvmField val segmentId: Long? = null,
        @JvmField val siteDesign: String? = null,
        @JvmField val dryRun: Boolean
    ) : Payload<BaseNetworkError>() {
        constructor(
            siteName: String?,
            language: String,
            visibility: SiteVisibility,
            dryRun: Boolean
        ) : this(siteName, null, language, null, visibility, null, null, dryRun)

        constructor(
            siteName: String?,
            language: String,
            visibility: SiteVisibility,
            segmentId: Long?,
            dryRun: Boolean
        ) : this(siteName, null, language, null, visibility, segmentId, null, dryRun)

        constructor(
            siteName: String?,
            language: String,
            timeZoneId: String,
            visibility: SiteVisibility,
            dryRun: Boolean
        ) : this(siteName, null, language, timeZoneId, visibility, null, null, dryRun)

        constructor(
            siteName: String?,
            siteTitle: String?,
            language: String,
            timeZoneId: String,
            visibility: SiteVisibility,
            dryRun: Boolean
        ) : this(siteName, siteTitle, language, timeZoneId, visibility, null, null, dryRun)
    }

    data class FetchedPostFormatsPayload(
        @JvmField val site: SiteModel,
        @JvmField val postFormats: List<PostFormatModel>
    ) : Payload<PostFormatsError>()

    data class DesignateMobileEditorForAllSitesPayload
    @JvmOverloads constructor(
        @JvmField val editor: String,
        @JvmField val setOnlyIfEmpty: Boolean = true
    ) : Payload<SiteEditorsError>()

    data class DesignateMobileEditorPayload(
        @JvmField val site: SiteModel,
        @JvmField val editor: String
    ) : Payload<SiteEditorsError>()

    data class FetchedEditorsPayload(
        @JvmField val site: SiteModel,
        @JvmField val webEditor: String,
        @JvmField val mobileEditor: String
    ) : Payload<SiteEditorsError>()

    data class FetchBlockLayoutsPayload(
        @JvmField val site: SiteModel,
        @JvmField val supportedBlocks: List<String>?,
        @JvmField val previewWidth: Float?,
        @JvmField val previewHeight: Float?,
        @JvmField val scale: Float?,
        @JvmField val isBeta: Boolean?,
        @JvmField val preferCache: Boolean?
    ) : Payload<BaseNetworkError>()

    data class FetchedBlockLayoutsResponsePayload(
        @JvmField val site: SiteModel,
        @JvmField val layouts: List<GutenbergLayout>? = null,
        @JvmField val categories: List<GutenbergLayoutCategory>? = null
    ) : Payload<SiteError>() {
        constructor(site: SiteModel, error: SiteError?) : this(site) {
            this.error = error
        }
    }

    data class DesignateMobileEditorForAllSitesResponsePayload(
        @JvmField val editors: Map<String, String>? = null
    ) : Payload<SiteEditorsError>()

    data class FetchedUserRolesPayload(
        @JvmField val site: SiteModel,
        @JvmField val roles: List<RoleModel>
    ) : Payload<UserRolesError>()

    data class FetchedPlansPayload(
        @JvmField val site: SiteModel,
        @JvmField val plans: List<PlanModel>? = null
    ) : Payload<PlansError>() {
        constructor(site: SiteModel, error: PlansError) : this(site) {
            this.error = error
        }
    }

    data class FetchedPrivateAtomicCookiePayload(
        @JvmField val site: SiteModel,
        @JvmField val cookie: PrivateAtomicCookieResponse?
    ) : Payload<PrivateAtomicCookieError>()

    data class FetchPrivateAtomicCookiePayload(@JvmField val siteId: Long)
    data class FetchJetpackCapabilitiesPayload(@JvmField val remoteSiteId: Long)
    data class FetchedJetpackCapabilitiesPayload(
        @JvmField val remoteSiteId: Long,
        @JvmField val capabilities: List<JetpackCapability> = listOf()
    ) : Payload<JetpackCapabilitiesError>() {
        constructor(remoteSiteId: Long, error: JetpackCapabilitiesError) : this(remoteSiteId) {
            this.error = error
        }
    }

    data class OnJetpackCapabilitiesFetched(
        @JvmField val remoteSiteId: Long,
        @JvmField val capabilities: List<JetpackCapability> = listOf(),
        @JvmField val error: JetpackCapabilitiesError? = null
    ) : OnChanged<JetpackCapabilitiesError>()

    data class SuggestDomainsPayload(
        @JvmField val query: String,
        @JvmField val onlyWordpressCom: Boolean? = null,
        @JvmField val includeWordpressCom: Boolean? = null,
        @JvmField val includeDotBlogSubdomain: Boolean? = null,
        @JvmField val tlds: String? = null,
        @JvmField val segmentId: Long? = null,
        @JvmField val quantity: Int,
        @JvmField val includeVendorDot: Boolean = false
    ) : Payload<BaseNetworkError>() {
        constructor(
            query: String,
            onlyWordpressCom: Boolean,
            includeWordpressCom: Boolean,
            includeDotBlogSubdomain: Boolean,
            quantity: Int,
            includeVendorDot: Boolean
        ) : this(
                query = query,
                onlyWordpressCom = onlyWordpressCom,
                includeWordpressCom = includeWordpressCom,
                includeDotBlogSubdomain = includeDotBlogSubdomain,
                quantity = quantity,
                includeVendorDot = includeVendorDot,
                segmentId = null,
                tlds = null
        )

        constructor(query: String, segmentId: Long?, quantity: Int, includeVendorDot: Boolean) : this(
                query = query,
                segmentId = segmentId,
                quantity = quantity,
                includeVendorDot = includeVendorDot,
                tlds = null
        )

        constructor(query: String, quantity: Int, tlds: String?) : this(
                query = query,
                quantity = quantity,
                tlds = tlds,
                segmentId = null
        )
    }

    data class SuggestDomainsResponsePayload(
        @JvmField val query: String,
        @JvmField val suggestions: List<DomainSuggestionResponse> = listOf()
    ) : Payload<SuggestDomainError>() {
        constructor(query: String, error: SuggestDomainError?) : this(query) {
            this.error = error
        }
    }

    data class ConnectSiteInfoPayload
    @JvmOverloads constructor(
        @JvmField val url: String,
        @JvmField val exists: Boolean = false,
        @JvmField val isWordPress: Boolean = false,
        @JvmField val hasJetpack: Boolean = false,
        @JvmField val isJetpackActive: Boolean = false,
        @JvmField val isJetpackConnected: Boolean = false,
        @JvmField val isWPCom: Boolean = false,
        @JvmField val urlAfterRedirects: String? = null
    ) : Payload<SiteError>() {
        constructor(url: String, error: SiteError?) : this(url) {
            this.error = error
        }

        fun description(): String {
            return String.format(
                Locale.US,
                "url: %s, e: %b, wp: %b, jp: %b, wpcom: %b, urlAfterRedirects: %s",
                url, exists, isWordPress, hasJetpack, isWPCom, urlAfterRedirects
            )
        }
    }

    data class DesignatePrimaryDomainPayload(
        @JvmField val site: SiteModel,
        @JvmField val domain: String
    ) : Payload<DesignatePrimaryDomainError>()

    data class InitiateAutomatedTransferPayload(
        @JvmField val site: SiteModel,
        @JvmField val pluginSlugToInstall: String
    ) : Payload<AutomatedTransferError>()

    data class AutomatedTransferEligibilityResponsePayload
    @JvmOverloads constructor(
        @JvmField val site: SiteModel,
        @JvmField val isEligible: Boolean = false,
        @JvmField val errorCodes: List<String> = listOf()
    ) : Payload<AutomatedTransferError>() {
        constructor(site: SiteModel, error: AutomatedTransferError) : this(site) {
            this.error = error
        }
    }

    data class InitiateAutomatedTransferResponsePayload
    @JvmOverloads constructor(
        @JvmField val site: SiteModel,
        @JvmField val pluginSlugToInstall: String,
        @JvmField val success: Boolean = false
    ) : Payload<AutomatedTransferError>()

    data class AutomatedTransferStatusResponsePayload(
        @JvmField val site: SiteModel,
        @JvmField val status: String? = null,
        @JvmField val currentStep: Int = 0,
        @JvmField val totalSteps: Int = 0
    ) : Payload<AutomatedTransferError>() {
        constructor(site: SiteModel, error: AutomatedTransferError?) : this(site) {
            this.error = error
        }
    }

    data class DomainAvailabilityResponsePayload(
        @JvmField val status: DomainAvailabilityStatus? = null,
        @JvmField val mappable: DomainMappabilityStatus? = null,
        @JvmField val supportsPrivacy: Boolean = false
    ) : Payload<DomainAvailabilityError>() {
        constructor(error: DomainAvailabilityError) : this() {
            this.error = error
        }
    }

    data class DomainSupportedStatesResponsePayload(
        @JvmField val supportedStates: List<SupportedStateResponse>? = null
    ) : Payload<DomainSupportedStatesError>() {
        constructor(error: DomainSupportedStatesError) : this() {
            this.error = error
        }
    }

    data class DomainSupportedCountriesResponsePayload(
        @JvmField val supportedCountries: List<SupportedCountryResponse>? = null
    ) : Payload<DomainSupportedCountriesError>() {
        constructor(error: DomainSupportedCountriesError) : this() {
            this.error = error
        }
    }

    data class SiteError @JvmOverloads constructor(
        @JvmField val type: SiteErrorType,
        @JvmField val message: String? = null,
        @JvmField val selfHostedErrorType: SelfHostedErrorType = NOT_SET
    ) : OnChangedError

    data class SiteEditorsError internal constructor(
        @JvmField val type: SiteEditorsErrorType?,
        @JvmField val message: String
    ) : OnChangedError {
        constructor(type: SiteEditorsErrorType?) : this(type, "")
    }

    data class PostFormatsError @JvmOverloads constructor(
        @JvmField val type: PostFormatsErrorType,
        @JvmField val message: String? = ""
    ) : OnChangedError

    data class UserRolesError internal constructor(
        @JvmField val type: UserRolesErrorType?,
        @JvmField val message: String
    ) : OnChangedError {
        constructor(type: UserRolesErrorType?) : this(type, "")
    }

    data class NewSiteError(@JvmField val type: NewSiteErrorType, @JvmField val message: String) : OnChangedError
    data class DeleteSiteError(
        @JvmField val type: DeleteSiteErrorType,
        @JvmField val message: String = ""
    ) : OnChangedError {
        constructor(errorType: String, message: String) : this(DeleteSiteErrorType.fromString(errorType), message)
    }

    data class ExportSiteError(@JvmField val type: ExportSiteErrorType) : OnChangedError
    data class AutomatedTransferError(@JvmField val type: AutomatedTransferErrorType?, @JvmField val message: String?) :
            OnChangedError {
        constructor(type: String, message: String) : this(AutomatedTransferErrorType.fromString(type), message)
    }

    data class DomainAvailabilityError
    @JvmOverloads
    constructor(
        @JvmField val type: DomainAvailabilityErrorType,
        @JvmField val message: String? = null
    ) : OnChangedError

    data class DomainSupportedStatesError
    @JvmOverloads
    constructor(
        @JvmField val type: DomainSupportedStatesErrorType,
        @JvmField val message: String? = null
    ) : OnChangedError

    data class DomainSupportedCountriesError(
        @JvmField val type: DomainSupportedCountriesErrorType,
        @JvmField val message: String?
    ) : OnChangedError

    data class QuickStartError(@JvmField val type: QuickStartErrorType, @JvmField val message: String?) : OnChangedError
    data class DesignatePrimaryDomainError(
        @JvmField val type: DesignatePrimaryDomainErrorType,
        @JvmField val message: String?
    ) : OnChangedError

    // OnChanged Events
    data class OnProfileFetched(@JvmField val site: SiteModel) : OnChanged<SiteError>()
    data class OnSiteChanged(
        @JvmField val rowsAffected: Int = 0,
        @JvmField val updatedSites: List<SiteModel> = emptyList()
    ) : OnChanged<SiteError>() {
        constructor(rowsAffected: Int = 0, siteError: SiteError?) : this(rowsAffected) {
            this.error = siteError
        }

        constructor(siteError: SiteError) : this(0, siteError)
    }

    data class OnSiteRemoved(@JvmField val mRowsAffected: Int) : OnChanged<SiteError>()
    data class OnAllSitesRemoved(@JvmField val mRowsAffected: Int) : OnChanged<SiteError>()
    data class OnBlockLayoutsFetched(
        @JvmField val layouts: List<GutenbergLayout>?,
        @JvmField val categories: List<GutenbergLayoutCategory>?
    ) : OnChanged<SiteError>() {
        constructor(
            layouts: List<GutenbergLayout>?,
            categories: List<GutenbergLayoutCategory>?,
            error: SiteError?
        ) : this(layouts, categories) {
            this.error = error
        }
    }

    data class OnNewSiteCreated(
        @JvmField val dryRun: Boolean = false,
        @JvmField val url: String? = null,
        @JvmField val newSiteRemoteId: Long = 0
    ) : OnChanged<NewSiteError>() {
        constructor(dryRun: Boolean, url: String?, newSiteRemoteId: Long, error: NewSiteError?) : this(
                dryRun,
                url,
                newSiteRemoteId
        ) {
            this.error = error
        }
    }

    data class OnSiteDeleted(@JvmField val error: DeleteSiteError?) : OnChanged<DeleteSiteError>() {
        init {
            this.error = error
        }
    }

    class OnSiteExported() : OnChanged<ExportSiteError>() {
        constructor(error: ExportSiteError?) : this() {
            this.error = error
        }
    }

    data class OnPostFormatsChanged(@JvmField val site: SiteModel) : OnChanged<PostFormatsError>()
    data class OnSiteEditorsChanged(
        @JvmField val site: SiteModel,
        @JvmField val rowsAffected: Int = 0
    ) : OnChanged<SiteEditorsError>() {
        constructor(site: SiteModel, error: SiteEditorsError?) : this(site) {
            this.error = error
        }
    }

    data class OnAllSitesMobileEditorChanged(
        @JvmField val rowsAffected: Int = 0,
            // True when all sites are self-hosted or wpcom backend response
        @JvmField val isNetworkResponse: Boolean = false,
        @JvmField val siteEditorsError: SiteEditorsError? = null
    ) : OnChanged<SiteEditorsError>() {
        init {
            this.error = siteEditorsError
        }
    }

    data class OnUserRolesChanged(@JvmField val site: SiteModel) : OnChanged<UserRolesError>()
    data class OnPlansFetched(
        @JvmField val site: SiteModel,
        @JvmField val plans: List<PlanModel>?
    ) : OnChanged<PlansError>() {
        constructor(
            site: SiteModel,
            plans: List<PlanModel>?,
            error: PlansError?
        ) : this(site, plans) {
            this.error = error
        }
    }

    data class OnPrivateAtomicCookieFetched(
        @JvmField val site: SiteModel?,
        @JvmField val success: Boolean,
        @JvmField val privateAtomicCookieError: PrivateAtomicCookieError? = null
    ) : OnChanged<PrivateAtomicCookieError>() {
        init {
            this.error = privateAtomicCookieError
        }
    }

    data class OnURLChecked(
        @JvmField val url: String,
        @JvmField val isWPCom: Boolean = false,
        val siteError: SiteError? = null
    ) : OnChanged<SiteError>() {
        init {
            this.error = siteError
        }
    }

    data class OnConnectSiteInfoChecked(@JvmField val info: ConnectSiteInfoPayload) : OnChanged<SiteError>()
    data class OnWPComSiteFetched(
        @JvmField val checkedUrl: String? = null,
        @JvmField val site: SiteModel? = null
    ) : OnChanged<SiteError>()

    data class SuggestDomainError(@JvmField val type: SuggestDomainErrorType, @JvmField val message: String) :
            OnChangedError {
        constructor(apiErrorType: String, message: String) : this(
                SuggestDomainErrorType.fromString(apiErrorType),
                message
        )
    }

    data class OnSuggestedDomains(
        val query: String,
        @JvmField val suggestions: List<DomainSuggestionResponse>
    ) : OnChanged<SuggestDomainError>()

    data class OnDomainAvailabilityChecked(
        @JvmField val status: DomainAvailabilityStatus?,
        @JvmField val mappable: DomainMappabilityStatus?,
        @JvmField val supportsPrivacy: Boolean
    ) : OnChanged<DomainAvailabilityError>() {
        constructor(
            status: DomainAvailabilityStatus?,
            mappable: DomainMappabilityStatus?,
            supportsPrivacy: Boolean,
            error: DomainAvailabilityError?
        ) : this(status, mappable, supportsPrivacy) {
            this.error = error
        }
    }

    enum class DomainAvailabilityStatus {
        BLACKLISTED_DOMAIN,
        INVALID_TLD,
        INVALID_DOMAIN,
        TLD_NOT_SUPPORTED,
        TRANSFERRABLE_DOMAIN,
        AVAILABLE,
        UNKNOWN_STATUS;

        companion object {
            @JvmStatic fun fromString(string: String): DomainAvailabilityStatus {
                if (!TextUtils.isEmpty(string)) {
                    for (v in values()) {
                        if (string.equals(v.name, ignoreCase = true)) {
                            return v
                        }
                    }
                }
                return UNKNOWN_STATUS
            }
        }
    }

    enum class DomainMappabilityStatus {
        BLACKLISTED_DOMAIN, INVALID_TLD, INVALID_DOMAIN, MAPPABLE_DOMAIN, UNKNOWN_STATUS;

        companion object {
            @JvmStatic fun fromString(string: String): DomainMappabilityStatus {
                if (!TextUtils.isEmpty(string)) {
                    for (v in values()) {
                        if (string.equals(v.name, ignoreCase = true)) {
                            return v
                        }
                    }
                }
                return UNKNOWN_STATUS
            }
        }
    }

    data class OnDomainSupportedStatesFetched(
        @JvmField val supportedStates: List<SupportedStateResponse>?
    ) : OnChanged<DomainSupportedStatesError>() {
        constructor(
            supportedStates: List<SupportedStateResponse>?,
            error: DomainSupportedStatesError?
        ) : this(supportedStates) {
            this.error = error
        }
    }

    class OnDomainSupportedCountriesFetched(
        @JvmField val supportedCountries: List<SupportedCountryResponse>?,
        error: DomainSupportedCountriesError?
    ) : OnChanged<DomainSupportedCountriesError>() {
        init {
            this.error = error
        }
    }

    data class FetchedDomainsPayload(
        @JvmField val site: SiteModel,
        @JvmField val domains: List<Domain>? = null
    ) : Payload<SiteError>() {
        constructor(site: SiteModel, error: SiteError) : this(site) {
            this.error = error
        }
    }

    data class OnApplicationPasswordDeleted(val site: SiteModel) : OnChanged<OnApplicationPasswordDeleteError>() {
        constructor(site: SiteModel, error: BaseNetworkError): this(site) {
            this.error = OnApplicationPasswordDeleteError(error)
        }
    }

    class OnApplicationPasswordDeleteError(error: BaseNetworkError) : OnChangedError {
        var errorCode: String? = null
        var message: String

        init {
            if (error is WPAPINetworkError) {
                errorCode = error.errorCode
            } else if (error is WPComGsonNetworkError) {
                errorCode = error.apiError
            }
            message = error.message
        }
    }

    class PlansError
    @JvmOverloads constructor(
        @JvmField val type: PlansErrorType,
        @JvmField val message: String? = null
    ) : OnChangedError {
        constructor(type: String?, message: String?) : this(PlansErrorType.fromString(type), message)
    }

    class PrivateAtomicCookieError(@JvmField val type: AccessCookieErrorType, @JvmField val message: String) :
            OnChangedError

    class JetpackCapabilitiesError(@JvmField val type: JetpackCapabilitiesErrorType, @JvmField val message: String?) :
            OnChangedError

    class OnAutomatedTransferEligibilityChecked(
        @JvmField val site: SiteModel,
        @JvmField val isEligible: Boolean,
        @JvmField val eligibilityErrorCodes: List<String>
    ) : OnChanged<AutomatedTransferError>() {
        constructor(
            site: SiteModel,
            isEligible: Boolean,
            eligibilityErrorCodes: List<String>,
            error: AutomatedTransferError?
        ) : this(site, isEligible, eligibilityErrorCodes) {
            this.error = error
        }
    }

    class OnAutomatedTransferInitiated(
        @JvmField val site: SiteModel,
        @JvmField val pluginSlugToInstall: String
    ) : OnChanged<AutomatedTransferError>() {
        constructor(
            site: SiteModel,
            pluginSlugToInstall: String,
            error: AutomatedTransferError?
        ) : this(site, pluginSlugToInstall) {
            this.error = error
        }
    }

    class OnAutomatedTransferStatusChecked(
        @JvmField val site: SiteModel,
        @JvmField val isCompleted: Boolean = false,
        @JvmField val currentStep: Int = 0,
        @JvmField val totalSteps: Int = 0
    ) : OnChanged<AutomatedTransferError>() {
        constructor(site: SiteModel, error: AutomatedTransferError?) : this(site) {
            this.error = error
        }
    }

    class QuickStartCompletedResponsePayload(
        @JvmField val site: SiteModel,
        @JvmField val success: Boolean
    ) : OnChanged<QuickStartError>()

    class OnQuickStartCompleted internal constructor(
        @JvmField val site: SiteModel,
        @JvmField val success: Boolean
    ) : OnChanged<QuickStartError>()

    class DesignatedPrimaryDomainPayload(
        @JvmField val site: SiteModel,
        @JvmField val success: Boolean
    ) : OnChanged<DesignatePrimaryDomainError>()

    class OnPrimaryDomainDesignated(
        @JvmField val site: SiteModel,
        @JvmField val success: Boolean
    ) : OnChanged<DesignatePrimaryDomainError>()

    data class UpdateSitesResult(
        @JvmField val rowsAffected: Int = 0,
        @JvmField val updatedSites: List<SiteModel> = emptyList(),
        @JvmField val duplicateSiteFound: Boolean = false
    )

    enum class SiteErrorType {
        INVALID_SITE, UNKNOWN_SITE, DUPLICATE_SITE, INVALID_RESPONSE, UNAUTHORIZED, GENERIC_ERROR
    }

    enum class SuggestDomainErrorType {
        EMPTY_RESULTS, EMPTY_QUERY, INVALID_MINIMUM_QUANTITY, INVALID_MAXIMUM_QUANTITY, INVALID_QUERY, GENERIC_ERROR;

        companion object {
            fun fromString(string: String): SuggestDomainErrorType {
                if (!TextUtils.isEmpty(string)) {
                    for (v in values()) {
                        if (string.equals(v.name, ignoreCase = true)) {
                            return v
                        }
                    }
                }
                return GENERIC_ERROR
            }
        }
    }

    enum class PostFormatsErrorType {
        INVALID_SITE, INVALID_RESPONSE, GENERIC_ERROR
    }

    enum class PlansErrorType {
        NOT_AVAILABLE, AUTHORIZATION_REQUIRED, UNAUTHORIZED, UNKNOWN_BLOG, GENERIC_ERROR;

        companion object {
            fun fromString(type: String?): PlansErrorType {
                if (!TextUtils.isEmpty(type)) {
                    for (v in values()) {
                        if (type.equals(v.name, ignoreCase = true)) {
                            return v
                        }
                    }
                }
                return GENERIC_ERROR
            }
        }
    }

    enum class AccessCookieErrorType {
        GENERIC_ERROR, INVALID_RESPONSE, SITE_MISSING_FROM_STORE, NON_PRIVATE_AT_SITE
    }

    enum class UserRolesErrorType {
        GENERIC_ERROR
    }

    enum class SiteEditorsErrorType {
        GENERIC_ERROR
    }

    enum class JetpackCapabilitiesErrorType {
        GENERIC_ERROR
    }

    enum class SelfHostedErrorType {
        NOT_SET,
        XML_RPC_SERVICES_DISABLED,
        UNABLE_TO_READ_SITE
    }

    enum class DeleteSiteErrorType {
        INVALID_SITE, UNAUTHORIZED, // user don't have permission to delete
        AUTHORIZATION_REQUIRED, // missing access token
        GENERIC_ERROR;

        companion object {
            @Suppress("ReturnCount")
            fun fromString(string: String): DeleteSiteErrorType {
                if (!TextUtils.isEmpty(string)) {
                    if (string == "unauthorized") {
                        return UNAUTHORIZED
                    } else if (string == "authorization_required") {
                        return AUTHORIZATION_REQUIRED
                    }
                }
                return GENERIC_ERROR
            }
        }
    }

    enum class ExportSiteErrorType {
        INVALID_SITE, GENERIC_ERROR
    }

    // Enums
    enum class NewSiteErrorType {
        SITE_NAME_REQUIRED,
        SITE_NAME_NOT_ALLOWED,
        SITE_NAME_MUST_BE_AT_LEAST_FOUR_CHARACTERS,
        SITE_NAME_MUST_BE_LESS_THAN_SIXTY_FOUR_CHARACTERS,
        SITE_NAME_CONTAINS_INVALID_CHARACTERS,
        SITE_NAME_CANT_BE_USED,
        SITE_NAME_ONLY_LOWERCASE_LETTERS_AND_NUMBERS,
        SITE_NAME_MUST_INCLUDE_LETTERS,
        SITE_NAME_EXISTS,
        SITE_NAME_RESERVED,
        SITE_NAME_RESERVED_BUT_MAY_BE_AVAILABLE,
        SITE_NAME_INVALID,
        SITE_TITLE_INVALID,
        GENERIC_ERROR;

        companion object {
            // SiteStore semantics prefers SITE over BLOG but errors reported from the API use BLOG
            // these are used to convert API errors to the appropriate enum value in fromString
            private const val BLOG = "BLOG"
            private const val SITE = "SITE"
            @JvmStatic fun fromString(string: String): NewSiteErrorType {
                if (!TextUtils.isEmpty(string)) {
                    val siteString = string.toUpperCase(Locale.US).replace(BLOG, SITE)
                    for (v in values()) {
                        if (siteString.equals(v.name, ignoreCase = true)) {
                            return v
                        }
                    }
                }
                return GENERIC_ERROR
            }
        }
    }

    enum class AutomatedTransferErrorType {
        AT_NOT_ELIGIBLE, // occurs if AT is initiated when the site is not eligible
        NOT_FOUND, // occurs if transfer status of a site with no active transfer is checked
        GENERIC_ERROR;

        companion object {
            fun fromString(type: String?): AutomatedTransferErrorType {
                if (!TextUtils.isEmpty(type)) {
                    for (v in values()) {
                        if (type.equals(v.name, ignoreCase = true)) {
                            return v
                        }
                    }
                }
                return GENERIC_ERROR
            }
        }
    }

    enum class DomainAvailabilityErrorType {
        INVALID_DOMAIN_NAME, GENERIC_ERROR
    }

    enum class DomainSupportedStatesErrorType {
        INVALID_COUNTRY_CODE, INVALID_QUERY, GENERIC_ERROR;

        companion object {
            @JvmStatic fun fromString(type: String): DomainSupportedStatesErrorType {
                if (!TextUtils.isEmpty(type)) {
                    for (v in values()) {
                        if (type.equals(v.name, ignoreCase = true)) {
                            return v
                        }
                    }
                }
                return GENERIC_ERROR
            }
        }
    }

    enum class DomainSupportedCountriesErrorType {
        GENERIC_ERROR
    }

    enum class QuickStartErrorType {
        GENERIC_ERROR
    }

    enum class DesignatePrimaryDomainErrorType {
        GENERIC_ERROR
    }

    enum class SiteVisibility(private val mValue: Int) {
        PRIVATE(-1), BLOCK_SEARCH_ENGINE(0), PUBLIC(1);

        fun value(): Int {
            return mValue
        }
    }

    enum class CompleteQuickStartVariant(private val mString: String) {
        NEXT_STEPS("next-steps");

        override fun toString(): String {
            return mString
        }
    }

    enum class SiteFilter(private val mString: String) {
        ATOMIC("atomic"), JETPACK("jetpack"), WPCOM("wpcom");

        override fun toString(): String {
            return mString
        }
    }

    override fun onRegister() {
        AppLog.d(T.API, "SiteStore onRegister")
    }

    /**
     * Returns all sites in the store as a [SiteModel] list.
     */
    val sites: List<SiteModel>
        get() = siteSqlUtils.getSites()

    /**
     * Returns the number of sites of any kind in the store.
     */
    val sitesCount: Int
        get() = siteSqlUtils.getSites().count()

    /**
     * Checks whether the store contains any sites of any kind.
     */
    fun hasSite(): Boolean {
        return sitesCount != 0
    }

    /**
     * Obtains the site with the given (local) id and returns it as a [SiteModel].
     *
     * NOTE: This method needs to be open because it's mocked in android tests in the WPAndroid project.
     *       TODO: consider adding https://kotlinlang.org/docs/all-open-plugin.html
     */
    @Suppress("ForbiddenComment")
    open fun getSiteByLocalId(id: Int): SiteModel? {
        val result = siteSqlUtils.getSitesWithLocalId(id)
        return if (result.isNotEmpty()) {
            result[0]
        } else null
    }

    /**
     * Checks whether the store contains a site matching the given (local) id.
     */
    fun hasSiteWithLocalId(id: Int): Boolean {
        return siteSqlUtils.getSitesWithLocalId(id).isNotEmpty()
    }

    /**
     * Returns all .COM sites in the store.
     */
    val wPComSites: List<SiteModel>
        get() = siteSqlUtils.getWpComSites()

    /**
     * Returns sites accessed via WPCom REST API (WPCom sites or Jetpack sites connected via WPCom REST API).
     */
    val sitesAccessedViaWPComRest: List<SiteModel>
        get() = siteSqlUtils.sitesAccessedViaWPComRest.asModel

    /**
     * Returns the number of sites accessed via WPCom REST API (WPCom sites or Jetpack sites connected
     * via WPCom REST API).
     */
    val sitesAccessedViaWPComRestCount: Int
        get() = siteSqlUtils.sitesAccessedViaWPComRest.count().toInt()

    /**
     * Checks whether the store contains at least one site accessed via WPCom REST API (WPCom sites or Jetpack
     * sites connected via WPCom REST API).
     */
    fun hasSitesAccessedViaWPComRest(): Boolean {
        return sitesAccessedViaWPComRestCount != 0
    }

    /**
     * Returns the number of .COM sites in the store.
     */
    val wPComSitesCount: Int
        get() = siteSqlUtils.getWpComSites().size

    /**
     * Returns the number of .COM Atomic sites in the store.
     */
    val wPComAtomicSitesCount: Int
        get() = siteSqlUtils.getWpComAtomicSites().size

    /**
     * Returns sites with a name or url matching the search string.
     */
    fun getSitesByNameOrUrlMatching(searchString: String): List<SiteModel> {
        return siteSqlUtils.getSitesByNameOrUrlMatching(searchString)
    }

    /**
     * Returns sites accessed via WPCom REST API (WPCom sites or Jetpack sites connected via WPCom REST API) with a
     * name or url matching the search string.
     */
    fun getSitesAccessedViaWPComRestByNameOrUrlMatching(searchString: String): List<SiteModel> {
        return siteSqlUtils.getSitesAccessedViaWPComRestByNameOrUrlMatching(searchString)
    }

    /**
     * Checks whether the store contains at least one .COM site.
     */
    fun hasWPComSite(): Boolean {
        return wPComSitesCount != 0
    }

    /**
     * Checks whether the store contains at least one .COM Atomic site.
     */
    fun hasWPComAtomicSite(): Boolean {
        return wPComAtomicSitesCount != 0
    }

    /**
     * Returns sites accessed via XMLRPC (self-hosted sites or Jetpack sites accessed via XMLRPC).
     */
    val sitesAccessedViaXMLRPC: List<SiteModel>
        get() = siteSqlUtils.sitesAccessedViaXMLRPC.asModel

    /**
     * Returns the number of sites accessed via XMLRPC (self-hosted sites or Jetpack sites accessed via XMLRPC).
     */
    val sitesAccessedViaXMLRPCCount: Int
        get() = siteSqlUtils.sitesAccessedViaXMLRPC.count().toInt()

    /**
     * Checks whether the store contains at least one site accessed via XMLRPC (self-hosted sites or
     * Jetpack sites accessed via XMLRPC).
     */
    fun hasSiteAccessedViaXMLRPC(): Boolean {
        return sitesAccessedViaXMLRPCCount != 0
    }

    /**
     * Returns all visible sites as [SiteModel]s. All self-hosted sites over XML-RPC are visible by default.
     */
    val visibleSites: List<SiteModel>
        get() = siteSqlUtils.getVisibleSites()

    /**
     * Returns the number of visible sites. All self-hosted sites over XML-RPC are visible by default.
     */
    val visibleSitesCount: Int
        get() = siteSqlUtils.getVisibleSites().size

    /**
     * Returns all visible .COM sites as [SiteModel]s.
     */
    val visibleSitesAccessedViaWPCom: List<SiteModel>
        get() = siteSqlUtils.visibleSitesAccessedViaWPCom.asModel

    /**
     * Returns the number of visible .COM sites.
     */
    val visibleSitesAccessedViaWPComCount: Int
        get() = siteSqlUtils.visibleSitesAccessedViaWPCom.count().toInt()

    /**
     * Checks whether the .COM site with the given (local) id is visible.
     */
    fun isWPComSiteVisibleByLocalId(id: Int): Boolean {
        return siteSqlUtils.isWPComSiteVisibleByLocalId(id)
    }

    /**
     * Given a (remote) site id, returns the corresponding (local) id.
     */
    fun getLocalIdForRemoteSiteId(siteId: Long): Int {
        return siteSqlUtils.getLocalIdForRemoteSiteId(siteId)
    }

    /**
     * Given a (remote) self-hosted site id and XML-RPC url, returns the corresponding (local) id.
     */
    fun getLocalIdForSelfHostedSiteIdAndXmlRpcUrl(selfHostedSiteId: Long, xmlRpcUrl: String?): Int {
        return siteSqlUtils.getLocalIdForSelfHostedSiteIdAndXmlRpcUrl(selfHostedSiteId, xmlRpcUrl)
    }

    /**
     * Given a (local) id, returns the (remote) site id. Searches first for .COM and Jetpack, then looks for self-hosted
     * sites.
     */
    fun getSiteIdForLocalId(id: Int): Long {
        return siteSqlUtils.getSiteIdForLocalId(id)
    }

    /**
     * Given a .COM site ID (either a .COM site id, or the .COM id of a Jetpack site), returns the site as a
     * [SiteModel].
     */
    fun getSiteBySiteId(siteId: Long): SiteModel? {
        if (siteId == 0L) {
            return null
        }
        val sites = siteSqlUtils.getSitesWithRemoteId(siteId)
        return if (sites.isEmpty()) {
            null
        } else {
            sites[0]
        }
    }

    /**
     * Gets the cached content of a page layout
     *
     * @param site the current site
     * @param slug the slug of the layout
     * @return the content or null if the content is not cached
     */
    fun getBlockLayoutContent(site: SiteModel, slug: String): String? {
        return siteSqlUtils.getBlockLayoutContent(site, slug)
    }

    /**
     * Gets the cached page layout
     *
     * @param site the current site
     * @param slug the slug of the layout
     * @return the layout or null if the layout is not cached
     */
    fun getBlockLayout(site: SiteModel, slug: String): GutenbergLayout? {
        return siteSqlUtils.getBlockLayout(site, slug)
    }

    fun getPostFormats(site: SiteModel?): List<PostFormatModel> {
        return siteSqlUtils.getPostFormats(site!!)
    }

    fun getUserRoles(site: SiteModel?): List<RoleModel> {
        return siteSqlUtils.getUserRoles(site!!)
    }

    @Subscribe(threadMode = ASYNC)
    @Suppress("LongMethod", "ComplexMethod")
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? SiteAction ?: return
        when (actionType) {
            FETCH_PROFILE_XML_RPC -> fetchProfileXmlRpc(action.payload as SiteModel)
            FETCHED_PROFILE_XML_RPC -> updateSiteProfile(action.payload as SiteModel)
            FETCH_SITE -> coroutineEngine.launch(T.MAIN, this, "Fetch site") {
                emitChange(fetchSite(action.payload as SiteModel))
            }
            FETCH_SITES -> coroutineEngine.launch(T.MAIN, this, "Fetch sites") {
                emitChange(fetchSites(action.payload as FetchSitesPayload))
            }
            FETCH_SITES_XML_RPC -> coroutineEngine.launch(T.MAIN, this, "Fetch XMLRPC sites") {
                emitChange(fetchSitesXmlRpc(action.payload as RefreshSitesXMLRPCPayload))
            }
            UPDATE_SITE -> {
                emitChange(updateSite(action.payload as SiteModel))
            }
            UPDATE_SITES -> updateSites(action.payload as SitesModel)
            DELETE_SITE -> deleteSite(action.payload as SiteModel)
            DELETED_SITE -> handleDeletedSite(action.payload as DeleteSiteResponsePayload)
            EXPORT_SITE -> exportSite(action.payload as SiteModel)
            EXPORTED_SITE -> handleExportedSite(action.payload as ExportSiteResponsePayload)
            REMOVE_SITE -> removeSite(action.payload as SiteModel)
            REMOVE_ALL_SITES -> removeAllSites()
            REMOVE_WPCOM_AND_JETPACK_SITES -> removeWPComAndJetpackSites()
            SHOW_SITES -> toggleSitesVisibility(action.payload as SitesModel, true)
            HIDE_SITES -> toggleSitesVisibility(action.payload as SitesModel, false)
            CREATE_NEW_SITE -> coroutineEngine.launch(T.MAIN, this, "Create a new site") {
                emitChange(createNewSite(action.payload as NewSitePayload))
            }
            FETCH_POST_FORMATS -> coroutineEngine.launch(T.MAIN, this, "Fetch post formats") {
                emitChange(fetchPostFormats(action.payload as SiteModel))
            }
            FETCH_SITE_EDITORS -> fetchSiteEditors(action.payload as SiteModel)
            FETCH_BLOCK_LAYOUTS -> fetchBlockLayouts(action.payload as FetchBlockLayoutsPayload)
            FETCHED_BLOCK_LAYOUTS -> handleFetchedBlockLayouts(action.payload as FetchedBlockLayoutsResponsePayload)
            DESIGNATE_MOBILE_EDITOR -> designateMobileEditor(action.payload as DesignateMobileEditorPayload)
            DESIGNATE_MOBILE_EDITOR_FOR_ALL_SITES -> designateMobileEditorForAllSites(
                    action.payload as DesignateMobileEditorForAllSitesPayload
            )
            FETCHED_SITE_EDITORS -> updateSiteEditors(action.payload as FetchedEditorsPayload)
            DESIGNATED_MOBILE_EDITOR_FOR_ALL_SITES -> handleDesignatedMobileEditorForAllSites(
                    action.payload as DesignateMobileEditorForAllSitesResponsePayload
            )
            FETCH_USER_ROLES -> fetchUserRoles(action.payload as SiteModel)
            FETCHED_USER_ROLES -> updateUserRoles(action.payload as FetchedUserRolesPayload)
            FETCH_CONNECT_SITE_INFO -> fetchConnectSiteInfo(action.payload as String)
            FETCHED_CONNECT_SITE_INFO -> handleFetchedConnectSiteInfo(action.payload as ConnectSiteInfoPayload)
            FETCH_WPCOM_SITE_BY_URL -> fetchWPComSiteByUrl(action.payload as String)
            FETCHED_WPCOM_SITE_BY_URL -> handleFetchedWPComSiteByUrl(action.payload as FetchWPComSiteResponsePayload)
            IS_WPCOM_URL -> checkUrlIsWPCom(action.payload as String)
            CHECKED_IS_WPCOM_URL -> handleCheckedIsWPComUrl(action.payload as IsWPComResponsePayload)
            SUGGEST_DOMAINS -> suggestDomains(action.payload as SuggestDomainsPayload)
            SUGGESTED_DOMAINS -> handleSuggestedDomains(action.payload as SuggestDomainsResponsePayload)
            FETCH_PLANS -> fetchPlans(action.payload as SiteModel)
            FETCHED_PLANS -> handleFetchedPlans(action.payload as FetchedPlansPayload)
            CHECK_DOMAIN_AVAILABILITY -> checkDomainAvailability(action.payload as String)
            CHECKED_DOMAIN_AVAILABILITY -> handleCheckedDomainAvailability(
                    action.payload as DomainAvailabilityResponsePayload
            )
            FETCH_DOMAIN_SUPPORTED_STATES -> fetchSupportedStates(action.payload as String)
            FETCHED_DOMAIN_SUPPORTED_STATES -> handleFetchedSupportedStates(
                    action.payload as DomainSupportedStatesResponsePayload
            )
            FETCH_DOMAIN_SUPPORTED_COUNTRIES -> siteRestClient.fetchSupportedCountries()
            FETCHED_DOMAIN_SUPPORTED_COUNTRIES -> handleFetchedSupportedCountries(
                    action.payload as DomainSupportedCountriesResponsePayload
            )
            CHECK_AUTOMATED_TRANSFER_ELIGIBILITY -> checkAutomatedTransferEligibility(action.payload as SiteModel)
            INITIATE_AUTOMATED_TRANSFER -> initiateAutomatedTransfer(action.payload as InitiateAutomatedTransferPayload)
            CHECK_AUTOMATED_TRANSFER_STATUS -> checkAutomatedTransferStatus(action.payload as SiteModel)
            CHECKED_AUTOMATED_TRANSFER_ELIGIBILITY -> handleCheckedAutomatedTransferEligibility(
                    action.payload as AutomatedTransferEligibilityResponsePayload
            )
            INITIATED_AUTOMATED_TRANSFER -> handleInitiatedAutomatedTransfer(
                    action.payload as InitiateAutomatedTransferResponsePayload
            )
            CHECKED_AUTOMATED_TRANSFER_STATUS -> handleCheckedAutomatedTransferStatus(
                    action.payload as AutomatedTransferStatusResponsePayload
            )
            COMPLETE_QUICK_START -> completeQuickStart(action.payload as CompleteQuickStartPayload)
            COMPLETED_QUICK_START -> handleQuickStartCompleted(action.payload as QuickStartCompletedResponsePayload)
            DESIGNATE_PRIMARY_DOMAIN -> designatePrimaryDomain(action.payload as DesignatePrimaryDomainPayload)
            DESIGNATED_PRIMARY_DOMAIN -> handleDesignatedPrimaryDomain(action.payload as DesignatedPrimaryDomainPayload)
            FETCH_PRIVATE_ATOMIC_COOKIE -> fetchPrivateAtomicCookie(action.payload as FetchPrivateAtomicCookiePayload)
            FETCHED_PRIVATE_ATOMIC_COOKIE -> handleFetchedPrivateAtomicCookie(
                    action.payload as FetchedPrivateAtomicCookiePayload
            )
            FETCH_JETPACK_CAPABILITIES -> fetchJetpackCapabilities(action.payload as FetchJetpackCapabilitiesPayload)
            FETCHED_JETPACK_CAPABILITIES -> handleFetchedJetpackCapabilities(
                    action.payload as FetchedJetpackCapabilitiesPayload
            )
        }
    }

    private fun fetchProfileXmlRpc(site: SiteModel) {
        siteXMLRPCClient.fetchProfile(site)
    }

    suspend fun fetchSite(site: SiteModel): OnSiteChanged {
        return coroutineEngine.withDefaultContext(T.API, this, "Fetch site") {
            val updatedSite = if (site.isUsingWpComRestApi) {
                siteRestClient.fetchSite(site)
            } else {
                siteXMLRPCClient.fetchSite(site)
            }

            updateSite(updatedSite)
        }
    }

    suspend fun fetchSites(payload: FetchSitesPayload): OnSiteChanged {
        return coroutineEngine.withDefaultContext(T.API, this, "Fetch sites") {
            val result = siteRestClient.fetchSites(payload.filters, payload.filterJetpackConnectedPackageSite)
            handleFetchedSitesWPComRest(result)
        }
    }

    suspend fun fetchSitesXmlRpc(payload: RefreshSitesXMLRPCPayload): OnSiteChanged {
        return coroutineEngine.withDefaultContext(T.API, this, "Fetch sites") {
            updateSites(siteXMLRPCClient.fetchSites(payload.url, payload.username, payload.password))
        }
    }

    @Suppress("ForbiddenComment", "SwallowedException")
    private fun updateSiteProfile(siteModel: SiteModel) {
        val event = OnProfileFetched(siteModel)
        if (siteModel.isError) {
            // TODO: what kind of error could we get here?
            event.error = SiteErrorUtils.genericToSiteError(siteModel.error)
        } else {
            try {
                siteSqlUtils.insertOrUpdateSite(siteModel)
            } catch (e: DuplicateSiteException) {
                event.error = SiteError(DUPLICATE_SITE)
            }
        }
        emitChange(event)
    }

    @Suppress("ForbiddenComment", "SwallowedException")
    private fun updateSite(siteModel: SiteModel): OnSiteChanged {
        return if (siteModel.isError) {
            // TODO: what kind of error could we get here?
            OnSiteChanged(SiteErrorUtils.genericToSiteError(siteModel.error))
        } else {
            try {
                // The REST API doesn't return info about the editor(s). Make sure to copy current values
                // available on the DB. Otherwise the apps will receive an update site without editor prefs set.
                // The apps will dispatch the action to update editor(s) when necessary.
                val freshSiteFromDB = getSiteByLocalId(siteModel.id)
                if (freshSiteFromDB != null) {
                    siteModel.mobileEditor = freshSiteFromDB.mobileEditor
                    siteModel.webEditor = freshSiteFromDB.webEditor
                }
                OnSiteChanged(siteSqlUtils.insertOrUpdateSite(siteModel))
            } catch (e: DuplicateSiteException) {
                OnSiteChanged(SiteError(DUPLICATE_SITE))
            }
        }
    }

    @Suppress("ForbiddenComment")
    private fun updateSites(sitesModel: SitesModel): OnSiteChanged {
        val event = if (sitesModel.isError) {
            // TODO: what kind of error could we get here?
            OnSiteChanged(SiteErrorUtils.genericToSiteError(sitesModel.error))
        } else {
            val res = createOrUpdateSites(sitesModel)
            if (res.duplicateSiteFound) {
                OnSiteChanged(res.rowsAffected, SiteError(DUPLICATE_SITE))
            } else {
                OnSiteChanged(res.rowsAffected)
            }
        }
        return event
    }

    @Suppress("ForbiddenComment")
    private fun handleFetchedSitesWPComRest(fetchedSites: SitesModel): OnSiteChanged {
        return if (fetchedSites.isError) {
            // TODO: what kind of error could we get here?
            OnSiteChanged(SiteErrorUtils.genericToSiteError(fetchedSites.error))
        } else {
            val res = createOrUpdateSites(fetchedSites)
            val result = if (res.duplicateSiteFound) {
                OnSiteChanged(res.rowsAffected, SiteError(DUPLICATE_SITE))
            } else {
                OnSiteChanged(res.rowsAffected, res.updatedSites)
            }
            siteSqlUtils.removeWPComRestSitesAbsentFromList(postSqlUtils, fetchedSites.sites)
            result
        }
    }

    @Suppress("SwallowedException")
    private fun createOrUpdateSites(sites: SitesModel): UpdateSitesResult {
        var rowsAffected = 0
        var duplicateSiteFound = false
        val updatedSites = mutableListOf<SiteModel>()
        for (site in sites.sites) {
            try {
                // The REST API doesn't return info about the editor(s). Make sure to copy current values
                // available on the DB. Otherwise the apps will receive an update site without editor prefs set.
                // The apps will dispatch the action to update editor(s) when necessary.
                val siteFromDB = getSiteBySiteId(site.siteId)
                if (siteFromDB != null) {
                    site.mobileEditor = siteFromDB.mobileEditor
                    site.webEditor = siteFromDB.webEditor
                }
                val isUpdated = (siteSqlUtils.insertOrUpdateSite(site) == 1)
                if (isUpdated) {
                    rowsAffected++
                    updatedSites.add(site)
                }
            } catch (caughtException: DuplicateSiteException) {
                duplicateSiteFound = true
            }
        }
        return UpdateSitesResult(rowsAffected, updatedSites, duplicateSiteFound)
    }

    private fun deleteSite(site: SiteModel) {
        // Not available for Jetpack sites
        if (!site.isWPCom) {
            val event = OnSiteDeleted(DeleteSiteError(INVALID_SITE))
            emitChange(event)
            return
        }
        siteRestClient.deleteSite(site)
    }

    private fun handleDeletedSite(payload: DeleteSiteResponsePayload) {
        val event = OnSiteDeleted(payload.error)
        if (!payload.isError) {
            siteSqlUtils.deleteSite(payload.site)
        }
        emitChange(event)
    }

    private fun exportSite(site: SiteModel) {
        // Not available for Jetpack sites
        if (!site.isWPCom) {
            emitChange(OnSiteExported(ExportSiteError(ExportSiteErrorType.INVALID_SITE)))
            return
        }
        siteRestClient.exportSite(site)
    }

    @Suppress("ForbiddenComment")
    private fun handleExportedSite(payload: ExportSiteResponsePayload) {
        val event = if (payload.isError) {
            // TODO: what kind of error could we get here?
            OnSiteExported(ExportSiteError(GENERIC_ERROR))
        } else {
            OnSiteExported()
        }
        emitChange(event)
    }

    private fun removeSite(site: SiteModel) {
        val rowsAffected = siteSqlUtils.deleteSite(site)
        emitChange(OnSiteRemoved(rowsAffected))
    }

    private fun removeAllSites() {
        val rowsAffected = siteSqlUtils.deleteAllSites()
        val event = OnAllSitesRemoved(rowsAffected)
        emitChange(event)
    }

    private fun removeWPComAndJetpackSites() {
        // Logging out of WP.com. Drop all WP.com sites, and all Jetpack sites that were fetched over the WP.com
        // REST API only (they don't have a .org site id)
        val wpcomAndJetpackSites = siteSqlUtils.sitesAccessedViaWPComRest.asModel
        val rowsAffected = removeSites(wpcomAndJetpackSites)
        emitChange(OnSiteRemoved(rowsAffected))
    }

    private fun toggleSitesVisibility(sites: SitesModel, visible: Boolean): Int {
        var rowsAffected = 0
        for (site in sites.sites) {
            rowsAffected += siteSqlUtils.setSiteVisibility(site, visible)
        }
        return rowsAffected
    }

    @VisibleForTesting
    suspend fun createNewSite(payload: NewSitePayload): OnNewSiteCreated {
        val result = siteRestClient.newSite(
                payload.siteName,
                payload.siteTitle,
                payload.language,
                payload.timeZoneId,
                payload.visibility,
                payload.segmentId,
                payload.siteDesign,
                payload.dryRun
        )
        return handleCreateNewSiteCompleted(
                payload = result
        )
    }

    private fun handleCreateNewSiteCompleted(payload: NewSiteResponsePayload): OnNewSiteCreated {
        return OnNewSiteCreated(payload.dryRun, payload.siteUrl, payload.newSiteRemoteId, payload.error)
    }

    suspend fun fetchPostFormats(site: SiteModel): OnPostFormatsChanged {
        val payload = if (site.isUsingWpComRestApi) {
            siteRestClient.fetchPostFormats(site)
        } else {
            siteXMLRPCClient.fetchPostFormats(site)
        }
        val event = OnPostFormatsChanged(payload.site)
        if (payload.isError) {
            event.error = payload.error
        } else {
            siteSqlUtils.insertOrReplacePostFormats(payload.site, payload.postFormats)
        }
        return event
    }

    private fun fetchSiteEditors(site: SiteModel) {
        if (site.isUsingWpComRestApi) {
            siteRestClient.fetchSiteEditors(site)
        }
    }

    private fun fetchBlockLayouts(payload: FetchBlockLayoutsPayload) {
        if (payload.preferCache == true && cachedLayoutsRetrieved(payload.site)) return
        if (payload.site.isUsingWpComRestApi) {
            siteRestClient
                    .fetchWpComBlockLayouts(
                            payload.site, payload.supportedBlocks,
                            payload.previewWidth, payload.previewHeight, payload.scale, payload.isBeta
                    )
        } else {
            siteRestClient.fetchSelfHostedBlockLayouts(
                    payload.site, payload.supportedBlocks,
                    payload.previewWidth, payload.previewHeight, payload.scale, payload.isBeta
            )
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun designateMobileEditor(payload: DesignateMobileEditorPayload) {
        // wpcom sites sync the new value with the backend
        if (payload.site.isUsingWpComRestApi) {
            siteRestClient.designateMobileEditor(payload.site, payload.editor)
        }

        // Update the editor pref on the DB, and emit the change immediately
        val site = payload.site
        site.mobileEditor = payload.editor
        val event = try {
            OnSiteEditorsChanged(site, siteSqlUtils.insertOrUpdateSite(site))
        } catch (e: Exception) {
            OnSiteEditorsChanged(site, SiteEditorsError(SiteEditorsErrorType.GENERIC_ERROR))
        }
        emitChange(event)
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun designateMobileEditorForAllSites(payload: DesignateMobileEditorForAllSitesPayload) {
        var rowsAffected = 0
        var wpcomPostRequestRequired = false
        var error: SiteEditorsError? = null
        for (site in sites) {
            site.mobileEditor = payload.editor
            if (!wpcomPostRequestRequired && site.isUsingWpComRestApi) {
                wpcomPostRequestRequired = true
            }
            try {
                rowsAffected += siteSqlUtils.insertOrUpdateSite(site)
            } catch (e: Exception) {
                error = SiteEditorsError(SiteEditorsErrorType.GENERIC_ERROR)
            }
        }
        val isNetworkResponse = if (wpcomPostRequestRequired) {
            siteRestClient.designateMobileEditorForAllSites(payload.editor, payload.setOnlyIfEmpty)
            false
        } else {
            true
        }

        emitChange(OnAllSitesMobileEditorChanged(rowsAffected, isNetworkResponse, error))
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun updateSiteEditors(payload: FetchedEditorsPayload) {
        val site = payload.site
        val event = if (payload.isError) {
            OnSiteEditorsChanged(site, payload.error ?: SiteEditorsError(SiteEditorsErrorType.GENERIC_ERROR))
        } else {
            site.mobileEditor = payload.mobileEditor
            site.webEditor = payload.webEditor
            try {
                OnSiteEditorsChanged(site, siteSqlUtils.insertOrUpdateSite(site))
            } catch (e: Exception) {
                OnSiteEditorsChanged(site, SiteEditorsError(SiteEditorsErrorType.GENERIC_ERROR))
            }
        }
        emitChange(event)
    }

    private fun handleDesignatedMobileEditorForAllSites(payload: DesignateMobileEditorForAllSitesResponsePayload) {
        val event = if (payload.isError) {
            OnAllSitesMobileEditorChanged(siteEditorsError = payload.error)
        } else {
            onAllSitesMobileEditorChanged(payload)
        }
        emitChange(event)
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun onAllSitesMobileEditorChanged(
        payload: DesignateMobileEditorForAllSitesResponsePayload
    ): OnAllSitesMobileEditorChanged {
        var rowsAffected = 0
        var error: SiteEditorsError? = null
        // Loop over the returned sites and make sure we've the fresh values for editor prop stored locally
        for ((key, value) in payload.editors ?: mapOf()) {
            val currentModel = getSiteBySiteId(key.toLong())
            if (currentModel == null) {
                // this could happen when a site was added to the current account with another app, or on the web
                AppLog.e(
                    T.API,
                    "handleDesignatedMobileEditorForAllSites - The backend returned info for the " +
                        "following siteID $key but there is no site with that remote ID in SiteStore."
                )
                continue
            }
            if (currentModel.mobileEditor == null || currentModel.mobileEditor != value) {
                // the current editor is either null or != from the value on the server. Update it
                currentModel.mobileEditor = value
                try {
                    rowsAffected += siteSqlUtils.insertOrUpdateSite(currentModel)
                } catch (e: Exception) {
                    error = SiteEditorsError(SiteEditorsErrorType.GENERIC_ERROR)
                }
            }
        }
        return OnAllSitesMobileEditorChanged(rowsAffected, true, error)
    }

    private fun fetchUserRoles(site: SiteModel) {
        if (site.isUsingWpComRestApi) {
            siteRestClient.fetchUserRoles(site)
        }
    }

    private fun updateUserRoles(payload: FetchedUserRolesPayload) {
        val event = OnUserRolesChanged(payload.site)
        if (payload.isError) {
            event.error = payload.error
        } else {
            siteSqlUtils.insertOrReplaceUserRoles(payload.site, payload.roles)
        }
        emitChange(event)
    }

    private fun removeSites(sites: List<SiteModel>): Int {
        var rowsAffected = 0
        for (site in sites) {
            rowsAffected += siteSqlUtils.deleteSite(site)
        }
        return rowsAffected
    }

    private fun fetchConnectSiteInfo(payload: String) {
        siteRestClient.fetchConnectSiteInfo(payload)
    }

    private fun handleFetchedConnectSiteInfo(payload: ConnectSiteInfoPayload) {
        val event = OnConnectSiteInfoChecked(payload)
        event.error = payload.error
        emitChange(event)
    }

    private fun fetchWPComSiteByUrl(payload: String) {
        siteRestClient.fetchWPComSiteByUrl(payload)
    }

    private fun handleFetchedWPComSiteByUrl(payload: FetchWPComSiteResponsePayload) {
        val event = OnWPComSiteFetched(payload.checkedUrl, payload.site)
        event.error = payload.error
        emitChange(event)
    }

    private fun checkUrlIsWPCom(payload: String) {
        siteRestClient.checkUrlIsWPCom(payload)
    }

    private fun handleCheckedIsWPComUrl(payload: IsWPComResponsePayload) {
        val error = if (payload.isError) {
            // Return invalid site for all errors (this endpoint seems a bit drunk).
            // Client likely needs to know if there was an error or not.
            SiteError(SiteErrorType.INVALID_SITE)
        } else {
            null
        }
        emitChange(OnURLChecked(payload.url ?: "", payload.isWPCom, error))
    }

    private fun suggestDomains(payload: SuggestDomainsPayload) {
        siteRestClient.suggestDomains(
                payload.query, payload.onlyWordpressCom, payload.includeWordpressCom,
                payload.includeDotBlogSubdomain, payload.segmentId, payload.quantity, payload.includeVendorDot,
                payload.tlds
        )
    }

    private fun handleSuggestedDomains(payload: SuggestDomainsResponsePayload) {
        val event = OnSuggestedDomains(payload.query, payload.suggestions)
        if (payload.isError) {
            event.error = payload.error
        }
        emitChange(event)
    }

    private fun fetchPrivateAtomicCookie(payload: FetchPrivateAtomicCookiePayload) {
        val site = getSiteBySiteId(payload.siteId)
        if (site == null) {
            val cookieError = PrivateAtomicCookieError(
                    SITE_MISSING_FROM_STORE,
                    "Requested site is missing from the store."
            )
            emitChange(OnPrivateAtomicCookieFetched(null, false, cookieError))
            return
        }
        if (!site.isPrivateWPComAtomic) {
            val cookieError = PrivateAtomicCookieError(
                    NON_PRIVATE_AT_SITE,
                    "Cookie can only be requested for private atomic site."
            )
            emitChange(OnPrivateAtomicCookieFetched(site, false, cookieError))
            return
        }
        siteRestClient.fetchAccessCookie(site)
    }

    private fun handleFetchedPrivateAtomicCookie(payload: FetchedPrivateAtomicCookiePayload) {
        if (payload.cookie == null || payload.cookie.cookies.isEmpty()) {
            emitChange(
                    OnPrivateAtomicCookieFetched(
                            payload.site, false,
                            PrivateAtomicCookieError(
                                    INVALID_RESPONSE,
                                    "Cookie is missing from response."
                            )
                    )
            )
            privateAtomicCookie.set(null)
            return
        }
        privateAtomicCookie.set(payload.cookie.cookies[0])
        emitChange(OnPrivateAtomicCookieFetched(payload.site, true, payload.error))
    }

    private fun fetchJetpackCapabilities(payload: FetchJetpackCapabilitiesPayload) {
        siteRestClient.fetchJetpackCapabilities(payload.remoteSiteId)
    }

    private fun handleFetchedJetpackCapabilities(payload: FetchedJetpackCapabilitiesPayload) {
        emitChange(OnJetpackCapabilitiesFetched(payload.remoteSiteId, payload.capabilities, payload.error))
    }

    private fun fetchPlans(siteModel: SiteModel) {
        if (siteModel.isUsingWpComRestApi) {
            siteRestClient.fetchPlans(siteModel)
        } else {
            val plansError = PlansError(NOT_AVAILABLE)
            handleFetchedPlans(FetchedPlansPayload(siteModel, plansError))
        }
    }

    private fun handleFetchedPlans(payload: FetchedPlansPayload) {
        emitChange(OnPlansFetched(payload.site, payload.plans, payload.error))
    }

    private fun checkDomainAvailability(domainName: String) {
        if (TextUtils.isEmpty(domainName)) {
            val error = DomainAvailabilityError(INVALID_DOMAIN_NAME)
            handleCheckedDomainAvailability(DomainAvailabilityResponsePayload(error))
        } else {
            siteRestClient.checkDomainAvailability(domainName)
        }
    }

    private fun handleCheckedDomainAvailability(payload: DomainAvailabilityResponsePayload) {
        emitChange(
                OnDomainAvailabilityChecked(
                        payload.status,
                        payload.mappable,
                        payload.supportsPrivacy,
                        payload.error
                )
        )
    }

    private fun fetchSupportedStates(countryCode: String) {
        if (TextUtils.isEmpty(countryCode)) {
            val error = DomainSupportedStatesError(INVALID_COUNTRY_CODE)
            handleFetchedSupportedStates(DomainSupportedStatesResponsePayload(error))
        } else {
            siteRestClient.fetchSupportedStates(countryCode)
        }
    }

    private fun handleFetchedSupportedStates(payload: DomainSupportedStatesResponsePayload) {
        emitChange(OnDomainSupportedStatesFetched(payload.supportedStates, payload.error))
    }

    private fun handleFetchedSupportedCountries(payload: DomainSupportedCountriesResponsePayload) {
        emitChange(OnDomainSupportedCountriesFetched(payload.supportedCountries, payload.error))
    }

    private fun handleFetchedBlockLayouts(payload: FetchedBlockLayoutsResponsePayload) {
        if (payload.isError) {
            // Return cached layouts on error
            if (!cachedLayoutsRetrieved(payload.site)) {
                emitChange(OnBlockLayoutsFetched(payload.layouts, payload.categories, payload.error))
            }
        } else {
            siteSqlUtils.insertOrReplaceBlockLayouts(payload.site, payload.categories!!, payload.layouts!!)
            emitChange(OnBlockLayoutsFetched(payload.layouts, payload.categories, payload.error))
        }
    }

    /**
     * Emits a new [OnBlockLayoutsFetched] event with cached layouts for a given site
     *
     * @param site the site for which the cached layouts should be retrieved
     * @return true if cached layouts were retrieved successfully
     */
    private fun cachedLayoutsRetrieved(site: SiteModel): Boolean {
        val layouts = siteSqlUtils.getBlockLayouts(site)
        val categories = siteSqlUtils.getBlockLayoutCategories(site)
        if (layouts.isNotEmpty() && categories.isNotEmpty()) {
            emitChange(OnBlockLayoutsFetched(layouts, categories, null))
            return true
        }
        return false
    }

    // Automated Transfers
    private fun checkAutomatedTransferEligibility(site: SiteModel) {
        siteRestClient.checkAutomatedTransferEligibility(site)
    }

    private fun handleCheckedAutomatedTransferEligibility(payload: AutomatedTransferEligibilityResponsePayload) {
        emitChange(
                OnAutomatedTransferEligibilityChecked(
                        payload.site, payload.isEligible, payload.errorCodes,
                        payload.error
                )
        )
    }

    private fun initiateAutomatedTransfer(payload: InitiateAutomatedTransferPayload) {
        siteRestClient.initiateAutomatedTransfer(payload.site, payload.pluginSlugToInstall)
    }

    private fun handleInitiatedAutomatedTransfer(payload: InitiateAutomatedTransferResponsePayload) {
        emitChange(OnAutomatedTransferInitiated(payload.site, payload.pluginSlugToInstall, payload.error))
    }

    private fun checkAutomatedTransferStatus(site: SiteModel) {
        siteRestClient.checkAutomatedTransferStatus(site)
    }

    private fun handleCheckedAutomatedTransferStatus(payload: AutomatedTransferStatusResponsePayload) {
        val event: OnAutomatedTransferStatusChecked = if (!payload.isError) {
            // We can't rely on the currentStep and totalSteps as it may not be equal when the transfer is complete
            val isTransferCompleted = payload.status.equals("complete", ignoreCase = true)
            OnAutomatedTransferStatusChecked(
                    payload.site, isTransferCompleted, payload.currentStep,
                    payload.totalSteps
            )
        } else {
            OnAutomatedTransferStatusChecked(payload.site, payload.error)
        }
        emitChange(event)
    }

    private fun completeQuickStart(payload: CompleteQuickStartPayload) {
        siteRestClient.completeQuickStart(payload.site, payload.variant)
    }

    private fun handleQuickStartCompleted(payload: QuickStartCompletedResponsePayload) {
        val event = OnQuickStartCompleted(payload.site, payload.success)
        event.error = payload.error
        emitChange(event)
    }

    private fun designatePrimaryDomain(payload: DesignatePrimaryDomainPayload) {
        siteRestClient.designatePrimaryDomain(payload.site, payload.domain)
    }

    private fun handleDesignatedPrimaryDomain(payload: DesignatedPrimaryDomainPayload) {
        val event = OnPrimaryDomainDesignated(payload.site, payload.success)
        event.error = payload.error
        emitChange(event)
    }

    suspend fun fetchSiteDomains(siteModel: SiteModel): FetchedDomainsPayload =
            coroutineEngine.withDefaultContext(T.API, this, "Fetch site domains") {
                return@withDefaultContext when (val response =
                        siteRestClient.fetchSiteDomains(siteModel)) {
                            is Success -> {
                                FetchedDomainsPayload(siteModel, response.data.domains)
                            }
                            is Error -> {
                                val siteErrorType = when (response.error.apiError) {
                                    "unauthorized" -> UNAUTHORIZED
                                    "unknown_blog" -> UNKNOWN_SITE
                                    else -> SiteErrorType.GENERIC_ERROR
                                }
                                val domainsError = SiteError(siteErrorType, response.error.message)
                                FetchedDomainsPayload(siteModel, domainsError)
                            }
                        }
            }

    suspend fun deleteApplicationPassword(site: SiteModel): OnApplicationPasswordDeleted =
        coroutineEngine.withDefaultContext(T.API, this, "Delete Application Password") {
            when (val result = applicationPasswordsManagerProvider.get().deleteApplicationCredentials(site)) {
                is ApplicationPasswordDeletionResult.Success -> OnApplicationPasswordDeleted(site)
                is ApplicationPasswordDeletionResult.Failure -> OnApplicationPasswordDeleted(site, result.error)
            }
        }

    suspend fun fetchSitePlans(siteModel: SiteModel): FetchedPlansPayload {
        return if (siteModel.isUsingWpComRestApi) {
            coroutineEngine.withDefaultContext(T.API, this, "Fetch site plans") {
                return@withDefaultContext when (val response =
                    siteRestClient.fetchSitePlans(siteModel)) {
                    is Success -> {
                        FetchedPlansPayload(siteModel, response.data.plansList)
                    }
                    is Error -> {
                        val siteErrorType = when (response.error.apiError) {
                            "unauthorized" -> PlansErrorType.UNAUTHORIZED
                            "unknown_blog" -> PlansErrorType.UNKNOWN_BLOG
                            else -> PlansErrorType.GENERIC_ERROR
                        }
                        val plansError = PlansError(siteErrorType, response.error.message)
                        FetchedPlansPayload(siteModel, plansError)
                    }
                }
            }
        } else {
            FetchedPlansPayload(siteModel, PlansError(NOT_AVAILABLE))
        }
    }

    suspend fun fetchDomainPrice(domainName: String): WPAPIResponse<DomainPriceResponse> {
        return coroutineEngine.withDefaultContext(T.API, this, "Fetch domain price") {
            when (val response =
                siteRestClient.fetchDomainPrice(domainName)) {
                is Success -> {
                    WPAPIResponse.Success(response.data)
                }
                is Error -> {
                    WPAPIResponse.Error(WPAPINetworkError(response.error))
                }
            }
        }
    }
}
