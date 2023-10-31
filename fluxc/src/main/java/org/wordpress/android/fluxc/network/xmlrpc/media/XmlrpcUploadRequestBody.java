package org.wordpress.android.fluxc.network.xmlrpc.media;

import android.util.Base64;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringEscapeUtils;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.BaseUploadRequestBody;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import okhttp3.MediaType;
import okio.BufferedSink;
import okio.Okio;

public class XmlrpcUploadRequestBody extends BaseUploadRequestBody {
    private static final MediaType MEDIA_TYPE = MediaType.parse("text/xml; charset=utf-8");

    /**
     * Expected XML content for wp.uploadFile method. Base64 encoded file should be inserted between
     * #PREPEND_XML_FORMAT and #APPEND_XML
     */
    private static final String PREPEND_XML_FORMAT =
            "<?xml version=\"1.0\"?><methodCall><methodName>wp.uploadFile</methodName><params>"
            + "<param><value><int>%d</int></value></param>" // siteId
            + "<param><value><string>%s</string></value></param>" // username
            + "<param><value><string>%s</string></value></param>" // password
            + "<param><value><struct>" // data
            + "<member><name>name</name><value><string>%s</string></value></member>" // name
            + "<member><name>type</name><value><string>%s</string></value></member>" // type
            + "<member><name>overwrite</name><value><boolean>1</boolean></value></member>"
            + "<member><name>post_id</name><value><int>%d</int></value></member>" // remote post ID
            + "<member><name>bits</name><value><base64>"; // bits
    private static final String APPEND_XML =
            "</base64></value></member></struct></value></param></params></methodCall>";

    @NonNull private final String mPrependString;
    private long mMediaSize;
    private long mContentSize = -1;
    private long mMediaBytesWritten = 0;

    @SuppressWarnings("deprecation")
    public XmlrpcUploadRequestBody(
            @NonNull MediaModel media,
            @NonNull ProgressListener listener,
            @NonNull SiteModel site) {
        super(media, listener);

        // TODO: we should use the XMLRPCSerializer instead of doing this
        mPrependString = String.format(Locale.ENGLISH, PREPEND_XML_FORMAT,
                site.getSelfHostedSiteId(),
                StringEscapeUtils.escapeXml(site.getUsername()),
                StringEscapeUtils.escapeXml(site.getPassword()),
                StringEscapeUtils.escapeXml(media.getFileName()),
                StringEscapeUtils.escapeXml(media.getMimeType()),
                media.getPostId());

        try {
            mMediaSize = contentLength();
        } catch (IOException e) {
            // Default to 1 (to avoid divide by zero errors)
            mMediaSize = 1;
        }
    }

    @Override
    protected float getProgress(long bytesWritten) {
        return (float) mMediaBytesWritten / mMediaSize;
    }

    @NonNull
    @Override
    public MediaType contentType() {
        return MEDIA_TYPE;
    }

    @Override
    public long contentLength() throws IOException {
        if (mContentSize == -1) {
            mContentSize = getMediaBase64EncodedSize()
                           + mPrependString.getBytes(StandardCharsets.UTF_8).length
                           + APPEND_XML.length();
        }
        return mContentSize;
    }

    private long getMediaBase64EncodedSize() throws IOException {
        FileInputStream fis = new FileInputStream(getMedia().getFilePath());
        int totalSize = 0;
        try {
            byte[] buffer = new byte[3600];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                totalSize += Base64.encodeToString(buffer, 0, length, Base64.DEFAULT).length();
            }
        } finally {
            fis.close();
        }
        return totalSize;
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        CountingSink countingSink = new CountingSink(sink);
        BufferedSink bufferedSink = Okio.buffer(countingSink);

        // write XML up to point of file
        bufferedSink.writeUtf8(mPrependString);

        // write file to xml

        try (FileInputStream fis = new FileInputStream(getMedia().getFilePath())) {
            byte[] buffer = new byte[3600]; // you must use a 24bit multiple
            int length;
            String chunk;
            while ((length = fis.read(buffer)) > 0) {
                chunk = Base64.encodeToString(buffer, 0, length, Base64.DEFAULT);
                mMediaBytesWritten += length;
                bufferedSink.writeUtf8(chunk);
            }
        }

        // write remainder or XML
        bufferedSink.writeUtf8(APPEND_XML);

        bufferedSink.flush();
    }
}
