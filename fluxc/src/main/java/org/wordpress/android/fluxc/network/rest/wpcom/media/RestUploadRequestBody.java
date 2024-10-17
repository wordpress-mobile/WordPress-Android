package org.wordpress.android.fluxc.network.rest.wpcom.media;

import androidx.annotation.NonNull;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.network.BaseUploadRequestBody;
import org.wordpress.android.util.AppLog;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;

/**
 * Wrapper for {@link MultipartBody} that reports upload progress as body data is written.
 * <p>
 * A {@link ProgressListener} is required, use {@link MultipartBody} if progress is not needed.
 * <p>
 * @see <a href="http://stackoverflow.com/questions/35528751/okhttp-3-tracking-multipart-upload-progress">doc</a>
 */
public class RestUploadRequestBody extends BaseUploadRequestBody {
    private static final String MEDIA_DATA_KEY = "media[0]";
    private static final String MEDIA_ATTRIBUTES_KEY = "attrs[0]";
    private static final String MEDIA_PARAM_FORMAT = MEDIA_ATTRIBUTES_KEY + "[%s]";

    @NonNull private final MultipartBody mMultipartBody;

    public RestUploadRequestBody(
            @NonNull MediaModel media,
            @NonNull Map<String, Object> params,
            @NonNull ProgressListener listener) {
        super(media, listener);
        mMultipartBody = buildMultipartBody(params);
    }

    @Override
    protected float getProgress(long bytesWritten) {
        return (float) bytesWritten / contentLength();
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

    @NonNull
    @Override
    public MediaType contentType() {
        return mMultipartBody.contentType();
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        CountingSink countingSink = new CountingSink(sink);
        BufferedSink bufferedSink = Okio.buffer(countingSink);
        mMultipartBody.writeTo(bufferedSink);
        bufferedSink.flush();
    }

    @NonNull
    @SuppressWarnings("deprecation")
    private MultipartBody buildMultipartBody(@NonNull Map<String, Object> params) {
        MediaModel media = getMedia();
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        // add media attributes
        for (String key : params.keySet()) {
            Object value = params.get(key);
            if (value != null) {
                builder.addFormDataPart(String.format(MEDIA_PARAM_FORMAT, key), value.toString());
            }
        }

        // add media file data
        String filePath = media.getFilePath();
        String mimeType = media.getMimeType();
        if (filePath != null && mimeType != null) {
            File mediaFile = new File(filePath);
            RequestBody body = RequestBody.create(MediaType.parse(mimeType), mediaFile);
            String fileName = media.getFileName();
            try {
                fileName = URLEncoder.encode(media.getFileName(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            builder.addFormDataPart(MEDIA_DATA_KEY, fileName, body);
        }

        return builder.build();
    }
}
