package org.wordpress.android.util;

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan;
import android.text.style.CharacterStyle;
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

import org.ccil.cowan.tagsoup.HTMLSchema;
import org.ccil.cowan.tagsoup.Parser;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.MediaFile;
import org.wordpress.android.models.MediaGallery;
import org.wordpress.android.models.Post;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * This class processes HTML strings into displayable styled text. Not all HTML
 * tags are supported.
 */
public class WPHtml {
    /**
     * Customzed QuoteSpan for use in SpannableString's
     */
    public static class WPQuoteSpan extends QuoteSpan {
        private static final int STRIPE_WIDTH=5;
        private static final int GAP_WIDTH=20;
        public static final int STRIPE_COLOR=0xFF21759B;
        private static final boolean IS_ICS= Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;

        public WPQuoteSpan(){
            super(STRIPE_COLOR);
        }

        @Override
        public int getLeadingMargin(boolean first){
            if (IS_ICS) {
                int margin = GAP_WIDTH * 2 + STRIPE_WIDTH;
                return margin;
            } else {
                return super.getLeadingMargin(first);
            }
        }
        /**
         * Draw a nice thick gray bar if Ice Cream Sandwhich or newer. There's a
         * bug on older devices that does not respect the increased margin.
         */
        @Override
        public void drawLeadingMargin(Canvas c, Paint p, int x, int dir,
                                      int top, int baseline, int bottom,
                                      CharSequence text, int start, int end,
                                      boolean first, Layout layout) {

            if (IS_ICS) {
                Paint.Style style = p.getStyle();
                int color = p.getColor();

                p.setStyle(Paint.Style.FILL);
                p.setColor(STRIPE_COLOR);

                c.drawRect(GAP_WIDTH + x, top, x + dir * STRIPE_WIDTH, bottom, p);

                p.setStyle(style);
                p.setColor(color);
            } else {
                super.drawLeadingMargin(c, p, x, dir, top, baseline, bottom,
                                        text, start, end, first, layout);
            }
        }
    }

    /**
     * Retrieves images for HTML &lt;img&gt; tags.
     */
    public static interface ImageGetter {
        /**
         * This method is called when the HTML parser encounters an &lt;img&gt;
         * tag. The <code>source</code> argument is the string from the "src"
         * attribute; the return value should be a Drawable representation of
         * the image or <code>null</code> for a generic replacement image. Make
         * sure you call setBounds() on your Drawable if it doesn't already have
         * its bounds set.
         */
        public Drawable getDrawable(String source);
    }

    /**
     * Is notified when HTML tags are encountered that the parser does not know
     * how to interpret.
     */
    public static interface TagHandler {
        /**
         * This method will be called whenn the HTML parser encounters a tag
         * that it does not know how to interpret.
         *
         * @param mysteryTagContent
         */
        public void handleTag(boolean opening, String tag, Editable output,
                XMLReader xmlReader, String mysteryTagContent);
    }

    private WPHtml() {
    }

    /**
     * Returns displayable styled text from the provided HTML string. Any
     * &lt;img&gt; tags in the HTML will display as a generic replacement image
     * which your program can then go through and replace with real images.
     *
     * <p>
     * This uses TagSoup to handle real HTML, including all of the brokenness
     * found in the wild.
     */
    public static Spanned fromHtml(String source, Context ctx, Post p) {
        return fromHtml(source, null, null, ctx, p);
    }

    /**
     * Lazy initialization holder for HTML parser. This class will a) be
     * preloaded by the zygote, or b) not loaded until absolutely necessary.
     */
    private static class HtmlParser {
        private static final HTMLSchema schema = new HTMLSchema();
    }

