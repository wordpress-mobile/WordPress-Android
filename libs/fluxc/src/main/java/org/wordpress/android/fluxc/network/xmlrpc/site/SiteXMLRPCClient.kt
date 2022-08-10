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
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequestBuilder
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequestBuilder.Response.Success
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
    httpAuthManager: HTTPAuthManager?,
    private val xmlrpcRequestBuilder: XMLRPCRequestBuilder
) : BaseXMLRPCClient(dispatcher, requestQueue, userAgent, httpAuthManager) {
    fun fetchProfile(site: SiteModel) {
        val params: MutableList<Any> = ArrayList()
        params.add(site.selfHostedSiteId)
        params.add(site.username)
        params.add(site.password)
        val request = xmlrpcRequestBuilder.buildGetRequest(site.xmlRpcUrl, GET_PROFILE, params, Map::class.java,
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

    suspend fun fetchSites(xmlrpcUrl: String, username: String, password: String): SitesModel {
        val params = listOf(username, password)
        val response = xmlrpcRequestBuilder.syncGetRequest(
                this,
                xmlrpcUrl,
                GET_USERS_SITES,
                params,
                Array<Any>::class.java
        )
        return when (response) {
            is Success -> {
                val sites = sitesResponseToSitesModel(response.data, username, password)
                if (sites != null) {
                    sites
                } else {
                    val result = SitesModel()
                    result.error = BaseNetworkError(INVALID_RESPONSE)
                    result
                }
            }
            is Error -> {
                val sites = SitesModel()
                sites.error = response.error
                sites
            }
        }
    }

    suspend fun fetchSite(site: SiteModel): SiteModel {
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
        val response = xmlrpcRequestBuilder.syncGetRequest(this, site.xmlRpcUrl, GET_OPTIONS, params, Map::class.java)
        return when (response) {
            is Success -> {
                val updatedSite = updateSiteFromOptions(response.data, site)
                updatedSite
            }
            is Error -> {
                SiteModel().apply { error = response.error }
            }
        }
    }

    suspend fun fetchPostFormats(site: SiteModel): FetchedPostFormatsPayload {
        val params = listOf(site.selfHostedSiteId, site.username, site.password)
        val response = xmlrpcRequestBuilder.syncGetRequest(
                this,
                site.xmlRpcUrl,
                GET_POST_FORMATS,
                params,
                Map::class.java
        )
        return when (response) {
            is Success -> {
                val postFormats = responseToPostFormats(response.data)
                if (postFormats != null) {
                    val payload = FetchedPostFormatsPayload(site, postFormats)
                    payload
                } else {
                    val payload = FetchedPostFormatsPayload(site, emptyList())
                    payload.error = PostFormatsError(PostFormatsErrorType.INVALID_RESPONSE)
                    payload
                }
            }
            is Error -> {
                val postFormatsError: PostFormatsError = when (response.error.type) {
                    INVALID_RESPONSE -> PostFormatsError(
                            PostFormatsErrorType.INVALID_RESPONSE,
                            response.error.message
                    )
                    else -> PostFormatsError(
                            GENERIC_ERROR,
                            response.error.message
                    )
                }
                val payload = FetchedPostFormatsPayload(site, emptyList())
                payload.error = postFormatsError
                payload
            }
        }
    }

    private fun profileResponseToAccountModel(response: Map<*, *>?, site: SiteModel): SiteModel? {
        if (response == null) return null
        site.email = MapUtils.getMapStr(response, "email")
        site.displayName = MapUtils.getMapStr(response, "display_name")
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

    @Suppress("ForbiddenComment")
    private fun updateSiteFromOptions(response: Map<*, *>, oldModel: SiteModel): SiteModel {
        val siteTitle = XMLRPCUtils.safeGetNestedMapValue(response, "blog_title", "")
        if (!siteTitle.isEmpty()) {
            oldModel.name = StringEscapeUtils.unescapeHtml4(siteTitle)
        }

        // TODO: set a canonical URL here
        val homeUrl = XMLRPCUtils.safeGetNestedMapValue(response, "home_url", "")
        if (!homeUrl.isEmpty()) {
            oldModel.url = homeUrl
        }
        oldModel.softwareVersion = XMLRPCUtils.safeGetNestedMapValue(
                response,
                "software_version",
                ""
        )
        oldModel.setIsFeaturedImageSupported(XMLRPCUtils.safeGetNestedMapValue(response, "post_thumbnail", false))
        oldModel.defaultCommentStatus = XMLRPCUtils.safeGetNestedMapValue(
                response, "default_comment_status",
                "open"
        )
        oldModel.timezone = XMLRPCUtils.safeGetNestedMapValue(
                response,
                "time_zone",
                "0"
        )
        oldModel.loginUrl = XMLRPCUtils.safeGetNestedMapValue(
                response,
                "login_url",
                ""
        )
        oldModel.adminUrl = XMLRPCUtils.safeGetNestedMapValue(
                response,
                "admin_url",
                ""
        )
        setJetpackStatus(response, oldModel)
        // If the site is not public, it's private. Note: this field doesn't always exist.
        val isPublic = XMLRPCUtils.safeGetNestedMapValue(response, "blog_public", true)
        oldModel.setIsPrivate(!isPublic)
        return oldModel
    }

    private fun responseToPostFormats(response: Map<*, *>): List<PostFormatModel>? {
        return SiteUtils.getValidPostFormatsOrNull(response)
    }
}
