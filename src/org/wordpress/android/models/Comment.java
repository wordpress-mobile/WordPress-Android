package org.wordpress.android.models;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.StringUtils;

public class Comment {
    public int postID;
    public int commentID;

    private String authorName;
    private String status;
    private String comment;
    private String postTitle;
    private String authorUrl;
    private String authorEmail;
    private String published;
    private String profileImageUrl;

    public Comment(int postID,
                   int commentID,
                   String authorName,
                   String pubDateGmt,
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
        this.authorUrl = authorURL;
        this.authorEmail = authorEmail;
        this.profileImageUrl = profileImageUrl;
        this.published = pubDateGmt;
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
        comment.published = JSONUtil.getString(json, "date");

        // note that the content often contains html, and on rare occasions may contain
        // script blocks that need to be removed (only seen with blogs that use the
        // sociable plugin)
        comment.comment = HtmlUtils.stripScript(JSONUtil.getString(json, "content"));

        JSONObject jsonPost = json.optJSONObject("post");
        if (jsonPost != null) {
            comment.postID = jsonPost.optInt("ID");
            // TODO: c.postTitle = ???
        }

        JSONObject jsonAuthor = json.optJSONObject("author");
        if (jsonAuthor!=null) {
            // author names may contain html entities (esp. pingbacks)
            comment.authorName = JSONUtil.getStringDecoded(jsonAuthor, "name");
            comment.authorUrl = JSONUtil.getString(jsonAuthor, "URL");

            // email address will be set to "false" when there isn't an email address
            comment.authorEmail = JSONUtil.getString(jsonAuthor, "email");
            if (comment.authorEmail.equals("false"))
                comment.authorEmail = "";

            comment.profileImageUrl = JSONUtil.getString(jsonAuthor, "avatar_URL");
        }

        return comment;
    }

    public String getProfileImageUrl() {
        return StringUtils.notNullStr(profileImageUrl);
    }
    public void setProfileImageUrl(String url) {
        profileImageUrl = StringUtils.notNullStr(url);
    }
    public boolean hasProfileImageUrl() {
        return !TextUtils.isEmpty(profileImageUrl);
    }

    public CommentStatus getStatusEnum() {
        return CommentStatus.fromString(status);
    }

    public String getStatus() {
        return StringUtils.notNullStr(status);
    }
    public void setStatus(String status) {
        this.status = StringUtils.notNullStr(status);
    }

    public String getPublished() {
        return StringUtils.notNullStr(published);
    }
    public void setPublished(String pubDate) {
        published = StringUtils.notNullStr(pubDate);
    }

    public boolean hasAuthorName() {
        return !TextUtils.isEmpty(authorName);
    }
    public String getAuthorName() {
        return StringUtils.notNullStr(authorName);
    }
    public void setAuthorName(String name) {
        authorName = StringUtils.notNullStr(name);
    }

    public boolean hasAuthorEmail() {
        return !TextUtils.isEmpty(authorEmail);
    }
    public String getAuthorEmail() {
        return StringUtils.notNullStr(authorEmail);
    }
    public void setAuthorEmail(String email) {
        authorEmail = StringUtils.notNullStr(email);
    }

    public boolean hasAuthorUrl() {
        return !TextUtils.isEmpty(authorUrl);
    }
    public String getAuthorUrl() {
        return StringUtils.notNullStr(authorUrl);
    }
    public void setAuthorUrl(String url) {
        authorUrl = StringUtils.notNullStr(url);
    }

    public String getCommentText() {
        return StringUtils.notNullStr(comment);
    }
    public void setCommentText(String text) {
        comment = StringUtils.notNullStr(text);
    }

    public boolean hasPostTitle() {
        return !TextUtils.isEmpty(postTitle);
    }
    public String getPostTitle() {
        return StringUtils.notNullStr(postTitle);
    }
    public void setPostTitle(String title) {
        postTitle = StringUtils.notNullStr(title);
    }

    /****
     * the following are transient variables whose sole purpose is to cache commonly-used values
     * for the comment that speeds up accessing them inside adapters
     ****/

    /*
     * converts iso8601 published date to an actual java date
     */
    private transient java.util.Date dtPublished;
    public java.util.Date getDatePublished() {
        if (dtPublished == null)
            dtPublished = DateTimeUtils.iso8601ToJavaDate(published);
        return dtPublished;
    }

    private transient String unescapedCommentText;
    public String getUnescapedCommentText() {
        if (unescapedCommentText == null)
            unescapedCommentText = StringUtils.unescapeHTML(getCommentText()).trim();
        return unescapedCommentText;
    }

    private transient String unescapedPostTitle;
    public String getUnescapedPostTitle() {
        if (unescapedPostTitle == null)
            unescapedPostTitle = StringUtils.unescapeHTML(getPostTitle().trim());
        return unescapedPostTitle;
    }

    /*
     * returns the avatar url as a photon/gravatar url set to the passed size
     */
    private transient String avatarForDisplay;
    public String getAvatarForDisplay(int avatarSize) {
        if (avatarForDisplay==null) {
            if (hasProfileImageUrl()) {
                avatarForDisplay = PhotonUtils.fixAvatar(profileImageUrl, avatarSize);
            } else if (hasAuthorEmail()) {
                avatarForDisplay = GravatarUtils.gravatarUrlFromEmail(authorEmail, avatarSize);
            } else {
                avatarForDisplay = "";
            }
        }
        return avatarForDisplay;
    }

    /*
     * returns the author + post title as "Author Name on Post Title" - used by comment list
     */
    private transient String formattedTitle;
    public String getFormattedTitle() {
        if (formattedTitle == null) {
            Context context = WordPress.getContext();
            final String author = (hasAuthorName() ? getAuthorName() : context.getString(R.string.anonymous));
            if (hasPostTitle()) {
                formattedTitle = author
                              + "<font color=" + HtmlUtils.colorResToHtmlColor(context, R.color.grey_medium) + ">"
                              + " " + context.getString(R.string.on) + " "
                              + "</font>"
                              + getUnescapedPostTitle();
            } else {
                formattedTitle = author;
            }
        }
        return formattedTitle;
    }
}
