package org.wordpress.android.stores.persistence;

import com.wellsql.generated.SiteModelTable;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.stores.model.SiteModel;

import java.util.List;

public class SiteSqlUtils {
    public static void insertOrUpdateSite(SiteModel site) {
        List<SiteModel> siteResult = WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.SITE_ID, site.getSiteId())
                .equals(SiteModelTable.URL, site.getUrl())
                .endGroup().endWhere().getAsModel();
        if (siteResult.isEmpty()) {
            // insert
            WellSql.insert(site).asSingleTransaction(true).execute();
        } else {
            // update
            int oldId = siteResult.get(0).getId();
            WellSql.update(SiteModel.class).whereId(oldId)
                    .put(site, new UpdateAllExceptId<SiteModel>()).execute();
        }
    }
}
