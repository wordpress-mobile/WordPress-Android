package org.wordpress.android.util;

import android.text.Html;
import android.text.TextUtils;

import org.wordpress.android.util.AppLog.T;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StringUtils {

    public static String[] mergeStringArrays(String array1[], String array2[]) {
        if (array1 == null || array1.length == 0) {
            return array2;
        }
        if (array2 == null || array2.length == 0) {
            return array1;
        }
        List<String> array1List = Arrays.asList(array1);
        List<String> array2List = Arrays.asList(array2);
        List<String> result = new ArrayList<String>(array1List);
        List<String> tmp = new ArrayList<String>(array1List);
        tmp.retainAll(array2List);
        result.addAll(array2List);
        return ((String[]) result.toArray(new String[result.size()]));
    }

    public static String convertHTMLTagsForUpload(String source) {
        // bold
        source = source.replace("<b>", "<strong>");
        source = source.replace("</b>", "</strong>");

        // italics
        source = source.replace("<i>", "<em>");
        source = source.replace("</i>", "</em>");

        return source;
    }

    public static String convertHTMLTagsForDisplay(String source) {

        // bold
        source = source.replace("<strong>", "<b>");
        source = source.replace("</strong>", "</b>");

        // italics
        source = source.replace("<em>", "<i>");
        source = source.replace("</em>", "</i>");

        return source;
    }

    public static String addPTags(String source) {
        String[] asploded = source.split("\n\n");

        if (asploded.length > 0) {
            StringBuilder wrappedHTML = new StringBuilder();
            for (int i = 0; i < asploded.length; i++) {
                String trimmed = asploded[i].trim();
                if (trimmed.length() > 0) {
                    trimmed = trimmed.replace("<br />", "<br>").replace("<br/>", "<br>").replace("<br>\n", "<br>")
                                     .replace("\n", "<br>");
                    wrappedHTML.append("<p>");
                    wrappedHTML.append(trimmed);
                    wrappedHTML.append("</p>");
                }
            }
            return wrappedHTML.toString();
        } else {
            return source;
        }
    }

    public static BigInteger getMd5IntHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            return number;
        } catch (NoSuchAlgorithmException e) {
            AppLog.e(T.UTILS, e);
            return null;
        }
    }

    public static String getMd5Hash(String input) {
        BigInteger number = getMd5IntHash(input);
        String md5 = number.toString(16);
        while (md5.length() < 32) {
            md5 = "0" + md5;
        }
        return md5;
    }

    public static String unescapeHTML(String html) {
        if (html != null) {
            return Html.fromHtml(html).toString();
        } else {
            return "";
        }
    }

    /*
     * nbradbury - adapted from Html.escapeHtml(), which was added in API Level 16
     * TODO: not thoroughly tested yet, so marked as private - not sure I like the way
     * this replaces two spaces with "&nbsp;"
     */
    private static String escapeHtml(final String text) {
        if (text == null) {
            return "";
        }

        StringBuilder out = new StringBuilder();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);

            if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else if (c == '&') {
                out.append("&amp;");
            } else if (c > 0x7E || c < ' ') {
                out.append("&#").append((int) c).append(";");
            } else if (c == ' ') {
                while (i + 1 < length && text.charAt(i + 1) == ' ') {
                    out.append("&nbsp;");
                    i++;
                }

                out.append(' ');
            } else {
                out.append(c);
            }
        }

        return out.toString();
    }

    /*
     * returns empty string if passed string is null, otherwise returns passed string
     */
    public static String notNullStr(String s) {
        if (s == null) {
            return "";
        }
        return s;
    }

    /**
     * returns true if two strings are equal or two strings are null
     */
    public static boolean equals(String s1, String s2) {
        if (s1 == null) {
            return s2 == null;
        }
        return s1.equals(s2);
    }

    /*
     * capitalizes the first letter in the passed string - based on Apache commons/lang3/StringUtils
     * http://svn.apache.org/viewvc/commons/proper/lang/trunk/src/main/java/org/apache/commons/lang3/StringUtils.java?revision=1497829&view=markup
     */
    public static String capitalize(final String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }

        char firstChar = str.charAt(0);
        if (Character.isTitleCase(firstChar)) {
            return str;
        }

        return new StringBuilder(strLen).append(Character.toTitleCase(firstChar)).append(str.substring(1)).toString();
    }

    /*
     * Wrap an image URL in a photon URL
     * Check out http://developer.wordpress.com/docs/photon/
     */
    public static String getPhotonUrl(String imageUrl, int size) {
        imageUrl = imageUrl.replace("http://", "").replace("https://", "");
        return "http://i0.wp.com/" + imageUrl + "?w=" + size;
    }

    public static String getHost(String url) {
        if (TextUtils.isEmpty(url)) {
            return "";
        }

        int doubleslash = url.indexOf("//");
        if (doubleslash == -1) {
            doubleslash = 0;
        } else {
            doubleslash += 2;
        }

        int end = url.indexOf('/', doubleslash);
        end = (end >= 0) ? end : url.length();

        return url.substring(doubleslash, end);
    }

    public static String replaceUnicodeSurrogateBlocksWithHTMLEntities(final String inputString) {
        final int length = inputString.length();
        StringBuilder out = new StringBuilder(); // Used to hold the output.
        for (int offset = 0; offset < length; ) {
            final int codepoint = inputString.codePointAt(offset);
            final char current = inputString.charAt(offset);
            if (Character.isHighSurrogate(current) || Character.isLowSurrogate(current)) {
                if (Emoticons.wpSmiliesCodePointToText.get(codepoint) != null) {
                    out.append(Emoticons.wpSmiliesCodePointToText.get(codepoint));
                } else {
                    final String htmlEscapedChar = "&#x" + Integer.toHexString(codepoint) + ";";
                    out.append(htmlEscapedChar);
                }
            } else {
                out.append(current);
            }
            offset += Character.charCount(codepoint);
        }
        return out.toString();
    }

    /**
     * This method ensures that the output String has only
     * valid XML unicode characters as specified by the
     * XML 1.0 standard. For reference, please see
     * <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
     * standard</a>. This method will return an empty
     * String if the input is null or empty.
     *
     * @param in The String whose non-valid characters we want to remove.
     * @return The in String, stripped of non-valid characters.
     */
    public static final String stripNonValidXMLCharacters(String in) {
        StringBuilder out = new StringBuilder(); // Used to hold the output.
        char current; // Used to reference the current character.

        if (in == null || ("".equals(in))) {
            return ""; // vacancy test.
        }
        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
            if ((current == 0x9) ||
                (current == 0xA) ||
                (current == 0xD) ||
                ((current >= 0x20) && (current <= 0xD7FF)) ||
                ((current >= 0xE000) && (current <= 0xFFFD)) ||
                ((current >= 0x10000) && (current <= 0x10FFFF))) {
                out.append(current);
            }
        }
        return out.toString();
    }
}