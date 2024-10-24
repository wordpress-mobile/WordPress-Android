package org.wordpress.android.fluxc.network.rest.wpapi.plugin

import com.google.gson.annotations.SerializedName
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.model.plugin.SitePluginModel

/**
 * [{"plugin":"akismet\/akismet",
 * "status":"inactive",
 * "name":"Akismet Anti-Spam",
 * "plugin_uri":"https:\/\/akismet.com\/",
 * "author":"Automattic",
 * "author_uri":"https:\/\/automattic.com\/wordpress-plugins\/",
 * "description":{
 *      "raw":"Used by millions, Akismet is quite possibly the best way in the world to <strong>protect your blog from spam<\/strong>. It keeps your site protected even while you sleep. To get started: activate the Akismet plugin and then go to your Akismet Settings page to set up your API key.",
 *      "rendered":"Used by millions, Akismet is quite possibly the best way in the world to <strong>protect your blog from spam<\/strong>. It keeps your site protected even while you sleep. To get started: activate the Akismet plugin and then go to your Akismet Settings page to set up your API key. <cite>By <a href=\"https:\/\/automattic.com\/wordpress-plugins\/\">Automattic<\/a>.<\/cite>"
 * },
 * "version":"4.1.9",
 * "network_only":false,
 * "requires_wp":"",
 * "requires_php":"",
 * "textdomain":"akismet",
 * "_links":{"self":[{"href":"https:\/\/ripe-peacock.jurassic.ninja\/wp-json\/wp\/v2\/plugins\/akismet\/akismet"}]}
 * },
 * {"plugin":"companion\/companion","status":"active","name":"Companion Plugin","plugin_uri":"https:\/\/github.com\/Automattic\/companion","author":"Osk","author_uri":"","description":{"raw":"Helps keep the launched WordPress in order.","rendered":"Helps keep the launched WordPress in order. <cite>By Osk.<\/cite>"},"version":"1.18","network_only":false,"requires_wp":"","requires_php":"","textdomain":"companion","_links":{"self":[{"href":"https:\/\/ripe-peacock.jurassic.ninja\/wp-json\/wp\/v2\/plugins\/companion\/companion"}]}},{"plugin":"hello","status":"inactive","name":"Hello Dolly","plugin_uri":"http:\/\/wordpress.org\/plugins\/hello-dolly\/","author":"Matt Mullenweg","author_uri":"http:\/\/ma.tt\/","description":{"raw":"This is not just a plugin, it symbolizes the hope and enthusiasm of an entire generation summed up in two words sung most famously by Louis Armstrong: Hello, Dolly. When activated you will randomly see a lyric from Hello, Dolly in the upper right of your admin screen on every page.","rendered":"This is not just a plugin, it symbolizes the hope and enthusiasm of an entire generation summed up in two words sung most famously by Louis Armstrong: Hello, Dolly. When activated you will randomly see a lyric from Hello, Dolly in the upper right of your admin screen on every page. <cite>By <a href=\"http:\/\/ma.tt\/\">Matt Mullenweg<\/a>.<\/cite>"},"version":"1.7.2","network_only":false,"requires_wp":"","requires_php":"","textdomain":"","_links":{"self":[{"href":"https:\/\/ripe-peacock.jurassic.ninja\/wp-json\/wp\/v2\/plugins\/hello"}]}},
 * {"plugin":"jetpack\/jetpack","status":"active","name":"Jetpack","plugin_uri":"https:\/\/jetpack.com","author":"Automattic","author_uri":"https:\/\/jetpack.com","description":{"raw":"Security, performance, and marketing tools made by WordPress experts. Jetpack keeps your site protected so you can focus on more important things.","rendered":"Security, performance, and marketing tools made by WordPress experts. Jetpack keeps your site protected so you can focus on more important things. <cite>By <a href=\"https:\/\/jetpack.com\">Automattic<\/a>.<\/cite>"},"version":"9.8.1","network_only":false,"requires_wp":"5.6","requires_php":"5.6",
 * "textdomain":"jetpack","_links":{"self":[{"href":"https:\/\/ripe-peacock.jurassic.ninja\/wp-json\/wp\/v2\/plugins\/jetpack\/jetpack"}]}}]
 */
@Suppress("MaxLineLength")
data class PluginResponseModel(
    val plugin: String?,
    val status: String?,
    val name: String?,
    @SerializedName("plugin_uri") val pluginUri: String?,
    val author: String?,
    @SerializedName("author_uri") val authorUri: String?,
    val description: Description?,
    val version: String?,
    @SerializedName("network_only") val networkOnly: Boolean,
    @SerializedName("requires_wp") val requiresWp: String?,
    @SerializedName("requires_php") val requiresPhp: String?,
    @SerializedName("textdomain") val textDomain: String
) {
    data class Description(
        val raw: String?,
        val rendered: String?
    )
}

fun PluginResponseModel.toDomainModel(siteId: Int): SitePluginModel {
    val model = SitePluginModel().apply {
        localSiteId = siteId
        name = this@toDomainModel.plugin
        displayName = this@toDomainModel.name
        authorName = StringEscapeUtils.unescapeHtml4(this@toDomainModel.author)
        authorUrl = this@toDomainModel.authorUri
        description = this@toDomainModel.description?.raw
        pluginUrl = this@toDomainModel.pluginUri
        slug = this@toDomainModel.textDomain
        version = this@toDomainModel.version
    }
    model.setIsActive(this.status == "active")
    return model
}