    /**
     * Returns displayable styled text from the provided HTML string. Any
     * &lt;img&gt; tags in the HTML will use the specified ImageGetter to
     * request a representation of the image (use null if you don't want this)
     * and the specified TagHandler to handle unknown tags (specify null if you
     * don't want this).
     *
     * <p>
     * This uses TagSoup to handle real HTML, including all of the brokenness
     * found in the wild.
     */
    public static Spanned fromHtml(String source, ImageGetter imageGetter,
            TagHandler tagHandler, Context ctx, Post post) {
        Parser parser = new Parser();
        try {
            parser.setProperty(Parser.schemaProperty, HtmlParser.schema);
        } catch (org.xml.sax.SAXNotRecognizedException e) {
            // Should not happen.
            throw new RuntimeException(e);
        } catch (org.xml.sax.SAXNotSupportedException e) {
            // Should not happen.
            throw new RuntimeException(e);
        }

        HtmlToSpannedConverter converter = new HtmlToSpannedConverter(source,
                imageGetter, tagHandler, parser, ctx, post);
        return converter.convert();
    }

    /**
     * Returns an HTML representation of the provided Spanned text.
     */
    public static String toHtml(Spanned text) {
        StringBuilder out = new StringBuilder();
        withinHtml(out, text);
        return out.toString();
    }

    private static void withinHtml(StringBuilder out, Spanned text) {
        int len = text.length();

        int next;
        for (int i = 0; i < text.length(); i = next) {
            next = text.nextSpanTransition(i, len, ParagraphStyle.class);
            /*ParagraphStyle[] style = text.getSpans(i, next,
                    ParagraphStyle.class);
            String elements = " ";
            boolean needDiv = false;

            for (int j = 0; j < style.length; j++) {
                if (style[j] instanceof AlignmentSpan) {
                    Layout.Alignment align = ((AlignmentSpan) style[j])
                            .getAlignment();
                    needDiv = true;
                    if (align == Layout.Alignment.ALIGN_CENTER) {
                        elements = "align=\"center\" " + elements;
                    } else if (align == Layout.Alignment.ALIGN_OPPOSITE) {
                        elements = "align=\"right\" " + elements;
                    } else {
                        elements = "align=\"left\" " + elements;
                    }
                }
            }
            if (needDiv) {
                out.append("<div " + elements + ">");
            }*/

            withinDiv(out, text, i, next);

            /*if (needDiv) {
                out.append("</div>");
            }*/
        }
    }

    @SuppressWarnings("unused")
    private static void withinDiv(StringBuilder out, Spanned text, int start,
            int end) {
        int next;
        for (int i = start; i < end; i = next) {
            next = text.nextSpanTransition(i, end, QuoteSpan.class);
            QuoteSpan[] quotes = text.getSpans(i, next, QuoteSpan.class);

            for (QuoteSpan quote : quotes) {
                out.append("<blockquote>");
            }

            withinBlockquote(out, text, i, next);

            for (QuoteSpan quote : quotes) {
                out.append("</blockquote>\n");
            }
        }
    }

    private static void withinBlockquote(StringBuilder out, Spanned text,
            int start, int end) {
        out.append("<p>");

        int next;
        for (int i = start; i < end; i = next) {
            next = TextUtils.indexOf(text, '\n', i, end);
            if (next < 0) {
                next = end;
            }

            int nl = 0;

            while (next < end && text.charAt(next) == '\n') {
                nl++;
                next++;
            }

            withinParagraph(out, text, i, next - nl, nl, next == end);
        }

        out.append("</p>\n");
    }

