package org.wordpress.android.fluxc.persistence;

import android.content.ContentValues;
import android.support.annotation.NonNull;

import com.wellsql.generated.PostFormatModelTable;
import com.wellsql.generated.SiteModelTable;
import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.mapper.InsertMapper;

import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.SiteModel;

import java.util.List;

public class SiteSqlUtils {
    public static List<SiteModel> getAllSitesWith(String field, Object value) {
        return WellSql.select(SiteModel.class)
                .where().equals(field, value).endWhere()
                .getAsModel();
    }

    public static List<SiteModel> getAllSitesWith(String field, boolean value) {
        return WellSql.select(SiteModel.class)
                .where().equals(field, value).endWhere()
                .getAsModel();
    }

    public static List<SiteModel> getAllSitesMatchingUrlOrNameWith(String field, boolean value, String searchString) {
        // Note: by default SQLite "LIKE" operator is case insensitive, and that's what we're looking for.
        return WellSql.select(SiteModel.class).where()
                .equals(field, value)
                .beginGroup() // AND ( x OR x )
                .contains(SiteModelTable.URL, searchString)
                .or().contains(SiteModelTable.NAME, searchString)
                .endGroup().endWhere().getAsModel();
    }

    public static List<SiteModel> getAllSitesMatchingUrlOrName(String searchString) {
        return WellSql.select(SiteModel.class).where()
                .contains(SiteModelTable.URL, searchString)
                .or().contains(SiteModelTable.NAME, searchString)
                .endWhere().getAsModel();
    }

    public static int getNumberOfSitesWith(String field, Object value) {
        return WellSql.select(SiteModel.class)
                .where().equals(field, value).endWhere()
                .getAsCursor().getCount();
    }

    public static int getNumberOfSitesWith(String field, boolean value) {
        return WellSql.select(SiteModel.class)
                .where().equals(field, value).endWhere()
                .getAsCursor().getCount();
    }

    public static int insertOrUpdateSite(SiteModel site) {
        if (site == null) {
            return 0;
        }

        // If the site already exist and has an id, we want to update it.
        List<SiteModel> siteResult = WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.ID, site.getId())
                .endGroup().endWhere().getAsModel();

        // Looks like a new site, make sure we don't already have it.
        if (siteResult.isEmpty()) {
            // TODO: Make the URL enough, we could get surprise with the site id with .org sites becoming jetpack sites
            siteResult = WellSql.select(SiteModel.class)
                    .where().beginGroup()
                    .equals(SiteModelTable.SITE_ID, site.getSiteId())
                    .equals(SiteModelTable.URL, site.getUrl())
                    .endGroup().endWhere().getAsModel();
        }

        if (siteResult.isEmpty()) {
            // No site with this local ID, or REMOTE_ID + URL, then insert it
            WellSql.insert(site).asSingleTransaction(true).execute();
            return 0;
        } else {
            // Update old site
            int oldId = siteResult.get(0).getId();
            return WellSql.update(SiteModel.class).whereId(oldId)
                    .put(site, new UpdateAllExceptId<SiteModel>()).execute();
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

    public static List<SiteModel> getAllWPComSites() {
        return WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.IS_WPCOM, true)
                .or()
                .beginGroup()
                .equals(SiteModelTable.IS_JETPACK, true)
                .equals(SiteModelTable.SELF_HOSTED_SITE_ID, false)
                .endGroup()
                .endGroup().endWhere()
                .getAsModel();
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
}
