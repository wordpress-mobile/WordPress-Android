package org.wordpress.android.ui.comments;

import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.models.Note;

/**
 * actions related to comments - replies, moderating, etc.
 * methods below do network calls in the background & update local DB upon success
 * all methods below MUST be called from UI thread
 */

public class CommentActions {
    private CommentActions() {
        throw new AssertionError();
    }

    /*
     * used by comment fragments to alert container activity of a change to one or more
     * comments (moderated, deleted, added, etc.)
     */
    public enum ChangeType {
        EDITED, REPLIED
    }

    public interface OnCommentActionListener {
        void onModerateComment(SiteModel site, CommentModel comment, CommentStatus newStatus);
    }

    public interface OnNoteCommentActionListener {
        void onModerateCommentForNote(Note note, CommentStatus newStatus);
    }
}
