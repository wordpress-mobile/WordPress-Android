package org.wordpress.android.fluxc.persistence;

import android.content.ContentValues;

import com.wellsql.generated.SiteModelTable;
import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.mapper.InsertMapper;

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
        // TODO: Check if the URL is not enough, we could get surprise with the site id with .org sites becoming
        // jetpack sites
        List<SiteModel> siteResult = WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.SITE_ID, site.getSiteId())
                .equals(SiteModelTable.URL, site.getUrl())
                .endGroup().endWhere().getAsModel();
        if (siteResult.isEmpty()) {
            // insert
            WellSql.insert(site).asSingleTransaction(true).execute();
            return 0;
        } else {
            // update
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
                .equals(SiteModelTable.DOT_ORG_SITE_ID, false)
                .endGroup()
                .endGroup().endWhere()
                .getAsModel();
    }
}
