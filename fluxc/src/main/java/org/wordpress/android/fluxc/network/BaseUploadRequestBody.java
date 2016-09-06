package org.wordpress.android.fluxc.network;

import android.text.TextUtils;

import org.wordpress.android.fluxc.model.MediaModel;
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
        if (media == null) return "media cannot be null";

        // validate MIME type is recognized
        String mimeType = media.getMimeType();
        if (!MediaUtils.isSupportedMimeType(mimeType)) {
            return "media must define a valid MIME type";
        }

        // verify file path is defined
        String filePath = media.getFilePath();
        if (TextUtils.isEmpty(filePath)) {
            return "media must define a local file path";
        }

        // verify file exists and is not a directory
        File file = new File(filePath);
        if (!file.exists()) {
            return "local file path for media does not exist";
        } else if (file.isDirectory()) {
            return "supplied file path is a directory, a file is required";
        }

        return null;
    }

    private final MediaModel       mMedia;
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
        private long mBytesWritten = 0;

        public CountingSink(Sink delegate) {
            super(delegate);
        }

        @Override
        public void write(Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);
            mBytesWritten += byteCount;
            mListener.onProgress(mMedia, getProgress(mBytesWritten));
        }
    }
}
