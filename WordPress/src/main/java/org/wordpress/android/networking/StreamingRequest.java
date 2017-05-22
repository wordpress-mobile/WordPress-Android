package org.wordpress.android.networking;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class StreamingRequest extends RequestBody {
    public static final int CHUNK_SIZE = 2048;

    private final File mFile;

    public StreamingRequest(File file) {
        mFile = file;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse("multipart/form-data");
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        Source source = null;
        try {
            source = Okio.source(mFile);

            while (source.read(sink.buffer(), CHUNK_SIZE) != -1) {
                sink.flush();
            }
        } finally {
            Util.closeQuietly(source);
        }
    }
};

