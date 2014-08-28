package org.wordpress.android.models;

import org.json.JSONObject;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;

public class ReaderAttachment {
    public long postId;
    public long blogId;
    public long attachmentId;

    private String url;
    private String mimeType;

    public int width;
    public int height;

    public static ReaderAttachment fromJson(long blogId, long postId, JSONObject json) {
        ReaderAttachment attach = new ReaderAttachment();
        if (json == null) {
            return attach;
        }

        attach.blogId = blogId;
        attach.postId = postId;
        attach.attachmentId = json.optLong("ID");

        attach.setMimeType(JSONUtil.getString(json, "mime_type"));
        attach.setUrl(JSONUtil.getString(json, "URL"));

        attach.width = json.optInt("width");
        attach.height = json.optInt("height");

        return attach;
    }

    public String getUrl() {
        return StringUtils.notNullStr(url);
    }
    public void setUrl(String url) {
        this.url = StringUtils.notNullStr(url);
        normUrl = null;
    }

    private transient String normUrl;
    public String getNormUrl() {
        if (normUrl == null) {
            normUrl = UrlUtils.normalizeUrl(url);
        }
        return normUrl;
    }

    public String getMimeType() {
        return StringUtils.notNullStr(mimeType);
    }
    public void setMimeType(String mimeType) {
        this.mimeType = StringUtils.notNullStr(mimeType);
    }

    public float getHWRatio() {
        if (width == 0 || height == 0) {
            return 0;
        } else {
            return ((float) height / (float) width);
        }
    }

    public boolean isImage() {
        return (mimeType != null && mimeType.startsWith("image"));
    }
}
