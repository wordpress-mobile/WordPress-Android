package org.wordpress.android.ui.reader.utils;

import java.util.HashMap;
import java.util.regex.Pattern;

public class ReaderEmbedScanner {
    private final String mContent;

    private final HashMap<Pattern, String> mKnownEmbeds = new HashMap<>();

    public ReaderEmbedScanner(String contentOfPost) {
        mContent = contentOfPost;
        mKnownEmbeds.put(Pattern.compile("<blockquote[^<>]class=\"instagram-", Pattern.CASE_INSENSITIVE),
                         "https://platform.instagram.com/en_US/embeds.js");
        mKnownEmbeds.put(Pattern.compile("<fb:post", Pattern.CASE_INSENSITIVE),
                         "https://connect.facebook.net/en_US/sdk.js#xfbml=1&amp;version=v2.8");
    }

    public void beginScan(ReaderHtmlUtils.HtmlScannerListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("HtmlScannerListener is required");
        }

        for (Pattern pattern : mKnownEmbeds.keySet()) {
            if (pattern.matcher(mContent).find()) {
                // Use the onTagFound callback to pass a URL. Not super clean, but avoid clutter with more kind
                // of listeners.
                listener.onTagFound("", mKnownEmbeds.get(pattern));
            }
        }
    }
}
