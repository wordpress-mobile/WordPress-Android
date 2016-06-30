package org.wordpress.android.ui.comments;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.HashSet;

/**
 * Used to store state of {@link CommentAdapter}
 */
public class CommentAdapterState implements Parcelable {
    public static final String KEY = "comments_adapter_state";

    private final HashSet<Long> mSelectedComments;
    private final HashSet<Long> mModeratedCommentsId;

    public CommentAdapterState(@NonNull HashSet<Long> selectedComments, @NonNull HashSet<Long> moderatedCommentsId) {
        mSelectedComments = selectedComments;
        mModeratedCommentsId = moderatedCommentsId;
    }

    public HashSet<Long> getSelectedComments() {
        return mSelectedComments;
    }

    public HashSet<Long> getModeratedCommentsId() {
        return mModeratedCommentsId;
    }


    public boolean hasSelectedComments() {
        return mSelectedComments != null && mSelectedComments.size() > 0;
    }

    public boolean hasModeratingComments() {
        return mModeratedCommentsId != null && mModeratedCommentsId.size() > 0;
    }

    @SuppressWarnings("unchecked")
    private CommentAdapterState(Parcel in) {
        mSelectedComments = (HashSet<Long>) in.readSerializable();
        mModeratedCommentsId = (HashSet<Long>) in.readSerializable();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(mSelectedComments);
        dest.writeSerializable(mModeratedCommentsId);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<CommentAdapterState> CREATOR = new Parcelable.Creator<CommentAdapterState>() {
        @Override
        public CommentAdapterState createFromParcel(Parcel in) {
            return new CommentAdapterState(in);
        }

        @Override
        public CommentAdapterState[] newArray(int size) {
            return new CommentAdapterState[size];
        }
    };
}
