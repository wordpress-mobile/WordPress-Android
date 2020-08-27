package org.wordpress.android.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AlignmentSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.ParagraphStyle;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;

import org.ccil.cowan.tagsoup.Parser;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostImmutableModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.WPImageSpan;
import org.wordpress.android.util.helpers.WPUnderlineSpan;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Locale;

import javax.inject.Inject;

public class HtmlToSpannedConverter implements ContentHandler {
    private static final float[] HEADER_SIZES = {1.5f, 1.4f, 1.3f, 1.2f, 1.1f, 1f};

    private String mSource;
    private XMLReader mReader;
    private SpannableStringBuilder mSpannableStringBuilder;
    private WPHtml.ImageGetter mImageGetter;
    private String mysteryTagContent;
    private boolean mysteryTagFound;
    private int mMaxImageWidth;
    private Context mContext;
    private PostImmutableModel mPost;

    private String mysteryTagName;

    @Inject MediaStore mMediaStore;

    public HtmlToSpannedConverter(String source,
                                  WPHtml.ImageGetter imageGetter, WPHtml.TagHandler tagHandler,
                                  Parser parser, Context context, PostImmutableModel p, int maxImageWidth) {
        ((WordPress) context.getApplicationContext()).component().inject(this);

        mSource = source;
        mSpannableStringBuilder = new SpannableStringBuilder();
        mImageGetter = imageGetter;
        mReader = parser;
        mysteryTagContent = "";
        mysteryTagName = null;
        mContext = context;
        mPost = p;
        mMaxImageWidth = maxImageWidth;
    }

    public Spanned convert() {
        mReader.setContentHandler(this);
        try {
            mReader.parse(new InputSource(new StringReader(mSource)));
        } catch (IOException e) {
            // We are reading from a string. There should not be IO problems.
            throw new RuntimeException(e);
        } catch (SAXException e) {
            // TagSoup doesn't throw parse exceptions.
            throw new RuntimeException(e);
        }

        // Fix flags and range for paragraph-type markup.
        Object[] obj = mSpannableStringBuilder.getSpans(0,
                                                        mSpannableStringBuilder.length(), ParagraphStyle.class);
        for (int i = 0; i < obj.length; i++) {
            int start = mSpannableStringBuilder.getSpanStart(obj[i]);
            int end = mSpannableStringBuilder.getSpanEnd(obj[i]);

            // If the last line of the range is blank, back off by one.
            if (end - 2 >= 0) {
                if (mSpannableStringBuilder.charAt(end - 1) == '\n'
                    && mSpannableStringBuilder.charAt(end - 2) == '\n') {
                    end--;
                }
            }

            if (end == start) {
                mSpannableStringBuilder.removeSpan(obj[i]);
            } else {
                try {
                    mSpannableStringBuilder.setSpan(obj[i], start, end,
                                                    Spannable.SPAN_PARAGRAPH);
                } catch (Exception e) {
                }
            }
        }

        return mSpannableStringBuilder;
    }