    private static void withinParagraph(StringBuilder out, Spanned text,
            int start, int end, int nl, boolean last) {
        int next;
        for (int i = start; i < end; i = next) {
            next = text.nextSpanTransition(i, end, CharacterStyle.class);
            CharacterStyle[] style = text.getSpans(i, next,
                    CharacterStyle.class);

            for (int j = 0; j < style.length; j++) {
                if (style[j] instanceof StyleSpan) {
                    int s = ((StyleSpan) style[j]).getStyle();

                    if ((s & Typeface.BOLD) != 0) {
                        out.append("<strong>");
                    }
                    if ((s & Typeface.ITALIC) != 0) {
                        out.append("<em>");
                    }
                }
                if (style[j] instanceof TypefaceSpan) {
                    String s = ((TypefaceSpan) style[j]).getFamily();

                    if (s.equals("monospace")) {
                        out.append("<tt>");
                    }
                }
                if (style[j] instanceof SuperscriptSpan) {
                    out.append("<sup>");
                }
                if (style[j] instanceof SubscriptSpan) {
                    out.append("<sub>");
                }
                if (style[j] instanceof WPUnderlineSpan) {
                    out.append("<u>");
                }
                if (style[j] instanceof StrikethroughSpan) {
                    out.append("<strike>");
                }
                if (style[j] instanceof URLSpan) {
                    out.append("<a href=\"");
                    out.append(((URLSpan) style[j]).getURL());
                    out.append("\">");
                }
                if (style[j] instanceof MediaGalleryImageSpan) {
                    out.append(getGalleryShortcode((MediaGalleryImageSpan) style[j]));
                } else if (style[j] instanceof WPImageSpan && ((WPImageSpan) style[j]).getMediaFile().getMediaId() != null) {
                    out.append(getContent((WPImageSpan) style[j]));
                } else if (style[j] instanceof WPImageSpan) {
                    out.append("<img src=\"");
                    out.append(((WPImageSpan) style[j]).getSource());
                    out.append("\" android-uri=\""
                            + ((WPImageSpan) style[j]).getImageSource()
                                    .toString() + "\"");
                    out.append(" />");
                    // Don't output the dummy character underlying the image.
                    i = next;
                }
                if (style[j] instanceof AbsoluteSizeSpan) {
                    out.append("<font size =\"");
                    out.append(((AbsoluteSizeSpan) style[j]).getSize() / 6);
                    out.append("\">");
                }
                if (style[j] instanceof ForegroundColorSpan) {
                    out.append("<font color =\"#");
                    String color = Integer
                            .toHexString(((ForegroundColorSpan) style[j])
                                    .getForegroundColor() + 0x01000000);
                    while (color.length() < 6) {
                        color = "0" + color;
                    }
                    out.append(color);
                    out.append("\">");
                }
            }

            processWPImage(out, text, i, next);

            for (int j = style.length - 1; j >= 0; j--) {
                if (style[j] instanceof ForegroundColorSpan) {
                    out.append("</font>");
                }
                if (style[j] instanceof AbsoluteSizeSpan) {
                    out.append("</font>");
                }
                if (style[j] instanceof URLSpan) {
                    out.append("</a>");
                }
                if (style[j] instanceof StrikethroughSpan) {
                    out.append("</strike>");
                }
                if (style[j] instanceof WPUnderlineSpan) {
                    out.append("</u>");
                }
                if (style[j] instanceof SubscriptSpan) {
                    out.append("</sub>");
                }
                if (style[j] instanceof SuperscriptSpan) {
                    out.append("</sup>");
                }
                if (style[j] instanceof TypefaceSpan) {
                    String s = ((TypefaceSpan) style[j]).getFamily();

                    if (s.equals("monospace")) {
                        out.append("</tt>");
                    }
                }
                if (style[j] instanceof StyleSpan) {
                    int s = ((StyleSpan) style[j]).getStyle();

                    if ((s & Typeface.BOLD) != 0) {
                        out.append("</strong>");
                    }
                    if ((s & Typeface.ITALIC) != 0) {
                        out.append("</em>");
                    }
                }
            }
        }

        String p = last ? "" : "</p>\n<p>";

        if (nl == 1) {
            out.append("<br>\n");
        } else if (nl == 2) {
            out.append(p);
        } else {
            for (int i = 2; i < nl; i++) {
                out.append("<br>");
            }

            out.append(p);
        }
    }

    /** Get gallery shortcode for a MediaGalleryImageSpan */
    public static String getGalleryShortcode(MediaGalleryImageSpan gallerySpan) {
        String shortcode = "";
        MediaGallery gallery = gallerySpan.getMediaGallery();
        shortcode += "[gallery ";
        if (gallery.isRandom())
            shortcode += " orderby=\"rand\"";
        if (gallery.getType().equals(""))
            shortcode += " columns=\"" + gallery.getNumColumns() + "\"";
        else
            shortcode += " type=\"" + gallery.getType() + "\"";
        shortcode += " ids=\"" + gallery.getIdsStr() + "\"";
        shortcode += "]";

        return shortcode;
    }

