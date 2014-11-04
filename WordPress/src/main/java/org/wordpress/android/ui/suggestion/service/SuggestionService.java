package org.wordpress.android.ui.suggestion.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.SparseArray;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.SuggestionTable;
import org.wordpress.android.models.Suggestion;
import org.wordpress.android.util.AppLog;

import java.util.List;

public class SuggestionService extends Service {
    private final IBinder mBinder = new SuggestionBinder();

    // broadcast action to notify clients when summary data has changed
    public static final String ACTION_SUGGESTIONS_LIST_UPDATED = "SUGGESTIONS_LIST_UPDATED";
    public static final String SUGGESTIONS_LIST_UPDATED_EXTRA = "SUGGESTIONS_LIST_UPDATED_EXTRA";

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(AppLog.T.SUGGESTION, "service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.SUGGESTION, "service destroyed");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void updateSuggestions(final int remoteBlogId) {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleSuggestionsUpdatedResponse(remoteBlogId, jsonObject);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.SUGGESTION, volleyError);
            }
        };

        AppLog.d(AppLog.T.SUGGESTION, "suggestion service > updating suggestions for siteId: " + remoteBlogId);
        String path = "/users/suggest" + "?site_id=" + remoteBlogId;
        WordPress.getRestClientUtils().get(path, listener, errorListener);
    }

    private void handleSuggestionsUpdatedResponse(final int remoteBlogId, final JSONObject jsonObject) {
        new Thread() {
            @Override
            public void run() {
                if (jsonObject == null) {
                    return;
                }

                JSONArray jsonSuggestions = jsonObject.optJSONArray("suggestions");
                List<Suggestion> suggestions = Suggestion.suggestionListFromJSON(jsonSuggestions, remoteBlogId);
                if (suggestions != null) {
                    SuggestionTable.insertSuggestionsForSite(remoteBlogId, suggestions);

                    LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(WordPress.getContext());
                    Intent intent = new Intent(SuggestionService.ACTION_SUGGESTIONS_LIST_UPDATED);
                    intent.putExtra(SuggestionService.SUGGESTIONS_LIST_UPDATED_EXTRA, remoteBlogId);
                    lbm.sendBroadcast(intent);
                }
            }
        }.start();
    }

    public class SuggestionBinder extends Binder {
        public SuggestionService getService() {
            return SuggestionService.this;
        }
    }
}
