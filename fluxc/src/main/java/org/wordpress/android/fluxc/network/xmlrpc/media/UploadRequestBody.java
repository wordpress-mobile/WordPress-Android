package org.wordpress.android.fluxc.network.xmlrpc.media;

import android.text.TextUtils;
import android.util.Base64;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.util.AppLog;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * A {@link ProgressListener} is required, use {@link MultipartBody} if progress is not needed.
 *
 * ref http://stackoverflow.com/questions/35528751/okhttp-3-tracking-multipart-upload-progress
 */
public class UploadRequestBody extends RequestBody {
    private static final MediaType MEDIA_TYPE = MediaType.parse("text/xml; charset=utf-8");
    private static final String PREPEND_XML_FORMAT =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<methodCall>" +
            "<methodName>wp.uploadFile</methodName>" +
            "<params>" +
            "<param><value><int>%d</int></value></param>" + // blogId
            "<param><value><string>%s</string></value></param>" + // username
            "<param><value><string>%s</string></value></param>" + // password
            "<param><value><struct>" + // data
            "<member><name>name</name><value><string>%s</string></value></member>" + // name
            "<member><name>type</name><value><string>%s</string></value></member>" + // type
            "<member><name>bits</name><value><base64>"; // bits
    private static final String APPEND_XML =
            "</base64></value></member>" +
            "<member><name>overwrite</name><value><boolean>1</boolean></value></member>" +
            "</struct></value></param>" +
            "</params>" +
            "</methodCall>";

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
    private final ProgressListener mListener;

    private final String mPrependString;
    private final long mContentLength;

    public UploadRequestBody(MediaModel media, ProgressListener listener, SiteModel site) {
        // validate arguments
        if (listener == null) {
            throw new IllegalArgumentException("progress listener cannot be null");
        }
        String mediaError = validateMedia(media);
        if (mediaError != null) {
            throw new IllegalArgumentException(mediaError);
        }

        mMedia = media;
        File mediaFile = new File(mMedia.getFilePath());
        mListener = listener;

        mPrependString = String.format(Locale.ENGLISH, PREPEND_XML_FORMAT,
                site.getDotOrgSiteId(), site.getUsername(), site.getPassword(),
                mMedia.getFileName(), mMedia.getMimeType());

        try {
            mContentLength = (mPrependString + APPEND_XML).getBytes("UTF-8").length + mediaFile.length();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("failed to determine content size");
        }
    }

    @Override
    public long contentLength() {
        return mContentLength;
    }

    @Override
    public MediaType contentType() {
        return MEDIA_TYPE;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        CountingSink countingSink = new CountingSink(sink);
        BufferedSink bufferedSink = Okio.buffer(countingSink);

        // write XML up to point of file
        bufferedSink.write(mPrependString.getBytes(), 0, mPrependString.length());

        // write file to xml
        InputStream is = new DataInputStream(new FileInputStream(mMedia.getFilePath()));
        byte[] buffer = new byte[3600];//you must use a 24bit multiple
        int length;
        String chunk;
        while ((length = is.read(buffer)) > 0) {
            chunk = Base64.encodeToString(buffer, 0, length, Base64.DEFAULT);
            bufferedSink.write(chunk.getBytes(), 0, length);
        }
        is.close();

        // write remainder or XML
        bufferedSink.write(APPEND_XML.getBytes(), 0, APPEND_XML.length());

        bufferedSink.flush();
    }

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
            mListener.onProgress(mMedia, (float) mBytesWritten / contentLength());
        }
    }
}
