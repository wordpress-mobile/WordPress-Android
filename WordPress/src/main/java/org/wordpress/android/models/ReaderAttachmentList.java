package org.wordpress.android.models;

import org.json.JSONObject;
import org.wordpress.android.util.UrlUtils;

import java.util.ArrayList;
import java.util.Iterator;

public class ReaderAttachmentList extends ArrayList<ReaderAttachment> {

    public static ReaderAttachmentList fromJson(long blogId, long postId, JSONObject json) {
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
            attachments.add(ReaderAttachment.fromJson(blogId, postId, jsonAttach));
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