    private void handleStartTag(String tag, Attributes attributes) {
        if (!mysteryTagFound) {
            if (mPost != null) {
                if (!mPost.isLocalDraft()) {
                    if (tag.equalsIgnoreCase("img")) {
                        startImg(mSpannableStringBuilder, attributes,
                                 mImageGetter);
                    }

                    return;
                }
            }

            if (tag.equalsIgnoreCase("br")) {
                // We don't need to handle this. TagSoup will ensure that
                // there's a
                // </br> for each <br>
                // so we can safely emite the linebreaks when we handle the
                // close
                // tag.
            } else if (tag.equalsIgnoreCase("p")) {
                handleP(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("div")) {
                handleP(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("em")) {
                start(mSpannableStringBuilder, new Italic());
            } else if (tag.equalsIgnoreCase("b")) {
                start(mSpannableStringBuilder, new Bold());
            } else if (tag.equalsIgnoreCase("strong")) {
                start(mSpannableStringBuilder, new Bold());
            } else if (tag.equalsIgnoreCase("cite")) {
                start(mSpannableStringBuilder, new Italic());
            } else if (tag.equalsIgnoreCase("dfn")) {
                start(mSpannableStringBuilder, new Italic());
            } else if (tag.equalsIgnoreCase("i")) {
                start(mSpannableStringBuilder, new Italic());
            } else if (tag.equalsIgnoreCase("big")) {
                start(mSpannableStringBuilder, new Big());
            } else if (tag.equalsIgnoreCase("small")) {
                start(mSpannableStringBuilder, new Small());
            } else if (tag.equalsIgnoreCase("font")) {
                startFont(mSpannableStringBuilder, attributes);
            } else if (tag.equalsIgnoreCase("blockquote")) {
                handleP(mSpannableStringBuilder);
                start(mSpannableStringBuilder, new Blockquote());
            } else if (tag.equalsIgnoreCase("tt")) {
                start(mSpannableStringBuilder, new Monospace());
            } else if (tag.equalsIgnoreCase("a")) {
                startA(mSpannableStringBuilder, attributes);
            } else if (tag.equalsIgnoreCase("u")) {
                start(mSpannableStringBuilder, new Underline());
            } else if (tag.equalsIgnoreCase("sup")) {
                start(mSpannableStringBuilder, new Super());
            } else if (tag.equalsIgnoreCase("sub")) {
                start(mSpannableStringBuilder, new Sub());
            } else if (tag.equalsIgnoreCase("strike")) {
                start(mSpannableStringBuilder, new Strike());
            } else if (tag.length() == 2
                       && Character.toLowerCase(tag.charAt(0)) == 'h'
                       && tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
                handleP(mSpannableStringBuilder);
                start(mSpannableStringBuilder, new Header(tag.charAt(1) - '1'));
            } else if (tag.equalsIgnoreCase("img")) {
                startImg(mSpannableStringBuilder, attributes, mImageGetter);
            } else {
                if (tag.equalsIgnoreCase("html") || tag.equalsIgnoreCase("body")) {
                    return;
                }

                mysteryTagFound = true;
                mysteryTagName = tag;
            }
            // mTagHandler.handleTag(true, tag, mSpannableStringBuilder,
            // mReader, mysteryTagContent);
        }
    }

    private void handleEndTag(String tag) {
        if (mPost != null) {
            if (!mPost.isLocalDraft()) {
                return;
            }
        }
        if (!mysteryTagFound) {
            if (tag.equalsIgnoreCase("br")) {
                handleBr(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("p")) {
                handleP(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("div")) {
                handleP(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("em")) {
                end(mSpannableStringBuilder, Italic.class, new StyleSpan(
                        Typeface.ITALIC));
            } else if (tag.equalsIgnoreCase("b")) {
                end(mSpannableStringBuilder, Bold.class, new StyleSpan(
                        Typeface.BOLD));
            } else if (tag.equalsIgnoreCase("strong")) {
                end(mSpannableStringBuilder, Bold.class, new StyleSpan(
                        Typeface.BOLD));
            } else if (tag.equalsIgnoreCase("cite")) {
                end(mSpannableStringBuilder, Italic.class, new StyleSpan(
                        Typeface.ITALIC));
            } else if (tag.equalsIgnoreCase("dfn")) {
                end(mSpannableStringBuilder, Italic.class, new StyleSpan(
                        Typeface.ITALIC));
            } else if (tag.equalsIgnoreCase("i")) {
                end(mSpannableStringBuilder, Italic.class, new StyleSpan(
                        Typeface.ITALIC));
            } else if (tag.equalsIgnoreCase("big")) {
                end(mSpannableStringBuilder, Big.class, new RelativeSizeSpan(
                        1.25f));
            } else if (tag.equalsIgnoreCase("small")) {
                end(mSpannableStringBuilder, Small.class, new RelativeSizeSpan(
                        0.8f));
            } else if (tag.equalsIgnoreCase("font")) {
                endFont(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("blockquote")) {
                handleP(mSpannableStringBuilder);
                end(mSpannableStringBuilder, Blockquote.class, new QuoteSpan());
            } else if (tag.equalsIgnoreCase("tt")) {
                end(mSpannableStringBuilder, Monospace.class, new TypefaceSpan(
                        "monospace"));
            } else if (tag.equalsIgnoreCase("a")) {
                endA(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("u")) {
                end(mSpannableStringBuilder, Underline.class,
                    new WPUnderlineSpan());
            } else if (tag.equalsIgnoreCase("sup")) {
                end(mSpannableStringBuilder, Super.class, new SuperscriptSpan());
            } else if (tag.equalsIgnoreCase("sub")) {
                end(mSpannableStringBuilder, Sub.class, new SubscriptSpan());
            } else if (tag.equalsIgnoreCase("strike")) {
                end(mSpannableStringBuilder, Strike.class,
                    new StrikethroughSpan());
            } else if (tag.length() == 2
                       && Character.toLowerCase(tag.charAt(0)) == 'h'
                       && tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
                handleP(mSpannableStringBuilder);
                endHeader(mSpannableStringBuilder);
            }
        } else {
            if (tag.equalsIgnoreCase("html") || tag.equalsIgnoreCase("body")) {
                return;
            }

            if (mysteryTagName.equals(tag)) {
                mysteryTagFound = false;
                mSpannableStringBuilder.append(mysteryTagContent);
            }
            // mTagHandler.handleTag(false, tag, mSpannableStringBuilder,
            // mReader,
            // mysteryTagContent);
        }
    }

    private static void handleP(SpannableStringBuilder text) {
        int len = text.length();

        if (len >= 1 && text.charAt(len - 1) == '\n') {
            if (len >= 2 && text.charAt(len - 2) == '\n') {
                return;
            }

            text.append("\n");
            return;
        }

        if (len != 0) {
            text.append("\n\n");
        }
    }

    private static void handleBr(SpannableStringBuilder text) {
        text.append("\n");
    }

    private static Object getLast(Spanned text, Class<?> kind) {
        /*
         * This knows that the last returned object from getSpans() will be the
         * most recently added.
         */
        Object[] objs = text.getSpans(0, text.length(), kind);

        if (objs.length == 0) {
            return null;
        } else {
            return objs[objs.length - 1];
        }
    }

    private static void start(SpannableStringBuilder text, Object mark) {
        int len = text.length();
        text.setSpan(mark, len, len, Spannable.SPAN_MARK_MARK);
    }

    private static void end(SpannableStringBuilder text, Class<?> kind,
                            Object repl) {
        int len = text.length();
        Object obj = getLast(text, kind);
        int where = text.getSpanStart(obj);
        if (where < 0) {
            where = 0;
        }

        text.removeSpan(obj);

        if (where != len) {
            text.setSpan(repl, where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return;
    }

    private void startImg(SpannableStringBuilder text, Attributes attributes, WPHtml.ImageGetter img) {
        if (mContext == null) {
            return;
        }

        String src = attributes.getValue("android-uri");

        Bitmap resizedBitmap = null;
        try {
            resizedBitmap = ImageUtils.getWPImageSpanThumbnailFromFilePath(mContext, src, mMaxImageWidth);
            if (resizedBitmap == null && src != null) {
                if (src.contains("video")) {
                    resizedBitmap = BitmapFactory.decodeResource(
                            mContext.getResources(), org.wordpress.android.editor.R.drawable.media_movieclip);
                } else {
                    resizedBitmap = BitmapFactory.decodeResource(
                            mContext.getResources(), org.wordpress.android.R.drawable.media_image_placeholder);
                }
            }
        } catch (OutOfMemoryError e) {
            AppLog.e(T.UTILS, "Out of memory in HtmlToSpannedConverter.startImg", e);
        }

        if (resizedBitmap != null) {
            int len = text.length();
            text.append("\uFFFC");

            Uri curStream = Uri.parse(src);

            if (curStream == null) {
                return;
            }

            WPImageSpan is = new WPImageSpan(mContext, resizedBitmap, curStream);

            // Get the MediaFile data from db
            MediaModel mediaModel = mMediaStore.getMediaForPostWithPath(mPost, src);
            MediaFile mediaFile = FluxCUtils.mediaFileFromMediaModel(mediaModel);

            if (mediaFile != null) {
                is.setMediaFile(mediaFile);
                is.setImageSource(curStream);
                text.setSpan(is, len, text.length(),
                             Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                AlignmentSpan.Standard as = new AlignmentSpan.Standard(
                        Layout.Alignment.ALIGN_CENTER);
                text.setSpan(as, text.getSpanStart(is), text.getSpanEnd(is),
                             Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } else if (mPost != null) {
            if (mPost.isLocalDraft()) {
                if (attributes != null) {
                    text.append("<img");
                    for (int i = 0; i < attributes.getLength(); i++) {
                        String aName = attributes.getLocalName(i); // Attr name
                        if ("".equals(aName)) {
                            aName = attributes.getQName(i);
                        }
                        text.append(" ");
                        text.append(aName + "=\"" + attributes.getValue(i) + "\"");
                    }
                    text.append(" />\n");
                }
            }
        } else if (src == null) {
            // get regular src value from <img/> tag's src attribute
            src = attributes.getValue("", "src");
            Drawable d = null;

            if (img != null) {
                d = img.getDrawable(src);
            }

            if (d != null) {
                int len = text.length();
                text.append("\uFFFC");

                text.setSpan(new ImageSpan(d, src), len, text.length(),
                             Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                // noop - we're not showing a default image here
            }
        }
    }

    private static void startFont(SpannableStringBuilder text,
                                  Attributes attributes) {
        String color = attributes.getValue("", "color");
        String face = attributes.getValue("", "face");

        int len = text.length();
        text.setSpan(new Font(color, face), len, len, Spannable.SPAN_MARK_MARK);
    }

    private static void endFont(SpannableStringBuilder text) {
        int len = text.length();
        Object obj = getLast(text, Font.class);
        int where = text.getSpanStart(obj);

        text.removeSpan(obj);

        if (where != len) {
            Font f = (Font) obj;

            if (!TextUtils.isEmpty(f.mColor)) {
                if (f.mColor.startsWith("@")) {
                    Resources res = Resources.getSystem();
                    String name = f.mColor.substring(1);
                    int colorRes = res.getIdentifier(name, "color", "android");
                    if (colorRes != 0) {
                        ColorStateList colors = res.getColorStateList(colorRes);
                        text.setSpan(new TextAppearanceSpan(null, 0, 0, colors,
                                                            null), where, len,
                                     Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                } else {
                    int c = getHtmlColor(f.mColor);
                    if (c != -1) {
                        text.setSpan(new ForegroundColorSpan(c | 0xFF000000),
                                     where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }

            if (f.mFace != null) {
                text.setSpan(new TypefaceSpan(f.mFace), where, len,
                             Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private static void startA(SpannableStringBuilder text,
                               Attributes attributes) {
        String href = attributes.getValue("", "href");

        int len = text.length();
        text.setSpan(new Href(href), len, len, Spannable.SPAN_MARK_MARK);
    }

    private static void endA(SpannableStringBuilder text) {
        int len = text.length();
        Object obj = getLast(text, Href.class);
        int where = text.getSpanStart(obj);

        text.removeSpan(obj);

        if (where != len) {
            Href h = (Href) obj;

            if (h != null) {
                if (h.mHref != null) {
                    text.setSpan(new URLSpan(h.mHref), where, len,
                                 Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }

    private static void endHeader(SpannableStringBuilder text) {
        int len = text.length();
        Object obj = getLast(text, Header.class);

        int where = text.getSpanStart(obj);

        text.removeSpan(obj);

        // Back off not to change only the text, not the blank line.
        while (len > where && text.charAt(len - 1) == '\n') {
            len--;
        }

        if (where != len) {
            Header h = (Header) obj;

            text.setSpan(new RelativeSizeSpan(HEADER_SIZES[h.mLevel]), where,
                         len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new StyleSpan(Typeface.BOLD), where, len,
                         Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    public void setDocumentLocator(Locator locator) {
    }

    public void startDocument() throws SAXException {
    }

    public void endDocument() throws SAXException {
    }

    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
    }

    public void endPrefixMapping(String prefix) throws SAXException {
    }

    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {
        if (!mysteryTagFound) {
            mysteryTagContent = "";
        }

        String eName = localName; // element name
        if ("".equals(eName)) {
            eName = qName; // not namespace-aware
        }
        mysteryTagContent += "<" + eName;
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                String aName = attributes.getLocalName(i); // Attr name
                if ("".equals(aName)) {
                    aName = attributes.getQName(i);
                }
                mysteryTagContent += " ";
                mysteryTagContent += aName + "=\"" + attributes.getValue(i)
                                     + "\"";
            }
        }
        mysteryTagContent += ">";

        handleStartTag(localName, attributes);
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (mysteryTagFound) {
            mysteryTagContent += "</" + localName + ">" + "\n";
        }
        handleEndTag(localName);
    }

    public void characters(char[] ch, int start, int length)
            throws SAXException {
        StringBuilder sb = new StringBuilder();

        /*
         * Ignore whitespace that immediately follows other whitespace; newlines
         * count as spaces.
         */

        for (int i = 0; i < length; i++) {
            char c = ch[i + start];

            if (c == ' ' || c == '\n') {
                char pred;
                int len = sb.length();

                if (len == 0) {
                    len = mSpannableStringBuilder.length();

                    if (len == 0) {
                        pred = '\n';
                    } else {
                        pred = mSpannableStringBuilder.charAt(len - 1);
                    }
                } else {
                    pred = sb.charAt(len - 1);
                }

                if (pred != ' ' && pred != '\n') {
                    sb.append(' ');
                }
            } else {
                sb.append(c);
            }
        }

        try {
            if (mysteryTagFound) {
                if (sb.length() < length) {
                    mysteryTagContent += sb.toString().substring(start,
                                                                 length - 1);
                } else {
                    mysteryTagContent += sb.toString().substring(start, length);
                }
            } else {
                mSpannableStringBuilder.append(sb);
            }
        } catch (RuntimeException e) {
            AppLog.e(AppLog.T.UTILS, e);
        }
    }

    public void ignorableWhitespace(char[] ch, int start, int length)
            throws SAXException {
    }

    public void processingInstruction(String target, String data)
            throws SAXException {
    }

    public void skippedEntity(String name) throws SAXException {
    }

    private static class Bold {
    }

    private static class Italic {
    }

    private static class Underline {
    }

    private static class Big {
    }

    private static class Small {
    }

    private static class Monospace {
    }

    private static class Blockquote {
    }

    private static class Super {
    }

    private static class Sub {
    }

    private static class Strike {
    }

    private static class Font {
        public String mColor;
        public String mFace;

        Font(String color, String face) {
            mColor = color;
            mFace = face;
        }
    }

    private static class Href {
        public String mHref;

        Href(String href) {
            mHref = href;
        }
    }

    private static class Header {
        private int mLevel;

        Header(int level) {
            mLevel = level;
        }
    }

    private static final HashMap<String, Integer> COLORS = buildColorMap();

    private static HashMap<String, Integer> buildColorMap() {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        map.put("aqua", 0x00FFFF);
        map.put("black", 0x000000);
        map.put("blue", 0x0000FF);
        map.put("fuchsia", 0xFF00FF);
        map.put("green", 0x008000);
        map.put("grey", 0x808080);
        map.put("lime", 0x00FF00);
        map.put("maroon", 0x800000);
        map.put("navy", 0x000080);
        map.put("olive", 0x808000);
        map.put("purple", 0x800080);
        map.put("red", 0xFF0000);
        map.put("silver", 0xC0C0C0);
        map.put("teal", 0x008080);
        map.put("white", 0xFFFFFF);
        map.put("yellow", 0xFFFF00);
        return map;
    }

    /**
     * Converts an HTML color (named or numeric) to an integer RGB value.
     *
     * @param color Non-null color string.
     * @return A color value, or {@code -1} if the color string could not be
     * interpreted.
     */
    private static int getHtmlColor(String color) {
        Integer i = COLORS.get(color.toLowerCase(Locale.ROOT));
        if (i != null) {
            return i;
        } else {
            try {
                return convertValueToInt(color, -1);
            } catch (NumberFormatException nfe) {
                return -1;
            }
        }
    }

    public static final int convertValueToInt(CharSequence charSeq,
                                              int defaultValue) {
        if (null == charSeq) {
            return defaultValue;
        }

        String nm = charSeq.toString();

        // XXX This code is copied from Integer.decode() so we don't
        // have to instantiate an Integer!

        int sign = 1;
        int index = 0;
        int len = nm.length();
        int base = 10;

        if ('-' == nm.charAt(0)) {
            sign = -1;
            index++;
        }

        if ('0' == nm.charAt(index)) {
            // Quick check for a zero by itself
            if (index == (len - 1)) {
                return 0;
            }

            char c = nm.charAt(index + 1);

            if ('x' == c || 'X' == c) {
                index += 2;
                base = 16;
            } else {
                index++;
                base = 8;
            }
        } else if ('#' == nm.charAt(index)) {
            index++;
            base = 16;
        }

        return Integer.parseInt(nm.substring(index), base) * sign;
    }
}
