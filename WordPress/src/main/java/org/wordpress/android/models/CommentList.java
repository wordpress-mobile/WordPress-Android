package org.wordpress.android.models;

import org.wordpress.android.fluxc.model.CommentModel;

import java.util.ArrayList;

public class CommentList extends ArrayList<CommentModel> {
    public int indexOfCommentId(long commentId) {
        for (int i = 0; i < this.size(); i++) {
            if (commentId == this.get(i).getRemoteCommentId()) {
                return i;
            }
        }
        return -1;
    }
}
