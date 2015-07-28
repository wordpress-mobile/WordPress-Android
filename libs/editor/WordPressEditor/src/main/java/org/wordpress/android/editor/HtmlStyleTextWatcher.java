package org.wordpress.android.editor;

import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

public class HtmlStyleTextWatcher implements TextWatcher {
    private enum Operation {
        INSERT, DELETE, REPLACE, NONE
    }

    private int mOffset;
    private CharSequence mModifiedText;
    private Operation mLastOperation;

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (s == null) {
            return;
        }

        int lastCharacterLocation = start + count - 1;
        if (s.length() > lastCharacterLocation && lastCharacterLocation >= 0) {
            if (after < count) {
                if (after > 0) {
                    // Text was deleted and replaced by some other text
                    mLastOperation = Operation.REPLACE;
                } else {
                    // Text was deleted only
                    mLastOperation = Operation.DELETE;
                }

                mOffset = start;
                mModifiedText = s.subSequence(start + after, start + count);
            }
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s == null) {
            return;
        }

        int lastCharacterLocation = start + count - 1;
        if (s.length() > lastCharacterLocation) {
            if (count > 0) {
                if (before > 0) {
                    // Text was added, replacing some existing text
                    mLastOperation = Operation.REPLACE;
                    mModifiedText = s.subSequence(start, start + count);
                } else {
                    // Text was added only
                    mLastOperation = Operation.INSERT;
                    mOffset = start;
                    mModifiedText = s.subSequence(start + before, start + count);
                }
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (mModifiedText == null || s == null) {
            return;
        }

        SpanRange spanRange;

        // If the modified text included a tag or entity symbol ("<", ">", "&" or ";"), find its match and restyle
        if (mModifiedText.toString().contains("<")) {
            spanRange = getRespanRangeForChangedOpeningSymbol(s, "<");
        } else if (mModifiedText.toString().contains(">")) {
            spanRange = getRespanRangeForChangedClosingSymbol(s, ">");
        } else if (mModifiedText.toString().contains("&")) {
            spanRange = getRespanRangeForChangedOpeningSymbol(s, "&");
        } else if (mModifiedText.toString().contains(";")) {
            spanRange = getRespanRangeForChangedClosingSymbol(s, ";");
        } else {
            // If the modified text didn't include any tag or entity symbols, restyle if the modified text is inside
            // a tag or entity
            spanRange = getRespanRangeForNormalText(s, "<");
            if (spanRange == null) {
                spanRange = getRespanRangeForNormalText(s, "&");
            }
        }

        if (spanRange != null) {
            updateSpans(s, spanRange);
        }

        mModifiedText = null;
        mLastOperation = Operation.NONE;
    }

    /**
     * For changes made which contain at least one opening symbol (e.g. '<' or '&'), whether added or deleted, returns
     * the range of text which should have its style reapplied.
     * @param content the content after modification
     * @param openingSymbol the opening symbol recognized (e.g. '<' or '&')
     * @return the range of characters to re-apply spans to
     */
    protected SpanRange getRespanRangeForChangedOpeningSymbol(Editable content, String openingSymbol) {
        // For simplicity, re-parse the document if text was replaced
        if (mLastOperation == Operation.REPLACE) {
            return new SpanRange(0, content.length());
        }

        String closingSymbol = getMatchingSymbol(openingSymbol);

        int firstOpeningTagLoc = mOffset + mModifiedText.toString().indexOf(openingSymbol);
        int closingTagLoc;
        if (mLastOperation == Operation.INSERT) {
            // Apply span from the first added opening symbol until the closing symbol in the content matching the
            // last added opening symbol
            // e.g. pasting "<b><" before "/b>" - we want the span to be applied to all of "<b></b>"
            int lastOpeningTagLoc = mOffset + mModifiedText.toString().lastIndexOf(openingSymbol);
            closingTagLoc = content.toString().indexOf(closingSymbol, lastOpeningTagLoc);
        } else {
            // Apply span until the first closing tag that appears after the deleted text
            closingTagLoc = content.toString().indexOf(closingSymbol, mOffset);
        }

        if (closingTagLoc > 0) {
            return new SpanRange(firstOpeningTagLoc, closingTagLoc + 1);
        }
        return null;
    }

    /**
     * For changes made which contain at least one closing symbol (e.g. '>' or ';') and no opening symbols, whether
     * added or deleted, returns the range of text which should have its style reapplied.
     * @param content the content after modification
     * @param closingSymbol the closing symbol recognized (e.g. '>' or ';')
     * @return the range of characters to re-apply spans to
     */
    protected SpanRange getRespanRangeForChangedClosingSymbol(Editable content, String closingSymbol) {
        // For simplicity, re-parse the document if text was replaced
        if (mLastOperation == Operation.REPLACE) {
            return new SpanRange(0, content.length());
        }

        String openingSymbol = getMatchingSymbol(closingSymbol);

        int firstClosingTagInModLoc = mOffset + mModifiedText.toString().indexOf(closingSymbol);
        int firstClosingTagAfterModLoc = content.toString().indexOf(closingSymbol, mOffset + mModifiedText.length());

        int openingTagLoc = content.toString().lastIndexOf(openingSymbol, firstClosingTagInModLoc - 1);
        if (openingTagLoc >= 0) {
            if (firstClosingTagAfterModLoc >= 0) {
                return new SpanRange(openingTagLoc, firstClosingTagAfterModLoc + 1);
            } else {
                return new SpanRange(openingTagLoc, content.length());
            }
        }
        return null;
    }

    /**
     * For changes made which contain no opening or closing symbols, checks whether the changed text is inside a tag,
     * and if so returns the range of text which should have its style reapplied.
     * @param content the content after modification
     * @param openingSymbol the opening symbol of the tag to check for (e.g. '<' or '&')
     * @return the range of characters to re-apply spans to
     */
    protected SpanRange getRespanRangeForNormalText(Editable content, String openingSymbol) {
        String closingSymbol = getMatchingSymbol(openingSymbol);

        int openingTagLoc = content.toString().lastIndexOf(openingSymbol, mOffset);
        if (openingTagLoc >= 0) {
            int closingTagLoc = content.toString().indexOf(closingSymbol, openingTagLoc);
            if (closingTagLoc >= mOffset) {
                return new SpanRange(openingTagLoc, closingTagLoc + 1);
            }
        }
        return null;
    }

    /**
     * Clears and re-applies spans to {@code content} within range {@code spanRange} according to rules in
     * {@link HtmlStyleUtils}.
     * @param content the content to re-style
     * @param spanRange the range within {@code content} to be re-styled
     */
    protected void updateSpans(Spannable content, SpanRange spanRange) {
        int spanStart = spanRange.getOpeningTagLoc();
        int spanEnd = spanRange.getClosingTagLoc();

        if (spanStart > content.length() || spanEnd > content.length()) {
            AppLog.d(T.EDITOR, "The specified span range was beyond the Spannable's length");
            return;
        }

        HtmlStyleUtils.clearSpans(content, spanStart, spanEnd);
        HtmlStyleUtils.styleHtmlForDisplay(content, spanStart, spanEnd);
    }

    /**
     * Returns the closing/opening symbol corresponding to the given opening/closing symbol.
     */
    private String getMatchingSymbol(String symbol) {
        switch(symbol) {
            case "<":
                return ">";
            case ">":
                return "<";
            case "&":
                return ";";
            case ";":
                return "&";
            default:
                return "";
        }
    }

    /**
     * Stores a pair of integers describing a range of values.
     */
    protected static class SpanRange {
        private final int mOpeningTagLoc;
        private final int mClosingTagLoc;

        public SpanRange(int openingTagLoc, int closingTagLoc) {
            mOpeningTagLoc = openingTagLoc;
            mClosingTagLoc = closingTagLoc;
        }

        public int getOpeningTagLoc() {
            return mOpeningTagLoc;
        }

        public int getClosingTagLoc() {
            return mClosingTagLoc;
        }
    }
}