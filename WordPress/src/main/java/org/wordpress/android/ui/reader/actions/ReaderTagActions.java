package org.wordpress.android.ui.reader.actions;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagList;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.VolleyUtils;

public class ReaderTagActions {
    private ReaderTagActions() {
        throw new AssertionError();
    }

    public static boolean deleteTag(final ReaderTag tag,
                                    final ReaderActions.ActionListener actionListener) {
        if (tag == null) {
            ReaderActions.callActionListener(actionListener, false);
            return false;
        }

        final String tagNameForApi = ReaderUtils.sanitizeWithDashes(tag.getTagSlug());
        final String path = "read/tags/" + tagNameForApi + "/mine/delete";

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                AppLog.i(T.READER, "delete tag succeeded");
                ReaderActions.callActionListener(actionListener, true);
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                // treat it as a success if the error says the user isn't following the deleted tag
                String error = VolleyUtils.errStringFromVolleyError(volleyError);
                if (error.equals("not_subscribed")) {
                    AppLog.w(T.READER, "delete tag succeeded with error " + error);
                    ReaderActions.callActionListener(actionListener, true);
                    return;
                }

                AppLog.w(T.READER, " delete tag failed");
                AppLog.e(T.READER, volleyError);

                // add back original tag
                ReaderTagTable.addOrUpdateTag(tag);

                ReaderActions.callActionListener(actionListener, false);
            }
        };

        ReaderTagTable.deleteTag(tag);
        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener);

        return true;
    }

    public static boolean addTag(final ReaderTag tag,
                                 final ReaderActions.ActionListener actionListener) {
        if (tag == null) {
            ReaderActions.callActionListener(actionListener, false);
            return false;
        }

        final String tagNameForApi = ReaderUtils.sanitizeWithDashes(tag.getTagSlug());
        final String path = "read/tags/" + tagNameForApi + "/mine/new";
        String endpoint = "/read/tags/" + tagNameForApi + "/posts";

        ReaderTag newTag = new ReaderTag(
                tag.getTagSlug(),
                tag.getTagDisplayName(),
                tag.getTagTitle(),
                endpoint,
                ReaderTagType.FOLLOWED);

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                AppLog.i(T.READER, "add tag  succeeded");
                // the response will contain the list of the user's followed tags
                ReaderTagList tags = parseFollowedTags(jsonObject);
                ReaderTagTable.replaceFollowedTags(tags);
                ReaderActions.callActionListener(actionListener, true);
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                // treat is as a success if we're adding a tag and the error says the user is
                // already following it
                String error = VolleyUtils.errStringFromVolleyError(volleyError);
                if (error.equals("already_subscribed")) {
                    AppLog.w(T.READER, "add tag succeeded with error " + error);
                    ReaderActions.callActionListener(actionListener, true);
                    return;
                }

                AppLog.w(T.READER, "add tag failed");
                AppLog.e(T.READER, volleyError);

                // revert on failure
                ReaderTagTable.deleteTag(tag);

                ReaderActions.callActionListener(actionListener, false);
            }
        };

        ReaderTagTable.addOrUpdateTag(newTag);
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
                "display_name": "fitness",
                "ID": "5189",
                "slug": "fitness",
                "title": "Fitness",
                "URL": "https://public-api.wordpress.com/rest/v1.1/read/tags/fitness/posts"
            },
            ...
        }
     */
    private static ReaderTagList parseFollowedTags(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        JSONArray jsonTags = jsonObject.optJSONArray(ReaderConstants.JSON_TAG_TAGS_ARRAY);
        if (jsonTags == null || jsonTags.length() == 0) {
            return null;
        }

        ReaderTagList tags = new ReaderTagList();
        for (int i=0; i < jsonTags.length(); i++) {
            JSONObject jsonThisTag = jsonTags.optJSONObject(i);
            String tagTitle = JSONUtils.getStringDecoded(jsonThisTag, ReaderConstants.JSON_TAG_TITLE);
            String tagDisplayName = JSONUtils.getStringDecoded(jsonThisTag, ReaderConstants.JSON_TAG_DISPLAY_NAME);
            String tagSlug = JSONUtils.getStringDecoded(jsonThisTag, ReaderConstants.JSON_TAG_SLUG);
            String endpoint = JSONUtils.getString(jsonThisTag, ReaderConstants.JSON_TAG_URL);
            tags.add(new ReaderTag(tagSlug, tagDisplayName, tagTitle, endpoint, ReaderTagType.FOLLOWED));
        }

        return tags;
    }

}
