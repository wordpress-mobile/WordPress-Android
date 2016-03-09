package org.wordpress.android.stores.store;

import android.content.ContentValues;
import android.database.Cursor;

import com.squareup.otto.Subscribe;
import com.wellsql.generated.SiteModelTable;
import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.mapper.InsertMapper;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.Payload;
import org.wordpress.android.stores.action.Action;
import org.wordpress.android.stores.action.IAction;
import org.wordpress.android.stores.action.SiteAction;
import org.wordpress.android.stores.model.SiteModel;
import org.wordpress.android.stores.model.SitesModel;
import org.wordpress.android.stores.network.rest.wpcom.site.SiteRestClient;
import org.wordpress.android.stores.network.xmlrpc.site.SiteXMLRPCClient;
import org.wordpress.android.stores.persistence.SiteSqlUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.List;

import javax.inject.Inject;

/**
 * SQLite based only. There is no in memory copy of mapped data, everything is queried from the DB.
 */
public class SiteStore extends Store {
    // Payloads
    public static class RefreshSitesXMLRPCPayload implements Payload {
        public RefreshSitesXMLRPCPayload() {}
        public String username;
        public String password;
        public String xmlrpcEndpoint;
    }

    // OnChanged Events
    public class OnSiteChanged extends OnChanged {}

    private SiteRestClient mSiteRestClient;
    private SiteXMLRPCClient mSiteXMLRPCClient;

    @Inject
    public SiteStore(Dispatcher dispatcher, SiteRestClient siteRestClient, SiteXMLRPCClient siteXMLRPCClient) {
        super(dispatcher);
        mSiteRestClient = siteRestClient;
        mSiteXMLRPCClient = siteXMLRPCClient;
    }

    @Override
    public void onRegister() {
        AppLog.d(T.API, "SiteStore onRegister");
        // TODO: I'm really not sure about emitting OnChange event here.
        emitChange(new OnSiteChanged());
    }

    public List<SiteModel> getSites() {
        return WellSql.select(SiteModel.class).getAsModel();
    }

    public Cursor getSitesCursor() {
        return WellSql.select(SiteModel.class).getAsCursor();
    }

    public int getSitesCount() {
        return getSitesCursor().getCount();
    }

    public boolean hasSite() {
        return getSitesCount() != 0;
    }

    public SiteModel getSiteByLocalId(int id) {
        return WellSql.selectUnique(SiteModel.class)
                .where().equals(SiteModelTable.ID, id).endWhere()
                .getAsModel().get(0);
    }

    public int getDotComSitesCount() {
        return WellSql.select(SiteModel.class)
                .where().equals(SiteModelTable.IS_WPCOM, 1).endWhere()
                .getAsCursor().getCount();
    }

    public boolean hasDotComSite() {
        return getDotComSitesCount() != 0;
    }

    public int getSelfHostedSitesCount() {
        return WellSql.select(SiteModel.class)
                .where().equals(SiteModelTable.IS_WPCOM, 0).endWhere()
                .getAsCursor().getCount();
    }

    public boolean hasSelfHostedSite() {
        return getSelfHostedSitesCount() != 0;
    }

    public int getJetpackSitesCount() {
        return WellSql.select(SiteModel.class)
                .where().equals(SiteModelTable.IS_JETPACK, 1).endWhere()
                .getAsCursor().getCount();
    }

    public boolean hasJetpackSite() {
        return getJetpackSitesCount() != 0;
    }

    public boolean hasSiteWithSiteId(int siteId, String xmlRpcUrl) {
        return WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.SITE_ID, siteId)
                .equals(SiteModelTable.X_MLRPC_URL, xmlRpcUrl)
                .endGroup().endWhere()
                .getAsCursor().getCount() > 0;
    }

    public boolean hasSiteWithLocalId(int id) {
        return WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.ID, id)
                .endGroup().endWhere()
                .getAsCursor().getCount() > 0;
    }

    public List<SiteModel> getVisibleDotComSites() {
        return WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.IS_WPCOM, 1)
                .equals(SiteModelTable.IS_VISIBLE, 1)
                .endGroup().endWhere()
                .getAsModel();
    }

    public int getVisibleDotComSitesCount() {
        return getVisibleDotComSites().size();
    }

    public void setAllDotComSitesVisibility(boolean visible) {
        WellSql.update(SiteModel.class)
                .where().equals(SiteModelTable.IS_WPCOM, 1).endWhere()
                .put(visible, new InsertMapper<Boolean>() {
                    @Override
                    public ContentValues toCv(Boolean item) {
                        ContentValues cv = new ContentValues();
                        cv.put(SiteModelTable.IS_VISIBLE, item);
                        return cv;
                    }
                }).execute();
    }

    public void setDotComSiteVisibilityByLocalId(int id, boolean visible) {
        WellSql.update(SiteModel.class)
                .whereId(id)
                .where().equals(SiteModelTable.IS_WPCOM, 1).endWhere()
                .put(visible, new InsertMapper<Boolean>() {
                    @Override
                    public ContentValues toCv(Boolean item) {
                        ContentValues cv = new ContentValues();
                        cv.put(SiteModelTable.IS_VISIBLE, item);
                        return cv;
                    }
                }).execute();
    }

    public boolean isDotComSiteVisibleByLocalId(int id) {
        return WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.ID, id)
                .equals(SiteModelTable.IS_WPCOM, 1)
                .equals(SiteModelTable.IS_VISIBLE, 1)
                .endGroup().endWhere()
                .getAsCursor().getCount() > 0;
    }

    @Subscribe
    @Override
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (actionType == SiteAction.UPDATE_SITE) {
            SiteSqlUtils.insertOrUpdateSite((SiteModel) action.getPayload());
            // Would be great to send an event only if the site actually changed.
            emitChange(new OnSiteChanged());
        } else if (actionType == SiteAction.UPDATE_SITES) {
            createOrUpdateSites((SitesModel) action.getPayload());
            // Would be great to send an event only if a site actually changed.
            emitChange(new OnSiteChanged());
        } else if (actionType == SiteAction.FETCH_SITES) {
            mSiteRestClient.pullSites();
        } else if (actionType == SiteAction.FETCH_SITES_XMLRPC) {
            RefreshSitesXMLRPCPayload payload = (RefreshSitesXMLRPCPayload) action.getPayload();
            mSiteXMLRPCClient.pullSites(payload.xmlrpcEndpoint, payload.username, payload.password);
        } else if (actionType == SiteAction.FETCH_SITE) {
            SiteModel site = (SiteModel) action.getPayload();
            if (site.isWPCom() || site.isJetpack()) {
                mSiteRestClient.pullSite(site);
            } else {
                // TODO: check for WP-REST-API plugin and use it here
                mSiteXMLRPCClient.pullSite(site);
            }
        }
    }

    private void createOrUpdateSites(SitesModel sites) {
        for (SiteModel site : sites) {
            SiteSqlUtils.insertOrUpdateSite(site);
        }
    }
}
