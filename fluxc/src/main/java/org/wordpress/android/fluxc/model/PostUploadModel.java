package org.wordpress.android.fluxc.model;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.RawConstraints;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.store.PostStore.PostError;
import org.wordpress.android.fluxc.store.PostStore.PostErrorType;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.annotation.RetentionPolicy.SOURCE;

@Table
@RawConstraints({"FOREIGN KEY(_id) REFERENCES PostModel(_id) ON DELETE CASCADE"})
public class PostUploadModel extends Payload implements Identifiable, Serializable {
    @Retention(SOURCE)
    @IntDef({PENDING, FAILED, CANCELLED})
    public @interface UploadState {}
    public static final int PENDING = 0;
    public static final int FAILED = 1;
    public static final int CANCELLED = 2;

    @PrimaryKey(autoincrement = false)
    @Column private int mId;

    @Column private int mUploadState = PENDING;

    @Column private String mAssociatedMediaIds;

    // Serialization of a PostError
    @Column private String mErrorType;
    @Column private String mErrorMessage;

    // TODO Progress?

    public PostUploadModel() {}

    public PostUploadModel(int id) {
        mId = id;
    }

    @Override
    public void setId(int id) {
        mId = id;
    }

    @Override
    public int getId() {
        return mId;
    }

    public @UploadState int getUploadState() {
        return mUploadState;
    }

    public void setUploadState(@UploadState int uploadState) {
        mUploadState = uploadState;
    }

    public String getAssociatedMediaIds() {
        return mAssociatedMediaIds;
    }

    public void setAssociatedMediaIds(String associatedMediaIds) {
        mAssociatedMediaIds = associatedMediaIds;
    }

    public @NonNull Set<Integer> getAssociatedMediaIdSet() {
        return mediaIdStringToSet(mAssociatedMediaIds);
    }

    public void setAssociatedMediaIdSet(Set<Integer> mediaIdSet) {
        mAssociatedMediaIds = mediaIdSetToString(mediaIdSet);
    }

    public String getErrorType() {
        return mErrorType;
    }

    public void setErrorType(String errorType) {
        mErrorType = errorType;
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        mErrorMessage = errorMessage;
    }

    public @Nullable PostError getPostError() {
        if (TextUtils.isEmpty(getErrorType())) {
            return null;
        }
        return new PostError(PostErrorType.fromString(getErrorType()), getErrorMessage());
    }

    public void setPostError(@Nullable PostError postError) {
        if (postError == null) {
            setErrorType(null);
            setErrorMessage(null);
            return;
        }

        setErrorType(postError.type.toString());
        setErrorMessage(postError.message);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof PostUploadModel)) return false;

        PostUploadModel otherPost = (PostUploadModel) other;

        return getId() == otherPost.getId()
                && getUploadState() == otherPost.getUploadState()
                && StringUtils.equals(getAssociatedMediaIds(), otherPost.getAssociatedMediaIds())
                && StringUtils.equals(getErrorType(), otherPost.getErrorType())
                && StringUtils.equals(getErrorMessage(), otherPost.getErrorMessage());
    }

    private static Set<Integer> mediaIdStringToSet(String ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptySet();
        }
        String[] stringArray = ids.split(",");
        Set<Integer> integerSet = new HashSet<>();
        for (String mediaIdStrong : stringArray) {
            integerSet.add(Integer.parseInt(mediaIdStrong));
        }
        return integerSet;
    }

    private static String mediaIdSetToString(Set<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        List<Integer> idList = new ArrayList<>(ids);
        Collections.sort(idList);
        return TextUtils.join(",", idList);
    }
}
