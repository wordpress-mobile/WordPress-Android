package org.wordpress.android.ui.reader.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderBlogList;
import org.wordpress.android.models.ReaderRecommendBlogList;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagList;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.JSONUtil;

import java.util.EnumSet;
import java.util.Iterator;

public class ReaderUpdateService extends Service {

    /*
     * service which updates followed/recommended tags and blogs for the Reader, only
     * sends broadcast when changes are found (ie: no broadcast for update start/stop
     * or for unchanged/failed)
     */

    public static final String ACTION_FOLLOWED_TAGS_CHANGED     = "reader_followed_tags_changed";
    public static final String ACTION_RECOMMENDED_TAGS_CHANGED  = "reader_recommended_tags_changed";
    public static final String ACTION_FOLLOWED_BLOGS_CHANGED    = "reader_followed_blogs_changed";
    public static final String ACTION_RECOMMENDED_BLOGS_CHANGED = "reader_recommended_blogs_changed";

    public static enum UpdateTask {
        TAGS,
        FOLLOWED_BLOGS,
        RECOMMENDED_BLOGS
    }

    private EnumSet<UpdateTask> mCurrentTasks;

    private static final String ARG_UPDATE_TASKS = "update_tasks";

    public static void startService(Context context, EnumSet<UpdateTask> tasks) {
        if (context == null || tasks == null || tasks.size() == 0) {
            return;
        }
        Intent intent = new Intent(context, ReaderUpdateService.class);
        intent.putExtra(ARG_UPDATE_TASKS, tasks);
        context.startService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(AppLog.T.READER, "reader service > created");
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.READER, "reader service > destroyed");
        super.onDestroy();
    }

    private void sendLocalBroadcast(Intent intent) {
        if (intent != null) {
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra(ARG_UPDATE_TASKS)) {
            EnumSet<UpdateTask> tasks = (EnumSet<UpdateTask>) intent.getSerializableExtra(ARG_UPDATE_TASKS);
            performTasks(tasks);
        }

        return START_NOT_STICKY;
    }

    private void performTasks(EnumSet<UpdateTask> tasks) {
        mCurrentTasks = EnumSet.copyOf(tasks);

        // perform in priority order - we want to update tags first since without them
        // the Reader can't show anything
        if (tasks.contains(UpdateTask.TAGS)) {
            updateTags();
        }
        if (tasks.contains(UpdateTask.FOLLOWED_BLOGS)) {
            updateFollowedBlogs();
        }
        if (tasks.contains(UpdateTask.RECOMMENDED_BLOGS)) {
            updateRecommendedBlogs();
        }
    }

    private void taskCompleted(UpdateTask task, boolean didFail) {
        mCurrentTasks.remove(task);
        if (mCurrentTasks.isEmpty()) {
            allTasksCompleted();
        }
    }

    private void allTasksCompleted() {
        AppLog.i(AppLog.T.READER, "reader service > all tasks completed");
        stopSelf();
    }

    /***
     * update the tags the user is followed - also handles recommended (popular) tags since
     * they're included in the response
     */
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
                taskCompleted(UpdateTask.TAGS, true);
            }
        };
        AppLog.d(AppLog.T.READER, "reader service > updating tags");
        WordPress.getRestClientUtils().get("read/menu", null, null, listener, errorListener);
    }

    private void handleUpdateTagsResponse(final JSONObject jsonObject) {
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
                    AppLog.d(AppLog.T.READER, "reader service > followed topics changed");
                    // if any local topics have been removed from the server, make sure to delete
                    // them locally (including their posts)
                    deleteTags(localTopics.getDeletions(serverTopics));
                    // now replace local topics with the server topics
                    ReaderTagTable.replaceTags(serverTopics);
                    // broadcast the fact that there are changes
                    sendLocalBroadcast(new Intent().setAction(ACTION_FOLLOWED_TAGS_CHANGED));
                }

                // save changes to recommended topics
                ReaderTagList serverRecommended = parseTags(jsonObject, "recommended", ReaderTagType.RECOMMENDED);
                ReaderTagList localRecommended = ReaderTagTable.getRecommendedTags(false);
                if (!serverRecommended.isSameList(localRecommended)) {
                    AppLog.d(AppLog.T.READER, "reader service > recommended topics changed");
                    ReaderTagTable.setRecommendedTags(serverRecommended);
                    sendLocalBroadcast(new Intent().setAction(ACTION_RECOMMENDED_TAGS_CHANGED));
                }

                taskCompleted(UpdateTask.TAGS, false);
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


    /***
     * request the list of blogs the current user is following
     */
    void updateFollowedBlogs() {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleFollowedBlogsResponse(jsonObject);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.READER, volleyError);
                taskCompleted(UpdateTask.FOLLOWED_BLOGS, true);
            }
        };

        AppLog.d(AppLog.T.READER, "reader service > updating followed blogs");
        // request using ?meta=site,feed to get extra info
        WordPress.getRestClientUtils().get("/read/following/mine?meta=site%2Cfeed", listener, errorListener);
    }
    private void handleFollowedBlogsResponse(final JSONObject jsonObject) {
        new Thread() {
            @Override
            public void run() {
                ReaderBlogList serverBlogs = ReaderBlogList.fromJson(jsonObject);
                ReaderBlogList localBlogs = ReaderBlogTable.getFollowedBlogs();

                if (!localBlogs.isSameList(serverBlogs)) {
                    ReaderBlogTable.setFollowedBlogs(serverBlogs);
                    AppLog.d(AppLog.T.READER, "reader blogs service > followed blogs changed");
                    sendLocalBroadcast(new Intent().setAction(ACTION_FOLLOWED_BLOGS_CHANGED));
                }

                taskCompleted(UpdateTask.FOLLOWED_BLOGS, false);
            }
        }.start();
    }

    /***
     * request the latest recommended blogs, replaces all local ones
     */
    void updateRecommendedBlogs() {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleRecommendedBlogsResponse(jsonObject);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.READER, volleyError);
                taskCompleted(UpdateTask.RECOMMENDED_BLOGS, true);
            }
        };

        AppLog.d(AppLog.T.READER, "reader service > updating recommended blogs");
        String path = "/read/recommendations/mine/"
                + "?source=mobile"
                + "&number=" + Integer.toString(ReaderConstants.READER_MAX_RECOMMENDED_TO_REQUEST);
        WordPress.getRestClientUtils().get(path, listener, errorListener);
    }
    private void handleRecommendedBlogsResponse(final JSONObject jsonObject) {
        new Thread() {
            @Override
            public void run() {
                ReaderRecommendBlogList serverBlogs = ReaderRecommendBlogList.fromJson(jsonObject);
                ReaderRecommendBlogList localBlogs = ReaderBlogTable.getAllRecommendedBlogs();

                if (!localBlogs.isSameList(serverBlogs)) {
                    ReaderBlogTable.setRecommendedBlogs(serverBlogs);
                    sendLocalBroadcast(new Intent().setAction(ACTION_RECOMMENDED_BLOGS_CHANGED));
                }

                taskCompleted(UpdateTask.RECOMMENDED_BLOGS, false);
            }
        }.start();
    }

}
