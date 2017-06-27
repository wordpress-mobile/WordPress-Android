package org.wordpress.android.fluxc.persistence;

import android.content.ContentValues;
import android.database.sqlite.SQLiteConstraintException;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.wellsql.generated.AccountModelTable;
import com.wellsql.generated.PostFormatModelTable;
import com.wellsql.generated.SiteModelTable;
import com.yarolegovich.wellsql.SelectQuery;
import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.mapper.InsertMapper;

import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.UrlUtils;

import java.util.List;

public class SiteSqlUtils {
    public static class DuplicateSiteException extends Exception {
    }

    public static SelectQuery<SiteModel> getSitesWith(String field, Object value) {
        return WellSql.select(SiteModel.class)
                .where().equals(field, value).endWhere();
    }

    public static SelectQuery<SiteModel> getSitesWith(String field, boolean value) {
        return WellSql.select(SiteModel.class)
                .where().equals(field, value).endWhere();
    }

    public static List<SiteModel> getSitesAccessedViaWPComRestByNameOrUrlMatching(String searchString) {
        // Note: by default SQLite "LIKE" operator is case insensitive, and that's what we're looking for.
        return WellSql.select(SiteModel.class).where()
                // ORIGIN = ORIGIN_WPCOM_REST AND (x in url OR x in name)
                .equals(SiteModelTable.ORIGIN, SiteModel.ORIGIN_WPCOM_REST)
                .beginGroup()
                .contains(SiteModelTable.URL, searchString)
                .or().contains(SiteModelTable.NAME, searchString)
                .endGroup().endWhere().getAsModel();
    }

    public static List<SiteModel> getSitesByNameOrUrlMatching(String searchString) {
        return WellSql.select(SiteModel.class).where()
                .contains(SiteModelTable.URL, searchString)
                .or().contains(SiteModelTable.NAME, searchString)
                .endWhere().getAsModel();
    }

    /**
     * Inserts the given SiteModel into the DB, or updates an existing entry where sites match.
     *
     * Possible cases:
     * 1. Exists in the DB already and matches by local id (simple update) -> UPDATE
     * 2. Exists in the DB, is a Jetpack or WordPress site and matches by remote id (SITE_ID) -> UPDATE
     * 3. Exists in the DB, is a pure self hosted and matches by remote id (SITE_ID) + URL -> UPDATE
     * 4. Exists in the DB, was not a Jetpack site but is now a Jetpack site, and matches by XMLRPC_URL -> UPDATE
     * 5. Exists in the DB, and matches by XMLRPC_URL -> THROW a DuplicateSiteException
     * 6. Not matching any previous cases -> INSERT
     */
    public static int insertOrUpdateSite(SiteModel site) throws DuplicateSiteException {
        if (site == null) {
            return 0;
        }

        // If we're inserting or updating a WP.com REST API site, validate that we actually have a WordPress.com
        // AccountModel present
        // This prevents a late UPDATE_SITES action from re-populating the database after sign out from WordPress.com
        if (site.isUsingWpComRestApi()) {
            List<AccountModel> accountModel = WellSql.select(AccountModel.class)
                    .where()
                    .not().equals(AccountModelTable.USER_ID, 0)
                    .endWhere()
                    .getAsModel();
            if (accountModel.isEmpty()) {
                AppLog.w(T.DB, "Can't insert WP.com site " + site.getUrl() + ", missing user account");
                return 0;
            }
        }

        // If the site already exist and has an id, we want to update it.
        List<SiteModel> siteResult = WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.ID, site.getId())
                .endGroup().endWhere().getAsModel();
        if (!siteResult.isEmpty()) {
            AppLog.d(T.DB, "Site found by (local) ID: " + site.getId());
        }

        // Looks like a new site, make sure we don't already have it.
        if (siteResult.isEmpty()) {
            if (site.getSiteId() > 0) {
                // For WordPress.com and Jetpack sites, the WP.com ID is a unique enough identifier
                siteResult = WellSql.select(SiteModel.class)
                        .where().beginGroup()
                        .equals(SiteModelTable.SITE_ID, site.getSiteId())
                        .endGroup().endWhere().getAsModel();
                if (!siteResult.isEmpty()) {
                    AppLog.d(T.DB, "Site found by SITE_ID: " + site.getSiteId());
                }
            } else {
                siteResult = WellSql.select(SiteModel.class)
                        .where().beginGroup()
                        .equals(SiteModelTable.SITE_ID, site.getSiteId())
                        .beginGroup()
                        .equals(SiteModelTable.URL, stripTrailingSlashes(site.getUrl()))
                        .or()
                        .equals(SiteModelTable.URL, addTrailingSlash(site.getUrl()))
                        .endGroup()
                        .endGroup().endWhere().getAsModel();
                if (!siteResult.isEmpty()) {
                    AppLog.d(T.DB, "Site found by SITE_ID: " + site.getSiteId() + " and URL: " + site.getUrl());
                }
            }
        }

