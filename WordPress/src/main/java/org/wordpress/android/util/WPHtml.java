package org.wordpress.android.util;

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.ParagraphStyle;
import android.text.style.QuoteSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;

import org.ccil.cowan.tagsoup.HTMLSchema;
import org.ccil.cowan.tagsoup.Parser;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.MediaGallery;
import org.wordpress.android.util.helpers.MediaGalleryImageSpan;
import org.wordpress.android.util.helpers.WPImageSpan;
import org.wordpress.android.util.helpers.WPUnderlineSpan;
import org.xml.sax.XMLReader;

import java.util.Locale;

/**
 * This class processes HTML strings into displayable styled text. Not all HTML
 * tags are supported.
 */
public class WPHtml {
    /**
     * Retrieves images for HTML &lt;img&gt; tags.
     */
    public interface ImageGetter {
        /**
         * This method is called when the HTML parser encounters an &lt;img&gt;
         * tag. The <code>source</code> argument is the string from the "src"
         * attribute; the return value should be a Drawable representation of
         * the image or <code>null</code> for a generic replacement image. Make
         * sure you call setBounds() on your Drawable if it doesn't already have
         * its bounds set.
         */
        Drawable getDrawable(String source);
    }

    /**
     * Is notified when HTML tags are encountered that the parser does not know
     * how to interpret.
     */
    public interface TagHandler {
        /**
         * This method will be called whenn the HTML parser encounters a tag
         * that it does not know how to interpret.
         *
         * @param mysteryTagContent
         */
        void handleTag(boolean opening, String tag, Editable output,
                       XMLReader xmlReader, String mysteryTagContent);
    }

    private WPHtml() {
    }

    /**
     * Returns displayable styled text from the provided HTML string. Any
     * &lt;img&gt; tags in the HTML will display as a generic replacement image
     * which your program can then go through and replace with real images.
     * <p>
     * <p>
     * This uses TagSoup to handle real HTML, including all of the brokenness
     * found in the wild.
     */
    public static Spanned fromHtml(String source, Context ctx, PostModel post, int maxImageWidth) {
        return fromHtml(source, null, null, ctx, post, maxImageWidth);
    }

    /**
     * Lazy initialization holder for HTML parser. This class will a) be
     * preloaded by the zygote, or b) not loaded until absolutely necessary.
     */
    private static class HtmlParser {
        private static final HTMLSchema HTML_SCHEMA = new HTMLSchema();
    }

    /**
     * Returns displayable styled text from the provided HTML string. Any
     * &lt;img&gt; tags in the HTML will use the specified ImageGetter to
     * request a representation of the image (use null if you don't want this)
     * and the specified TagHandler to handle unknown tags (specify null if you
     * don't want this).
     * <p>
     * <p>
     * This uses TagSoup to handle real HTML, including all of the brokenness
     * found in the wild.
     */
    public static Spanned fromHtml(String source, ImageGetter imageGetter,
                                   TagHandler tagHandler, Context ctx, PostModel post, int maxImageWidth) {
        Parser parser = new Parser();
        try {
            parser.setProperty(Parser.schemaProperty, HtmlParser.HTML_SCHEMA);
        } catch (org.xml.sax.SAXNotRecognizedException e) {
            // Should not happen.
            throw new RuntimeException(e);
        } catch (org.xml.sax.SAXNotSupportedException e) {
            // Should not happen.
            throw new RuntimeException(e);
        }

        HtmlToSpannedConverter converter = new HtmlToSpannedConverter(source,
                                                                      imageGetter, tagHandler, parser, ctx, post,
                                                                      maxImageWidth);
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
                } else if (style[j] instanceof WPImageSpan
                           && ((WPImageSpan) style[j]).getMediaFile().getMediaId() != null) {
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

    /**
     * Get gallery shortcode for a MediaGalleryImageSpan
     */
    public static String getGalleryShortcode(MediaGalleryImageSpan gallerySpan) {
        String shortcode = "";
        MediaGallery gallery = gallerySpan.getMediaGallery();
        shortcode += "[gallery ";
        if (gallery.isRandom()) {
            shortcode += " orderby=\"rand\"";
        }
        if (gallery.getType().equals("")) {
            shortcode += " columns=\"" + gallery.getNumColumns() + "\"";
        } else {
            shortcode += " type=\"" + gallery.getType() + "\"";
        }
        shortcode += " ids=\"" + gallery.getIdsStr() + "\"";
        shortcode += "]";

        return shortcode;
    }

    /**
     * Retrieve an image span content for a media file that exists on the server
     **/
    public static String getContent(WPImageSpan imageSpan) {
        // based on PostUploadService

        String content = "";
        MediaFile mediaFile = imageSpan.getMediaFile();
        if (mediaFile == null) {
            return content;
        }
        String mediaId = mediaFile.getMediaId();
        if (mediaId == null || mediaId.length() == 0) {
            return content;
        }

        boolean isVideo = mediaFile.isVideo();
        String url = imageSpan.getImageSource().toString();

        if (isVideo) {
            if (!TextUtils.isEmpty(mediaFile.getVideoPressShortCode())) {
                content = mediaFile.getVideoPressShortCode();
            } else {
                int xRes = mediaFile.getWidth();
                int yRes = mediaFile.getHeight();
                String mimeType = mediaFile.getMimeType();
                content = String.format(Locale.US,
                        "<video width=\"%s\" height=\"%s\" controls=\"controls\">"
                        + "<source src=\"%s\" type=\"%s\" /><a href=\"%s\">Click to view video</a>.</video>",
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

            String inlineCSS = " ";
            content = content + "<a href=\"" + url + "\"><img" + inlineCSS + "title=\"" + title + "\" "
                      + alignmentCSS + "alt=\"image\" src=\"" + url + "?w=" + width + "\" /></a>";

            if (!caption.equals("")) {
                content = String.format(Locale.US,
                                        "[caption id=\"\" align=\"%s\" width=\"%d\"]%s%s[/caption]",
                                        alignment, width, content, TextUtils.htmlEncode(caption));
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
