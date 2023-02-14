package org.wordpress.android.fluxc.network.rest.wpcom.site

import android.content.Context
import android.text.TextUtils
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.google.gson.reflect.TypeToken
import org.apache.commons.text.StringEscapeUtils
import org.json.JSONException
import org.json.JSONObject
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.JetpackCapability
import org.wordpress.android.fluxc.model.RoleModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.SitesModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteWPComRestResponse.SitesResponse
import org.wordpress.android.fluxc.network.rest.wpcom.site.UserRoleWPComRestResponse.UserRolesResponse
import org.wordpress.android.fluxc.store.SiteStore.AccessCookieErrorType
import org.wordpress.android.fluxc.store.SiteStore.AutomatedTransferEligibilityResponsePayload
import org.wordpress.android.fluxc.store.SiteStore.AutomatedTransferError
import org.wordpress.android.fluxc.store.SiteStore.AutomatedTransferStatusResponsePayload
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload
import org.wordpress.android.fluxc.store.SiteStore.DeleteSiteError
import org.wordpress.android.fluxc.store.SiteStore.DesignateMobileEditorForAllSitesResponsePayload
import org.wordpress.android.fluxc.store.SiteStore.DesignatePrimaryDomainError
import org.wordpress.android.fluxc.store.SiteStore.DesignatePrimaryDomainErrorType
import org.wordpress.android.fluxc.store.SiteStore.DesignatedPrimaryDomainPayload
import org.wordpress.android.fluxc.store.SiteStore.DomainAvailabilityError
import org.wordpress.android.fluxc.store.SiteStore.DomainAvailabilityErrorType
import org.wordpress.android.fluxc.store.SiteStore.DomainAvailabilityResponsePayload
import org.wordpress.android.fluxc.store.SiteStore.DomainAvailabilityStatus
import org.wordpress.android.fluxc.store.SiteStore.DomainMappabilityStatus
import org.wordpress.android.fluxc.store.SiteStore.DomainSupportedCountriesError
import org.wordpress.android.fluxc.store.SiteStore.DomainSupportedCountriesErrorType
import org.wordpress.android.fluxc.store.SiteStore.DomainSupportedCountriesResponsePayload
import org.wordpress.android.fluxc.store.SiteStore.DomainSupportedStatesError
import org.wordpress.android.fluxc.store.SiteStore.DomainSupportedStatesErrorType
import org.wordpress.android.fluxc.store.SiteStore.DomainSupportedStatesResponsePayload
import org.wordpress.android.fluxc.store.SiteStore.FetchedBlockLayoutsResponsePayload
import org.wordpress.android.fluxc.store.SiteStore.FetchedEditorsPayload
import org.wordpress.android.fluxc.store.SiteStore.FetchedJetpackCapabilitiesPayload
import org.wordpress.android.fluxc.store.SiteStore.FetchedPlansPayload
import org.wordpress.android.fluxc.store.SiteStore.FetchedPostFormatsPayload
import org.wordpress.android.fluxc.store.SiteStore.FetchedPrivateAtomicCookiePayload
import org.wordpress.android.fluxc.store.SiteStore.FetchedUserRolesPayload
import org.wordpress.android.fluxc.store.SiteStore.InitiateAutomatedTransferResponsePayload
import org.wordpress.android.fluxc.store.SiteStore.JetpackCapabilitiesError
import org.wordpress.android.fluxc.store.SiteStore.JetpackCapabilitiesErrorType
import org.wordpress.android.fluxc.store.SiteStore.NewSiteError
import org.wordpress.android.fluxc.store.SiteStore.NewSiteErrorType
import org.wordpress.android.fluxc.store.SiteStore.PlansError
import org.wordpress.android.fluxc.store.SiteStore.PostFormatsError
import org.wordpress.android.fluxc.store.SiteStore.PostFormatsErrorType
import org.wordpress.android.fluxc.store.SiteStore.PrivateAtomicCookieError
import org.wordpress.android.fluxc.store.SiteStore.QuickStartCompletedResponsePayload
import org.wordpress.android.fluxc.store.SiteStore.QuickStartError
import org.wordpress.android.fluxc.store.SiteStore.QuickStartErrorType
import org.wordpress.android.fluxc.store.SiteStore.SiteEditorsError
import org.wordpress.android.fluxc.store.SiteStore.SiteEditorsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.INVALID_SITE
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.UNAUTHORIZED
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.UNKNOWN_SITE
import org.wordpress.android.fluxc.store.SiteStore.SiteFilter
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainError
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainErrorType.EMPTY_RESULTS
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsResponsePayload
import org.wordpress.android.fluxc.store.SiteStore.UserRolesError
import org.wordpress.android.fluxc.store.SiteStore.UserRolesErrorType
import org.wordpress.android.fluxc.utils.SiteUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.API
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.UrlUtils
import java.io.UnsupportedEncodingException
import java.net.URI
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.math.max

