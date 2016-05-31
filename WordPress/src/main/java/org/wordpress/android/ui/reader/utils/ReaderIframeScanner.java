package org.wordpress.android.ui.reader.utils;

import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReaderIframeScanner {

    private final String mContent;

    private static final Pattern IFRAME_TAG_PATTERN = Pattern.compile(
            "<iframe(\\s+.*?)(?:src\\s*=\\s*(?:'|\")(.*?)(?:'|\"))(.*?)>",
            Pattern.DOTALL| Pattern.CASE_INSENSITIVE);

    public ReaderIframeScanner(String contentOfPost) {
        mContent = contentOfPost;
    }

    public void beginScan(ReaderHtmlUtils.HtmlScannerListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("HtmlScannerListener is required");
        }

        Matcher matcher = IFRAME_TAG_PATTERN.matcher(mContent);
        while (matcher.find()) {
            String tag = mContent.substring(matcher.start(), matcher.end());
            String src = ReaderHtmlUtils.getSrcAttrValue(tag);
            if (!TextUtils.isEmpty(src)) {
                listener.onTagFound(tag, src);
            }
        }
    }
}
