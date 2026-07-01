package org.wordpress.android.fluxc.persistence

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import com.wellsql.generated.AccountModelTable
import com.wellsql.generated.GutenbergLayoutCategoriesModelTable
import com.wellsql.generated.GutenbergLayoutCategoryModelTable
import com.wellsql.generated.GutenbergLayoutModelTable
import com.wellsql.generated.PostFormatModelTable
import com.wellsql.generated.RoleModelTable
import com.wellsql.generated.SiteModelTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.encryption.EncryptionUtils
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.PostFormatModel
import org.wordpress.android.fluxc.model.RoleModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.layouts.GutenbergLayoutCategoriesModel
import org.wordpress.android.fluxc.model.layouts.GutenbergLayoutCategoryModel
import org.wordpress.android.fluxc.model.layouts.GutenbergLayoutModel
import org.wordpress.android.fluxc.model.layouts.connections
import org.wordpress.android.fluxc.model.layouts.transform
import org.wordpress.android.fluxc.network.rest.wpcom.site.GutenbergLayout
import org.wordpress.android.fluxc.network.rest.wpcom.site.GutenbergLayoutCategory
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.DB
import org.wordpress.android.util.UrlUtils
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SiteSqlUtils
@Inject constructor(
    private val encryptionUtils: EncryptionUtils
) {
    object DuplicateSiteException : Exception() {
        private const val serialVersionUID = -224883903136726226L
    }

    fun getSiteWithLocalId(id: LocalId): SiteModel? = WellSql.select(SiteModel::class.java)
            .where()
            .equals(SiteModelTable.ID, id.value)
            .endWhere()
            .asModel
            .firstOrNull()
            ?.decryptAPIRestCredentials()

    fun getSitesWithLocalId(id: Int): List<SiteModel> {
        return WellSql.select(SiteModel::class.java)
                .where().equals(SiteModelTable.ID, id).endWhere().asModel
                .decryptAPIRestCredentials()
    }

    fun getSitesWithRemoteId(id: Long): List<SiteModel> {
        return WellSql.select(SiteModel::class.java)
                .where().equals(SiteModelTable.SITE_ID, id).endWhere().asModel
                .decryptAPIRestCredentials()
    }

    fun getWpComSites(): List<SiteModel> {
        return WellSql.select(SiteModel::class.java)
                .where().equals(SiteModelTable.IS_WPCOM, true).endWhere().asModel
                .decryptAPIRestCredentials()
    }

    fun getWpComAtomicSites(): List<SiteModel> {
        return WellSql.select(SiteModel::class.java)
                .where().equals(SiteModelTable.IS_WPCOM_ATOMIC, true).endWhere().asModel
                .decryptAPIRestCredentials()
    }

    fun getSitesWith(field: String?, value: Boolean): SelectQuery<SiteModel> {
        return WellSql.select(SiteModel::class.java)
                .where().equals(field, value).endWhere()
    }

    fun getSitesAccessedViaWPComRestByNameOrUrlMatching(searchString: String?): List<SiteModel> {
        // Note: by default SQLite "LIKE" operator is case insensitive, and that's what we're looking for.
        return WellSql.select(SiteModel::class.java).where() // ORIGIN = ORIGIN_WPCOM_REST AND (x in url OR x in name)
                .equals(SiteModelTable.ORIGIN, SiteModel.ORIGIN_WPCOM_REST)
                .beginGroup()
                .contains(SiteModelTable.URL, searchString)
                .or().contains(SiteModelTable.NAME, searchString)
                .endGroup().endWhere().asModel
                .decryptAPIRestCredentials()
    }

    fun getSitesByNameOrUrlMatching(searchString: String?): List<SiteModel> {
        return WellSql.select(SiteModel::class.java).where()
                .contains(SiteModelTable.URL, searchString)
                .or().contains(SiteModelTable.NAME, searchString)
                .endWhere().asModel
                .decryptAPIRestCredentials()
    }

    fun getSites(): List<SiteModel> =
        WellSql.select(SiteModel::class.java)
            .where()
            .equals(SiteModelTable.IS_DELETED, false)
            .endWhere()
            .asModel
            .decryptAPIRestCredentials()

    /**
     * Inserts or updates [site] and returns the number of rows affected (1 if a row was written, 0
     * otherwise). Use [insertOrUpdateSiteReturningId] when you need the local id of the written row.
     */
    @Throws(DuplicateSiteException::class)
    fun insertOrUpdateSite(site: SiteModel?): Int =
        if (insertOrUpdateSiteReturningId(site) != 0) 1 else 0

    /**
     * Inserts the given SiteModel into the DB, or updates an existing entry where sites match, returning the
     * local id of the written row (0 if nothing was written). Returning the id lets callers target that exact
     * row with the single-column writers without a second, fragile lookup.
     *
     * Possible cases:
     * 1. Exists in the DB already and matches by local id (simple update) -> UPDATE
     * 2. Exists in the DB, is a Jetpack or WordPress site and matches by remote id (SITE_ID) -> UPDATE
     * 3. Exists in the DB, is a pure self hosted and matches by remote id (SITE_ID) + URL -> UPDATE
     * 4. Exists in the DB, originally a WP.com REST site, and matches by XMLRPC_URL -> THROW a DuplicateSiteException
     * 5. Exists in the DB, originally an XML-RPC site, and matches by XMLRPC_URL -> UPDATE
     * 6. Not matching any previous cases -> INSERT
     */
    @Suppress("LongMethod", "ReturnCount", "ComplexMethod")
    @Throws(DuplicateSiteException::class)
    fun insertOrUpdateSiteReturningId(site: SiteModel?): Int {
        if (site == null) {
            return 0
        }

        val finalSiteModel = site.encryptAPIRestCredentials()

        // If we're inserting or updating a WP.com REST API site, validate that we actually have a WordPress.com
        // AccountModel present
        // This prevents a late UPDATE_SITES action from re-populating the database after sign out from WordPress.com
        if (finalSiteModel.isUsingWpComRestApi) {
            val accountModel = WellSql.select(AccountModel::class.java)
                    .where()
                    .not().equals(AccountModelTable.USER_ID, 0)
                    .endWhere()
                    .asModel
            if (accountModel.isEmpty()) {
                AppLog.w(DB, "Can't insert WP.com site " + finalSiteModel.url + ", missing user account")
                return 0
            }
        }

        // If the site already exist and has an id, we want to update it.
        var siteResult = WellSql.select(SiteModel::class.java)
                .where().beginGroup()
                .equals(SiteModelTable.ID, finalSiteModel.id)
                .endGroup().endWhere().asModel
        if (!siteResult.isEmpty()) {
            AppLog.d(DB, "Site found by (local) ID: " + finalSiteModel.id)
        }

        // Looks like a new site, make sure we don't already have it.
        if (siteResult.isEmpty()) {
            if (finalSiteModel.siteId > 0) {
                // For WordPress.com and Jetpack sites, the WP.com ID is a unique enough identifier
                siteResult = WellSql.select(SiteModel::class.java)
                        .where().beginGroup()
                        .equals(SiteModelTable.SITE_ID, finalSiteModel.siteId)
                        .endGroup().endWhere().asModel
                if (!siteResult.isEmpty()) {
                    AppLog.d(DB, "Site found by SITE_ID: " + finalSiteModel.siteId)
                }
            } else {
                siteResult = WellSql.select(SiteModel::class.java)
                        .where().beginGroup()
                        .equals(SiteModelTable.SITE_ID, finalSiteModel.siteId)
                        .equals(SiteModelTable.URL, finalSiteModel.url)
                        .endGroup().endWhere().asModel
                if (!siteResult.isEmpty()) {
                    AppLog.d(DB, "Site found by SITE_ID: " + finalSiteModel.siteId + " and URL: " + finalSiteModel.url)
                }
            }
        }

        // If the site is a self hosted, maybe it's already in the DB as a Jetpack site, and we don't want to create
        // a duplicate.
        if (siteResult.isEmpty()) {
            val forcedHttpXmlRpcUrl = "http://" + UrlUtils.removeScheme(finalSiteModel.xmlRpcUrl)
            val forcedHttpsXmlRpcUrl = "https://" + UrlUtils.removeScheme(finalSiteModel.xmlRpcUrl)
            siteResult = WellSql.select(SiteModel::class.java)
                    .where()
                    .beginGroup()
                    .equals(SiteModelTable.XMLRPC_URL, forcedHttpXmlRpcUrl)
                    .or().equals(SiteModelTable.XMLRPC_URL, forcedHttpsXmlRpcUrl)
                    .endGroup()
                    .endWhere()
                    .asModel
            if (siteResult.isNotEmpty()) {
                AppLog.d(DB, "Site found using XML-RPC url: " + finalSiteModel.xmlRpcUrl)
                // Four possibilities here:
                // 1. DB site is WP.com, new site is WP.com with different siteIds:
                // The site could be having an "Identity Crisis", while this should be fixed on the site itself,
                // it shouldn't block sign-in -> proceed
                // 2. DB site is WP.com, new site is XML-RPC:
                // It looks like an existing Jetpack-connected site over the REST API was added again as an XML-RPC
                // Wed don't allow this --> DuplicateSiteException
                // 3. DB site is XML-RPC, new site is WP.com:
                // Upgrading a self-hosted site to Jetpack --> proceed
                // 4. DB site is XML-RPC, new site is XML-RPC:
                // An existing self-hosted site was logged-into again, and we couldn't identify it by URL or
                // by WP.com site ID + URL --> proceed
                if (siteResult[0].origin == SiteModel.ORIGIN_WPCOM_REST &&
                    finalSiteModel.origin == SiteModel.ORIGIN_WPCOM_REST) {
                    AppLog.d(
                        DB,
                        "Duplicate WPCom sites with same URLs, it could be an Identity Crisis, insert both sites"
                    )
                    siteResult = emptyList()
                } else if (siteResult[0].origin == SiteModel.ORIGIN_WPCOM_REST) {
                    AppLog.d(DB, "Site is a duplicate")
                    throw DuplicateSiteException
                }
            }
        }
        return if (siteResult.isEmpty()) {
            // No site with this local ID, REMOTE_ID + URL, or XMLRPC URL, then insert it
            AppLog.d(DB, "Inserting site: " + finalSiteModel.url)
            // WellSql back-fills the auto-assigned id onto the model on insert (Identifiable.setId).
            WellSql.insert(finalSiteModel).asSingleTransaction(true).execute()
            finalSiteModel.id
        } else {
            // Update old site
            AppLog.d(DB, "Updating site: " + finalSiteModel.url)
            val oldId = siteResult[0].id
            try {
                // WP_API_REST_URL and the application-password credential columns are discovered/healed out
                // of band — never carried by the general site-sync responses — so they're excluded from the
                // generic mapper and written only by their dedicated writers (updateWpApiRestUrl /
                // updateApplicationPasswordCredentials); a stale full-row write must not clobber them.
                //
                // XMLRPC_URL is different: the WP.com REST sync reliably carries it (meta.links.xmlrpc), so
                // the generic write persists it — that's how a changed endpoint (e.g. a domain migration)
                // lands. But partial writers that don't carry it (the WPAPI fetch builds a model with a null
                // xmlRpcUrl) must not clear a stored/rediscovered value, so preserve it on absence.
                if (finalSiteModel.xmlRpcUrl.isNullOrEmpty()) {
                    finalSiteModel.xmlRpcUrl = siteResult[0].xmlRpcUrl
                }
                WellSql.update(SiteModel::class.java).whereId(oldId)
                        .put(
                                finalSiteModel,
                                UpdateAllExceptId(
                                        SiteModel::class.java,
                                        SiteModelTable.WP_API_REST_URL,
                                        SiteModelTable.API_REST_USERNAME,
                                        SiteModelTable.API_REST_PASSWORD,
                                        SiteModelTable.API_REST_USERNAME_IV,
                                        SiteModelTable.API_REST_PASSWORD_IV
                                )
                        ).execute()
                oldId
            } catch (e: SQLiteConstraintException) {
                AppLog.e(
                        DB,
                        "Error while updating site: siteId=${finalSiteModel.siteId} url=${finalSiteModel.url} " +
                                "xmlrpc=${finalSiteModel.xmlRpcUrl}",
                        e
                )
                throw DuplicateSiteException
            }
        }
    }

    fun deleteSite(site: SiteModel?): Int {
        return if (site == null) {
            0
        } else WellSql.delete(SiteModel::class.java)
                .where().equals(SiteModelTable.ID, site.id).endWhere()
                .execute()
    }

    fun deleteAllSites(): Int {
        return WellSql.delete(SiteModel::class.java).execute()
    }

    fun setSiteVisibility(site: SiteModel?, visible: Boolean): Int {
        return if (site == null) {
            0
        } else WellSql.update(SiteModel::class.java)
                .whereId(site.id)
                .where().equals(SiteModelTable.IS_WPCOM, true).endWhere()
                .put(visible, { item ->
                    val cv = ContentValues()
                    cv.put(SiteModelTable.IS_VISIBLE, item)
                    cv
                }).execute()
    }

    /**
     * Targeted writer for [SiteModel.wpApiRestUrl]. This is the sole writer of WP_API_REST_URL on an
     * existing row: the generic full-row update path ([insertOrUpdateSite]) excludes the column so that
     * stale in-memory sites can't clobber a value that was healed/discovered out of band.
     */
    fun updateWpApiRestUrl(localId: Int, wpApiRestUrl: String): Int {
        return WellSql.update(SiteModel::class.java)
                .whereId(localId)
                .put(wpApiRestUrl, { value ->
                    val cv = ContentValues()
                    cv.put(SiteModelTable.WP_API_REST_URL, value)
                    cv
                }).execute()
    }

    /**
     * Clears [SiteModel.wpApiRestUrl] for the given local id. Use this instead of a full-row update when an
     * explicit action (e.g. removing an application password) needs to drop the stored REST URL, since the
     * generic update path no longer touches the column.
     */
    fun clearWpApiRestUrl(localId: Int): Int = updateWpApiRestUrl(localId, "")

    /**
     * Targeted writer for [SiteModel.xmlRpcUrl], used by the XML-RPC rediscovery heal to persist just that
     * one column without a full-row write of an in-memory model. Unlike [updateWpApiRestUrl], XMLRPC_URL is
     * NOT excluded from the generic mapper — the WP.com sync reliably carries it — but the generic update
     * path preserves it on absence, so a partial write can't clobber a rediscovered value. (The recovery flow
     * that calls this lands separately.)
     */
    fun updateXmlRpcUrl(localId: Int, xmlRpcUrl: String): Int {
        return WellSql.update(SiteModel::class.java)
                .whereId(localId)
                .put(xmlRpcUrl, { value ->
                    val cv = ContentValues()
                    cv.put(SiteModelTable.XMLRPC_URL, value)
                    cv
                }).execute()
    }

    /**
     * Targeted writer for the application-password credential columns: the encrypted username and password
     * and their IVs. The four are written as a set — the IVs are required to decrypt the ciphertext, so
     * persisting the values without their IVs would break reads. This is the sole writer of these columns on
     * an existing row: the generic full-row update path excludes them so a credential-less inbound SiteModel
     * (e.g. a /me/sites sync model) can't zero them out.
     */
    fun updateApplicationPasswordCredentials(localId: Int, usernamePlain: String, passwordPlain: String): Int {
        val username = encryptionUtils.encrypt(usernamePlain)
        val password = encryptionUtils.encrypt(passwordPlain)
        return writeApplicationPasswordCredentialColumns(
                localId, username.first, username.second, password.first, password.second
        )
    }

    /**
     * Clears the application-password credential columns (encrypted username/password and their IVs) for the
     * given local id. Companion to [updateApplicationPasswordCredentials] for the sign-out / revoke paths,
     * since the generic update path no longer touches these columns.
     */
    fun clearApplicationPasswordCredentials(localId: Int): Int =
            writeApplicationPasswordCredentialColumns(localId, "", "", "", "")

    /**
     * The single place that maps the four application-password credential columns to their values, so the
     * column set lives in one spot. Callers pass already-encrypted values (or empty strings to clear); each
     * IV always travels with its ciphertext, so a row can never be left holding one without the other.
     */
    private fun writeApplicationPasswordCredentialColumns(
        localId: Int,
        usernameCipher: String,
        usernameIv: String,
        passwordCipher: String,
        passwordIv: String
    ): Int {
        return WellSql.update(SiteModel::class.java)
                .whereId(localId)
                .put(localId, { _ ->
                    val cv = ContentValues()
                    cv.put(SiteModelTable.API_REST_USERNAME, usernameCipher)
                    cv.put(SiteModelTable.API_REST_USERNAME_IV, usernameIv)
                    cv.put(SiteModelTable.API_REST_PASSWORD, passwordCipher)
                    cv.put(SiteModelTable.API_REST_PASSWORD_IV, passwordIv)
                    cv
                }).execute()
    }

    /**
     * URL-keyed variant of [updateWpApiRestUrl] for application-password (ORIGIN_WPAPI) sites, which are
     * fetched as fresh models with no local id (see SiteWPAPIRestClient.fetchWPAPISite). Scoped to
     * ORIGIN_WPAPI so it can't touch a WP.com/Jetpack row that happens to share the same URL.
     */
    fun updateWpApiRestUrlForWPAPISite(siteUrl: String, wpApiRestUrl: String): Int {
        val localId = wpApiSiteLocalIdByUrl(siteUrl) ?: return 0
        return updateWpApiRestUrl(localId, wpApiRestUrl)
    }

    /**
     * URL-keyed variant of [updateApplicationPasswordCredentials] for ORIGIN_WPAPI sites fetched without a
     * local id. See [updateWpApiRestUrlForWPAPISite].
     */
    fun updateApplicationPasswordCredentialsForWPAPISite(
        siteUrl: String,
        usernamePlain: String,
        passwordPlain: String
    ): Int {
        val localId = wpApiSiteLocalIdByUrl(siteUrl) ?: return 0
        return updateApplicationPasswordCredentials(localId, usernamePlain, passwordPlain)
    }

    private fun wpApiSiteLocalIdByUrl(siteUrl: String): Int? =
        WellSql.select(SiteModel::class.java)
                .where().beginGroup()
                .equals(SiteModelTable.URL, siteUrl)
                .equals(SiteModelTable.ORIGIN, SiteModel.ORIGIN_WPAPI)
                .endGroup().endWhere()
                .asModel
                .firstOrNull()?.id

    val wPComSites: SelectQuery<SiteModel>
        get() = WellSql.select(SiteModel::class.java)
                .where().beginGroup()
                .equals(SiteModelTable.IS_WPCOM, true)
                .endGroup().endWhere()

    val sitesAccessedViaXMLRPC: List<SiteModel>
        get() = WellSql.select(SiteModel::class.java)
                .where().beginGroup()
                .equals(SiteModelTable.ORIGIN, SiteModel.ORIGIN_XMLRPC)
                .endGroup().endWhere()
            .asModel
            .decryptAPIRestCredentials()

    val sitesAccessedViaWPComRest: SelectQuery<SiteModel>
        get() = WellSql.select(SiteModel::class.java)
                .where().beginGroup()
                .equals(SiteModelTable.ORIGIN, SiteModel.ORIGIN_WPCOM_REST)
                .equals(SiteModelTable.IS_DELETED, false)
                .endGroup().endWhere()

    val sitesAccessedViaWPAPI: List<SiteModel>
        get() = WellSql.select(SiteModel::class.java)
                .where().beginGroup()
                .equals(SiteModelTable.ORIGIN, SiteModel.ORIGIN_WPAPI)
                .endGroup().endWhere()
            .asModel
            .decryptAPIRestCredentials()

    fun getPostFormats(site: SiteModel): List<PostFormatModel> {
        return WellSql.select(PostFormatModel::class.java)
                .where()
                .equals(PostFormatModelTable.SITE_ID, site.id)
                .endWhere().asModel
    }

    fun insertOrReplacePostFormats(site: SiteModel, postFormats: List<PostFormatModel>) {
        // Remove previous post formats for this site
        WellSql.delete(PostFormatModel::class.java)
                .where()
                .equals(PostFormatModelTable.SITE_ID, site.id)
                .endWhere().execute()
        // Insert new post formats for this site
        for (postFormat in postFormats) {
            postFormat.siteId = site.id
        }
        WellSql.insert(postFormats).execute()
    }

    fun getUserRoles(site: SiteModel): List<RoleModel> {
        return WellSql.select(RoleModel::class.java)
                .where()
                .equals(RoleModelTable.SITE_ID, site.id)
                .endWhere().asModel
    }

    fun insertOrReplaceUserRoles(site: SiteModel, roles: List<RoleModel>) {
        // Remove previous roles for this site
        WellSql.delete(RoleModel::class.java)
                .where()
                .equals(RoleModelTable.SITE_ID, site.id)
                .endWhere().execute()
        // Insert new user roles for this site
        for (role in roles) {
            role.siteId = site.id
        }
        WellSql.insert(roles).execute()
    }

    fun getBlockLayoutCategories(site: SiteModel): List<GutenbergLayoutCategory> {
        val categories = WellSql.select(
                GutenbergLayoutCategoryModel::class.java
        )
                .where()
                .equals(GutenbergLayoutCategoryModelTable.SITE_ID, site.id)
                .endWhere().asModel
        return categories.transform()
    }

    fun getBlockLayouts(site: SiteModel): List<GutenbergLayout> {
        val blockLayouts = ArrayList<GutenbergLayout>()
        val layouts = WellSql.select(
                GutenbergLayoutModel::class.java
        )
                .where()
                .equals(GutenbergLayoutModelTable.SITE_ID, site.id)
                .endWhere().asModel
        for (layout in layouts) {
            blockLayouts.add(getGutenbergLayout(site, layout))
        }
        return blockLayouts
    }

    fun getBlockLayout(site: SiteModel, slug: String): GutenbergLayout? {
        val layoutModel = getGutenbergLayoutModel(site, slug)
        return layoutModel?.let { getGutenbergLayout(site, it) }
    }

    private fun getGutenbergLayout(site: SiteModel, layout: GutenbergLayoutModel): GutenbergLayout {
        val connections = WellSql.select(
                GutenbergLayoutCategoriesModel::class.java
        )
                .where()
                .equals(
                        GutenbergLayoutCategoriesModelTable.SITE_ID,
                        site.id
                )
                .equals(
                        GutenbergLayoutCategoriesModelTable.LAYOUT_ID,
                        layout.id
                )
                .endWhere().asModel
        val categories = ArrayList<GutenbergLayoutCategoryModel>()
        for (connection in connections) {
            categories.addAll(
                    WellSql.select(GutenbergLayoutCategoryModel::class.java)
                            .where()
                            .equals(GutenbergLayoutCategoriesModelTable.ID, connection.categoryId)
                            .endWhere().asModel
            )
        }
        return layout.transform(categories)
    }

    private fun getGutenbergLayoutModel(
        site: SiteModel,
        slug: String
    ): GutenbergLayoutModel? {
        val layouts = WellSql.select(
                GutenbergLayoutModel::class.java
        )
                .where()
                .equals(GutenbergLayoutModelTable.SITE_ID, site.id)
                .equals(GutenbergLayoutModelTable.SLUG, slug)
                .endWhere().asModel
        return if (layouts.size == 1) {
            layouts[0]
        } else null
    }

    fun getBlockLayoutContent(site: SiteModel, slug: String): String? {
        val layout = getGutenbergLayoutModel(site, slug)
        return layout?.content
    }

    fun insertOrReplaceBlockLayouts(
        site: SiteModel,
        categories: List<GutenbergLayoutCategory>,
        layouts: List<GutenbergLayout>
    ) {
        // Update categories
        WellSql.delete(GutenbergLayoutCategoryModel::class.java)
                .where()
                .equals(GutenbergLayoutCategoryModelTable.SITE_ID, site.id)
                .endWhere().execute()
        WellSql.insert(categories.transform(site)).execute()
        // Update layouts
        WellSql.delete(GutenbergLayoutModel::class.java)
                .where()
                .equals(GutenbergLayoutModelTable.SITE_ID, site.id)
                .endWhere().execute()
        WellSql.insert(layouts.transform(site)).execute()
        // Update connections
        WellSql.delete(GutenbergLayoutCategoriesModel::class.java)
                .where()
                .equals(GutenbergLayoutCategoriesModelTable.SITE_ID, site.id)
                .endWhere().execute()
        WellSql.insert<GutenbergLayoutCategoriesModel>(layouts.connections(site)).execute()
    }

    /**
     * Removes all sites from local database with the following criteria:
     * 1. Site is a WP.com -or- Jetpack connected site
     * 2. Site has no local-only data (posts/pages/drafts)
     * 3. Remote site ID does not match a site ID found in given sites list
     *
     * @param sites
     * list of sites to keep in local database
     */
    @Suppress("NestedBlockDepth")
    fun removeWPComRestSitesAbsentFromList(postSqlUtils: PostSqlUtils, sites: List<SiteModel>): Int {
        // get all local WP.com+Jetpack sites
        val localSites = WellSql.select(SiteModel::class.java)
                .where()
                .equals(SiteModelTable.ORIGIN, SiteModel.ORIGIN_WPCOM_REST)
                .endWhere().asModel
        if (localSites.size > 0) {
            // iterate through all local WP.com+Jetpack sites
            val localIterator = localSites.iterator()
            while (localIterator.hasNext()) {
                val localSite = localIterator.next()

                // don't remove sites with local changes
                if (postSqlUtils.getSiteHasLocalChanges(localSite)) {
                    localIterator.remove()
                } else {
                    // don't remove local site if the remote ID matches a given site's ID
                    for (site in sites) {
                        if (site.siteId == localSite.siteId) {
                            localIterator.remove()
                            break
                        }
                    }
                }
            }

            // delete applicable sites
            for (site in localSites) {
                deleteSite(site)
            }
        }
        return localSites.size
    }

    /**
     * Given a (remote) site id, returns the corresponding (local) id.
     */
    fun getLocalIdForRemoteSiteId(siteId: Long): Int {
        val sites = WellSql.select(SiteModel::class.java)
                .where().beginGroup()
                .equals(SiteModelTable.SITE_ID, siteId)
                .or()
                .equals(SiteModelTable.SELF_HOSTED_SITE_ID, siteId)
                .endGroup().endWhere()
                .getAsModel(this::toSiteModel)
        return if (sites.size > 0) {
            sites[0].id
        } else 0
    }

    private fun toSiteModel(cursor: Cursor): SiteModel {
        val siteModel = SiteModel()
        siteModel.id = cursor.getInt(cursor.getColumnIndexOrThrow(SiteModelTable.ID))
        return siteModel.decryptAPIRestCredentials()
    }

    /**
     * Given a (remote) self-hosted site id and XML-RPC url, returns the corresponding (local) id.
     */
    fun getLocalIdForSelfHostedSiteIdAndXmlRpcUrl(selfHostedSiteId: Long, xmlRpcUrl: String?): Int {
        val sites = WellSql.select(SiteModel::class.java)
                .where().beginGroup()
                .equals(SiteModelTable.SELF_HOSTED_SITE_ID, selfHostedSiteId)
                .equals(SiteModelTable.XMLRPC_URL, xmlRpcUrl)
                .endGroup().endWhere()
                .getAsModel(this::toSiteModel)
        return if (sites.size > 0) {
            sites[0].id
        } else 0
    }

    /**
     * Given a (local) id, returns the (remote) site id. Searches first for .COM and Jetpack, then looks for self-hosted
     * sites.
     */
    fun getSiteIdForLocalId(id: Int): Long {
        val result = WellSql.select(SiteModel::class.java)
                .where().beginGroup()
                .equals(SiteModelTable.ID, id)
                .endGroup().endWhere()
                .getAsModel { cursor ->
                    val siteModel = SiteModel()
                    siteModel.siteId = cursor.getInt(
                            cursor.getColumnIndexOrThrow(SiteModelTable.SITE_ID)
                    ).toLong()
                    siteModel.selfHostedSiteId = cursor.getLong(
                            cursor.getColumnIndexOrThrow(SiteModelTable.SELF_HOSTED_SITE_ID)
                    )
                    siteModel
                }
        if (result.isEmpty()) {
            return 0
        }
        return if (result[0].siteId > 0) {
            result[0].siteId
        } else {
            result[0].selfHostedSiteId
        }
    }

    @Suppress("ReturnCount")
    private fun SiteModel.encryptAPIRestCredentials(): SiteModel {
        // If already encrypted, do nothing
        if (!apiRestUsernameEncrypted.isNullOrEmpty() && !apiRestPasswordEncrypted.isNullOrEmpty()) {
            return this
        }
        // If the plain credentials are empty, there's nothing to encrypt
        if (apiRestUsernamePlain.isNullOrEmpty() || apiRestPasswordPlain.isNullOrEmpty()) {
            return this
        }
        val userNameEncryption = encryptionUtils.encrypt(apiRestUsernamePlain)
        apiRestUsernameEncrypted = userNameEncryption.first
        apiRestUsernameIV = userNameEncryption.second
        val passwordEncryption = encryptionUtils.encrypt(apiRestPasswordPlain)
        apiRestPasswordEncrypted = passwordEncryption.first
        apiRestPasswordIV = passwordEncryption.second
        return this
    }

    private fun List<SiteModel>.decryptAPIRestCredentials(): List<SiteModel> {
        return this.map { it.decryptAPIRestCredentials() }
    }

    @Suppress("ReturnCount", "ComplexCondition")
    private fun SiteModel.decryptAPIRestCredentials(): SiteModel {
        // If already decrypted, do nothing
        if (!apiRestUsernamePlain.isNullOrEmpty() && !apiRestPasswordPlain.isNullOrEmpty()) {
            return this
        }
        // If the encrypted credentials — or the IVs required to decrypt them — are empty, there's nothing to
        // safely decrypt. The ciphertext/IV pairs are always written together, so a row missing any one is
        // treated as having no decryptable credentials rather than risking a decrypt failure on a blank IV.
        if (apiRestUsernameEncrypted.isNullOrEmpty() || apiRestPasswordEncrypted.isNullOrEmpty() ||
            apiRestUsernameIV.isNullOrEmpty() || apiRestPasswordIV.isNullOrEmpty()) {
            return this
        }
        // Decryption relies on the AndroidKeyStore, which can fail on some devices (e.g. an invalidated
        // key or an unavailable secure element). Because this runs inside the hot getSites() path, a
        // thrown KeyStoreException would crash the whole app, so we swallow it and leave the credentials
        // unset — the site simply behaves as if it has no stored REST credentials and can re-fetch them.
        try {
            apiRestUsernamePlain = encryptionUtils.decrypt(apiRestUsernameEncrypted, apiRestUsernameIV)
            apiRestPasswordPlain = encryptionUtils.decrypt(apiRestPasswordEncrypted, apiRestPasswordIV)
        } catch (e: GeneralSecurityException) {
            AppLog.e(DB, "Failed to decrypt API REST credentials for site $id", e)
            apiRestUsernamePlain = null
            apiRestPasswordPlain = null
        }
        return this
    }
}
