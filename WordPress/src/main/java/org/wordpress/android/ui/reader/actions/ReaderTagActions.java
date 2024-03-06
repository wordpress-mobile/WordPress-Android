package org.wordpress.android.ui.reader.actions;

import androidx.annotation.NonNull;

import com.wordpress.rest.RestRequest;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagList;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.ReaderEvents.FollowedTagsFetched;
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.VolleyUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReaderTagActions {
    private ReaderTagActions() {
        throw new AssertionError();
    }

    public static boolean deleteTag(final ReaderTag tag,
                                    final ReaderActions.ActionListener actionListener,
                                    final boolean isLoggedIn) {
        if (tag == null) {
            ReaderActions.callActionListener(actionListener, false);
            return false;
        }

        boolean result;
        if (!isLoggedIn) {
            result = deleteTagsLocallyOnly(actionListener, tag);
        } else {
            result = deleteTagsLocallyAndRemotely(actionListener, tag);
        }

        return result;
    }

    private static boolean deleteTagsLocallyOnly(ActionListener actionListener, ReaderTag tag) {
        ReaderTagTable.deleteTag(tag);
        ReaderActions.callActionListener(actionListener, true);
        EventBus.getDefault().post(new FollowedTagsFetched(true, ReaderTagTable.getFollowedTags().size()));

        return true;
    }

    private static boolean deleteTagsLocallyAndRemotely(ActionListener actionListener, ReaderTag tag) {
        final String tagNameForApi = ReaderUtils.sanitizeWithDashes(tag.getTagSlug());
        final String path = "read/tags/" + tagNameForApi + "/mine/delete";

        com.wordpress.rest.RestRequest.Listener listener = jsonObject -> {
            AppLog.i(T.READER, "delete tag succeeded");
            ReaderActions.callActionListener(actionListener, true);
        };

        RestRequest.ErrorListener errorListener = volleyError -> {
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
        };
        ReaderTagTable.deleteTag(tag);
        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener);

        return true;
    }

    public static boolean addTag(@NonNull final ReaderTag tag,
                                 final ReaderActions.ActionListener actionListener,
                                 final boolean isLoggedIn) {
        ReaderTagList tags = new ReaderTagList();
        tags.add(tag);
        return addTags(tags, actionListener, isLoggedIn);
    }

    public static boolean addTags(@NonNull final List<ReaderTag> tags,
                                  final boolean isLoggedIn) {
        return addTags(tags, null, isLoggedIn);
    }

    public static boolean addTags(@NonNull final List<ReaderTag> tags,
                                  final ReaderActions.ActionListener actionListener,
                                  final boolean isLoggedIn) {
        ReaderTagList newTags = new ReaderTagList();
        for (ReaderTag tag : tags) {
            final String tagNameForApi = ReaderUtils.sanitizeWithDashes(tag.getTagSlug());
            String endpoint = "/read/tags/" + tagNameForApi + "/posts";

            ReaderTag newTag = new ReaderTag(
                    tag.getTagSlug(),
                    tag.getTagDisplayName(),
                    tag.getTagTitle(),
                    endpoint,
                    ReaderTagType.FOLLOWED);
            newTags.add(newTag);
        }
        boolean result;
        if (!isLoggedIn) {
            result = saveTagsLocallyOnly(actionListener, newTags);
        } else {
            result = saveTagsLocallyAndRemotely(actionListener, newTags);
        }

        return result;
    }

    private static boolean saveTagsLocallyOnly(ActionListener actionListener, ReaderTagList newTags) {
        ReaderTagTable.addOrUpdateTags(newTags);
        ReaderActions.callActionListener(actionListener, true);
        EventBus.getDefault().post(new FollowedTagsFetched(true, ReaderTagTable.getFollowedTags().size()));

        return true;
    }

    private static boolean saveTagsLocallyAndRemotely(ActionListener actionListener,
                                                   ReaderTagList newTags) {
        ReaderTagList existingFollowedTags = ReaderTagTable.getFollowedTags();

        RestRequest.Listener listener = jsonObject -> {
            AppLog.i(T.READER, "add tag succeeded");
            // the response will contain the list of the user's followed tags
            ReaderTagList followedTags = parseFollowedTags(jsonObject);
            ReaderTagTable.replaceFollowedTags(followedTags);
            if (actionListener != null) {
                ReaderActions.callActionListener(actionListener, true);
            }
            EventBus.getDefault().post(new FollowedTagsFetched(true, ReaderTagTable.getFollowedTags().size()));
        };

        RestRequest.ErrorListener errorListener = volleyError -> {
            // treat is as a success if we're adding a tag and the error says the user is
            // already following it
            String error = VolleyUtils.errStringFromVolleyError(volleyError);
            if (error.equals("already_subscribed")) {
                AppLog.w(T.READER, "add tag succeeded with error " + error);
                if (actionListener != null) {
                    ReaderActions.callActionListener(actionListener, true);
                }
                EventBus.getDefault().post(new FollowedTagsFetched(true, ReaderTagTable.getFollowedTags().size()));
                return;
            }

            AppLog.w(T.READER, "add tag failed");
            AppLog.e(T.READER, volleyError);

            // revert on failure
            ReaderTagTable.replaceFollowedTags(existingFollowedTags);
            if (actionListener != null) {
                ReaderActions.callActionListener(actionListener, false);
            }
            EventBus.getDefault().post(new FollowedTagsFetched(false, ReaderTagTable.getFollowedTags().size()));
        };

        ReaderTagTable.addOrUpdateTags(newTags);

        final String path = "read/tags/mine/new";

        Map<String, String> params = new HashMap<>();
        String newTagSlugs = ReaderUtils.getCommaSeparatedTagSlugs(newTags);
        params.put("tags", newTagSlugs);

        WordPress.getRestClientUtilsV1_2().post(path, params, null, listener, errorListener);

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
        for (int i = 0; i < jsonTags.length(); i++) {
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