    /** Retrieve an image span content for a media file that exists on the server **/
    public static String getContent(WPImageSpan imageSpan) {
        // based on PostUploadService

        String content = "";
        MediaFile mediaFile = imageSpan.getMediaFile();
        if (mediaFile == null)
            return content;
        String mediaId = mediaFile.getMediaId();
        if (mediaId == null || mediaId.length() == 0)
            return content;

        boolean isVideo = mediaFile.isVideo();
        String url = imageSpan.getImageSource().toString();

        if (isVideo) {
            if (!TextUtils.isEmpty(mediaFile.getVideoPressShortCode())) {
                content = mediaFile.getVideoPressShortCode();
            } else {
                int xRes = mediaFile.getWidth();
                int yRes = mediaFile.getHeight();
                String mimeType = mediaFile.getMimeType();
                content = String.format("<video width=\"%s\" height=\"%s\" controls=\"controls\"><source src=\"%s\" type=\"%s\" /><a href=\"%s\">Click to view video</a>.</video>",
                        xRes, yRes, url, mimeType, url);
            }
        } else {
            String alignment = "";
            switch (mediaFile.getHorizontalAlignment()) {
            case 0:
                alignment = "alignnone";
                break;
            case 1:
                alignment = "alignleft";
                break;
            case 2:
                alignment = "aligncenter";
                break;
            case 3:
                alignment = "alignright";
                break;
            }
            String alignmentCSS = "class=\"" + alignment + " size-full\" ";
            String title = mediaFile.getTitle();
            String caption = mediaFile.getCaption();
            int width = mediaFile.getWidth();

            content = content + "<a href=\"" + url + "\"><img title=\"" + title + "\" "
                    + alignmentCSS + "alt=\"image\" src=\"" + url + "?w=" + width +"\" /></a>";

            if (!caption.equals("")) {
                content = String.format("[caption id=\"\" align=\"%s\" width=\"%d\" caption=\"%s\"]%s[/caption]",
                        alignment, width, TextUtils.htmlEncode(caption), content);
            }
        }

        return content;
    }

    private static void processWPImage(StringBuilder out, Spanned text,
            int start, int end) {
        int next;

        for (int i = start; i < end; i = next) {
            next = text.nextSpanTransition(i, end, SpannableString.class);
            SpannableString[] images = text.getSpans(i, next,
                    SpannableString.class);

            for (SpannableString image : images) {
                out.append(image.toString());
            }

            withinStyle(out, text, i, next);

        }
    }

    private static void withinStyle(StringBuilder out, Spanned text, int start,
            int end) {
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);

            /*
             * if (c == '<') { out.append("&lt;"); } else if (c == '>') {
             * out.append("&gt;"); } else if (c == '&') { out.append("&amp;");
             * if (c > 0x7E || c < ' ') { out.append("&#" + ((int) c) + ";"); }
             * else
             */
            if (c == ' ') {
                while (i + 1 < end && text.charAt(i + 1) == ' ') {
                    out.append("&nbsp;");
                    i++;
                }

                out.append(' ');
            } else {
                out.append(c);
            }
        }
    }
}

class HtmlToSpannedConverter implements ContentHandler {

    private static final float[] HEADER_SIZES = { 1.5f, 1.4f, 1.3f, 1.2f, 1.1f,
            1f, };

    private String mSource;
    private XMLReader mReader;
    private SpannableStringBuilder mSpannableStringBuilder;
    private WPHtml.ImageGetter mImageGetter;
    private String mysteryTagContent;
    private boolean mysteryTagFound;
    private static Context ctx;
    private static Post post;

    private String mysteryTagName;