        // If the site is a self hosted, maybe it's already in the DB as a Jetpack site, and we don't want to create
        // a duplicate.
        if (siteResult.isEmpty()) {
            String forcedHttpXmlRpcUrl = "http://" + UrlUtils.removeScheme(site.getXmlRpcUrl());
            String forcedHttpsXmlRpcUrl = "https://" + UrlUtils.removeScheme(site.getXmlRpcUrl());

            siteResult = WellSql.select(SiteModel.class)
                    .where()
                    .beginGroup()
                    .equals(SiteModelTable.XMLRPC_URL, forcedHttpXmlRpcUrl)
                    .or().equals(SiteModelTable.XMLRPC_URL, forcedHttpsXmlRpcUrl)
                    .endGroup()
                    .endWhere().getAsModel();
            if (!siteResult.isEmpty()) {
                AppLog.d(T.DB, "Site found using XML-RPC url: " + site.getXmlRpcUrl());
                // If the site already in the DB is a self hosted and the new one is a Jetpack connected site, it means
                // we upgraded from self hosted to jetpack, we want to update the site with the new informations.
                if (siteResult.get(0).isJetpackConnected() || !site.isJetpackConnected()) {
                    AppLog.d(T.DB, "Site is a duplicate");
                    // In other cases (examples: adding the same self hosted twice or adding self hosted on top of an
                    // existing jetpack site), we consider it as an error.
                    throw new DuplicateSiteException();
                }
            }
        }

        if (siteResult.isEmpty()) {
            // No site with this local ID, REMOTE_ID + URL, or XMLRPC URL, then insert it
            AppLog.d(T.DB, "Inserting site: " + site.getUrl());
            WellSql.insert(site).asSingleTransaction(true).execute();
            return 1;
        } else {
            // Update old site
            AppLog.d(T.DB, "Updating site: " + site.getUrl());
            int oldId = siteResult.get(0).getId();
            try {
                return WellSql.update(SiteModel.class).whereId(oldId)
                        .put(site, new UpdateAllExceptId<>(SiteModel.class)).execute();
            } catch (SQLiteConstraintException e) {
                AppLog.e(T.DB, "Error while updating site: siteId=" + site.getSiteId() + " url=" + site.getUrl()
                        + " xmlrpc=" + site.getXmlRpcUrl(), e);
                // Can happen on self hosted sites with incorrect url values in wp.getOption response.
                // See https://github.com/wordpress-mobile/WordPress-FluxC-Android/issues/397
                throw new DuplicateSiteException();
            }
        }
    }

    public static int deleteSite(SiteModel site) {
        if (site == null) {
            return 0;
        }
        return WellSql.delete(SiteModel.class)
                 .where().equals(SiteModelTable.ID, site.getId()).endWhere()
                 .execute();
    }

    public static int deleteAllSites() {
        return WellSql.delete(SiteModel.class).execute();
    }

    public static int setSiteVisibility(SiteModel site, boolean visible) {
        if (site == null) {
            return 0;
        }
        return WellSql.update(SiteModel.class)
                .whereId(site.getId())
                .where().equals(SiteModelTable.IS_WPCOM, true).endWhere()
                .put(visible, new InsertMapper<Boolean>() {
                    @Override
                    public ContentValues toCv(Boolean item) {
                        ContentValues cv = new ContentValues();
                        cv.put(SiteModelTable.IS_VISIBLE, item);
                        return cv;
                    }
                }).execute();
    }

    public static SelectQuery<SiteModel> getWPComSites() {
        return WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.IS_WPCOM, true)
                .endGroup().endWhere();
    }

    /**
     * @return A selectQuery to get all the sites accessed via the XMLRPC, this includes: pure self hosted sites,
     * but also Jetpack sites connected via XMLRPC.
     */
    public static SelectQuery<SiteModel> getSitesAccessedViaXMLRPC() {
        return WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.ORIGIN, SiteModel.ORIGIN_XMLRPC)
                .endGroup().endWhere();
    }

    public static SelectQuery<SiteModel> getSitesAccessedViaWPComRest() {
        return WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.ORIGIN, SiteModel.ORIGIN_WPCOM_REST)
                .endGroup().endWhere();
    }

    public static SelectQuery<SiteModel> getVisibleSitesAccessedViaWPCom() {
        return WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.ORIGIN, SiteModel.ORIGIN_WPCOM_REST)
                .equals(SiteModelTable.IS_VISIBLE, true)
                .endGroup().endWhere();
    }

    public static List<PostFormatModel> getPostFormats(@NonNull SiteModel site) {
        return WellSql.select(PostFormatModel.class)
                .where()
                .equals(PostFormatModelTable.SITE_ID, site.getId())
                .endWhere().getAsModel();
    }

    public static void insertOrReplacePostFormats(@NonNull SiteModel site, @NonNull List<PostFormatModel> postFormats) {
        // Remove previous post formats for this site
        WellSql.delete(PostFormatModel.class)
                .where()
                .equals(PostFormatModelTable.SITE_ID, site.getId())
                .endWhere().execute();
        // Insert new post formats for this site
        for (PostFormatModel postFormat : postFormats) {
            postFormat.setSiteId(site.getId());
        }
        WellSql.insert(postFormats).execute();
    }

    private static String stripTrailingSlashes(String url) {
        if (TextUtils.isEmpty(url)) return url;

        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static String addTrailingSlash(String url) {
        if (TextUtils.isEmpty(url)) return url;

        if (!url.endsWith("/")) {
            url = url + "/";
        }
        return url;
    }
}
