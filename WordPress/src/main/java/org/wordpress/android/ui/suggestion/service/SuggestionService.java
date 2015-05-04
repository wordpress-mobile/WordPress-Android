package org.wordpress.android.ui.suggestion.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.SuggestionTable;
import org.wordpress.android.models.Suggestion;
import org.wordpress.android.models.Tag;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class SuggestionService extends Service {
    private final IBinder mBinder = new SuggestionBinder();
    private final List<Integer> mCurrentlyRequestingSuggestionsSiteIds = new ArrayList<Integer>();
    private final List<Integer> mCurrentlyRequestingTagsSiteIds = new ArrayList<Integer>();

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
        if (mCurrentlyRequestingSuggestionsSiteIds.contains(remoteBlogId)) {
            return;
        }
        mCurrentlyRequestingSuggestionsSiteIds.add(remoteBlogId);
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleSuggestionsUpdatedResponse(remoteBlogId, jsonObject);
                removeSiteIdFromSuggestionRequestsAndStopServiceIfNecessary(remoteBlogId);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.SUGGESTION, volleyError);
                removeSiteIdFromSuggestionRequestsAndStopServiceIfNecessary(remoteBlogId);
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
                    EventBus.getDefault().post(new SuggestionEvents.SuggestionNameListUpdated(remoteBlogId));
                }
            }
        }.start();
    }

    private void removeSiteIdFromSuggestionRequestsAndStopServiceIfNecessary(Integer remoteBlogId) {
        mCurrentlyRequestingSuggestionsSiteIds.remove(remoteBlogId);

        // if there are no requests being made, we want to stop the service
        if (mCurrentlyRequestingSuggestionsSiteIds.isEmpty()) {
            AppLog.d(AppLog.T.SUGGESTION, "stopping suggestion service");
            stopSelf();
        }
    }

    public void updateTags(final int remoteBlogId) {
        if (mCurrentlyRequestingTagsSiteIds.contains(remoteBlogId)) {
            return;
        }
        mCurrentlyRequestingTagsSiteIds.add(remoteBlogId);
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleTagsUpdatedResponse(remoteBlogId, jsonObject);
                removeSiteIdFromTagRequestsAndStopServiceIfNecessary(remoteBlogId);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.SUGGESTION, volleyError);
                removeSiteIdFromTagRequestsAndStopServiceIfNecessary(remoteBlogId);
            }
        };

        AppLog.d(AppLog.T.SUGGESTION, "suggestion service > updating tags for siteId: " + remoteBlogId);
        String path = "/sites/" + remoteBlogId + "/tags";
        WordPress.getRestClientUtils().get(path, listener, errorListener);
    }

    private void handleTagsUpdatedResponse(final int remoteBlogId, final JSONObject jsonObject) {
        new Thread() {
            @Override
            public void run() {
                if (jsonObject == null) {
                    return;
                }

                JSONArray jsonTags = jsonObject.optJSONArray("tags");
                List<Tag> tags = Tag.tagListFromJSON(jsonTags, remoteBlogId);
                if (tags != null) {
                    SuggestionTable.insertTagsForSite(remoteBlogId, tags);
                    EventBus.getDefault().post(new SuggestionEvents.SuggestionTagListUpdated(remoteBlogId));
                }
            }
        }.start();
    }

    private void removeSiteIdFromTagRequestsAndStopServiceIfNecessary(Integer remoteBlogId) {
        mCurrentlyRequestingTagsSiteIds.remove(remoteBlogId);

        // if there are no requests being made, we want to stop the service
        if (mCurrentlyRequestingTagsSiteIds.isEmpty()) {
            AppLog.d(AppLog.T.SUGGESTION, "stopping suggestion service");
            stopSelf();
        }
    }

    public class SuggestionBinder extends Binder {
        public SuggestionService getService() {
            return SuggestionService.this;
        }
    }
}
