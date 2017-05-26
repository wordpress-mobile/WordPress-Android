package org.wordpress.android.ui.publicize.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.PublicizeTable;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.models.PublicizeConnectionList;
import org.wordpress.android.models.PublicizeServiceList;
import org.wordpress.android.ui.publicize.PublicizeConstants;
import org.wordpress.android.ui.publicize.PublicizeEvents;
import org.wordpress.android.util.AppLog;

import de.greenrobot.event.EventBus;

/**
 * service which requests the user's available sharing services and publicize connections
 */

public class PublicizeUpdateService extends IntentService {

    private static boolean mHasUpdatedServices;

    /*
     * update the publicize connections for the passed site
     */
    public static void updateConnectionsForSite(Context context, @NonNull SiteModel site) {
        Intent intent = new Intent(context, PublicizeUpdateService.class);
        intent.putExtra(WordPress.SITE, site);
        context.startService(intent);
    }

    public PublicizeUpdateService() {
        super("PublicizeUpdateService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(AppLog.T.SHARING, "publicize service > created");
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.SHARING, "publicize service > destroyed");
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;

        // update list of services if we haven't done so yet - only done once per session
        // since it rarely changes
        if (!mHasUpdatedServices || PublicizeTable.getNumServices() == 0) {
            updateServices();
            AppLog.d(AppLog.T.SHARING, "publicize service > updating services");
            mHasUpdatedServices = true;
        }

        SiteModel site = (SiteModel) intent.getSerializableExtra(WordPress.SITE);
        updateConnections(site.getSiteId());
    }

    /*
     * update the list of publicize services
     */
    private void updateServices() {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                PublicizeServiceList serverList = PublicizeServiceList.fromJson(jsonObject);
                PublicizeServiceList localList = PublicizeTable.getServiceList();
                if (!serverList.isSameAs(localList)) {
                    PublicizeTable.setServiceList(serverList);
                    EventBus.getDefault().post(new PublicizeEvents.ConnectionsChanged());
                }
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.SHARING, volleyError);
            }
        };

        String path = "/meta/external-services?type=publicize";
        WordPress.getRestClientUtilsV1_1().get(path, null, null, listener, errorListener);
    }

    /*
     * update the connections for the passed blog
     */
    private void updateConnections(final long siteId) {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                PublicizeConnectionList serverList = PublicizeConnectionList.fromJson(jsonObject);
                PublicizeConnectionList localList = PublicizeTable.getConnectionsForSite(siteId);
                if (!serverList.isSameAs(localList)) {
                    PublicizeTable.setConnectionsForSite(siteId, serverList);
                    EventBus.getDefault().post(new PublicizeEvents.ConnectionsChanged());
                }
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.SHARING, volleyError);
            }
        };

        String path = String.format("sites/%d/publicize-connections", siteId);
        WordPress.getRestClientUtilsV1_1().get(path, null, null, listener, errorListener);
    }
}
