package org.wordpress.android.stores.store;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

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

    public class OnSitesDeleted extends OnChanged {
        public SitesModel mSites;

        public OnSitesDeleted(SiteModel site) {
            mSites = new SitesModel();
            mSites.add(site);
        }

        public OnSitesDeleted(SitesModel sites) {
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
        return WellSql.selectUnique(SiteModel.class)
                .where().equals(SiteModelTable.ID, id).endWhere()
                .getAsModel().get(0);
    }

    /**
     * Returns the number of .COM sites in the store.
     */
    public int getDotComSitesCount() {
        return WellSql.select(SiteModel.class)
                .where().equals(SiteModelTable.IS_WPCOM, 1).endWhere()
                .getAsCursor().getCount();
    }

    /**
     * Checks whether the store contains at least one .COM site.
     */
    public boolean hasDotComSite() {
        return getDotComSitesCount() != 0;
    }

    /**
     * Returns the number of self-hosted sites (can be Jetpack) in the store.
     */
    public int getSelfHostedSitesCount() {
        return WellSql.select(SiteModel.class)
                .where().equals(SiteModelTable.IS_WPCOM, 0).endWhere()
                .getAsCursor().getCount();
    }

    /**
     * Checks whether the store contains at least one self-hosted site (can be Jetpack).
     */
    public boolean hasSelfHostedSite() {
        return getSelfHostedSitesCount() != 0;
    }

    /**
     * Returns the number of Jetpack sites in the store.
     */
    public int getJetpackSitesCount() {
        return WellSql.select(SiteModel.class)
                .where().equals(SiteModelTable.IS_JETPACK, 1).endWhere()
                .getAsCursor().getCount();
    }

    /**
     * Checks whether the store contains at least one Jetpack site.
     */
    public boolean hasJetpackSite() {
        return getJetpackSitesCount() != 0;
    }

    /**
     * Checks whether the store contains a site matching the given (remote) site id and XML-RPC URL.
     */
    public boolean hasSiteWithSiteIdAndXmlRpcUrl(long siteId, String xmlRpcUrl) {
        return WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.SITE_ID, siteId)
                .equals(SiteModelTable.X_MLRPC_URL, xmlRpcUrl)
                .endGroup().endWhere()
                .getAsCursor().getCount() > 0;
    }

    /**
     * Checks whether the store contains a site matching the given (local) id.
     */
    public boolean hasSiteWithLocalId(int id) {
        return WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.ID, id)
                .endGroup().endWhere()
                .getAsCursor().getCount() > 0;
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
                .where().beginGroup()
                .equals(SiteModelTable.SITE_ID, siteId)
                .equals(SiteModelTable.IS_ADMIN, 1)
                .endGroup().endWhere()
                .getAsCursor().getCount() > 0;
    }

    /**
     * Given a (remote) site id, returns the corresponding (local) id.
     */
    public int getLocalIdForRemoteSiteId(long siteId) {
        List<SiteModel> sites =  WellSql.select(SiteModel.class)
                .where().equals(SiteModelTable.SITE_ID, siteId).endWhere()
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
        } else {
            return getLocalIdForJetpackRemoteId(siteId, null);
        }
    }

    /**
     * Given a (remote) site id and XML-RPC url, returns the corresponding (local) id.
     */
    public int getLocalIdForRemoteSiteIdAndXmlRpcUrl(long siteId, String xmlRpcUrl) {
        int localId =  WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.SITE_ID, siteId)
                .equals(SiteModelTable.X_MLRPC_URL, xmlRpcUrl)
                .endGroup().endWhere()
                .getAsModel(new SelectMapper<SiteModel>() {
                    @Override
                    public SiteModel convert(Cursor cursor) {
                        SiteModel siteModel = new SiteModel();
                        siteModel.setId(cursor.getInt(cursor.getColumnIndex(SiteModelTable.ID)));
                        return siteModel;
                    }
                }).get(0).getId();

        if (localId == 0) {
            localId = getLocalIdForJetpackRemoteId(siteId, xmlRpcUrl);
        }

        return localId;
    }

    /**
     * Given a (remote) Jetpack id and optional XML-RPC URL, returns the corresponding (local) id.
     */
    public int getLocalIdForJetpackRemoteId(long jetpackRemoteId, String xmlRpcUrl) {
        List<SiteModel> sites;
        if (TextUtils.isEmpty(xmlRpcUrl)) {
            sites = WellSql.select(SiteModel.class)
                    .where().beginGroup()
                    .equals(SiteModelTable.IS_WPCOM, 0)
                    .equals(SiteModelTable.DOT_COM_ID_FOR_JETPACK, jetpackRemoteId)
                    .endGroup().endWhere()
                    .getAsModel(new SelectMapper<SiteModel>() {
                        @Override
                        public SiteModel convert(Cursor cursor) {
                            SiteModel siteModel = new SiteModel();
                            siteModel.setId(cursor.getInt(cursor.getColumnIndex(SiteModelTable.ID)));
                            return siteModel;
                        }
                    });
        } else {
            sites = WellSql.select(SiteModel.class)
                    .where().beginGroup()
                    .equals(SiteModelTable.IS_WPCOM, 0)
                    .equals(SiteModelTable.DOT_COM_ID_FOR_JETPACK, jetpackRemoteId)
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
        }

        if (sites.size() > 0) {
            return sites.get(0).getId();
        }

        return 0;
    }

    /**
     * Given a (local) id, returns the (remote) site id. Searches first for self-hosted and .COM, then looks for Jetpack
     * sites.
     */
    public long getSiteIdForLocalId(int id) {
        SiteModel result =  WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.ID, id)
                .endGroup().endWhere()
                .getAsModel(new SelectMapper<SiteModel>() {
                    @Override
                    public SiteModel convert(Cursor cursor) {
                        SiteModel siteModel = new SiteModel();
                        siteModel.setSiteId(cursor.getInt(cursor.getColumnIndex(SiteModelTable.SITE_ID)));
                        siteModel.setDotComIdForJetpack(cursor.getLong(cursor.getColumnIndex(
                                SiteModelTable.DOT_COM_ID_FOR_JETPACK)));
                        return siteModel;
                    }
                }).get(0);

        if (result.getSiteId() > 0) {
            return result.getSiteId();
        } else {
            return result.getDotComIdForJetpack();
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
    public SiteModel getSiteByDotComSiteId(long dotComSiteId) {
        return WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.DOT_COM_ID_FOR_JETPACK, dotComSiteId)
                .or()
                .beginGroup()
                .equals(SiteModelTable.SITE_ID, dotComSiteId)
                .equals(SiteModelTable.IS_WPCOM, 1)
                .endGroup()
                .endGroup().endWhere()
                .getAsModel().get(0);
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
        } else if (actionType == SiteAction.DELETE_SITE) {
            SiteSqlUtils.deleteSite((SiteModel) action.getPayload());
            // TODO: This should be captured by 'QuickPressShortcutsStore' so it can handle deleting any QP shortcuts
            // TODO: Probably, we can inject QuickPressShortcutsStore into SiteStore and act on it directly
            // See WordPressDB.deleteQuickPressShortcutsForLocalTableBlogId(Context ctx, int blogId)
            emitChange(new OnSitesDeleted((SiteModel) action.getPayload()));
        } else if (actionType == SiteAction.DELETE_WPCOM_SITES) {
            SitesModel wpComSites = SiteSqlUtils.getAllSitesWith(SiteModelTable.IS_WPCOM, 1);
            deleteSites(wpComSites);
            // TODO: Same as above, this needs to be captured and handled by QuickPressShortcutsStore
            emitChange(new OnSitesDeleted(wpComSites));
        }
    }

    private void createOrUpdateSites(SitesModel sites) {
        for (SiteModel site : sites) {
            SiteSqlUtils.insertOrUpdateSite(site);
        }
    }

    private void deleteSites(SitesModel sites) {
        for (SiteModel site : sites) {
            SiteSqlUtils.deleteSite(site);
        }
    }
}
