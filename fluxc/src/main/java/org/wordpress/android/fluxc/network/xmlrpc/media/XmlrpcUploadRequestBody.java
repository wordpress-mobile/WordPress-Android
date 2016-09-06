package org.wordpress.android.fluxc.network.xmlrpc.media;

import android.util.Base64;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.BaseUploadRequestBody;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
            + "<member><name>bits</name><value><base64>"; // bits
    private static final String APPEND_XML =
            "</base64></value></member></struct></value></param></params></methodCall>";

    private final String mPrependString;
    private final long mMediaSize;

    private long mMediaBytesWritten = 0;

    public XmlrpcUploadRequestBody(MediaModel media, ProgressListener listener, SiteModel site) {
        super(media, listener);

        File mediaFile = new File(media.getFilePath());
        mMediaSize = mediaFile.length();

        mPrependString = String.format(Locale.ENGLISH, PREPEND_XML_FORMAT,
                site.getSelfHostedSiteId(), site.getUsername(), site.getPassword(),
                media.getFileName(), media.getMimeType());
    }

    @Override
    protected float getProgress(long bytesWritten) {
        return (float) mMediaBytesWritten / mMediaSize;
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
        bufferedSink.writeUtf8(mPrependString);

        // write file to xml
        InputStream is = new DataInputStream(new FileInputStream(getMedia().getFilePath()));
        byte[] buffer = new byte[3600]; // you must use a 24bit multiple
        int length;
        String chunk;
        while ((length = is.read(buffer)) > 0) {
            chunk = Base64.encodeToString(buffer, 0, length, Base64.DEFAULT);
            mMediaBytesWritten += length;
            bufferedSink.writeUtf8(chunk);
        }
        is.close();

        // write remainder or XML
        bufferedSink.writeUtf8(APPEND_XML);

        bufferedSink.flush();
    }
}
