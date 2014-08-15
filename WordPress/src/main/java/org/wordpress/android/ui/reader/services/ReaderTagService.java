package org.wordpress.android.ui.reader.services;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagList;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.JSONUtil;

import java.util.Iterator;

/*
 * background service for keeping list of followed tags updated
 */

public class ReaderTagService extends ReaderBaseService {

    public static void startService(Context context) {
        Intent intent = new Intent(context, ReaderTagService.class);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(AppLog.T.READER, "reader tag service > created");
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.READER, "reader tag service > destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateTags();
        return START_NOT_STICKY;
    }

    private void updateTags() {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdateTagsResponse(jsonObject);
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.READER, volleyError);
            }
        };
        AppLog.d(AppLog.T.READER, "reader tag service > updating tags");
        WordPress.getRestClientUtils().get("read/menu", null, null, listener, errorListener);
    }

    private void handleUpdateTagsResponse(final JSONObject jsonObject) {
        if (jsonObject==null) {
            return;
        }

        new Thread() {
            @Override
            public void run() {
                // get server topics, both default & followed
                ReaderTagList serverTopics = new ReaderTagList();
                serverTopics.addAll(parseTags(jsonObject, "default", ReaderTagType.DEFAULT));
                serverTopics.addAll(parseTags(jsonObject, "subscribed", ReaderTagType.FOLLOWED));

                // parse topics from the response, detect whether they're different from local
                ReaderTagList localTopics = new ReaderTagList();
                localTopics.addAll(ReaderTagTable.getDefaultTags());
                localTopics.addAll(ReaderTagTable.getFollowedTags());

                if (!localTopics.isSameList(serverTopics)) {
                    AppLog.d(AppLog.T.READER, "reader tag service > followed topics changed");
                    // if any local topics have been removed from the server, make sure to delete
                    // them locally (including their posts)
                    deleteTags(localTopics.getDeletions(serverTopics));
                    // now replace local topics with the server topics
                    ReaderTagTable.replaceTags(serverTopics);
                    // broadcast the fact that there are changes
                    sendLocalBroadcast(new Intent().setAction(ReaderServiceActions.ACTION_FOLLOWED_TAGS_CHANGED));
                }

                // save changes to recommended topics
                ReaderTagList serverRecommended = parseTags(jsonObject, "recommended", ReaderTagType.RECOMMENDED);
                ReaderTagList localRecommended = ReaderTagTable.getRecommendedTags(false);
                if (!serverRecommended.isSameList(localRecommended)) {
                    AppLog.d(AppLog.T.READER, "reader tag service > recommended topics changed");
                    ReaderTagTable.setRecommendedTags(serverRecommended);
                    sendLocalBroadcast(new Intent().setAction(ReaderServiceActions.ACTION_RECOMMENDED_TAGS_CHANGED));
                }
            }
        }.start();
    }

    /*
     * parse a specific topic section from the topic response
     */
    private static ReaderTagList parseTags(JSONObject jsonObject, String name, ReaderTagType topicType) {
        ReaderTagList topics = new ReaderTagList();

        if (jsonObject == null) {
            return topics;
        }

        JSONObject jsonTopics = jsonObject.optJSONObject(name);
        if (jsonTopics == null) {
            return topics;
        }

        Iterator<String> it = jsonTopics.keys();
        while (it.hasNext()) {
            String internalName = it.next();
            JSONObject jsonTopic = jsonTopics.optJSONObject(internalName);
            if (jsonTopic!=null) {
                String tagName = JSONUtil.getStringDecoded(jsonTopic, "title");
                String endpoint = JSONUtil.getString(jsonTopic, "URL");
                topics.add(new ReaderTag(tagName, endpoint, topicType));
            }
        }

        return topics;
    }

    private static void deleteTags(ReaderTagList tagList) {
        if (tagList == null || tagList.size() == 0) {
            return;
        }

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        try {
            for (ReaderTag tag: tagList) {
                ReaderTagTable.deleteTag(tag);
                ReaderPostTable.deletePostsWithTag(tag);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
