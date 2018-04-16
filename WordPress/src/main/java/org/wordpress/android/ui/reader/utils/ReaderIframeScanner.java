package org.wordpress.android.ui.reader.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReaderIframeScanner {
    private final String mContent;

    private static final Pattern IFRAME_TAG_PATTERN = Pattern.compile(
            ".*(<iframe\\s+.*src\\s*=\\s*'([^']+)'.*>).*",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    public ReaderIframeScanner(String contentOfPost) {
        mContent = contentOfPost;
    }

    public void beginScan(ReaderHtmlUtils.HtmlScannerListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("HtmlScannerListener is required");
        }

        Matcher matcher = IFRAME_TAG_PATTERN.matcher(mContent);
        while (matcher.find()) {
            String tag = matcher.group(1);
            String src = matcher.group(2);
            listener.onTagFound(tag, src);
        }
    }

    /*
     * scans the post for iframes containing usable videos, returns the first one found
     */
    public String getFirstUsableVideo() {
        Matcher matcher = IFRAME_TAG_PATTERN.matcher(mContent);
        while (matcher.find()) {
            String src = matcher.group(2);
            if (ReaderVideoUtils.canShowVideoThumbnail(src)) {
                return src;
            }
        }
        return null;
    }
}
