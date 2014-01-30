package org.wordpress.android.models;

import android.text.TextUtils;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.JSONUtil;
import org.xmlrpc.android.ApiHelper;

public class Comment {
    public int postID;
    public int commentID;
    public String authorName = "";
    private String status = "";
    public String comment = "";
    public String postTitle = "";
    public String authorURL = "";
    public String authorEmail = "";
    public String dateCreatedFormatted = "";
    private String profileImageUrl = null;

    public Comment(int postID,
                   int commentID,
                   String authorName,
                   String dateCreatedFormatted,
                   String comment,
                   String status,
                   String postTitle,
                   String authorURL,
                   String authorEmail,
                   String profileImageUrl) {
        this.postID = postID;
        this.commentID = commentID;
        this.authorName = authorName;
        this.status = status;
        this.comment = comment;
        this.postTitle = postTitle;
        this.authorURL = authorURL;
        this.authorEmail = authorEmail;
        this.profileImageUrl = profileImageUrl;
        this.dateCreatedFormatted = dateCreatedFormatted;
    }

    private Comment() {
        // nop
    }

    /*
     * nbradbury 11/14/13 - create a comment from JSON (REST response)
     * https://developer.wordpress.com/docs/api/1/get/sites/%24site/comments/%24comment_ID/
     */
    public static Comment fromJSON(JSONObject json) {
        if (json == null)
            return null;

        Comment comment = new Comment();
        comment.commentID = json.optInt("ID");
        comment.status = JSONUtil.getString(json, "status");

        // note that the content often contains html, and on rare occasions may contain
        // script blocks that need to be removed (only seen with blogs that use the
        // sociable plugin)
        comment.comment = HtmlUtils.stripScript(JSONUtil.getString(json, "content"));

        java.util.Date date = DateTimeUtils.iso8601ToJavaDate(JSONUtil.getString(json, "date"));
        if (date != null)
            comment.dateCreatedFormatted = ApiHelper.getFormattedCommentDate(WordPress.getContext(), date);

        JSONObject jsonPost = json.optJSONObject("post");
        if (jsonPost != null) {
            comment.postID = jsonPost.optInt("ID");
            // c.postTitle = ???
        }

        JSONObject jsonAuthor = json.optJSONObject("author");
        if (jsonAuthor!=null) {
            // author names may contain html entities (esp. pingbacks)
            comment.authorName = JSONUtil.getStringDecoded(jsonAuthor, "name");
            comment.authorURL = JSONUtil.getString(jsonAuthor, "URL");

            // email address will be set to "false" when there isn't an email address
            comment.authorEmail = JSONUtil.getString(jsonAuthor, "email");
            if (comment.authorEmail.equals("false"))
                comment.authorEmail = "";

            comment.profileImageUrl = JSONUtil.getString(jsonAuthor, "avatar_URL");
        }

        return comment;
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

    public boolean hasAuthorEmail() {
        return !TextUtils.isEmpty(authorEmail);
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
