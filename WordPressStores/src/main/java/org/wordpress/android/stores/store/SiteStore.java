package org.wordpress.android.stores.store;

import android.database.Cursor;

import com.squareup.otto.Subscribe;
import com.wellsql.generated.SiteModelTable;
import com.yarolegovich.wellsql.WellSql;

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
        return WellSql.select(SiteModel.class).getAsCursor().getCount();
    }

    public boolean hasSelfHostedSite() {
        return WellSql.select(SiteModel.class)
                .where().equals(SiteModelTable.IS_WPCOM, false).endWhere()
                .getAsCursor().getCount() != 0;
    }

    public boolean hasSite() {
        return WellSql.select(SiteModel.class).getAsCursor().getCount() != 0;
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
