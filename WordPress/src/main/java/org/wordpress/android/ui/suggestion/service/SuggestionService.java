package org.wordpress.android.ui.suggestion.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.wordpress.rest.RestRequest;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.UserSuggestionTable;
import org.wordpress.android.models.UserSuggestion;
import org.wordpress.android.models.Tag;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;
import java.util.List;

public class SuggestionService extends Service {
    private static final RequestThrottler<Long> SUGGESTION_REQUEST_THROTTLER = new RequestThrottler<>();
    private static final RequestThrottler<Long> TAG_REQUEST_THROTTLER = new RequestThrottler<>();

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

    public void update(final long siteId) {
        boolean currentlyRequestingSuggestions = mCurrentlyRequestingSuggestionsSiteIds.contains(siteId);
        boolean suggestionsAreStale = SUGGESTION_REQUEST_THROTTLER.areResultsStale(siteId);
        if (!currentlyRequestingSuggestions && suggestionsAreStale) {
            updateSuggestions(siteId);
        } else {
            String reason = currentlyRequestingSuggestions
                    ? "a suggestions request is already in progress."
                    : "the suggestions were recently updated.";
            AppLog.d(T.SUGGESTION, "Skipping suggestion update for site " + siteId + " because " + reason);
        }

        boolean currentlyRequestingTags = mCurrentlyRequestingTagsSiteIds.contains(siteId);
        boolean tagsAreStale = TAG_REQUEST_THROTTLER.areResultsStale(siteId);
        if (!currentlyRequestingTags && tagsAreStale) {
            updateTags(siteId);
        } else {
            String reason = currentlyRequestingTags
                    ? "a tags request is already in progress."
                    : "the tags were recently updated.";
            AppLog.d(T.SUGGESTION, "Skipping tags update for site " + siteId + " because " + reason);
        }
    }

    // We're still using `siteId` here and not a SiteModel, because suggestions the endpoint works on any site, not only
    // on sites the user is a member of. Username suggestions are used in the Reader to auto complete usernames
    // when replying to comments.
    private void updateSuggestions(final long siteId) {
        mCurrentlyRequestingSuggestionsSiteIds.add(siteId);
        RestRequest.Listener listener = jsonObject -> {
            handleSuggestionsUpdatedResponse(siteId, jsonObject);
            SUGGESTION_REQUEST_THROTTLER.onResponseReceived(siteId);
            removeSiteIdFromSuggestionRequestsAndStopServiceIfNecessary(siteId);
        };
        RestRequest.ErrorListener errorListener = volleyError -> {
            AppLog.e(AppLog.T.SUGGESTION, volleyError);
            removeSiteIdFromSuggestionRequestsAndStopServiceIfNecessary(siteId);
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
                List<UserSuggestion> suggestions = UserSuggestion.suggestionListFromJSON(jsonSuggestions, siteId);
                if (suggestions != null) {
                    UserSuggestionTable.insertSuggestionsForSite(siteId, suggestions);
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

    private void updateTags(final long siteId) {
        mCurrentlyRequestingTagsSiteIds.add(siteId);
        RestRequest.Listener listener = jsonObject -> {
            handleTagsUpdatedResponse(siteId, jsonObject);
            TAG_REQUEST_THROTTLER.onResponseReceived(siteId);
            removeSiteIdFromTagRequestsAndStopServiceIfNecessary(siteId);
        };
        RestRequest.ErrorListener errorListener = volleyError -> {
            AppLog.e(AppLog.T.SUGGESTION, volleyError);
            removeSiteIdFromTagRequestsAndStopServiceIfNecessary(siteId);
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
                    UserSuggestionTable.insertTagsForSite(siteId, tags);
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
