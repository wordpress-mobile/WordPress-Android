package org.wordpress.android.ui.stats.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class PublishedPostsAndPagesModel implements Serializable {
    private final String mBlogID;
    private List<PostModel> mPublishedPostsAndPages;
    private int mFound = 0;


    public PublishedPostsAndPagesModel(String blogID, JSONObject response) throws JSONException {
        this.mBlogID = blogID;

        try {
            this.mFound = response.getInt("found");
        } catch (JSONException e) {
            throw new JSONException("Invalid document returned from the REST API. The field 'found' is not available in the response, or" +
                    " it's not an int ");
        }

        JSONArray postViewsArray = response.optJSONArray("posts");
        if (postViewsArray != null && postViewsArray.length() > 0) {
            mPublishedPostsAndPages = new ArrayList<>(postViewsArray.length());
            for (int i = 0; i < postViewsArray.length(); i++) {
                try {
                    JSONObject postObject = postViewsArray.getJSONObject(i);
                    String itemID = postObject.getString("ID");
                    String itemTitle = postObject.getString("title");
                    int itemTotal = 0;
                    String itemURL = postObject.getString("URL");
                    String itemType = postObject.getString("type");
                    PostModel currentModel = new PostModel(blogID, 0L, itemID, itemTitle,
                            itemTotal, itemURL, null, itemType);
                    mPublishedPostsAndPages.add(currentModel);
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.STATS, "Unexpected PostModel object in published posts and pages array" +
                            "at position " + i + " Response: " + response.toString(), e);
                }
            }
        }
    }

    public String getBlogID() {
        return mBlogID;
    }

    public List<PostModel> getPublishedPostsAndPages() {
        return mPublishedPostsAndPages;
    }

    public boolean hasPublishedPostsAndPages() {
        return mPublishedPostsAndPages != null && mPublishedPostsAndPages.size() > 0;
    }

    public int getFound() {
        return mFound;
    }
}
