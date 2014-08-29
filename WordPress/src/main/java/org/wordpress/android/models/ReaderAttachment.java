package org.wordpress.android.models;

import org.json.JSONObject;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;

public class ReaderAttachment {
    public long attachmentId;
    public int width;
    public int height;

    private String url;
    private String mimeType;

    public static ReaderAttachment fromJson(JSONObject json) {
        ReaderAttachment attach = new ReaderAttachment();
        if (json == null) {
            return attach;
        }

        attach.attachmentId = json.optLong("ID");
        attach.width = json.optInt("width");
        attach.height = json.optInt("height");

        attach.mimeType = JSONUtil.getString(json, "mime_type");
        attach.setUrl(JSONUtil.getString(json, "URL"));

        return attach;
    }

    public String getUrl() {
        return StringUtils.notNullStr(url);
    }
    private void setUrl(String url) {
        // always store normalized URLs without a query string
        if (url != null) {
            this.url = UrlUtils.normalizeUrl(UrlUtils.removeQuery(url));
        } else {
            this.url = "";
        }
    }

    public boolean isImage() {
        return (mimeType != null && mimeType.startsWith("image"));
    }
}
