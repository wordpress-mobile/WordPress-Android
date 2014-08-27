package org.wordpress.android.models;

import org.json.JSONObject;

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
    }}