    public HtmlToSpannedConverter(String source,
            WPHtml.ImageGetter imageGetter, WPHtml.TagHandler tagHandler,
            Parser parser, Context context, Post p) {
        mSource = source;
        mSpannableStringBuilder = new SpannableStringBuilder();
        mImageGetter = imageGetter;
        mReader = parser;
        mysteryTagContent = "";
        mysteryTagName = null;
        ctx = context;
        post = p;
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
            if (post != null) {
                if (!post.isLocalDraft()) {
                    if (tag.equalsIgnoreCase("img"))
                        startImg(mSpannableStringBuilder, attributes,
                                mImageGetter);

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

                if (tag.equalsIgnoreCase("html")
                        || tag.equalsIgnoreCase("body")) {
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
        if (post != null) {
            if (!post.isLocalDraft())
                return;
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
        if (where < 0)
            where = 0;

        text.removeSpan(obj);

        if (where != len) {
            text.setSpan(repl, where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return;
    }

    private static void startImg(SpannableStringBuilder text,
            Attributes attributes, WPHtml.ImageGetter img) {
        String src = attributes.getValue("android-uri");
        ImageHelper ih = new ImageHelper();

        Map<String, Object> mediaData = ih.getImageBytesForPath(src, ctx);

        if (mediaData != null || (src != null && src.contains("video"))) {
            Bitmap resizedBitmap;

            if (mediaData != null)
                resizedBitmap = ih.getThumbnailForWPImageSpan(ctx, (byte[]) mediaData.get("bytes"), (String) mediaData.get("orientation"));
            else
                resizedBitmap = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.media_movieclip);

            int len = text.length();
            text.append("\uFFFC");

            Uri curStream = Uri.parse(src);

            if (curStream == null) {
                return;
            }

            WPImageSpan is = new WPImageSpan(ctx, resizedBitmap, curStream);

            // get the MediaFile data from db
            MediaFile mf = WordPress.wpDB.getMediaFile(src, post);
            if (mf != null) {
                is.setMediaFile(mf);
                is.setImageSource(curStream);
                text.setSpan(is, len, text.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                AlignmentSpan.Standard as = new AlignmentSpan.Standard(
                        Layout.Alignment.ALIGN_CENTER);
                text.setSpan(as, text.getSpanStart(is), text.getSpanEnd(is),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } else if (post != null) {
            if (post.isLocalDraft()) {
                if (attributes != null) {
                    text.append("<img");
                    for (int i = 0; i < attributes.getLength(); i++) {
                        String aName = attributes.getLocalName(i); // Attr name
                        if ("".equals(aName))
                            aName = attributes.getQName(i);
                        text.append(" ");
                        text.append(aName + "=\"" + attributes.getValue(i) + "\"");
                    }
                    text.append(" />\n");
                }
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
        if ("".equals(eName))
            eName = qName; // not namespace-aware
        mysteryTagContent += "<" + eName;
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                String aName = attributes.getLocalName(i); // Attr name
                if ("".equals(aName))
                    aName = attributes.getQName(i);
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

    public void characters(char ch[], int start, int length)
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
                if (sb.length() < length)
                    mysteryTagContent += sb.toString().substring(start,
                            length - 1);
                else
                    mysteryTagContent += sb.toString().substring(start, length);
            } else
                mSpannableStringBuilder.append(sb);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ignorableWhitespace(char ch[], int start, int length)
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

        public Font(String color, String face) {
            mColor = color;
            mFace = face;
        }
    }

    private static class Href {
        public String mHref;

        public Href(String href) {
            mHref = href;
        }
    }

    private static class Header {
        private int mLevel;

        public Header(int level) {
            mLevel = level;
        }
    }

    private static HashMap<String, Integer> COLORS = buildColorMap();

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
     * @param color
     *            Non-null color string.
     * @return A color value, or {@code -1} if the color string could not be
     *         interpreted.
     */
    private static int getHtmlColor(String color) {
        Integer i = COLORS.get(color.toLowerCase());
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
        if (null == charSeq)
            return defaultValue;

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
            if (index == (len - 1))
                return 0;

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
