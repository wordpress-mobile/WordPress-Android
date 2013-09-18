package org.wordpress.android.models;

import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import org.json.JSONObject;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.Emoticons;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.StringUtils;

/**
 * Created by nbradbury on 7/8/13.
 */
public class ReaderComment {
    public long commentId;
    public long blogId;
    public long postId;
    public long parentId;

    private String authorName;
    private String authorAvatar;

    private String authorUrl;
    private String status;
    private String text;

    public long timestamp;
    private String published;

    // not stored in db - denotes the indentation level when displaying this comment
    public int level = 0;

    public static ReaderComment fromJson(JSONObject json, long blogId) {
        if (json==null)
            throw new IllegalArgumentException("null json comment");

        ReaderComment comment = new ReaderComment();

        comment.blogId = blogId;
        comment.commentId = json.optLong("ID");
        comment.status = JSONUtil.getString(json, "status");
        comment.text = makePlainText(JSONUtil.getString(json, "content"));

        comment.published = JSONUtil.getString(json, "date");
        comment.timestamp = DateTimeUtils.iso8601ToTimestamp(comment.published);

        JSONObject jsonPost = json.optJSONObject("post");
        if (jsonPost!=null)
            comment.postId = jsonPost.optLong("ID");

        JSONObject jsonAuthor = json.optJSONObject("author");
        if (jsonAuthor!=null) {
            // author names may contain html entities (esp. pingbacks)
            comment.authorName = JSONUtil.getStringDecoded(jsonAuthor, "name");
            comment.authorAvatar = JSONUtil.getString(jsonAuthor, "avatar_URL");
            comment.authorUrl = JSONUtil.getString(jsonAuthor, "URL");
        }

        JSONObject jsonParent = json.optJSONObject("parent");
        if (jsonParent!=null)
            comment.parentId = jsonParent.optLong("ID");

        return comment;
    }

    /*
     * strips html from comment text and replaces emoticons with emoji
     */
    private static String makePlainText(final String text) {
        if (text==null)
            return "";

        final String plainText;

        // replace emoticons with emoji and strip html
        if (text.contains("icon_")) {
            SpannableStringBuilder spannable = (SpannableStringBuilder) Html.fromHtml(text);
            Emoticons.replaceEmoticonsWithEmoji(spannable);
            plainText = spannable.toString().trim();
        } else {
            plainText = HtmlUtils.fastStripHtml(text);
        }

        // some comments have a CDATA section that's preceded by <!--//-->, and stripping HTML
        // above doesn't appear to remove this - so handle this by removing everything
        // after <!--//--> (the actual comment appears before that)
        int pos = plainText.indexOf("<!--//-->");
        if (pos > 0)
            return plainText.substring(0, pos-1);

        return plainText;
    }

    public String getAuthorName() {
        return StringUtils.notNullStr(authorName);
    }

    public void setAuthorName(String authorName) {
        this.authorName = StringUtils.notNullStr(authorName);
    }

    public String getAuthorAvatar() {
        return StringUtils.notNullStr(authorAvatar);
    }
    public void setAuthorAvatar(String authorAvatar) {
        this.authorAvatar = StringUtils.notNullStr(authorAvatar);
    }

    public String getAuthorUrl() {
        return StringUtils.notNullStr(authorUrl);
    }
    public void setAuthorUrl(String authorUrl) {
        this.authorUrl = StringUtils.notNullStr(authorUrl);
    }

    public String getText() {
        return StringUtils.notNullStr(text);
    }
    public void setText(String text) {
        this.text = StringUtils.notNullStr(text);
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
    public void setPublished(String published) {
        this.published = StringUtils.notNullStr(published);
    }

    //

    public boolean hasAvatar() {
        return !TextUtils.isEmpty(authorAvatar);
    }

    public boolean hasAuthorUrl() {
        return !TextUtils.isEmpty(authorUrl);
    }}
