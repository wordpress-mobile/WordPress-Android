package org.wordpress.android.fluxc.persistence;

import android.content.ContentValues;
import android.database.sqlite.SQLiteConstraintException;

import androidx.annotation.NonNull;

import com.wellsql.generated.AccountModelTable;
import com.wellsql.generated.PostFormatModelTable;
import com.wellsql.generated.RoleModelTable;
import com.wellsql.generated.SiteModelTable;
import com.yarolegovich.wellsql.SelectQuery;
import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.mapper.InsertMapper;

import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.RoleModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.UrlUtils;

import java.util.Iterator;
import java.util.List;

public class SiteSqlUtils {
    public static class DuplicateSiteException extends Exception {
        private static final long serialVersionUID = -224883903136726226L;
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
     * 4. Exists in the DB, originally a WP.com REST site, and matches by XMLRPC_URL -> THROW a DuplicateSiteException
     * 5. Exists in the DB, originally an XML-RPC site, and matches by XMLRPC_URL -> UPDATE
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
                        .equals(SiteModelTable.URL, site.getUrl())
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
                // Four possibilities here:
                // 1. DB site is WP.com, new site is WP.com:
                // Something really weird is happening, this should have been caught earlier --> DuplicateSiteException
                // 2. DB site is WP.com, new site is XML-RPC:
                // It looks like an existing Jetpack-connected site over the REST API was added again as an XML-RPC
                // Wed don't allow this --> DuplicateSiteException
                // 3. DB site is XML-RPC, new site is WP.com:
                // Upgrading a self-hosted site to Jetpack --> proceed
                // 4. DB site is XML-RPC, new site is XML-RPC:
                // An existing self-hosted site was logged-into again, and we couldn't identify it by URL or
                // by WP.com site ID + URL --> proceed
                if (siteResult.get(0).getOrigin() == SiteModel.ORIGIN_WPCOM_REST) {
                    AppLog.d(T.DB, "Site is a duplicate");
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

    public static List<RoleModel> getUserRoles(@NonNull SiteModel site) {
        return WellSql.select(RoleModel.class)
                .where()
                .equals(RoleModelTable.SITE_ID, site.getId())
                .endWhere().getAsModel();
    }

    public static void insertOrReplaceUserRoles(@NonNull SiteModel site, @NonNull List<RoleModel> roles) {
        // Remove previous roles for this site
        WellSql.delete(RoleModel.class)
                .where()
                .equals(RoleModelTable.SITE_ID, site.getId())
                .endWhere().execute();
        // Insert new user roles for this site
        for (RoleModel role : roles) {
            role.setSiteId(site.getId());
        }
        WellSql.insert(roles).execute();
    }

    /**
     * Removes all sites from local database with the following criteria:
     * 1. Site is a WP.com -or- Jetpack connected site
     * 2. Site has no local-only data (posts/pages/drafts)
     * 3. Remote site ID does not match a site ID found in given sites list
     *
     * @param sites
     *  list of sites to keep in local database
     */
    public static int removeWPComRestSitesAbsentFromList(PostSqlUtils postSqlUtils, @NonNull List<SiteModel> sites) {
        // get all local WP.com+Jetpack sites
        List<SiteModel> localSites = WellSql.select(SiteModel.class)
                .where()
                .equals(SiteModelTable.ORIGIN, SiteModel.ORIGIN_WPCOM_REST)
                .endWhere().getAsModel();

        if (localSites.size() > 0) {
            // iterate through all local WP.com+Jetpack sites
            Iterator<SiteModel> localIterator = localSites.iterator();
            while (localIterator.hasNext()) {
                SiteModel localSite = localIterator.next();

                // don't remove sites with local changes
                if (postSqlUtils.getSiteHasLocalChanges(localSite)) {
                    localIterator.remove();
                } else {
                    // don't remove local site if the remote ID matches a given site's ID
                    for (SiteModel site : sites) {
                        if (site.getSiteId() == localSite.getSiteId()) {
                            localIterator.remove();
                            break;
                        }
                    }
                }
            }

            // delete applicable sites
            for (SiteModel site : localSites) {
                deleteSite(site);
            }
        }

        return localSites.size();
    }
}
