package org.wordpress.android.models;

import android.text.TextUtils;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.JSONUtil;
import org.xmlrpc.android.ApiHelper;

public class Comment {
    public String postID = "";
    public int commentID;
    public int position;
    public String name = "";
    public String emailURL = "";
    private String status = "";
    public String comment = "";
    public String postTitle = "";
    public String authorURL = "";
    public String authorEmail = "";
    public String dateCreatedFormatted = "";
    private String profileImageUrl = null;

    public Comment(String postID,
            int commentID,
            int position,
            String name,
            String dateCreatedFormatted,
            String comment,
            String status,
            String postTitle,
            String authorURL,
            String authorEmail,
            String profileImageUrl) {
        this.postID = postID;
        this.commentID = commentID;
        this.position = position;
        this.name = name;
        this.emailURL = authorEmail;
        this.status = status;
        this.comment = comment;
        this.postTitle = postTitle;
        this.authorURL = authorURL;
        this.authorEmail = authorEmail; // why is this the same as emailURL above?
        this.profileImageUrl = profileImageUrl;
        this.dateCreatedFormatted = dateCreatedFormatted;
    }

    /*
     * nbradbury 11/14/13 - create a comment from JSON (REST response)
     * https://developer.wordpress.com/docs/api/1/get/sites/%24site/comments/%24comment_ID/
     */
    public Comment(JSONObject json) {
        if (json == null)
            return;

        this.commentID = json.optInt("ID");
        this.status = JSONUtil.getString(json, "status");

        // note that the content often contains html, and on rare occasions may contain
        // script blocks that need to be removed (only seen with blogs that use the
        // sociable plugin)
        this.comment = HtmlUtils.stripScript(JSONUtil.getString(json, "content"));

        java.util.Date date = DateTimeUtils.iso8601ToJavaDate(JSONUtil.getString(json, "date"));
        if (date != null)
            this.dateCreatedFormatted = ApiHelper.getFormattedCommentDate(WordPress.getContext(), date);

        JSONObject jsonPost = json.optJSONObject("post");
        if (jsonPost != null) {
            this.postID = Integer.toString(jsonPost.optInt("ID"));
            // c.postTitle = ???
        }

        JSONObject jsonAuthor = json.optJSONObject("author");
        if (jsonAuthor!=null) {
            // author names may contain html entities (esp. pingbacks)
            this.name = JSONUtil.getStringDecoded(jsonAuthor, "name");
            this.authorURL = JSONUtil.getString(jsonAuthor, "URL");

            // email address will be set to "false" when there isn't an email address
            this.authorEmail = JSONUtil.getString(jsonAuthor, "email");
            if (this.authorEmail.equals("false"))
                this.authorEmail = "";
            this.emailURL = this.authorEmail;

            this.profileImageUrl = JSONUtil.getString(jsonAuthor, "avatar_URL");
        }
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }
    public void setProfileImageUrl(String url) {
        profileImageUrl = url;
    }
    public boolean hasProfileImageUrl() {
        return !TextUtils.isEmpty(profileImageUrl);
    }

    public CommentStatus getStatusEnum() {
        return CommentStatus.fromString(status);
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }



}
