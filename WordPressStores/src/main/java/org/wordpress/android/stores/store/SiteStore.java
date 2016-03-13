package org.wordpress.android.stores.store;

import android.content.ContentValues;
import android.database.Cursor;

import com.squareup.otto.Subscribe;
import com.wellsql.generated.SiteModelTable;
import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.mapper.InsertMapper;
import com.yarolegovich.wellsql.mapper.SelectMapper;

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

    public class OnSitesRemoved extends OnChanged {
        public List<SiteModel> mSites;

        public OnSitesRemoved(SiteModel site) {
            mSites = new SitesModel();
            mSites.add(site);
        }

        public OnSitesRemoved(List<SiteModel> sites) {
            mSites = sites;
        }
    }

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

    /**
     * Returns all sites in the store as a {@link SiteModel} list.
     */
    public List<SiteModel> getSites() {
        return WellSql.select(SiteModel.class).getAsModel();
    }

    /**
     * Returns all sites in the store as a {@link Cursor}.
     */
    public Cursor getSitesCursor() {
        return WellSql.select(SiteModel.class).getAsCursor();
    }

    /**
     * Returns the number of sites of any kind in the store.
     */
    public int getSitesCount() {
        return getSitesCursor().getCount();
    }

    /**
     * Checks whether the store contains any sites of any kind.
     */
    public boolean hasSite() {
        return getSitesCount() != 0;
    }

    /**
     * Obtains the site with the given (local) id and returns it as a {@link SiteModel}.
     */
    public SiteModel getSiteByLocalId(int id) {
        List<SiteModel> result = SiteSqlUtils.getAllSitesWith(SiteModelTable.ID, id);
        if (result.size() > 0) {
            return result.get(0);
        }
        return null;
    }

    /**
     * Checks whether the store contains a site matching the given (local) id.
     */
    public boolean hasSiteWithLocalId(int id) {
        return SiteSqlUtils.getNumberOfSitesWith(SiteModelTable.ID, id) > 0;
    }

    /**
     * Returns all .COM sites in the store.
     */
    public List<SiteModel> getDotComSites() {
        return SiteSqlUtils.getAllSitesWith(SiteModelTable.IS_WPCOM, 1);
    }

    /**
     * Returns the number of .COM sites in the store.
     */
    public int getDotComSitesCount() {
        return SiteSqlUtils.getNumberOfSitesWith(SiteModelTable.IS_WPCOM, 1);
    }

    /**
     * Checks whether the store contains at least one .COM site.
     */
    public boolean hasDotComSite() {
        return getDotComSitesCount() != 0;
    }

    /**
     * Returns all self-hosted sites in the store.
     */
    public List<SiteModel> getDotOrgSites() {
        return SiteSqlUtils.getAllSitesWith(SiteModelTable.IS_WPCOM, 0);
    }

    /**
     * Returns the number of self-hosted sites (can be Jetpack) in the store.
     */
    public int getDotOrgSitesCount() {
        return SiteSqlUtils.getNumberOfSitesWith(SiteModelTable.IS_WPCOM, 0);
    }

    /**
     * Checks whether the store contains at least one self-hosted site (can be Jetpack).
     */
    public boolean hasDotOrgSite() {
        return getDotOrgSitesCount() != 0;
    }

    /**
     * Returns all Jetpack sites in the store.
     */
    public List<SiteModel> getJetpackSites() {
        return SiteSqlUtils.getAllSitesWith(SiteModelTable.IS_JETPACK, 1);
    }

    /**
     * Returns the number of Jetpack sites in the store.
     */
    public int getJetpackSitesCount() {
        return SiteSqlUtils.getNumberOfSitesWith(SiteModelTable.IS_JETPACK, 1);
    }

    /**
     * Checks whether the store contains at least one Jetpack site.
     */
    public boolean hasJetpackSite() {
        return getJetpackSitesCount() != 0;
    }

    /**
     * Checks whether the store contains a self-hosted site matching the given (remote) site id and XML-RPC URL.
     */
    public boolean hasDotOrgSiteWithSiteIdAndXmlRpcUrl(long dotOrgSiteId, String xmlRpcUrl) {
        return WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.DOT_ORG_SITE_ID, dotOrgSiteId)
                .equals(SiteModelTable.X_MLRPC_URL, xmlRpcUrl)
                .endGroup().endWhere()
                .getAsCursor().getCount() > 0;
    }

    /**
     * Returns all visible sites as {@link SiteModel}s. All self-hosted sites over XML-RPC are visible by default.
     */
    public List<SiteModel> getVisibleSites() {
        return SiteSqlUtils.getAllSitesWith(SiteModelTable.IS_VISIBLE, 1);
    }

    /**
     * Returns the number of visible sites. All self-hosted sites over XML-RPC are visible by default.
     */
    public int getVisibleSitesCount() {
        return getVisibleSites().size();
    }

    /**
     * Returns all visible .COM sites as {@link SiteModel}s.
     */
    public List<SiteModel> getVisibleDotComSites() {
        return WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.IS_WPCOM, 1)
                .equals(SiteModelTable.IS_VISIBLE, 1)
                .endGroup().endWhere()
                .getAsModel();
    }

    /**
     * Returns the number of visible .COM sites.
     */
    public int getVisibleDotComSitesCount() {
        return getVisibleDotComSites().size();
    }

    /**
     * Sets the visibility of all .COM sites to the given value.
     */
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

    /**
     * Sets the visibility of the .COM site with the given (local) id to the given value.
     */
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

    /**
     * Checks whether the .COM site with the given (local) id is visible.
     */
    public boolean isDotComSiteVisibleByLocalId(int id) {
        return WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.ID, id)
                .equals(SiteModelTable.IS_WPCOM, 1)
                .equals(SiteModelTable.IS_VISIBLE, 1)
                .endGroup().endWhere()
                .getAsCursor().getCount() > 0;
    }

    /**
     * Checks whether the user is an admin of the given (remote) site id.
     */
    public boolean isCurrentUserAdminOfSiteId(long siteId) {
        return WellSql.select(SiteModel.class)
                .where().beginGroup().beginGroup()
                .equals(SiteModelTable.SITE_ID, siteId)
                .or()
                .equals(SiteModelTable.DOT_ORG_SITE_ID, siteId)
                .endGroup()
                .equals(SiteModelTable.IS_ADMIN, 1)
                .endGroup().endWhere()
                .getAsCursor().getCount() > 0;
    }

    /**
     * Given a (remote) site id, returns the corresponding (local) id.
     */
    public int getLocalIdForRemoteSiteId(long siteId) {
        List<SiteModel> sites =  WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.SITE_ID, siteId)
                .or()
                .equals(SiteModelTable.DOT_ORG_SITE_ID, siteId)
                .endGroup().endWhere()
                .getAsModel(new SelectMapper<SiteModel>() {
                    @Override
                    public SiteModel convert(Cursor cursor) {
                        SiteModel siteModel = new SiteModel();
                        siteModel.setId(cursor.getInt(cursor.getColumnIndex(SiteModelTable.ID)));
                        return siteModel;
                    }
                });
        if (sites.size() > 0) {
            return sites.get(0).getId();
        }
        return 0;
    }

    /**
     * Given a (remote) self-hosted site id and XML-RPC url, returns the corresponding (local) id.
     */
    public int getLocalIdForDotOrgSiteIdAndXmlRpcUrl(long dotOrgSiteId, String xmlRpcUrl) {
        List<SiteModel> sites =  WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.DOT_ORG_SITE_ID, dotOrgSiteId)
                .equals(SiteModelTable.X_MLRPC_URL, xmlRpcUrl)
                .endGroup().endWhere()
                .getAsModel(new SelectMapper<SiteModel>() {
                    @Override
                    public SiteModel convert(Cursor cursor) {
                        SiteModel siteModel = new SiteModel();
                        siteModel.setId(cursor.getInt(cursor.getColumnIndex(SiteModelTable.ID)));
                        return siteModel;
                    }
                });
        if (sites.size() > 0) {
            return sites.get(0).getId();
        }
        return 0;
    }

    /**
     * Given a (local) id, returns the (remote) site id. Searches first for .COM and Jetpack, then looks for self-hosted
     * sites.
     */
    public long getSiteIdForLocalId(int id) {
        List<SiteModel> result =  WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.ID, id)
                .endGroup().endWhere()
                .getAsModel(new SelectMapper<SiteModel>() {
                    @Override
                    public SiteModel convert(Cursor cursor) {
                        SiteModel siteModel = new SiteModel();
                        siteModel.setSiteId(cursor.getInt(cursor.getColumnIndex(SiteModelTable.SITE_ID)));
                        siteModel.setDotOrgSiteId(cursor.getLong(cursor.getColumnIndex(SiteModelTable.DOT_ORG_SITE_ID)));
                        return siteModel;
                    }
                });
        if (result.isEmpty()) {
            return 0;
        }

        if (result.get(0).getSiteId() > 0) {
            return result.get(0).getSiteId();
        } else {
            return result.get(0).getDotOrgSiteId();
        }
    }

    /**
     * Given a (remote) site id, returns true if the given site is WP.com or Jetpack-enabled
     * (returns false for non-Jetpack self-hosted sites).
     */
    public boolean hasDotComOrJetpackSiteWithSiteId(long siteId) {
        int localId = getLocalIdForRemoteSiteId(siteId);
        return WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.ID, localId)
                .beginGroup()
                .equals(SiteModelTable.IS_WPCOM, 1).or().equals(SiteModelTable.IS_JETPACK, 1)
                .endGroup().endGroup().endWhere()
                .getAsCursor().getCount() > 0;
    }

    /**
     * Given a .COM site ID (either a .COM site id, or the .COM id of a Jetpack site), returns the site as a
     * {@link SiteModel}.
     */
    public SiteModel getSiteBySiteId(long siteId) {
        if (siteId == 0) {
            return null;
        }

        List<SiteModel> sites = SiteSqlUtils.getAllSitesWith(SiteModelTable.SITE_ID, siteId);

        if (sites.isEmpty()) {
            return null;
        } else {
            return sites.get(0);
        }
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
        } else if (actionType == SiteAction.REMOVE_SITE) {
            SiteSqlUtils.deleteSite((SiteModel) action.getPayload());
            // TODO: This should be captured by 'QuickPressShortcutsStore' so it can handle deleting any QP shortcuts
            // TODO: Probably, we can inject QuickPressShortcutsStore into SiteStore and act on it directly
            // See WordPressDB.deleteQuickPressShortcutsForLocalTableBlogId(Context ctx, int blogId)
            emitChange(new OnSitesRemoved((SiteModel) action.getPayload()));
        } else if (actionType == SiteAction.REMOVE_WPCOM_SITES) {
            // TODO: This should also remove Jetpack sites downloaded over REST but not over XML-RPC (no dotOrgSiteId)
            List<SiteModel> wpComSites = SiteSqlUtils.getAllSitesWith(SiteModelTable.IS_WPCOM, 1);
            deleteSites(wpComSites);
            // TODO: Same as above, this needs to be captured and handled by QuickPressShortcutsStore
            emitChange(new OnSitesRemoved(wpComSites));
        }
    }

    private void createOrUpdateSites(SitesModel sites) {
        for (SiteModel site : sites) {
            SiteSqlUtils.insertOrUpdateSite(site);
        }
    }

    private void deleteSites(List<SiteModel> sites) {
        for (SiteModel site : sites) {
            SiteSqlUtils.deleteSite(site);
        }
    }
}
