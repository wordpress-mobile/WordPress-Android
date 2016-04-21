package org.wordpress.android.ui.reader.actions;

import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagList;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.VolleyUtils;

public class ReaderTagActions {
    public enum TagAction {ADD, DELETE}

    private ReaderTagActions() {
        throw new AssertionError();
    }

    /**
     * perform the passed action on the passed tag - this is optimistic (returns before API call completes)
     **/
    public static boolean performTagAction(final ReaderTag tag,
                                           final TagAction action,
                                           final ReaderActions.ActionListener actionListener) {
        if (tag == null) {
            if (actionListener != null) {
                actionListener.onActionResult(false);
            }
            return false;
        }

        // don't allow actions on default tags
        if (tag.tagType == ReaderTagType.DEFAULT) {
            AppLog.w(T.READER, "cannot add or delete default tag");
            if (actionListener != null) {
                actionListener.onActionResult(false);
            }
            return false;
        }

        final String path;
        final String tagNameForApi = ReaderUtils.sanitizeWithDashes(tag.getTagSlug());

        switch (action) {
            case DELETE:
                ReaderTagTable.deleteTag(tag);
                path = "read/tags/" + tagNameForApi + "/mine/delete";
                break;

            case ADD :
                String endpoint = "/read/tags/" + tagNameForApi + "/posts";
                ReaderTag newTopic = new ReaderTag(
                        tag.getTagSlug(),
                        tag.getTagDisplayName(),
                        tag.getTagTitle(),
                        endpoint,
                        ReaderTagType.FOLLOWED);
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
                // if we're adding a tag, the response will contain the list of the
                // user's followed tags
                if (action == TagAction.ADD) {
                    ReaderTagList tags = parseFollowedTags(jsonObject);
                    ReaderTagTable.replaceFollowedTags(tags);
                }
                if (actionListener != null) {
                    actionListener.onActionResult(true);
                }
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
                    if (actionListener != null) {
                        actionListener.onActionResult(true);
                    }
                    return;
                }

                AppLog.w(T.READER, "tag action " + action.name() + " failed");
                AppLog.e(T.READER, volleyError);

                // revert on failure
                switch (action) {
                    case DELETE:
                        // add back original tag
                        ReaderTagTable.addOrUpdateTag(tag);
                        break;
                    case ADD:
                        // remove new topic
                        ReaderTagTable.deleteTag(tag);
                        break;
                }

                if (actionListener != null) {
                    actionListener.onActionResult(false);
                }
            }
        };
        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener);

        return true;
    }

    /*
     * return the user's followed tags from the response to read/tags/{tag}/mine/new
     */
    /*
        {
        "added_tag": "84776",
        "subscribed": true,
        "tags": [
            {
                "display_name": "未分類",
                "ID": "1982",
                "slug": "%e6%9c%aa%e5%88%86%e9%a1%9e",
                "title": "未分類",
                "URL": "https://public-api.wordpress.com/rest/v1.1/read/tags/%e6%9c%aa%e5%88%86%e9%a1%9e/posts"
            },
            {
                "display_name": "fitness",
                "ID": "5189",
                "slug": "fitness",
                "title": "Fitness",
                "URL": "https://public-api.wordpress.com/rest/v1.1/read/tags/fitness/posts"
            },
        }
     */
    private static ReaderTagList parseFollowedTags(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        JSONArray jsonTags = jsonObject.optJSONArray("tags");
        if (jsonTags == null || jsonTags.length() == 0) {
            return null;
        }

        ReaderTagList tags = new ReaderTagList();
        for (int i=0; i < jsonTags.length(); i++) {
            JSONObject jsonThisTag = jsonTags.optJSONObject(i);
            String tagTitle = JSONUtils.getStringDecoded(jsonThisTag, "title");
            String tagDisplayName = JSONUtils.getStringDecoded(jsonThisTag, "display_name");
            String tagSlug = JSONUtils.getStringDecoded(jsonThisTag, "slug");
            String endpoint = JSONUtils.getString(jsonThisTag, "URL");
            tags.add(new ReaderTag(tagSlug, tagDisplayName, tagTitle, endpoint, ReaderTagType.FOLLOWED));
        }

        return tags;
    }
}
