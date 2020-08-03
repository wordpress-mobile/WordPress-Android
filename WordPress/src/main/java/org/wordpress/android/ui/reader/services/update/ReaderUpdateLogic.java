package org.wordpress.android.ui.reader.services.update;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.models.ReaderBlogList;
import org.wordpress.android.models.ReaderRecommendBlogList;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagList;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.ReaderEvents;
import org.wordpress.android.ui.reader.ReaderEvents.InterestTagsFetchEnded;
import org.wordpress.android.ui.reader.services.ServiceCompletionListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.LocaleManager;

import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;

import javax.inject.Inject;

public class ReaderUpdateLogic {
    /***
     * This class holds the business logic for Reader Updates, serving both ReaderUpdateService (<API26)
     * and ReaderUpdateJobService (API26+).
     * Updates followed/recommended tags and blogs for the Reader, relies
     * on EventBus to notify of changes
     */

    public enum UpdateTask {
        TAGS,
        INTEREST_TAGS,
        FOLLOWED_BLOGS,
        RECOMMENDED_BLOGS
    }
    private static final String INTERESTS = "interests";
    private EnumSet<UpdateTask> mCurrentTasks;
    private ServiceCompletionListener mCompletionListener;
    private Object mListenerCompanion;
    private String mLanguage;
    private Context mContext;

    @Inject AccountStore mAccountStore;
    @Inject TagUpdateClientUtilsProvider mClientUtilsProvider;

    public ReaderUpdateLogic(Context context, WordPress app, ServiceCompletionListener listener) {
        mCompletionListener = listener;
        app.component().inject(this);
        mLanguage = LocaleManager.getLanguage(app);
        mContext = context;
    }

    public void performTasks(EnumSet<UpdateTask> tasks, Object companion) {
        mCurrentTasks = EnumSet.copyOf(tasks);
        mListenerCompanion = companion;

        // perform in priority order - we want to update tags first since without them
        // the Reader can't show anything
        if (tasks.contains(UpdateTask.TAGS)) {
            updateTags();
        }
        if (tasks.contains(UpdateTask.INTEREST_TAGS)) {
            fetchInterestTags();
        }
        if (tasks.contains(UpdateTask.FOLLOWED_BLOGS)) {
            updateFollowedBlogs();
        }
        if (tasks.contains(UpdateTask.RECOMMENDED_BLOGS)) {
            updateRecommendedBlogs();
        }
    }

    private void taskCompleted(UpdateTask task) {
        mCurrentTasks.remove(task);
        if (mCurrentTasks.isEmpty()) {
            allTasksCompleted();
        }
    }

