package org.wordpress.android.models;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.UrlUtils;

import java.util.ArrayList;
import java.util.Iterator;

public class ReaderAttachmentList extends ArrayList<ReaderAttachment> {

    public static ReaderAttachmentList fromJsonString(String strJson) {
        if (TextUtils.isEmpty(strJson)) {
            return new ReaderAttachmentList();
        }

        try {
            return fromJson(new JSONObject(strJson));
        } catch (JSONException e) {
            AppLog.e(AppLog.T.READER, e);
            return new ReaderAttachmentList();
        }
    }

    private static ReaderAttachmentList fromJson(JSONObject json) {
        ReaderAttachmentList attachments = new ReaderAttachmentList();
        if (json == null) {
            return attachments;
        }

        Iterator<String> it = json.keys();
        if (!it.hasNext()) {
            return attachments;
        }

        while (it.hasNext()) {
            JSONObject jsonAttach = json.optJSONObject(it.next());
            attachments.add(ReaderAttachment.fromJson(jsonAttach));
        }

        return attachments;
    }

    public ReaderAttachment get(final String imageUrl) {
        if (imageUrl == null) {
            return null;
        }

        String normUrl = UrlUtils.normalizeUrl(UrlUtils.removeQuery(imageUrl));
        for (ReaderAttachment attachment: this) {
            if (normUrl.equals(attachment.getUrl())) {
                return attachment;
            }
        }

        return null;
    }
}
