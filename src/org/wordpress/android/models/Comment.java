package org.wordpress.android.models;

import org.json.JSONObject;
import org.wordpress.android.util.JSONUtil;

import java.net.URI;

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
    public URI profileImageUrl = null;

    public Comment(String postID, int commentID, int position, String name,
            String dateCreatedFormatted, String comment, String status,
            String postTitle, String authorURL, String authorEmail,
            URI profileImageUrl) {
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

    private Comment() {

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
        comment.comment = JSONUtil.getString(json, "content"); // contains html
        comment.dateCreatedFormatted = JSONUtil.getString(json, "date");

        JSONObject jsonPost = json.optJSONObject("post");
        if (jsonPost != null) {
            comment.postID = Integer.toString(jsonPost.optInt("ID"));
            // c.postTitle = ???
        }

        JSONObject jsonAuthor = json.optJSONObject("author");
        if (jsonAuthor!=null) {
            // author names may contain html entities (esp. pingbacks)
            comment.name = JSONUtil.getStringDecoded(jsonAuthor, "name");
            comment.profileImageUrl = URI.create(JSONUtil.getString(jsonAuthor, "avatar_URL"));
            comment.authorURL = JSONUtil.getString(jsonAuthor, "URL");
            comment.authorEmail = JSONUtil.getString(jsonAuthor, "email");
            comment.emailURL = comment.authorURL;
        }

        return comment;
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
