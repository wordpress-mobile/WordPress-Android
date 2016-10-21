package org.wordpress.android.util.helpers;

import android.text.Editable;
import android.text.Html;
import android.text.style.BulletSpan;
import android.text.style.LeadingMarginSpan;

import org.xml.sax.XMLReader;

import java.util.ArrayList;
import java.util.List;

/**
 * Handle tags that the Html class doesn't understand
 * Tweaked from source at http://stackoverflow.com/questions/4044509/android-how-to-use-the-html-taghandler
 */
public class WPHtmlTagHandler implements Html.TagHandler {

    private static final int SPAN_INDENT_WIDTH = 15;

    private int mListItemCount = 0;
    private List<String> mListParents = new ArrayList<>();

    @Override
    public void handleTag(final boolean opening, final String tag, Editable output,
                          final XMLReader xmlReader) {
        if (tag != null) {
            if (tag.equals("ul") || tag.equals("ol") || tag.equals("dd")) {
                if (opening) {
                    mListParents.add(tag);
                } else {
                    mListParents.remove(tag);
                }
                mListItemCount = 0;
            } else if (tag.equals("li") && !opening) {
                handleListTag(output);
            }
        }
    }

    private void handleListTag(Editable output) {
        int size = mListParents.size();
        if (size > 0 && output != null) {
            if ("ul".equals(mListParents.get(size - 1))) {
                output.append("\n");
                String[] split = output.toString().split("\n");
                int start = 0;
                if (split.length != 1) {
                    int lastIndex = split.length - 1;
                    start = output.length() - split[lastIndex].length() - 1;
                }
                output.setSpan(new BulletSpan(SPAN_INDENT_WIDTH * mListParents.size()), start, output.length(), 0);
            } else if ("ol".equals(mListParents.get(size - 1))) {
                mListItemCount++;
                output.append("\n");
                String[] split = output.toString().split("\n");
                int start = 0;
                if (split.length != 1) {
                    int lastIndex = split.length - 1;
                    start = output.length() - split[lastIndex].length() - 1;
                }
                output.insert(start, mListItemCount + ". ");
                output.setSpan(new LeadingMarginSpan.Standard(SPAN_INDENT_WIDTH * mListParents.size()), start,
                        output.length(), 0);
            }
        }
    }
}
