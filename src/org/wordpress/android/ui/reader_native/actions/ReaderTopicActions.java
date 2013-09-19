package org.wordpress.android.ui.reader_native.actions;

import android.os.Handler;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderTopicTable;
import org.wordpress.android.models.ReaderTopic;
import org.wordpress.android.models.ReaderTopic.ReaderTopicType;
import org.wordpress.android.models.ReaderTopicList;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.ReaderLog;
import org.wordpress.android.util.VolleyUtils;

import java.util.Iterator;

/**
 * Created by nbradbury on 8/13/13.
 */
public class ReaderTopicActions {

    public enum TopicAction {ADD, DELETE}

    private ReaderTopicActions() {
        throw new AssertionError();
    }

    /**
     * perform the passed action on the passed topic - this is optimistic (returns before API call completes)
     **/
    public static boolean performTopicAction(final TopicAction action,
                                             final String topicName,
                                             final ReaderActions.ActionListener actionListener) {
        if (TextUtils.isEmpty(topicName))
            return false;

        final ReaderTopic originalTopic;
        final String path;
        final String topicNameForApi = sanitizeTitle(topicName);

        switch (action) {
            case DELETE:
                originalTopic = ReaderTopicTable.getTopic(topicName);
                if (originalTopic==null)
                    return false;
                // delete topic & all posts in this topic
                ReaderTopicTable.deleteTopic(topicName);
                ReaderPostTable.deletePostsInTopic(topicName);
                path = "read/topics/" + topicNameForApi + "/mine/delete";
                break;

            case ADD :
                originalTopic = null; // prevent compiler warning
                ReaderTopic newTopic = new ReaderTopic();
                newTopic.setTopicName(topicName);
                newTopic.topicType = ReaderTopicType.SUBSCRIBED;
                newTopic.setEndpoint("/read/topics/" + topicNameForApi + "/posts");
                ReaderTopicTable.addOrUpdateTopic(newTopic);
                path = "read/topics/" + topicNameForApi + "/mine/new";
                break;

            default :
                return false;
        }

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                ReaderLog.i("topic action " + action.name() + " succeeded");
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
                boolean isSuccess = (action== TopicAction.ADD    && error.equals("already_subscribed"))
                                 || (action== TopicAction.DELETE && error.equals("not_subscribed"));
                if (isSuccess) {
                    ReaderLog.w("topic action " + action.name() + " succeeded with error " + error);
                    if (actionListener!=null)
                        actionListener.onActionResult(true);
                    return;
                }

                ReaderLog.w("topic action " + action.name() + " failed");
                ReaderLog.e(volleyError);

                // revert on failure
                switch (action) {
                    case DELETE:
                        // add back original topic
                        ReaderTopicTable.addOrUpdateTopic(originalTopic);
                        break;
                    case ADD:
                        // remove new topic
                        ReaderTopicTable.deleteTopic(topicName);
                        break;
                }

                if (actionListener!=null)
                    actionListener.onActionResult(false);
            }
        };
        WordPress.restClient.post(path, listener, errorListener);

        return true;
    }


    /*
     * returns the passed topicName formatted for use with our API
     * see sanitize_title_with_dashes in http://core.trac.wordpress.org/browser/tags/3.6/wp-includes/formatting.php#L0
     */
    private static String sanitizeTitle(final String topicName) {
        if (topicName==null)
            return "";

        // remove ampersands, replace spaces & periods with dashes
        String sanitized = topicName.replace("&", "")
                                    .replace(" ", "-")
                                    .replace(".", "-");

        // replace double dashes with single dash (may have been added above)
        while (sanitized.contains("--"))
            sanitized = sanitized.replace("--", "-");

        return sanitized.trim();
    }

    /**
     * update list of reader topics from the server
     **/
    public static void updateTopics(final ReaderActions.UpdateResultListener resultListener) {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleUpdateTopicsResponse(jsonObject, resultListener);
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                ReaderLog.e(volleyError);
                if (resultListener!=null)
                    resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);
            }
        };
        ReaderLog.d("updating reader topics");
        WordPress.restClient.get("read/menu", null, null, listener, errorListener);
    }
    private static void handleUpdateTopicsResponse(final JSONObject jsonObject, final ReaderActions.UpdateResultListener resultListener) {
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
                // get server topics, both default & subscribed
                ReaderTopicList serverTopics = new ReaderTopicList();
                serverTopics.addAll(parseTopics(jsonObject, "default", ReaderTopicType.DEFAULT));
                serverTopics.addAll(parseTopics(jsonObject, "subscribed", ReaderTopicType.SUBSCRIBED));

                // parse topics from the response, detect whether they're different from local
                ReaderTopicList localTopics = new ReaderTopicList();
                localTopics.addAll(ReaderTopicTable.getDefaultTopics());
                localTopics.addAll(ReaderTopicTable.getSubscribedTopics());
                final boolean hasChanges = !localTopics.isSameList(serverTopics);

                if (hasChanges) {
                    // if any local topics have been removed from the server, make sure to delete
                    // them locally (including their posts)
                    deleteTopics(localTopics.getDeletions(serverTopics));
                    // now replace local topics with the server topics
                    ReaderTopicTable.replaceTopics(serverTopics);
                }

                // save changes to recommended topics
                ReaderTopicList serverRecommended = parseTopics(jsonObject, "recommended", ReaderTopicType.RECOMMENDED);
                ReaderTopicList localRecommended = ReaderTopicTable.getRecommendedTopics(false);
                if (!serverRecommended.isSameList(localRecommended)) {
                    ReaderLog.d("recommended topics changed");
                    ReaderTopicTable.setRecommendedTopics(serverRecommended);
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
    private static ReaderTopicList parseTopics(JSONObject jsonObject, String name, ReaderTopicType topicType) {
        ReaderTopicList topics = new ReaderTopicList();

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
                ReaderTopic topic = new ReaderTopic();
                topic.topicType = topicType;
                topic.setTopicName(JSONUtil.getStringDecoded(jsonTopic, "title"));
                topic.setEndpoint(JSONUtil.getString(jsonTopic, "URL"));
                topics.add(topic);
            }
        }

        return topics;
    }

    private static void deleteTopics(ReaderTopicList topics) {
        if (topics==null || topics.size()==0)
            return;
        ReaderDatabase.getWritableDb().beginTransaction();
        try {
            for (ReaderTopic topic: topics) {
                ReaderTopicTable.deleteTopic(topic.getTopicName());
                ReaderPostTable.deletePostsInTopic(topic.getTopicName());
            }
            ReaderDatabase.getWritableDb().setTransactionSuccessful();
        } finally {
            ReaderDatabase.getWritableDb().endTransaction();
        }
    }



}
