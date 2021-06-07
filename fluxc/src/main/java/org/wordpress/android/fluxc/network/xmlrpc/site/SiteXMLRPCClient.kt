package org.wordpress.android.fluxc.network.xmlrpc.site

import com.android.volley.RequestQueue
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.XMLRPC.GET_OPTIONS
import org.wordpress.android.fluxc.generated.endpoint.XMLRPC.GET_POST_FORMATS
import org.wordpress.android.fluxc.generated.endpoint.XMLRPC.GET_PROFILE
import org.wordpress.android.fluxc.generated.endpoint.XMLRPC.GET_USERS_SITES
import org.wordpress.android.fluxc.model.PostFormatModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.SitesModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.HTTPAuthManager
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.xmlrpc.BaseXMLRPCClient
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCUtils
import org.wordpress.android.fluxc.store.SiteStore.FetchedPostFormatsPayload
import org.wordpress.android.fluxc.store.SiteStore.PostFormatsError
import org.wordpress.android.fluxc.store.SiteStore.PostFormatsErrorType
import org.wordpress.android.fluxc.store.SiteStore.PostFormatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.utils.SiteUtils
import org.wordpress.android.util.MapUtils
import java.util.ArrayList
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SiteXMLRPCClient @Inject constructor(
    dispatcher: Dispatcher?,
    @Named("custom-ssl") requestQueue: RequestQueue?,
    userAgent: UserAgent?,
    httpAuthManager: HTTPAuthManager?
) : BaseXMLRPCClient(dispatcher, requestQueue, userAgent, httpAuthManager) {
    fun fetchProfile(site: SiteModel) {
        val params: MutableList<Any> = ArrayList(3)
        params.add(site.selfHostedSiteId)
        params.add(site.username)
        params.add(site.password)
        val request = XMLRPCRequest(site.xmlRpcUrl, GET_PROFILE, params,
                { response ->
                    val updatedSite = profileResponseToAccountModel(response, site)
                    mDispatcher.dispatch(SiteActionBuilder.newFetchedProfileXmlRpcAction(updatedSite))
                }
        ) { error ->
            val site = SiteModel()
            site.error = error
            mDispatcher.dispatch(SiteActionBuilder.newFetchedProfileXmlRpcAction(site))
        }
        add(request)
    }

    fun fetchSites(xmlrpcUrl: String?, username: String, password: String) {
        val params = listOf(username, password)
        val request = XMLRPCRequest(
                xmlrpcUrl, GET_USERS_SITES, params,
                { response ->
                    var sites = sitesResponseToSitesModel(response, username, password)
                    if (sites != null) {
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedSitesXmlRpcAction(sites))
                    } else {
                        sites = SitesModel()
                        sites.error = BaseNetworkError(INVALID_RESPONSE)
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedSitesXmlRpcAction(sites))
                    }
                }
        ) { error ->
            val sites = SitesModel()
            sites.error = error
            mDispatcher.dispatch(SiteActionBuilder.newFetchedSitesXmlRpcAction(sites))
        }
        add(request)
    }

    fun fetchSite(site: SiteModel) {
        val params = listOf(
                site.selfHostedSiteId, site.username, site.password,
                arrayOf(
                        "software_version",
                        "post_thumbnail",
                        "default_comment_status",
                        "jetpack_client_id",
                        "blog_public",
                        "home_url",
                        "admin_url",
                        "login_url",
                        "blog_title",
                        "time_zone",
                        "jetpack_user_email"
                )
        )
        val request = XMLRPCRequest(
                site.xmlRpcUrl, GET_OPTIONS, params,
                { response ->
                    val updatedSite = updateSiteFromOptions(response, site)
                    if (updatedSite != null) {
                        mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(updatedSite))
                    } else {
                        val site = SiteModel()
                        site.error = BaseNetworkError(INVALID_RESPONSE)
                        mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(site))
                    }
                }
        ) { error ->
            val site = SiteModel()
            site.error = error
            mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(site))
        }
        add(request)
    }

    fun fetchPostFormats(site: SiteModel) {
        val params = listOf(site.selfHostedSiteId, site.username, site.password)
        val request = XMLRPCRequest(
                site.xmlRpcUrl, GET_POST_FORMATS, params,
                { response ->
                    val postFormats = responseToPostFormats(response, site)
                    if (postFormats != null) {
                        val payload = FetchedPostFormatsPayload(site, postFormats)
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedPostFormatsAction(payload))
                    } else {
                        val payload = FetchedPostFormatsPayload(site, emptyList())
                        payload.error = PostFormatsError(PostFormatsErrorType.INVALID_RESPONSE)
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedPostFormatsAction(payload))
                    }
                }
        ) { error ->
            val postFormatsError: PostFormatsError = when (error.type) {
                INVALID_RESPONSE -> PostFormatsError(
                        PostFormatsErrorType.INVALID_RESPONSE,
                        error.message
                )
                else -> PostFormatsError(
                        GENERIC_ERROR,
                        error.message
                )
            }
            val payload = FetchedPostFormatsPayload(site, emptyList())
            payload.error = postFormatsError
            mDispatcher.dispatch(SiteActionBuilder.newFetchedPostFormatsAction(payload))
        }
        add(request)
    }

    private fun profileResponseToAccountModel(response: Any?, site: SiteModel): SiteModel? {
        if (response == null) return null
        val userMap = response as Map<*, *>
        site.email = MapUtils.getMapStr(userMap, "email")
        site.displayName = MapUtils.getMapStr(userMap, "display_name")
        return site
    }

    private fun sitesResponseToSitesModel(response: Array<Any>?, username: String, password: String): SitesModel? {
        if (response == null) return null
        val siteArray: MutableList<SiteModel> = ArrayList()
        for (siteObject in response) {
            if (siteObject !is Map<*, *>) {
                continue
            }
            val siteMap = siteObject
            val site = SiteModel()
            site.selfHostedSiteId = MapUtils.getMapInt(siteMap, "blogid", 1).toLong()
            site.name = StringEscapeUtils.unescapeHtml4(MapUtils.getMapStr(siteMap, "blogName"))
            site.url = MapUtils.getMapStr(siteMap, "url")
            site.xmlRpcUrl = MapUtils.getMapStr(siteMap, "xmlrpc")
            site.setIsSelfHostedAdmin(MapUtils.getMapBool(siteMap, "isAdmin"))
            // From what we know about the host
            site.setIsWPCom(false)
            site.username = username
            site.password = password
            site.origin = SiteModel.ORIGIN_XMLRPC
            siteArray.add(site)
        }
        return if (siteArray.isEmpty()) {
            null
        } else SitesModel(siteArray)
    }

    private fun string2Long(s: String, defvalue: Long): Long {
        return try {
            java.lang.Long.valueOf(s)
        } catch (e: NumberFormatException) {
            defvalue
        }
    }

    private fun setJetpackStatus(siteOptions: Map<*, *>, oldModel: SiteModel) {
        // * Jetpack not installed: field "jetpack_client_id" not included in the response
        // * Jetpack installed but not activated: field "jetpack_client_id" not included in the response
        // * Jetpack installed, activated but not connected: field "jetpack_client_id" included
        //   and is "0" (boolean)
        // * Jetpack installed, activated and connected: field "jetpack_client_id" included and is correctly
        //   set to wpcom unique id eg. "1234"
        val jetpackClientIdStr = XMLRPCUtils.safeGetNestedMapValue(siteOptions, "jetpack_client_id", "")
        var jetpackClientId: Long = 0
        // jetpackClientIdStr can be a boolean "0" (false), in that case we keep the default value "0".
        if ("false" != jetpackClientIdStr) {
            jetpackClientId = string2Long(jetpackClientIdStr, -1)
        }

        // Field "jetpack_client_id" not found:
        if (jetpackClientId == -1L) {
            oldModel.setIsJetpackInstalled(false)
            oldModel.setIsJetpackConnected(false)
        }

        // Field "jetpack_client_id" is "0"
        if (jetpackClientId == 0L) {
            oldModel.setIsJetpackInstalled(true)
            oldModel.setIsJetpackConnected(false)
        }

        // jetpack_client_id is set then it's a Jetpack connected site
        if (jetpackClientId != 0L && jetpackClientId != -1L) {
            oldModel.setIsJetpackInstalled(true)
            oldModel.setIsJetpackConnected(true)
            oldModel.siteId = jetpackClientId
        } else {
            oldModel.siteId = 0
        }

        // * Jetpack not installed: field "jetpack_user_email" not included in the response
        // * Jetpack installed but not activated: field "jetpack_user_email" not included in the response
        // * Jetpack installed, activated but not connected: field "jetpack_user_email" not included in the response
        // * Jetpack installed, activated and connected: field "jetpack_user_email" included and is correctly
        //   set to the email of the jetpack connected user
        oldModel.jetpackUserEmail = XMLRPCUtils.safeGetNestedMapValue(
                siteOptions,
                "jetpack_user_email",
                ""
        )
    }

    private fun updateSiteFromOptions(response: Any, oldModel: SiteModel): SiteModel? {
        if (response !is Map<*, *>) {
            reportParseError(response, oldModel.xmlRpcUrl, MutableMap::class.java)
            return null
        }
        val siteOptions = response
        val siteTitle = XMLRPCUtils.safeGetNestedMapValue(siteOptions, "blog_title", "")
        if (!siteTitle.isEmpty()) {
            oldModel.name = StringEscapeUtils.unescapeHtml4(siteTitle)
        }

        // TODO: set a canonical URL here
        val homeUrl = XMLRPCUtils.safeGetNestedMapValue(siteOptions, "home_url", "")
        if (!homeUrl.isEmpty()) {
            oldModel.url = homeUrl
        }
        oldModel.softwareVersion = XMLRPCUtils.safeGetNestedMapValue(
                siteOptions,
                "software_version",
                ""
        )
        oldModel.setIsFeaturedImageSupported(XMLRPCUtils.safeGetNestedMapValue(siteOptions, "post_thumbnail", false))
        oldModel.defaultCommentStatus = XMLRPCUtils.safeGetNestedMapValue(
                siteOptions, "default_comment_status",
                "open"
        )
        oldModel.timezone = XMLRPCUtils.safeGetNestedMapValue(
                siteOptions,
                "time_zone",
                "0"
        )
        oldModel.loginUrl = XMLRPCUtils.safeGetNestedMapValue(
                siteOptions,
                "login_url",
                ""
        )
        oldModel.adminUrl = XMLRPCUtils.safeGetNestedMapValue(
                siteOptions,
                "admin_url",
                ""
        )
        setJetpackStatus(siteOptions, oldModel)
        // If the site is not public, it's private. Note: this field doesn't always exist.
        val isPublic = XMLRPCUtils.safeGetNestedMapValue(siteOptions, "blog_public", true)
        oldModel.setIsPrivate(!isPublic)
        return oldModel
    }

    private fun responseToPostFormats(response: Any, site: SiteModel): List<PostFormatModel>? {
        if (response !is Map<*, *>) {
            reportParseError(response, site.xmlRpcUrl, MutableMap::class.java)
            return null
        }
        return SiteUtils.getValidPostFormatsOrNull(response)
    }
}
