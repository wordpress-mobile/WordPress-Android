package org.wordpress.android.fluxc.model;

import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.RawConstraints;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.apache.commons.lang3.StringUtils;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;

import java.io.Serializable;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

@Table
@RawConstraints({"FOREIGN KEY(_id) REFERENCES MediaModel(_id) ON DELETE CASCADE"})
public class MediaUploadModel extends Payload<BaseNetworkError> implements Identifiable, Serializable {
    private static final long serialVersionUID = -2575578916186403789L;

    @Retention(SOURCE)
    @IntDef({UPLOADING, COMPLETED, FAILED})
    public @interface UploadState {}
    public static final int UPLOADING = 0;
    public static final int COMPLETED = 1;
    public static final int FAILED = 2;

    @PrimaryKey(autoincrement = false)
    @Column private int mId;

    @Column private int mUploadState = UPLOADING;

    @Column private float mProgress;

    // Serialization of a MediaError
    @Column private String mErrorType;
    @Column private String mErrorMessage;

    public MediaUploadModel() {}

    public MediaUploadModel(int id) {
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

    public void setProgress(float progress) {
        mProgress = progress;
    }

    public float getProgress() {
        return mProgress;
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

    public @Nullable MediaError getMediaError() {
        if (TextUtils.isEmpty(getErrorType())) {
            return null;
        }
        return new MediaError(MediaErrorType.fromString(getErrorType()), getErrorMessage());
    }

    public void setMediaError(@Nullable MediaError mediaError) {
        if (mediaError == null) {
            setErrorType(null);
            setErrorMessage(null);
            return;
        }

        setErrorType(mediaError.type.toString());
        setErrorMessage(mediaError.message);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof MediaUploadModel)) return false;

        MediaUploadModel otherMedia = (MediaUploadModel) other;

        return getId() == otherMedia.getId()
                && getUploadState() == otherMedia.getUploadState()
                && Float.compare(getProgress(), otherMedia.getProgress()) == 0
                && StringUtils.equals(getErrorType(), otherMedia.getErrorType())
                && StringUtils.equals(getErrorMessage(), otherMedia.getErrorMessage());
    }
}
