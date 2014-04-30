package org.wordpress.android.ui.reader.actions;

import android.os.Handler;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTag.ReaderTagType;
import org.wordpress.android.models.ReaderTagList;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.VolleyUtils;

import java.util.Iterator;

public class ReaderTagActions {

    public enum TagAction {ADD, DELETE}

    private ReaderTagActions() {
        throw new AssertionError();
    }

    protected static String getReadEndpointForTag(final String tagName) {
        return String.format("/read/tags/%s/posts", sanitizeTitle(tagName));
    }

    /**
     * perform the passed action on the passed tag - this is optimistic (returns before API call completes)
     **/
    public static boolean performTagAction(final TagAction action,
                                           final String tagName,
                                           final ReaderActions.ActionListener actionListener) {
        if (TextUtils.isEmpty(tagName))
            return false;

        final ReaderTag originalTopic;
        final String path;
        final String tagNameForApi = sanitizeTitle(tagName);

        switch (action) {
            case DELETE:
                originalTopic = ReaderTagTable.getTag(tagName);
                // delete tag & all related posts
                ReaderTagTable.deleteTag(tagName);
                ReaderPostTable.deletePostsWithTag(tagName);
                path = "read/tags/" + tagNameForApi + "/mine/delete";
                break;

            case ADD :
                originalTopic = null; // prevent compiler warning
                String endpoint = "/read/tags/" + tagNameForApi + "/posts";
                ReaderTag newTopic = new ReaderTag(tagName, endpoint, ReaderTagType.FOLLOWED);
                ReaderTagTable.addOrUpdateTag(newTopic);
                path = "read/tags/" + tagNameForApi + "/mine/new";
                break;

            default :
                return false;
        }

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                AppLog.i(T.READER, "tag action " + action.name() + " succeeded");
                if (actionListener!=null)
                    actionListener.onActionResult(true);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                // if we're adding a topic and the error says the user is already following
                // this topic, or we're removing a topic and the error says the user isn't
                // following it, treat it as a success - this can happen if the user edits
                // topics in the web reader while this app is running
                String error = VolleyUtils.errStringFromVolleyError(volleyError);
                boolean isSuccess = (action== TagAction.ADD    && error.equals("already_subscribed"))
                                 || (action== TagAction.DELETE && error.equals("not_subscribed"));
                if (isSuccess) {
                    AppLog.w(T.READER, "tag action " + action.name() + " succeeded with error " + error);
                    if (actionListener!=null)
                        actionListener.onActionResult(true);
                    return;
                }

                AppLog.w(T.READER, "tag action " + action.name() + " failed");
                AppLog.e(T.READER, volleyError);

                // revert on failure
                switch (action) {
                    case DELETE:
                        // add back original topic
                        if (originalTopic != null) {
                            ReaderTagTable.addOrUpdateTag(originalTopic);
                        }
                        break;
                    case ADD:
                        // remove new topic
                        ReaderTagTable.deleteTag(tagName);
                        break;
                }

                if (actionListener!=null)
                    actionListener.onActionResult(false);
            }
        };
        WordPress.getRestClientUtils().post(path, listener, errorListener);

        return true;
    }

    /*
     * returns the passed tagName formatted for use with our API
     * see sanitize_title_with_dashes in http://core.trac.wordpress.org/browser/tags/3.6/wp-includes/formatting.php#L0
     */
    static String sanitizeTitle(final String tagName) {
        if (tagName == null) {
            return "";
        }

        // remove ampersands and number signs, replace spaces & periods with dashes
        String sanitized = tagName.replace("&", "")
                                  .replace("#", "")
                                  .replace(" ", "-")
                                  .replace(".", "-");

        // replace double dashes with single dash (may have been added above)
        while (sanitized.contains("--"))
            sanitized = sanitized.replace("--", "-");

        return sanitized.trim();
    }

    /**
     * update list of reader tags from the server
     **/
    public static void updateTags(final ReaderActions.UpdateResultListener resultListener) {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdateTagsResponse(jsonObject, resultListener);
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                if (resultListener!=null)
                    resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);
            }
        };
        AppLog.d(T.READER, "updating reader tags");
        WordPress.getRestClientUtils().get("read/menu", null, null, listener, errorListener);
    }
    private static void handleUpdateTagsResponse(final JSONObject jsonObject, final ReaderActions.UpdateResultListener resultListener) {
        if (jsonObject==null) {
            if (resultListener!=null)
                resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);
            return;
        }

        // create handler on main thread, process & store response in separate thread
        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                // get server topics, both default & followed
                ReaderTagList serverTopics = new ReaderTagList();
                serverTopics.addAll(parseTags(jsonObject, "default", ReaderTag.ReaderTagType.DEFAULT));
                serverTopics.addAll(parseTags(jsonObject, "subscribed", ReaderTag.ReaderTagType.FOLLOWED));

                // parse topics from the response, detect whether they're different from local
                ReaderTagList localTopics = new ReaderTagList();
                localTopics.addAll(ReaderTagTable.getDefaultTags());
                localTopics.addAll(ReaderTagTable.getFollowedTags());
                final boolean hasChanges = !localTopics.isSameList(serverTopics);

                if (hasChanges) {
                    // if any local topics have been removed from the server, make sure to delete
                    // them locally (including their posts)
                    deleteTags(localTopics.getDeletions(serverTopics));
                    // now replace local topics with the server topics
                    ReaderTagTable.replaceTags(serverTopics);
                }

                // save changes to recommended topics
                ReaderTagList serverRecommended = parseTags(jsonObject, "recommended", ReaderTag.ReaderTagType.RECOMMENDED);
                ReaderTagList localRecommended = ReaderTagTable.getRecommendedTags(false);
                if (!serverRecommended.isSameList(localRecommended)) {
                    AppLog.d(T.READER, "recommended topics changed");
                    ReaderTagTable.setRecommendedTags(serverRecommended);
                }

                // listener must run on the main thread
                if (resultListener!=null) {
                    handler.post(new Runnable() {
                        public void run() {
                            resultListener.onUpdateResult(hasChanges ? ReaderActions.UpdateResult.CHANGED : ReaderActions.UpdateResult.UNCHANGED);
                        }
                    });
                }
            }
        }.start();
    }

    /*
     * parse a specific topic section from the topic response
     */
    private static ReaderTagList parseTags(JSONObject jsonObject, String name, ReaderTag.ReaderTagType topicType) {
        ReaderTagList topics = new ReaderTagList();

        if (jsonObject==null)
            return topics;

        JSONObject jsonTopics = jsonObject.optJSONObject(name);
        if (jsonTopics==null)
            return topics;

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

    private static void deleteTags(ReaderTagList topics) {
        if (topics==null || topics.size()==0)
            return;
        ReaderDatabase.getWritableDb().beginTransaction();
        try {
            for (ReaderTag topic: topics) {
                ReaderTagTable.deleteTag(topic.getTagName());
                ReaderPostTable.deletePostsWithTag(topic.getTagName());
            }
            ReaderDatabase.getWritableDb().setTransactionSuccessful();
        } finally {
            ReaderDatabase.getWritableDb().endTransaction();
        }
    }



}
