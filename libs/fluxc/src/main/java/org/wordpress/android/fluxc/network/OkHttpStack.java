package org.wordpress.android.fluxc.network;

import com.android.volley.toolbox.HurlStack;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.OkUrlFactory;

public class OkHttpStack extends HurlStack {
    private final OkUrlFactory mOkUrlFactory;

    public OkHttpStack(OkUrlFactory okUrlFactory) {
        if (okUrlFactory == null) {
            throw new NullPointerException("Client must not be null.");
        }
        mOkUrlFactory = okUrlFactory;
    }

    @Override
    protected HttpURLConnection createConnection(URL url) throws IOException {
        return mOkUrlFactory.open(url);
    }
}