@Suppress("LargeClass")
@Singleton
class SiteRestClient @Inject constructor(
    appContext: Context?,
    dispatcher: Dispatcher?,
    @Named("regular") requestQueue: RequestQueue?,
    private val appSecrets: AppSecrets,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    accessToken: AccessToken?,
    userAgent: UserAgent?
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    data class NewSiteResponsePayload(
        val newSiteRemoteId: Long = 0,
        val siteUrl: String? = null,
        val dryRun: Boolean = false
    ) : Payload<NewSiteError>()

    data class DeleteSiteResponsePayload(val site: SiteModel? = null) : Payload<DeleteSiteError>()

    class ExportSiteResponsePayload : Payload<BaseNetworkError>()
    data class IsWPComResponsePayload(
        val url: String,
        val isWPCom: Boolean = false
    ) : Payload<BaseNetworkError>()

    data class FetchWPComSiteResponsePayload(
        val checkedUrl: String,
        val site: SiteModel? = null
    ) : Payload<SiteError>()

    suspend fun fetchSites(filters: List<SiteFilter?>, filterJetpackConnectedPackageSite: Boolean): SitesModel {
        val params = getFetchSitesParams(filters)
        val url = WPCOMREST.me.sites.urlV1_2
        val response = wpComGsonRequestBuilder.syncGetRequest(this, url, params, SitesResponse::class.java)
        return when (response) {
            is Success -> {
                val siteArray = mutableListOf<SiteModel>()
                for (siteResponse in response.data.sites) {
                    val siteModel = siteResponseToSiteModel(siteResponse)
                    // see https://github.com/wordpress-mobile/WordPress-Android/issues/15540#issuecomment-993752880
                    if (filterJetpackConnectedPackageSite && siteModel.isJetpackCPConnected) continue
                    siteArray.add(siteModel)
                }
                SitesModel(siteArray)
            }
            is Error -> {
                val payload = SitesModel(emptyList())
                payload.error = response.error
                payload
            }
        }
    }

    private fun getFetchSitesParams(filters: List<SiteFilter?>): Map<String, String> {
        val params = mutableMapOf<String, String>()
        if (filters.isNotEmpty()) params[FILTERS] = TextUtils.join(",", filters)
        params[FIELDS] = SITE_FIELDS
        return params
    }

    suspend fun fetchSite(site: SiteModel): SiteModel {
        val params = mutableMapOf<String, String>()
        params[FIELDS] = SITE_FIELDS
        val url = WPCOMREST.sites.urlV1_1 + site.siteId
        val response = wpComGsonRequestBuilder.syncGetRequest(this, url, params, SiteWPComRestResponse::class.java)
        return when (response) {
            is Success -> {
                val newSite = siteResponseToSiteModel(response.data)
                // local ID is not copied into the new model, let's make sure it is
                // otherwise the call that updates the DB can add a new row?
                if (site.id > 0) {
                    newSite.id = site.id
                }
                newSite
            }
            is Error -> {
                val payload = SiteModel()
                payload.error = response.error
                payload
            }
        }
    }

    /**
     * Calls the API at https://public-api.wordpress.com/rest/v1.1/sites/new/ to create a new site
     * @param siteName The domain of the site
     * @param siteTitle The title of the site
     * @param language The language of the site
     * @param timeZoneId The timezone of the site
     * @param visibility The visibility of the site (public or private)
     * @param segmentId The segment that the site belongs to
     * @param siteDesign The design template of the site
     * @param dryRun If set to true the call only validates the parameters passed
     *
     * The domain of the site is generated with the following logic:
     *
     * 1. If the [siteName] is provided it is used as a domain
     * 2. If the [siteName] is not provided the [siteTitle] is passed and the API generates the domain from it
     * 3. If neither the [siteName] or the [siteTitle] is passed the api generates a domain of the form siteXXXXXX
     *
     * In the cases 2 and 3 two extra parameters are passed:
     * - `options.site_creation_flow` with value `with-design-picker`
     * - `find_available_url` with value `1`
     *
     * @return the response of the API call  as [NewSiteResponsePayload]
     */
    @Suppress("ComplexMethod", "LongParameterList")
    suspend fun newSite(
        siteName: String?,
        siteTitle: String?,
        language: String,
        timeZoneId: String?,
        visibility: SiteVisibility,
        segmentId: Long?,
        siteDesign: String?,
        dryRun: Boolean
    ): NewSiteResponsePayload {
        val url = WPCOMREST.sites.new_.urlV1_1
        val body = mutableMapOf<String, Any>()
        val options = mutableMapOf<String, Any>()

        body["lang_id"] = language
        body["public"] = visibility.value().toString()
        body["validate"] = if (dryRun) "1" else "0"
        body["client_id"] = appSecrets.appId
        body["client_secret"] = appSecrets.appSecret

        if (siteTitle != null) {
            body["blog_title"] = siteTitle
        }
        body["blog_name"] = siteName ?: siteTitle ?: ""
        siteName ?: run {
            body["find_available_url"] = "1"
            options["site_creation_flow"] = "with-design-picker"
        }

        if (segmentId != null) {
            options["site_segment"] = segmentId
        }
        if (siteDesign != null) {
            options["template"] = siteDesign
        }
        if (timeZoneId != null) {
            options["timezone_string"] = timeZoneId
        }

        // Add site options if available
        if (options.isNotEmpty()) {
            body["options"] = options
        }

        // Disable retries and increase timeout for site creation (it can sometimes take a long time to complete)
        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                null,
                body,
                NewSiteResponse::class.java,
                DefaultRetryPolicy(NEW_SITE_TIMEOUT_MS, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        )
        return when (response) {
            is Success -> {
                var siteId: Long = 0
                if (response.data.blog_details != null) {
                    try {
                        siteId = response.data.blog_details.blogid.toLong()
                    } catch (e: NumberFormatException) {
                        // No op: In dry run mode, returned newSiteRemoteId is "Array"
                    }
                }
                NewSiteResponsePayload(siteId, response.data.blog_details?.url, dryRun)
            }
            is Error -> {
                volleyErrorToAccountResponsePayload(response.error.volleyError, dryRun)
            }
        }
    }

    fun fetchSiteEditors(site: SiteModel) {
        val params = mutableMapOf<String, String>()
        val url = WPCOMV2.sites.site(site.siteId).gutenberg.url
        val request = WPComGsonRequest.buildGetRequest(url, params,
                SiteEditorsResponse::class.java,
                { response ->
                    if (response != null) {
                        val payload = FetchedEditorsPayload(site, response.editor_web, response.editor_mobile)
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedSiteEditorsAction(payload))
                    } else {
                        AppLog.e(API, "Received empty response to /sites/\$site/gutenberg for " + site.url)
                        val payload = FetchedEditorsPayload(site, "", "")
                        payload.error = SiteEditorsError(GENERIC_ERROR)
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedSiteEditorsAction(payload))
                    }
                }
        ) {
            val payload = FetchedEditorsPayload(site, "", "")
            payload.error = SiteEditorsError(GENERIC_ERROR)
            mDispatcher.dispatch(SiteActionBuilder.newFetchedSiteEditorsAction(payload))
        }
        add(request)
    }

    fun designateMobileEditor(site: SiteModel, mobileEditorName: String) {
        val params = mutableMapOf<String, Any>()
        val url = WPCOMV2.sites.site(site.siteId).gutenberg.url
        params["editor"] = mobileEditorName
        params["platform"] = "mobile"
        val request = WPComGsonRequest
                .buildPostRequest(url, params, SiteEditorsResponse::class.java,
                        { response ->
                            val payload = FetchedEditorsPayload(site, response.editor_web, response.editor_mobile)
                            mDispatcher.dispatch(SiteActionBuilder.newFetchedSiteEditorsAction(payload))
                        }
                ) {
                    val payload = FetchedEditorsPayload(site, "", "")
                    payload.error = SiteEditorsError(GENERIC_ERROR)
                    mDispatcher.dispatch(SiteActionBuilder.newFetchedSiteEditorsAction(payload))
                }
        add(request)
    }

    fun designateMobileEditorForAllSites(mobileEditorName: String, setOnlyIfEmpty: Boolean) {
        val params = mutableMapOf<String, Any>()
        val url = WPCOMV2.me.gutenberg.url
        params["editor"] = mobileEditorName
        params["platform"] = "mobile"
        if (setOnlyIfEmpty) {
            params["set_only_if_empty"] = "true"
        }
        // Else, omit the "set_only_if_empty" parameters.
        // There is an issue in the API implementation. It only checks
        // for "set_only_if_empty" presence but don't check for its value.
        add(
                WPComGsonRequest
                        .buildPostRequest<Map<String, String>>(url, params, MutableMap::class.java,
                                { response ->
                                    val payload = DesignateMobileEditorForAllSitesResponsePayload(response)
                                    mDispatcher.dispatch(
                                            SiteActionBuilder.newDesignatedMobileEditorForAllSitesAction(payload)
                                    )
                                },
                                {
                                    val payload = DesignateMobileEditorForAllSitesResponsePayload(null)
                                    payload.error = SiteEditorsError(GENERIC_ERROR)
                                    mDispatcher.dispatch(
                                            SiteActionBuilder.newDesignatedMobileEditorForAllSitesAction(payload)
                                    )
                                })
        )
    }

    suspend fun fetchPostFormats(site: SiteModel): FetchedPostFormatsPayload {
        val url = WPCOMREST.sites.site(site.siteId).post_formats.urlV1_1
        val response = wpComGsonRequestBuilder.syncGetRequest(this, url, mapOf(), PostFormatsResponse::class.java)
        return when (response) {
            is Success -> {
                val postFormats = SiteUtils.getValidPostFormatsOrNull(response.data.formats)
                if (postFormats != null) {
                    FetchedPostFormatsPayload(
                            site,
                            postFormats
                    )
                } else {
                    val payload = FetchedPostFormatsPayload(site, emptyList())
                    payload.error = PostFormatsError(PostFormatsErrorType.INVALID_RESPONSE)
                    payload
                }
            }
            is Error -> {
                val payload = FetchedPostFormatsPayload(site, emptyList())
                payload.error = PostFormatsError(PostFormatsErrorType.GENERIC_ERROR)
                payload
            }
        }
    }

    @Suppress("ForbiddenComment")
    fun fetchUserRoles(site: SiteModel) {
        val url = WPCOMREST.sites.site(site.siteId).roles.urlV1_1
        val request = WPComGsonRequest.buildGetRequest(url, null,
                UserRolesResponse::class.java,
                { response ->
                    val roleArray = mutableListOf<RoleModel>()
                    for (roleResponse in response.roles) {
                        val roleModel = RoleModel()
                        roleModel.name = roleResponse.name
                        roleModel.displayName = StringEscapeUtils.unescapeHtml4(roleResponse.display_name)
                        roleArray.add(roleModel)
                    }
                    mDispatcher.dispatch(
                            SiteActionBuilder.newFetchedUserRolesAction(
                                    FetchedUserRolesPayload(
                                            site,
                                            roleArray
                                    )
                            )
                    )
                }
        ) {
            val payload = FetchedUserRolesPayload(site, emptyList())
            // TODO: what other kind of error could we get here?
            payload.error = UserRolesError(UserRolesErrorType.GENERIC_ERROR)
            mDispatcher.dispatch(SiteActionBuilder.newFetchedUserRolesAction(payload))
        }
        add(request)
    }

    fun fetchPlans(site: SiteModel) {
        val url = WPCOMREST.sites.site(site.siteId).plans.urlV1_3
        val request = WPComGsonRequest.buildGetRequest(url, null, PlansResponse::class.java,
                { response ->
                    val plans = response.plansList
                    mDispatcher.dispatch(
                            SiteActionBuilder.newFetchedPlansAction(FetchedPlansPayload(site, plans))
                    )
                }
        ) { error ->
            val plansError = PlansError(error.apiError, error.message)
            val payload = FetchedPlansPayload(site, plansError)
            mDispatcher.dispatch(SiteActionBuilder.newFetchedPlansAction(payload))
        }
        add(request)
    }

    fun deleteSite(site: SiteModel) {
        val url = WPCOMREST.sites.site(site.siteId).delete.urlV1_1
        val request = WPComGsonRequest.buildPostRequest(url, null,
                SiteWPComRestResponse::class.java,
                {
                    val payload = DeleteSiteResponsePayload(site)
                    mDispatcher.dispatch(SiteActionBuilder.newDeletedSiteAction(payload))
                }
        ) { error ->
            val payload = DeleteSiteResponsePayload(site)
            payload.error = DeleteSiteError(error.apiError, error.message)
            mDispatcher.dispatch(SiteActionBuilder.newDeletedSiteAction(payload))
        }
        add(request)
    }

    fun exportSite(site: SiteModel) {
        val url = WPCOMREST.sites.site(site.siteId).exports.start.urlV1_1
        val request = WPComGsonRequest.buildPostRequest(url, null,
                ExportSiteResponse::class.java,
                {
                    val payload = ExportSiteResponsePayload()
                    mDispatcher.dispatch(SiteActionBuilder.newExportedSiteAction(payload))
                }
        ) { error ->
            val payload = ExportSiteResponsePayload()
            payload.error = error
            mDispatcher.dispatch(SiteActionBuilder.newExportedSiteAction(payload))
        }
        add(request)
    }

    @Suppress("LongParameterList")
    fun suggestDomains(
        query: String,
        quantity: Int,
        vendor: String?,
        onlyWordpressCom: Boolean?,
        includeWordpressCom: Boolean?,
        includeDotBlogSubdomain: Boolean?,
        segmentId: Long?,
        tlds: String?
    ) {
        val url = WPCOMREST.domains.suggestions.urlV1_1
        val params = mutableMapOf<String, String>()
        params["query"] = query
        params["quantity"] = quantity.toString()
        if (vendor != null) {
            params["vendor"] = vendor
        }
        if (onlyWordpressCom != null) {
            params["only_wordpressdotcom"] = onlyWordpressCom.toString() // CHECKSTYLE IGNORE
        }
        if (includeWordpressCom != null) {
            params["include_wordpressdotcom"] = includeWordpressCom.toString() // CHECKSTYLE IGNORE
        }
        if (includeDotBlogSubdomain != null) {
            params["include_dotblogsubdomain"] = includeDotBlogSubdomain.toString()
        }
        if (segmentId != null) {
            params["segment_id"] = segmentId.toString()
        }
        if (tlds != null) {
            params["tlds"] = tlds
        }
        val request = WPComGsonRequest.buildGetRequest<List<DomainSuggestionResponse>>(url, params,
                object : TypeToken<List<DomainSuggestionResponse>>() {}.type,
                { response ->
                    val payload = SuggestDomainsResponsePayload(
                            query,
                            response
                    )
                    mDispatcher.dispatch(SiteActionBuilder.newSuggestedDomainsAction(payload))
                },
                { error ->
                    val suggestDomainError = SuggestDomainError(error.apiError, error.message)
                    if (suggestDomainError.type === EMPTY_RESULTS) {
                        // Empty results is not an actual error, the API should return 200 for it
                        val payload = SuggestDomainsResponsePayload(query, emptyList())
                        mDispatcher.dispatch(SiteActionBuilder.newSuggestedDomainsAction(payload))
                    } else {
                        val payload = SuggestDomainsResponsePayload(query, suggestDomainError)
                        mDispatcher.dispatch(SiteActionBuilder.newSuggestedDomainsAction(payload))
                    }
                }
        )
        add(request)
    }

    @Suppress("LongParameterList")
    fun fetchWpComBlockLayouts(
        site: SiteModel,
        supportedBlocks: List<String?>?,
        previewWidth: Float?,
        previewHeight: Float?,
        scale: Float?,
        isBeta: Boolean?
    ) {
        val url = WPCOMV2.sites.site(site.siteId).block_layouts.url
        fetchBlockLayouts(site, url, supportedBlocks, previewWidth, previewHeight, scale, isBeta)
    }

    @Suppress("LongParameterList")
    fun fetchSelfHostedBlockLayouts(
        site: SiteModel,
        supportedBlocks: List<String?>?,
        previewWidth: Float?,
        previewHeight: Float?,
        scale: Float?,
        isBeta: Boolean?
    ) {
        val url = WPCOMV2.common_block_layouts.url
        fetchBlockLayouts(site, url, supportedBlocks, previewWidth, previewHeight, scale, isBeta)
    }

    @Suppress("LongParameterList")
    private fun fetchBlockLayouts(
        site: SiteModel,
        url: String,
        supportedBlocks: List<String?>?,
        previewWidth: Float?,
        previewHeight: Float?,
        scale: Float?,
        isBeta: Boolean?
    ) {
        val params = mutableMapOf<String, String>()
        if (supportedBlocks != null && supportedBlocks.isNotEmpty()) {
            params["supported_blocks"] = TextUtils.join(",", supportedBlocks)
        }
        if (previewWidth != null) {
            params["preview_width"] = String.format(Locale.US, "%.1f", previewWidth)
        }
        if (previewHeight != null) {
            params["preview_height"] = String.format(Locale.US, "%.1f", previewHeight)
        }
        if (scale != null) {
            params["scale"] = String.format(Locale.US, "%.1f", scale)
        }
        params["type"] = "mobile"
        if (isBeta != null) {
            params["is_beta"] = isBeta.toString()
        }
        val request = WPComGsonRequest.buildGetRequest(url, params,
                BlockLayoutsResponse::class.java,
                { (layouts, categories) ->
                    val payload = FetchedBlockLayoutsResponsePayload(
                            site, layouts,
                            categories
                    )
                    mDispatcher.dispatch(SiteActionBuilder.newFetchedBlockLayoutsAction(payload))
                }
        ) { error ->
            val siteErrorType = when (error.apiError) {
                "unauthorized" -> UNAUTHORIZED
                "unknown_blog" -> UNKNOWN_SITE
                else -> SiteErrorType.GENERIC_ERROR
            }
            val siteError = SiteError(siteErrorType, error.message)
            val payload = FetchedBlockLayoutsResponsePayload(site, siteError)
            mDispatcher.dispatch(SiteActionBuilder.newFetchedBlockLayoutsAction(payload))
        }
        add(request)
    }

    // Unauthenticated network calls
    @Suppress("SwallowedException")
    fun fetchConnectSiteInfo(siteUrl: String) {
        // Get a proper URI to reliably retrieve the scheme.
        val uri: URI = try {
            URI.create(UrlUtils.addUrlSchemeIfNeeded(siteUrl, false))
        } catch (e: IllegalArgumentException) {
            val siteError = SiteError(INVALID_SITE)
            val payload = ConnectSiteInfoPayload(siteUrl, siteError)
            mDispatcher.dispatch(SiteActionBuilder.newFetchedConnectSiteInfoAction(payload))
            return
        }
        val params = mutableMapOf<String, String>()
        params["url"] = uri.toString()

        // Make the call.
        val url = WPCOMREST.connect.site_info.urlV1_1
        val request = WPComGsonRequest.buildGetRequest(url, params,
                ConnectSiteInfoResponse::class.java,
                { response ->
                    val info = connectSiteInfoFromResponse(siteUrl, response)
                    mDispatcher.dispatch(SiteActionBuilder.newFetchedConnectSiteInfoAction(info))
                }
        ) {
            val siteError = SiteError(INVALID_SITE)
            val info = ConnectSiteInfoPayload(siteUrl, siteError)
            mDispatcher.dispatch(SiteActionBuilder.newFetchedConnectSiteInfoAction(info))
        }
        addUnauthedRequest(request)
    }

    @Suppress("SwallowedException")
    fun fetchWPComSiteByUrl(siteUrl: String) {
        val sanitizedUrl: String
        try {
            val uri = URI.create(UrlUtils.addUrlSchemeIfNeeded(siteUrl, false))
            sanitizedUrl = URLEncoder.encode(UrlUtils.removeScheme(uri.toString()), "UTF-8")
        } catch (e: IllegalArgumentException) {
            val payload = FetchWPComSiteResponsePayload(siteUrl)
            payload.error = SiteError(INVALID_SITE)
            mDispatcher.dispatch(SiteActionBuilder.newFetchedWpcomSiteByUrlAction(payload))
            return
        } catch (e: UnsupportedEncodingException) {
            // This should be impossible (it means an Android device without UTF-8 support)
            throw IllegalStateException(e)
        }
        val requestUrl = WPCOMREST.sites.siteUrl(sanitizedUrl).urlV1_1
        val request = WPComGsonRequest.buildGetRequest(requestUrl, null,
                SiteWPComRestResponse::class.java,
                { response ->
                    val payload = FetchWPComSiteResponsePayload(siteUrl, siteResponseToSiteModel(response))
                    mDispatcher.dispatch(SiteActionBuilder.newFetchedWpcomSiteByUrlAction(payload))
                }
        ) { error ->
            val payload = FetchWPComSiteResponsePayload(siteUrl)
            val siteErrorType = when (error.apiError) {
                "unauthorized" -> UNAUTHORIZED
                "unknown_blog" -> UNKNOWN_SITE
                else -> SiteErrorType.GENERIC_ERROR
            }
            payload.error = SiteError(siteErrorType)
            mDispatcher.dispatch(SiteActionBuilder.newFetchedWpcomSiteByUrlAction(payload))
        }
        addUnauthedRequest(request)
    }

    fun checkUrlIsWPCom(testedUrl: String) {
        val url = WPCOMREST.sites.urlV1_1 + testedUrl
        val request = WPComGsonRequest.buildGetRequest(url, null,
                SiteWPComRestResponse::class.java,
                {
                    val payload = IsWPComResponsePayload(testedUrl, true)
                    mDispatcher.dispatch(SiteActionBuilder.newCheckedIsWpcomUrlAction(payload))
                }
        ) { error ->
            val payload = IsWPComResponsePayload(testedUrl)
            if ("unauthorized" != error.apiError && "unknown_blog" != error.apiError) {
                payload.error = error
            }
            mDispatcher.dispatch(SiteActionBuilder.newCheckedIsWpcomUrlAction(payload))
        }
        addUnauthedRequest(request)
    }

    /**
     * Performs an HTTP GET call to v1.3 /domains/$domainName/is-available/ endpoint. Upon receiving a response
     * (success or error) a [SiteAction.CHECKED_DOMAIN_AVAILABILITY] action is dispatched with a
     * payload of type [DomainAvailabilityResponsePayload].
     *
     * [DomainAvailabilityResponsePayload.isError] can be used to check the request result.
     */
    fun checkDomainAvailability(domainName: String) {
        val url = WPCOMREST.domains.domainName(domainName).is_available.urlV1_3
        val request = WPComGsonRequest.buildGetRequest(url, null, DomainAvailabilityResponse::class.java,
                { response ->
                    val payload = responseToDomainAvailabilityPayload(response)
                    mDispatcher.dispatch(SiteActionBuilder.newCheckedDomainAvailabilityAction(payload))
                }
        ) { error -> // Domain availability API should always return a response for a valid,
            // authenticated user. Therefore, only GENERIC_ERROR is identified here.
            val domainAvailabilityError = DomainAvailabilityError(
                    DomainAvailabilityErrorType.GENERIC_ERROR, error.message
            )
            val payload = DomainAvailabilityResponsePayload(domainAvailabilityError)
            mDispatcher.dispatch(SiteActionBuilder.newCheckedDomainAvailabilityAction(payload))
        }
        add(request)
    }

    /**
     * Performs an HTTP GET call to v1.1 /domains/supported-states/$countryCode endpoint. Upon receiving a response
     * (success or error) a [SiteAction.FETCHED_DOMAIN_SUPPORTED_STATES] action is dispatched with a
     * payload of type [DomainSupportedStatesResponsePayload].
     *
     * [DomainSupportedStatesResponsePayload.isError] can be used to check the request result.
     */
    fun fetchSupportedStates(countryCode: String) {
        val url = WPCOMREST.domains.supported_states.countryCode(countryCode).urlV1_1
        val request = WPComGsonRequest.buildGetRequest<List<SupportedStateResponse>>(url, null,
                object : TypeToken<List<SupportedStateResponse>>() {}.type,
                { response ->
                    val payload = DomainSupportedStatesResponsePayload(response)
                    mDispatcher.dispatch(SiteActionBuilder.newFetchedDomainSupportedStatesAction(payload))
                },
                { error ->
                    val domainSupportedStatesError = DomainSupportedStatesError(
                            DomainSupportedStatesErrorType.fromString(error.apiError), error.message
                    )
                    val payload = DomainSupportedStatesResponsePayload(domainSupportedStatesError)
                    mDispatcher.dispatch(SiteActionBuilder.newFetchedDomainSupportedStatesAction(payload))
                })
        add(request)
    }

    /**
     * Performs an HTTP GET call to v1.1 /domains/supported-countries/ endpoint. Upon receiving a response
     * (success or error) a [SiteAction.FETCHED_DOMAIN_SUPPORTED_COUNTRIES] action is dispatched with a
     * payload of type [DomainSupportedCountriesResponsePayload].
     *
     * [DomainSupportedCountriesResponsePayload.isError] can be used to check the request result.
     */
    fun fetchSupportedCountries() {
        val url = WPCOMREST.domains.supported_countries.urlV1_1
        val request = WPComGsonRequest.buildGetRequest<List<SupportedCountryResponse>>(url, null,
                object : TypeToken<List<SupportedCountryResponse>>() {}.type,
                { response ->
                    val payload = DomainSupportedCountriesResponsePayload(response)
                    mDispatcher.dispatch(
                            SiteActionBuilder.newFetchedDomainSupportedCountriesAction(payload)
                    )
                },
                { error -> // Supported Countries API should always return a response for a valid,
                    // authenticated user. Therefore, only GENERIC_ERROR is identified here.
                    val domainSupportedCountriesError = DomainSupportedCountriesError(
                            DomainSupportedCountriesErrorType.GENERIC_ERROR,
                            error.message
                    )
                    val payload = DomainSupportedCountriesResponsePayload(domainSupportedCountriesError)
                    mDispatcher.dispatch(
                            SiteActionBuilder.newFetchedDomainSupportedCountriesAction(payload)
                    )
                })
        add(request)
    }

    suspend fun fetchSiteDomains(site: SiteModel): Response<DomainsResponse> {
        val url = WPCOMREST.sites.site(site.siteId).domains.urlV1_1
        return wpComGsonRequestBuilder.syncGetRequest(this, url, mapOf(), DomainsResponse::class.java)
    }

    suspend fun fetchSitePlans(site: SiteModel): Response<PlansResponse> {
        val url = WPCOMREST.sites.site(site.siteId).plans.urlV1_3
        return wpComGsonRequestBuilder.syncGetRequest(this, url, mapOf(), PlansResponse::class.java)
    }

    fun designatePrimaryDomain(site: SiteModel, domain: String) {
        val url = WPCOMREST.sites.site(site.siteId).domains.primary.urlV1_1
        val params = mutableMapOf<String, Any>()
        params["domain"] = domain
        val request = WPComGsonRequest
                .buildPostRequest(url, params, DesignatePrimaryDomainResponse::class.java,
                        { (success) ->
                            mDispatcher.dispatch(
                                    SiteActionBuilder.newDesignatedPrimaryDomainAction(
                                            DesignatedPrimaryDomainPayload(site, success)
                                    )
                            )
                        }
                ) { networkError ->
                    val error = DesignatePrimaryDomainError(
                            DesignatePrimaryDomainErrorType.GENERIC_ERROR, networkError.message
                    )
                    val payload = DesignatedPrimaryDomainPayload(site, false)
                    payload.error = error
                    mDispatcher.dispatch(SiteActionBuilder.newDesignatedPrimaryDomainAction(payload))
                }
        add(request)
    }

    // Automated Transfers
    fun checkAutomatedTransferEligibility(site: SiteModel) {
        val url = WPCOMREST.sites.site(site.siteId).automated_transfers.eligibility.urlV1_1
        val request = WPComGsonRequest
                .buildGetRequest(url, null, AutomatedTransferEligibilityCheckResponse::class.java,
                        { response ->
                            val strErrorCodes = mutableListOf<String>()
                            if (response.errors != null) {
                                for (eligibilityError in response.errors) {
                                    strErrorCodes.add(eligibilityError.code)
                                }
                            }
                            mDispatcher.dispatch(
                                    SiteActionBuilder.newCheckedAutomatedTransferEligibilityAction(
                                            AutomatedTransferEligibilityResponsePayload(
                                                    site, response.isEligible,
                                                    strErrorCodes
                                            )
                                    )
                            )
                        }
                ) { networkError ->
                    val payloadError = AutomatedTransferError(
                            networkError.apiError, networkError.message
                    )
                    mDispatcher.dispatch(
                            SiteActionBuilder.newCheckedAutomatedTransferEligibilityAction(
                                    AutomatedTransferEligibilityResponsePayload(site, payloadError)
                            )
                    )
                }
        add(request)
    }

    fun initiateAutomatedTransfer(site: SiteModel, pluginSlugToInstall: String) {
        val url = WPCOMREST.sites.site(site.siteId).automated_transfers.initiate.urlV1_1
        val params = mutableMapOf<String, Any>()
        params["plugin"] = pluginSlugToInstall
        val request = WPComGsonRequest
                .buildPostRequest(url, params, InitiateAutomatedTransferResponse::class.java,
                        { response ->
                            val payload = InitiateAutomatedTransferResponsePayload(
                                    site, pluginSlugToInstall,
                                    response.success
                            )
                            mDispatcher.dispatch(SiteActionBuilder.newInitiatedAutomatedTransferAction(payload))
                        }
                ) { networkError ->
                    val payload = InitiateAutomatedTransferResponsePayload(site, pluginSlugToInstall)
                    payload.error = AutomatedTransferError(networkError.apiError, networkError.message)
                    mDispatcher.dispatch(SiteActionBuilder.newInitiatedAutomatedTransferAction(payload))
                }
        add(request)
    }

    fun checkAutomatedTransferStatus(site: SiteModel) {
        val url = WPCOMREST.sites.site(site.siteId).automated_transfers.status.urlV1_1
        val request = WPComGsonRequest
                .buildGetRequest(url, null, AutomatedTransferStatusResponse::class.java,
                        { response ->
                            mDispatcher.dispatch(
                                    SiteActionBuilder.newCheckedAutomatedTransferStatusAction(
                                            AutomatedTransferStatusResponsePayload(
                                                    site, response.status,
                                                    response.currentStep, response.totalSteps
                                            )
                                    )
                            )
                        }
                ) { networkError ->
                    val error = AutomatedTransferError(
                            networkError.apiError, networkError.message
                    )
                    mDispatcher.dispatch(
                            SiteActionBuilder.newCheckedAutomatedTransferStatusAction(
                                    AutomatedTransferStatusResponsePayload(site, error)
                            )
                    )
                }
        add(request)
    }

    fun completeQuickStart(site: SiteModel, variant: String) {
        val url = WPCOMREST.sites.site(site.siteId).mobile_quick_start.urlV1_1
        val params = mutableMapOf<String, Any>()
        params["variant"] = variant
        val request = WPComGsonRequest
                .buildPostRequest(url, params, QuickStartCompletedResponse::class.java,
                        { response ->
                            mDispatcher.dispatch(
                                    SiteActionBuilder.newCompletedQuickStartAction(
                                            QuickStartCompletedResponsePayload(site, response.success)
                                    )
                            )
                        }
                ) { networkError ->
                    val error = QuickStartError(
                            QuickStartErrorType.GENERIC_ERROR, networkError.message
                    )
                    val payload = QuickStartCompletedResponsePayload(site, false)
                    payload.error = error
                    mDispatcher.dispatch(SiteActionBuilder.newCompletedQuickStartAction(payload))
                }
        add(request)
    }

    fun fetchAccessCookie(site: SiteModel) {
        val params = mutableMapOf<String, String>()
        val url = WPCOMV2.sites.site(site.siteId).atomic_auth_proxy.read_access_cookies.url
        val request = WPComGsonRequest.buildGetRequest(url, params,
                PrivateAtomicCookieResponse::class.java,
                { response ->
                    if (response != null) {
                        mDispatcher.dispatch(
                                SiteActionBuilder
                                        .newFetchedPrivateAtomicCookieAction(
                                                FetchedPrivateAtomicCookiePayload(site, response)
                                        )
                        )
                    } else {
                        AppLog.e(API, "Failed to fetch private atomic cookie for " + site.url)
                        val payload = FetchedPrivateAtomicCookiePayload(
                                site, null
                        )
                        payload.error = PrivateAtomicCookieError(
                                AccessCookieErrorType.INVALID_RESPONSE, "Empty response"
                        )
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedPrivateAtomicCookieAction(payload))
                    }
                }
        ) { error ->
            val cookieError = PrivateAtomicCookieError(
                    AccessCookieErrorType.GENERIC_ERROR, error.message
            )
            val payload = FetchedPrivateAtomicCookiePayload(site, null)
            payload.error = cookieError
            mDispatcher.dispatch(SiteActionBuilder.newFetchedPrivateAtomicCookieAction(payload))
        }
        add(request)
    }

    fun fetchJetpackCapabilities(remoteSiteId: Long) {
        val params = mutableMapOf<String, String>()
        val url = WPCOMV2.sites.site(remoteSiteId).rewind.capabilities.url
        val request = WPComGsonRequest.buildGetRequest(url, params,
                JetpackCapabilitiesResponse::class.java,
                { response ->
                    if (response?.capabilities != null) {
                        val payload = responseToJetpackCapabilitiesPayload(remoteSiteId, response)
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedJetpackCapabilitiesAction(payload))
                    } else {
                        AppLog.e(API, "Failed to fetch jetpack capabilities for site with id: $remoteSiteId")
                        val error = JetpackCapabilitiesError(
                                JetpackCapabilitiesErrorType.GENERIC_ERROR,
                                "Empty response"
                        )
                        val payload = FetchedJetpackCapabilitiesPayload(remoteSiteId, error)
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedJetpackCapabilitiesAction(payload))
                    }
                }
        ) { error ->
            val jetpackError = JetpackCapabilitiesError(JetpackCapabilitiesErrorType.GENERIC_ERROR, error.message)
            val payload = FetchedJetpackCapabilitiesPayload(remoteSiteId, jetpackError)
            mDispatcher.dispatch(SiteActionBuilder.newFetchedJetpackCapabilitiesAction(payload))
        }
        add(request)
    }

    @Suppress("LongMethod", "ComplexMethod")
    private fun siteResponseToSiteModel(from: SiteWPComRestResponse): SiteModel {
        val site = SiteModel()
        site.siteId = from.ID
        site.url = from.URL
        site.name = StringEscapeUtils.unescapeHtml4(from.name)
        site.description = StringEscapeUtils.unescapeHtml4(from.description)
        site.setIsJetpackConnected(from.jetpack && from.jetpack_connection)
        site.setIsJetpackInstalled(from.jetpack)
        site.setIsJetpackCPConnected(from.jetpack_connection && !from.jetpack)
        site.setIsVisible(from.visible)
        site.setIsPrivate(from.is_private)
        site.setIsComingSoon(from.is_coming_soon)
        site.organizationId = from.organization_id
        // Depending of user's role, options could be "hidden", for instance an "Author" can't read site options.
        if (from.options != null) {
            site.setIsFeaturedImageSupported(from.options.featured_images_enabled)
            site.setIsVideoPressSupported(from.options.videopress_enabled)
            site.setIsAutomatedTransfer(from.options.is_automated_transfer)
            site.setIsWpComStore(from.options.is_wpcom_store)
            site.hasWooCommerce = from.options.woocommerce_is_active
            site.adminUrl = from.options.admin_url
            site.loginUrl = from.options.login_url
            site.timezone = from.options.gmt_offset
            site.frameNonce = from.options.frame_nonce
            site.unmappedUrl = from.options.unmapped_url
            site.jetpackVersion = from.options.jetpack_version
            site.softwareVersion = from.options.software_version
            site.setIsWPComAtomic(from.options.is_wpcom_atomic)
            site.setIsWpForTeamsSite(from.options.is_wpforteams_site)
            site.showOnFront = from.options.show_on_front
            site.pageOnFront = from.options.page_on_front
            site.pageForPosts = from.options.page_for_posts
            site.setIsPublicizePermanentlyDisabled(from.options.publicize_permanently_disabled)
            if (from.options.active_modules != null) {
                site.activeModules = from.options.active_modules.joinToString(",")
            }
            from.options.jetpack_connection_active_plugins?.let {
                site.activeJetpackConnectionPlugins = it.joinToString(",")
            }
            try {
                site.maxUploadSize = java.lang.Long.valueOf(from.options.max_upload_size)
            } catch (e: NumberFormatException) {
                // Do nothing - the value probably wasn't set ('false'), but we don't want to overwrite any existing
                // value we stored earlier, as /me/sites/ and /sites/$site/ can return different responses for this
            }

            // Set the memory limit for media uploads on the site. Normally, this is just WP_MAX_MEMORY_LIMIT,
            // but it's possible for a site to have its php memory_limit > WP_MAX_MEMORY_LIMIT, and have
            // WP_MEMORY_LIMIT == memory_limit, in which WP_MEMORY_LIMIT reflects the real limit for media uploads.
            val wpMemoryLimit = StringUtils.stringToLong(from.options.wp_memory_limit)
            val wpMaxMemoryLimit = StringUtils.stringToLong(from.options.wp_max_memory_limit)
            if (wpMemoryLimit > 0 || wpMaxMemoryLimit > 0) {
                // Only update the value if we received one from the server - otherwise, the original value was
                // probably not set ('false'), but we don't want to overwrite any existing value we stored earlier,
                // as /me/sites/ and /sites/$site/ can return different responses for this
                site.memoryLimit = max(wpMemoryLimit, wpMaxMemoryLimit)
            }

            val bloggingPromptsSettings = from.options.blogging_prompts_settings

            bloggingPromptsSettings?.let {
                site.setIsBloggingPromptsOptedIn(it.prompts_reminders_opted_in)
                site.setIsBloggingPromptsCardOptedIn(it.prompts_card_opted_in)
                site.setIsPotentialBloggingSite(it.is_potential_blogging_site)
                site.setIsBloggingReminderOnMonday(it.reminders_days["monday"] ?: false)
                site.setIsBloggingReminderOnTuesday(it.reminders_days["tuesday"] ?: false)
                site.setIsBloggingReminderOnWednesday(it.reminders_days["wednesday"] ?: false)
                site.setIsBloggingReminderOnThursday(it.reminders_days["thursday"] ?: false)
                site.setIsBloggingReminderOnFriday(it.reminders_days["friday"] ?: false)
                site.setIsBloggingReminderOnSaturday(it.reminders_days["saturday"] ?: false)
                site.setIsBloggingReminderOnSunday(it.reminders_days["sunday"] ?: false)
                try {
                    site.bloggingReminderHour = it.reminders_time.split(".")[0].toInt()
                    site.bloggingReminderMinute = it.reminders_time.split(".")[1].toInt()
                } catch (ex: NumberFormatException) {
                    AppLog.e(API, "Received malformed blogging reminder time: " + ex.message)
                }
            }
        }
        if (from.plan != null) {
            try {
                site.planId = java.lang.Long.valueOf(from.plan.product_id)
            } catch (e: NumberFormatException) {
                // VIP sites return a String plan ID ('vip') rather than a number
                if (from.plan.product_id == "vip") {
                    site.planId = SiteModel.VIP_PLAN_ID
                }
            }
            site.planShortName = from.plan.product_name_short
            site.hasFreePlan = from.plan.is_free
        }
        if (from.capabilities != null) {
            site.hasCapabilityEditPages = from.capabilities.edit_pages
            site.hasCapabilityEditPosts = from.capabilities.edit_posts
            site.hasCapabilityEditOthersPosts = from.capabilities.edit_others_posts
            site.hasCapabilityEditOthersPages = from.capabilities.edit_others_pages
            site.hasCapabilityDeletePosts = from.capabilities.delete_posts
            site.hasCapabilityDeleteOthersPosts = from.capabilities.delete_others_posts
            site.hasCapabilityEditThemeOptions = from.capabilities.edit_theme_options
            site.hasCapabilityEditUsers = from.capabilities.edit_users
            site.hasCapabilityListUsers = from.capabilities.list_users
            site.hasCapabilityManageCategories = from.capabilities.manage_categories
            site.hasCapabilityManageOptions = from.capabilities.manage_options
            site.hasCapabilityActivateWordads = from.capabilities.activate_wordads
            site.hasCapabilityPromoteUsers = from.capabilities.promote_users
            site.hasCapabilityPublishPosts = from.capabilities.publish_posts
            site.hasCapabilityUploadFiles = from.capabilities.upload_files
            site.hasCapabilityDeleteUser = from.capabilities.delete_user
            site.hasCapabilityRemoveUsers = from.capabilities.remove_users
            site.hasCapabilityViewStats = from.capabilities.view_stats
        }
        if (from.quota != null) {
            site.spaceAvailable = from.quota.space_available
            site.spaceAllowed = from.quota.space_allowed
            site.spaceUsed = from.quota.space_used
            site.spacePercentUsed = from.quota.percent_used
        }
        if (from.icon != null) {
            site.iconUrl = from.icon.img
        }
        if (from.meta != null) {
            if (from.meta.links != null) {
                site.xmlRpcUrl = from.meta.links.xmlrpc
            }
        }
        if (from.zendesk_site_meta != null) {
            site.zendeskPlan = from.zendesk_site_meta.plan
            site.zendeskAddOns = from.zendesk_site_meta.addon
                    ?.let { TextUtils.join(",", from.zendesk_site_meta.addon) } ?: ""
        }
        // Only set the isWPCom flag for "pure" WPCom sites
        if (!from.jetpack_connection) {
            site.setIsWPCom(true)
        }
        site.origin = SiteModel.ORIGIN_WPCOM_REST
        return site
    }

    @Suppress("SwallowedException")
    private fun volleyErrorToAccountResponsePayload(
        error: VolleyError,
        dryRun: Boolean = false
    ): NewSiteResponsePayload {
        val payload = NewSiteResponsePayload(dryRun = dryRun)
        payload.error = NewSiteError(NewSiteErrorType.GENERIC_ERROR, "")
        if (error.networkResponse != null && error.networkResponse.data != null) {
            val jsonString = String(error.networkResponse.data)
            try {
                val errorObj = JSONObject(jsonString)
                payload.error = NewSiteError(
                        NewSiteErrorType.fromString((errorObj["error"] as String)),
                        (errorObj["message"] as String)
                )
            } catch (e: JSONException) {
                // Do nothing (keep default error)
            }
        }
        return payload
    }

    private fun connectSiteInfoFromResponse(url: String, response: ConnectSiteInfoResponse): ConnectSiteInfoPayload {
        return ConnectSiteInfoPayload(
                url,
                response.exists,
                response.isWordPress,
                response.hasJetpack,
                response.isJetpackActive,
                response.isJetpackConnected,
                response.isWordPressDotCom, // CHECKSTYLE IGNORE
                response.urlAfterRedirects
        )
    }

    private fun responseToDomainAvailabilityPayload(
        response: DomainAvailabilityResponse
    ): DomainAvailabilityResponsePayload {
        val status = DomainAvailabilityStatus.fromString(response.status!!)
        val mappable = DomainMappabilityStatus.fromString(response.mappable!!)
        val supportsPrivacy = response.supports_privacy
        return DomainAvailabilityResponsePayload(status, mappable, supportsPrivacy)
    }

    private fun responseToJetpackCapabilitiesPayload(
        remoteSiteId: Long,
        response: JetpackCapabilitiesResponse
    ): FetchedJetpackCapabilitiesPayload {
        val capabilities = mutableListOf<JetpackCapability>()
        for (item in response.capabilities ?: listOf()) {
            capabilities.add(JetpackCapability.fromString(item))
        }
        return FetchedJetpackCapabilitiesPayload(remoteSiteId, capabilities)
    }

    companion object {
        private const val NEW_SITE_TIMEOUT_MS = 90000
        private const val SITE_FIELDS = "ID,URL,name,description,jetpack,jetpack_connection,visible,is_private," +
                "options,plan,capabilities,quota,icon,meta,zendesk_site_meta,organization_id"
        private const val FIELDS = "fields"
        private const val FILTERS = "filters"
    }
}