    private void allTasksCompleted() {
        AppLog.i(AppLog.T.READER, "reader service > all tasks completed");
        mCompletionListener.onCompleted(mListenerCompanion);
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
                taskCompleted(UpdateTask.TAGS);
            }
        };
        AppLog.d(AppLog.T.READER, "reader service > updating tags");
        HashMap<String, String> params = new HashMap<>();
        params.put("locale", mLanguage);
        mClientUtilsProvider.getRestClientForTagUpdate()
                            .get("read/menu", params, null, listener, errorListener);
    }

    private boolean displayNameUpdateWasNeeded(ReaderTagList serverTopics) {
        boolean updateDone = false;

        for (ReaderTag tag : serverTopics) {
            String tagNameBefore = tag.getTagDisplayName();
            if (tag.isFollowedSites()) {
                tag.setTagDisplayName(mContext.getString(R.string.reader_following_display_name));
                if (!tagNameBefore.equals(tag.getTagDisplayName())) updateDone = true;
            } else if (tag.isDiscover()) {
                tag.setTagDisplayName(mContext.getString(R.string.reader_discover_display_name));
                if (!tagNameBefore.equals(tag.getTagDisplayName())) updateDone = true;
            } else if (tag.isPostsILike()) {
                tag.setTagDisplayName(mContext.getString(R.string.reader_my_likes_display_name));
                if (!tagNameBefore.equals(tag.getTagDisplayName())) updateDone = true;
            }
        }

        return updateDone;
    }

    private void handleUpdateTagsResponse(final JSONObject jsonObject) {
        new Thread() {
            @Override
            public void run() {
                // get server topics, both default & followed - but use "recommended" for logged-out
                // reader since user won't have any followed tags
                ReaderTagList serverTopics = new ReaderTagList();
                serverTopics.addAll(parseTags(jsonObject, "default", ReaderTagType.DEFAULT));

                boolean displayNameUpdateWasNeeded = displayNameUpdateWasNeeded(serverTopics);

                if (!mAccountStore.hasAccessToken() && !AppPrefs.isReaderImprovementsPhase2Enabled()) {
                    serverTopics.addAll(parseTags(jsonObject, "recommended", ReaderTagType.FOLLOWED));
                } else {
                    serverTopics.addAll(parseTags(jsonObject, "subscribed", ReaderTagType.FOLLOWED));
                }

                // manually insert Bookmark tag, as server doesn't support bookmarking yet
                // and check if we are going to change it to trigger UI update in case of downgrade
                serverTopics.add(
                        new ReaderTag(
                                "",
                                mContext.getString(R.string.reader_save_for_later_display_name),
                                mContext.getString(R.string.reader_save_for_later_title),
                                "",
                                ReaderTagType.BOOKMARKED
                        )
                );

                // parse topics from the response, detect whether they're different from local
                ReaderTagList localTopics = new ReaderTagList();
                localTopics.addAll(ReaderTagTable.getDefaultTags());
                localTopics.addAll(ReaderTagTable.getFollowedTags());
                localTopics.addAll(ReaderTagTable.getBookmarkTags());
                localTopics.addAll(ReaderTagTable.getCustomListTags());

                if (
                        !localTopics.isSameList(serverTopics)
                        || displayNameUpdateWasNeeded
                ) {
                    AppLog.d(AppLog.T.READER, "reader service > followed topics changed "
                                              + "updatedDisplayNames [" + displayNameUpdateWasNeeded + "]");

                    if (!mAccountStore.hasAccessToken() && AppPrefs.isReaderImprovementsPhase2Enabled()) {
                        // Do not delete locally saved tags for logged out user
                        ReaderTagTable.addOrUpdateTags(serverTopics);
                    } else {
                        // if any local topics have been removed from the server, make sure to delete
                        // them locally (including their posts)
                        deleteTagsAndPostsWithTags(localTopics.getDeletions(serverTopics));
                        // now replace local topics with the server topics
                        ReaderTagTable.replaceTags(serverTopics);
                    }
                    // broadcast the fact that there are changes
                    EventBus.getDefault().post(new ReaderEvents.FollowedTagsChanged(true));
                }
                AppPrefs.setReaderTagsUpdatedTimestamp(new Date().getTime());

                taskCompleted(UpdateTask.TAGS);
            }
        }.start();
    }

    /*
     * parse a specific topic section from the topic response
     */
    private static ReaderTagList parseTags(JSONObject jsonObject, String name, ReaderTagType tagType) {
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
            if (jsonTopic != null) {
                String tagTitle = JSONUtils.getStringDecoded(jsonTopic, ReaderConstants.JSON_TAG_TITLE);
                String tagDisplayName = JSONUtils.getStringDecoded(jsonTopic, ReaderConstants.JSON_TAG_DISPLAY_NAME);
                String tagSlug = JSONUtils.getStringDecoded(jsonTopic, ReaderConstants.JSON_TAG_SLUG);
                String endpoint = JSONUtils.getString(jsonTopic, ReaderConstants.JSON_TAG_URL);

                // if the endpoint contains `read/list` then this is a custom list - these are
                // included in the response as default tags
                if (tagType == ReaderTagType.DEFAULT && endpoint.contains("/read/list/")) {
                    topics.add(new ReaderTag(tagSlug, tagDisplayName, tagTitle, endpoint, ReaderTagType.CUSTOM_LIST));
                } else {
                    topics.add(new ReaderTag(tagSlug, tagDisplayName, tagTitle, endpoint, tagType));
                }
            }
        }

        return topics;
    }

    private static ReaderTagList parseInterestTags(JSONObject jsonObject) {
        ReaderTagList interestTags = new ReaderTagList();

        if (jsonObject == null) {
            return interestTags;
        }

        JSONArray jsonInterests = jsonObject.optJSONArray(INTERESTS);

        if (jsonInterests == null) {
            return interestTags;
        }

        for (int i = 0; i < jsonInterests.length(); i++) {
            JSONObject jsonInterest = jsonInterests.optJSONObject(i);
            if (jsonInterest != null) {
                String tagTitle = JSONUtils.getStringDecoded(jsonInterest, ReaderConstants.JSON_TAG_TITLE);
                String tagSlug = JSONUtils.getStringDecoded(jsonInterest, ReaderConstants.JSON_TAG_SLUG);
                interestTags.add(new ReaderTag(tagSlug, tagTitle, tagTitle, "", ReaderTagType.INTERESTS));
            }
        }

        return interestTags;
    }

    private static void deleteTagsAndPostsWithTags(ReaderTagList tagList) {
        if (tagList == null || tagList.size() == 0) {
            return;
        }
        ReaderTagTable.deleteTags(tagList);

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        try {
            for (ReaderTag tag : tagList) {
                ReaderPostTable.deletePostsWithTag(tag);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void fetchInterestTags() {
        RestRequest.Listener listener = this::handleInterestTagsResponse;
        RestRequest.ErrorListener errorListener = volleyError -> {
            AppLog.e(AppLog.T.READER, volleyError);
            EventBus.getDefault().post(new InterestTagsFetchEnded(new ReaderTagList(), false));
            taskCompleted(UpdateTask.INTEREST_TAGS);
        };

        AppLog.d(AppLog.T.READER, "reader service > fetching interest tags");

        HashMap<String, String> params = new HashMap<>();
        params.put("_locale", mLanguage);
        mClientUtilsProvider.getRestClientForInterestTags()
                            .get("read/interests", params, null, listener, errorListener);
    }

    private void handleInterestTagsResponse(final JSONObject jsonObject) {
        new Thread() {
            @Override
            public void run() {
                ReaderTagList interestTags = new ReaderTagList();
                interestTags.addAll(parseInterestTags(jsonObject));
                EventBus.getDefault().post(new InterestTagsFetchEnded(interestTags, true));
                taskCompleted(UpdateTask.INTEREST_TAGS);
            }
        }.start();
    }

    /***
     * request the list of blogs the current user is following
     */
    private void updateFollowedBlogs() {
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
                taskCompleted(UpdateTask.FOLLOWED_BLOGS);
            }
        };

        AppLog.d(AppLog.T.READER, "reader service > updating followed blogs");
        // request using ?meta=site,feed to get extra info
        WordPress.getRestClientUtilsV1_1().get("read/following/mine?meta=site%2Cfeed", listener, errorListener);
    }

    private void handleFollowedBlogsResponse(final JSONObject jsonObject) {
        new Thread() {
            @Override
            public void run() {
                ReaderBlogList serverBlogs = ReaderBlogList.fromJson(jsonObject);
                ReaderBlogList localBlogs = ReaderBlogTable.getFollowedBlogs();

                if (!localBlogs.isSameList(serverBlogs)) {
                    // always update the list of followed blogs if there are *any* changes between
                    // server and local (including subscription count, description, etc.)
                    ReaderBlogTable.setFollowedBlogs(serverBlogs);
                    // ...but only update the follow status and alert that followed blogs have
                    // changed if the server list doesn't have the same blogs as the local list
                    // (ie: a blog has been followed/unfollowed since local was last updated)
                    if (!localBlogs.hasSameBlogs(serverBlogs)) {
                        ReaderPostTable.updateFollowedStatus();
                        AppLog.i(AppLog.T.READER, "reader blogs service > followed blogs changed");
                        EventBus.getDefault().post(new ReaderEvents.FollowedBlogsChanged());
                    }
                }

                taskCompleted(UpdateTask.FOLLOWED_BLOGS);
            }
        }.start();
    }

    /***
     * request the latest recommended blogs, replaces all local ones
     */
    private void updateRecommendedBlogs() {
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
                taskCompleted(UpdateTask.RECOMMENDED_BLOGS);
            }
        };

        AppLog.d(AppLog.T.READER, "reader service > updating recommended blogs");
        String path = "read/recommendations/mine/"
                      + "?source=mobile"
                      + "&number=" + Integer.toString(ReaderConstants.READER_MAX_RECOMMENDED_TO_REQUEST);
        WordPress.getRestClientUtilsV1_1().get(path, listener, errorListener);
    }

    private void handleRecommendedBlogsResponse(final JSONObject jsonObject) {
        new Thread() {
            @Override
            public void run() {
                ReaderRecommendBlogList serverBlogs = ReaderRecommendBlogList.fromJson(jsonObject);
                ReaderRecommendBlogList localBlogs = ReaderBlogTable.getRecommendedBlogs();

                if (!localBlogs.isSameList(serverBlogs)) {
                    ReaderBlogTable.setRecommendedBlogs(serverBlogs);
                    EventBus.getDefault().post(new ReaderEvents.RecommendedBlogsChanged());
                }

                taskCompleted(UpdateTask.RECOMMENDED_BLOGS);
            }
        }.start();
    }
}
