package org.wordpress.android.ui.reader.models;

import android.support.annotation.NonNull;

import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostList;

import java.util.ArrayList;

public class ReaderRelatedPostList extends ArrayList<ReaderRelatedPost> {

    public ReaderRelatedPostList(@NonNull ReaderPostList posts) {
        for (ReaderPost post: posts) {
            add(new ReaderRelatedPost(post));
        }
    }

}
