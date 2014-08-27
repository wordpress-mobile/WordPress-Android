package org.wordpress.android.models;

import org.json.JSONObject;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.StringUtils;

public class ReaderAttachment {
    public long postId;
    public long blogId;
    public long attachmentId;

    private String url;
    private String mimeType;

    public int width;
    public int height;

    /*
        2211": {
            "ID": 2211,
            "URL": "https://mroselamb.files.wordpress.com/2014/08/img_5939.jpg",
            "guid": "http://mroselamb.files.wordpress.com/2014/08/img_5939.jpg",
            "mime_type": "image/jpeg",
            "width": 2448,
            "height": 2448
        },
     */
    public static ReaderAttachment fromJson(long blogId, long postId, JSONObject json) {
        ReaderAttachment attach = new ReaderAttachment();
        if (json == null) {
            return attach;
        }

        attach.blogId = blogId;
        attach.postId = postId;

        attach.attachmentId = json.optLong("ID");
        attach.mimeType = JSONUtil.getString(json, "mime_type");
        attach.url = JSONUtil.getString(json, "URL");
        attach.width = json.optInt("width");
        attach.height = json.optInt("height");

        return attach;
    }

    public String getUrl() {
        return StringUtils.notNullStr(url);
    }
    public void setUrl(String url) {
        this.url = StringUtils.notNullStr(url);
    }

    public String getMimeType() {
        return StringUtils.notNullStr(mimeType);
    }
    public void setMimeType(String mimeType) {
        this.mimeType = StringUtils.notNullStr(mimeType);
    }}
