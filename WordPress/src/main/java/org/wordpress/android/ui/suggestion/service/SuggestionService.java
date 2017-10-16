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
    private final List<Long> mCurrentlyRequestingSuggestionsSiteIds = new ArrayList<>();
    private final List<Long> mCurrentlyRequestingTagsSiteIds = new ArrayList<>();

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

    // We're still using `siteId` here and not a SiteModel, because suggestions the enpoint works on any site, not only
    // on sites the user is a member of. Username suggestions are used in the Reader to auto complete usernames
    // when replying to comments.
    public void updateSuggestions(final long siteId) {
        if (mCurrentlyRequestingSuggestionsSiteIds.contains(siteId)) {
            return;
        }
        mCurrentlyRequestingSuggestionsSiteIds.add(siteId);
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleSuggestionsUpdatedResponse(siteId, jsonObject);
                removeSiteIdFromSuggestionRequestsAndStopServiceIfNecessary(siteId);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.SUGGESTION, volleyError);
                removeSiteIdFromSuggestionRequestsAndStopServiceIfNecessary(siteId);
            }
        };

        AppLog.d(AppLog.T.SUGGESTION, "suggestion service > updating suggestions for siteId: " + siteId);
        String path = "/users/suggest" + "?site_id=" + siteId;
        WordPress.getRestClientUtils().get(path, listener, errorListener);
    }

    private void handleSuggestionsUpdatedResponse(final long siteId, final JSONObject jsonObject) {
        new Thread() {
            @Override
            public void run() {
                if (jsonObject == null) {
                    return;
                }

                JSONArray jsonSuggestions = jsonObject.optJSONArray("suggestions");
                List<Suggestion> suggestions = Suggestion.suggestionListFromJSON(jsonSuggestions, siteId);
                if (suggestions != null) {
                    SuggestionTable.insertSuggestionsForSite(siteId, suggestions);
                    EventBus.getDefault().post(new SuggestionEvents.SuggestionNameListUpdated(siteId));
                }
            }
        }.start();
    }

    private void removeSiteIdFromSuggestionRequestsAndStopServiceIfNecessary(long siteId) {
        mCurrentlyRequestingSuggestionsSiteIds.remove(siteId);

        // if there are no requests being made, we want to stop the service
        if (mCurrentlyRequestingSuggestionsSiteIds.isEmpty()) {
            AppLog.d(AppLog.T.SUGGESTION, "stopping suggestion service");
            stopSelf();
        }
    }

    public void updateTags(final long siteId) {
        if (mCurrentlyRequestingTagsSiteIds.contains(siteId)) {
            return;
        }
        mCurrentlyRequestingTagsSiteIds.add(siteId);
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleTagsUpdatedResponse(siteId, jsonObject);
                removeSiteIdFromTagRequestsAndStopServiceIfNecessary(siteId);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.SUGGESTION, volleyError);
                removeSiteIdFromTagRequestsAndStopServiceIfNecessary(siteId);
            }
        };

        AppLog.d(AppLog.T.SUGGESTION, "suggestion service > updating tags for siteId: " + siteId);
        String path = "/sites/" + siteId + "/tags";
        WordPress.getRestClientUtils().get(path, listener, errorListener);
    }

    private void handleTagsUpdatedResponse(final long siteId, final JSONObject jsonObject) {
        new Thread() {
            @Override
            public void run() {
                if (jsonObject == null) {
                    return;
                }

                JSONArray jsonTags = jsonObject.optJSONArray("tags");
                List<Tag> tags = Tag.tagListFromJSON(jsonTags, siteId);
                if (tags != null) {
                    SuggestionTable.insertTagsForSite(siteId, tags);
                    EventBus.getDefault().post(new SuggestionEvents.SuggestionTagListUpdated(siteId));
                }
            }
        }.start();
    }

    private void removeSiteIdFromTagRequestsAndStopServiceIfNecessary(long siteId) {
        mCurrentlyRequestingTagsSiteIds.remove(siteId);

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
