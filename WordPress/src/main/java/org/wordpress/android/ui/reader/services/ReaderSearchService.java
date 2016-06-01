package org.wordpress.android.ui.reader.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderPostList;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.ReaderEvents;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;

import de.greenrobot.event.EventBus;

/**
 * service which searches for reader posts on wordpress.com
 */

public class ReaderSearchService extends Service {

    private static final String ARG_QUERY   = "query";
    private static final String ARG_OFFSET  = "offset";

    public static void startService(Context context, @NonNull String query, int offset) {
        Intent intent = new Intent(context, ReaderSearchService.class);
        intent.putExtra(ARG_QUERY, query);
        intent.putExtra(ARG_OFFSET, offset);
        context.startService(intent);
    }

    public static void stopService(Context context) {
        if (context == null) return;

        Intent intent = new Intent(context, ReaderSearchService.class);
        context.stopService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(AppLog.T.READER, "reader search service > created");
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.READER, "reader search service > destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        String query = intent.getStringExtra(ARG_QUERY);
        int offset = intent.getIntExtra(ARG_OFFSET, 0);
        startSearch(query, offset);

        return START_NOT_STICKY;
    }

    private void startSearch(final String query, final int offset) {
        String path = "read/search?q="
                + UrlUtils.urlEncode(query)
                + "&number=" + ReaderConstants.READER_MAX_SEARCH_POSTS_TO_REQUEST
                + "&offset=" + offset
                + "&meta=site,likes";

        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null) {
                    handleSearchResponse(query, offset, jsonObject);
                } else {
                    EventBus.getDefault().post(new ReaderEvents.SearchPostsEnded(query, offset, false));
                }
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.READER, volleyError);
                EventBus.getDefault().post(new ReaderEvents.SearchPostsEnded(query, offset, false));
            }
        };

        AppLog.d(AppLog.T.READER, "reader search service > starting search for " + query);
        EventBus.getDefault().post(new ReaderEvents.SearchPostsStarted(query, offset));
        WordPress.getRestClientUtilsV1_2().get(path, null, null, listener, errorListener);
    }

    private static void handleSearchResponse(final String query, final int offset, final JSONObject jsonObject) {
        new Thread() {
            @Override
            public void run() {
                ReaderPostList serverPosts = ReaderPostList.fromJson(jsonObject);
                if (ReaderPostTable.comparePosts(serverPosts).isNewOrChanged()) {
                    ReaderPostTable.addOrUpdatePosts(getTagForSearchQuery(query), serverPosts);
                }
                EventBus.getDefault().post(new ReaderEvents.SearchPostsEnded(query, offset, true));
            }
        }.start();
    }

    /*
     * used when storing search results in the reader post table
     */
    public static ReaderTag getTagForSearchQuery(@NonNull String query) {
        String trimQuery = query != null ? query.trim() : "";
        String slug = ReaderUtils.sanitizeWithDashes(trimQuery);
        return new ReaderTag(slug, trimQuery, trimQuery, null, ReaderTagType.SEARCH);
    }
}
