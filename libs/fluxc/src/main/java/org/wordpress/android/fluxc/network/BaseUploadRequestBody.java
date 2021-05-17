package org.wordpress.android.fluxc.network;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType;
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType.Type;
import org.wordpress.android.fluxc.utils.MediaUtils;

import java.io.File;
import java.io.IOException;

import okhttp3.RequestBody;
import okio.Buffer;
import okio.ForwardingSink;
import okio.Sink;

/**
 * Wrapper for {@link okhttp3.MultipartBody} that reports upload progress as body data is written.
 *
 * A {@link ProgressListener} is required, use {@link okhttp3.MultipartBody} if progress is not needed.
 *
 * ref http://stackoverflow.com/questions/35528751/okhttp-3-tracking-multipart-upload-progress
 */
public abstract class BaseUploadRequestBody extends RequestBody {
    /**
     * Callback to report upload progress as body data is written to the sink for network delivery.
     */
    public interface ProgressListener {
        void onProgress(MediaModel media, float progress);
    }

    /**
     * Determines if media data is sufficient for upload. Valid media must:
     * <ul>
     *     <li>be non-null</li>
     *     <li>define a recognized MIME type</li>
     *     <li>define a file path to a valid local file</li>
     * </ul>
     *
     * @return null if {@code media} is valid, otherwise a string describing why it's invalid
     */
    public static String hasRequiredData(MediaModel media) {
        return checkMediaArg(media).getType().getErrorLogDescription();
    }

    public static MalformedMediaArgSubType checkMediaArg(MediaModel media) {
        if (media == null) return new MalformedMediaArgSubType(Type.MEDIA_WAS_NULL);

        // validate MIME type is recognized
        String mimeType = media.getMimeType();
        if (!MediaUtils.isSupportedMimeType(mimeType)) {
            return new MalformedMediaArgSubType(Type.UNSUPPORTED_MIME_TYPE);
        }

        // verify file path is defined
        String filePath = media.getFilePath();
        if (TextUtils.isEmpty(filePath)) {
            return new MalformedMediaArgSubType(Type.NOT_VALID_LOCAL_FILE_PATH);
        }

        // verify file exists and is not a directory
        File file = new File(filePath);
        if (!file.exists()) {
            return new MalformedMediaArgSubType(Type.MEDIA_FILE_NOT_FOUND_LOCALLY);
        } else if (file.isDirectory()) {
            return new MalformedMediaArgSubType(Type.DIRECTORY_PATH_SUPPLIED_FILE_NEEDED);
        }

        return new MalformedMediaArgSubType(Type.NO_ERROR);
    }

    private final MediaModel mMedia;
    private final ProgressListener mListener;

    public BaseUploadRequestBody(MediaModel media, ProgressListener listener) {
        // validate arguments
        if (listener == null) {
            throw new IllegalArgumentException("progress listener cannot be null");
        }
        String mediaError = hasRequiredData(media);
        if (mediaError != null) {
            throw new IllegalArgumentException(mediaError);
        }

        mMedia = media;
        mListener = listener;
    }

    protected abstract float getProgress(long bytesWritten);

    public MediaModel getMedia() {
        return mMedia;
    }

    /**
     * Custom Sink that reports progress to listener as bytes are written.
     */
    protected final class CountingSink extends ForwardingSink {
        private static final int ON_PROGRESS_THROTTLE_RATE = 100;
        private long mBytesWritten = 0;
        private long mLastTimeOnProgressCalled = 0;

        public CountingSink(Sink delegate) {
            super(delegate);
        }

        @Override
        public void write(@NonNull Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);
            mBytesWritten += byteCount;
            long currentTimeMillis = System.currentTimeMillis();
            // Call the mListener.onProgress callback at maximum every 100ms.
            if ((currentTimeMillis - mLastTimeOnProgressCalled) > ON_PROGRESS_THROTTLE_RATE
                || mLastTimeOnProgressCalled == 0) {
                mLastTimeOnProgressCalled = currentTimeMillis;
                mListener.onProgress(mMedia, getProgress(mBytesWritten));
            }
        }
    }
}
