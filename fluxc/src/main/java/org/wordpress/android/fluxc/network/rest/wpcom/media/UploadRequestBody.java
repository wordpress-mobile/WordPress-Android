package org.wordpress.android.fluxc.network.rest.wpcom.media;

import android.text.TextUtils;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.util.AppLog;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * Wrapper for {@link MultipartBody} that reports upload progress as body data is written.
 *
 * A {@link ProgressListener} is required, use {@link MultipartBody} if progress is not needed.
 *
 * ref http://stackoverflow.com/questions/35528751/okhttp-3-tracking-multipart-upload-progress
 */
public class UploadRequestBody extends RequestBody {
    private static final String MEDIA_DATA_KEY = "media[0]";
    private static final String MEDIA_ATTRIBUTES_KEY = "attrs[0]";
    private static final String MEDIA_PARAM_FORMAT = MEDIA_ATTRIBUTES_KEY + "[%s]";

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
    public static String validateMedia(MediaModel media) {
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
    private final MultipartBody    mMultipartBody;
    private final ProgressListener mListener;

    public UploadRequestBody(MediaModel media, ProgressListener listener) {
        // validate arguments
        if (listener == null) {
            throw new IllegalArgumentException("progress listener cannot be null");
        }
        String mediaError = validateMedia(media);
        if (mediaError != null) {
            throw new IllegalArgumentException(mediaError);
        }

        mMedia = media;
        mListener = listener;
        mMultipartBody = buildMultipartBody();
    }

    @Override
    public long contentLength() {
        try {
            return mMultipartBody.contentLength();
        } catch (IOException e) {
            AppLog.w(AppLog.T.MEDIA, "Error determining mMultipartBody content length: " + e);
        }
        return -1L;
    }

    @Override
    public MediaType contentType() {
        return mMultipartBody.contentType();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        CountingSink countingSink = new CountingSink(sink);
        BufferedSink bufferedSink = Okio.buffer(countingSink);
        mMultipartBody.writeTo(bufferedSink);
        bufferedSink.flush();
    }

    public MediaModel getMedia() {
        return mMedia;
    }

    private MultipartBody buildMultipartBody() {
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        // add media attributes
        Map<String, String> mediaData = MediaUtils.getMediaRestParams(mMedia);
        for (String key : mediaData.keySet()) {
            builder.addFormDataPart(String.format(MEDIA_PARAM_FORMAT, key), mediaData.get(key));
        }

        // add media file data
        File mediaFile = new File(mMedia.getFilePath());
        RequestBody body = RequestBody.create(MediaType.parse(mMedia.getMimeType()), mediaFile);
        builder.addFormDataPart(MEDIA_DATA_KEY, mMedia.getFileName(), body);

        return builder.build();
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
            mListener.onProgress(mMedia, (float) mBytesWritten / contentLength());
        }
    }
}
