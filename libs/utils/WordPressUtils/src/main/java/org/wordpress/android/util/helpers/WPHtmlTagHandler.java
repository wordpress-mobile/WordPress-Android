package org.wordpress.android.util.helpers;

import android.text.Editable;
import android.text.Html;
import android.text.style.BulletSpan;
import android.text.style.LeadingMarginSpan;

import org.xml.sax.XMLReader;

import java.util.Vector;

/**
 * Handle tags that the Html class doesn't understand
 * Tweaked from source at http://stackoverflow.com/questions/4044509/android-how-to-use-the-html-taghandler
 */
public class WPHtmlTagHandler implements Html.TagHandler {
    private int mListItemCount = 0;
    private Vector<String> mListParents = new Vector<String>();

    @Override
    public void handleTag(final boolean opening, final String tag, Editable output,
                          final XMLReader xmlReader) {
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

    private void handleListTag(Editable output) {
        if (mListParents.lastElement().equals("ul")) {
            output.append("\n");
            String[] split = output.toString().split("\n");
            int start = 0;
            if (split.length != 1) {
                int lastIndex = split.length - 1;
                start = output.length() - split[lastIndex].length() - 1;
            }
            output.setSpan(new BulletSpan(15 * mListParents.size()), start, output.length(), 0);
        } else if (mListParents.lastElement().equals("ol")) {
            mListItemCount++;
            output.append("\n");
            String[] split = output.toString().split("\n");
            int start = 0;
            if (split.length != 1) {
                int lastIndex = split.length - 1;
                start = output.length() - split[lastIndex].length() - 1;
            }
            output.insert(start, mListItemCount + ". ");
            output.setSpan(new LeadingMarginSpan.Standard(15 * mListParents.size()), start,
                    output.length(), 0);
        }
    }
}
