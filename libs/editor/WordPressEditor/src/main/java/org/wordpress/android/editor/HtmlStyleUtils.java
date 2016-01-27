package org.wordpress.android.editor;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import org.wordpress.android.util.AppLog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlStyleUtils {
    public static final int TAG_COLOR = Color.rgb(0, 80, 130);
    public static final int ATTRIBUTE_COLOR = Color.rgb(158, 158, 158);

    public static final String REGEX_HTML_TAGS = "(<\\/?[a-z][^<>]*>)";
    public static final String REGEX_HTML_ATTRIBUTES = "(?<==)('|\")(.*?\\1)(?=.*?>)";
    public static final String REGEX_HTML_COMMENTS = "(<!--.*?-->)";
    public static final String REGEX_HTML_ENTITIES = "(&#34;|&#38;|&#39;|&#60;|&#62;|&#160;|&#161;|&#162;|&#163;" +
            "|&#164;|&#165;|&#166;|&#167;|&#168;|&#169;|&#170;|&#171;|&#172;|&#173;|&#174;|&#175;|&#176;|&#177;" +
            "|&#178;|&#179;|&#180;|&#181;|&#182;|&#183;|&#184;|&#185;|&#186;|&#187;|&#188;|&#189;|&#190;|&#191;" +
            "|&#192;|&#193;|&#194;|&#195;|&#196;|&#197;|&#198;|&#199;|&#200;|&#201;|&#202;|&#203;|&#204;|&#205;" +
            "|&#206;|&#207;|&#208;|&#209;|&#210;|&#211;|&#212;|&#213;|&#214;|&#215;|&#216;|&#217;|&#218;|&#219;" +
            "|&#220;|&#221;|&#222;|&#223;|&#224;|&#225;|&#226;|&#227;|&#228;|&#229;|&#230;|&#231;|&#232;|&#233;" +
            "|&#234;|&#235;|&#236;|&#237;|&#238;|&#239;|&#240;|&#241;|&#242;|&#243;|&#244;|&#245;|&#246;|&#247;" +
            "|&#248;|&#249;|&#250;|&#251;|&#252;|&#253;|&#254;|&#255;|&#338;|&#339;|&#352;|&#353;|&#376;|&#402;" +
            "|&#710;|&#732;|&#913;|&#914;|&#915;|&#916;|&#917;|&#918;|&#919;|&#920;|&#921;|&#922;|&#923;|&#924;" +
            "|&#925;|&#926;|&#927;|&#928;|&#929;|&#931;|&#932;|&#933;|&#934;|&#935;|&#936;|&#937;|&#945;|&#946;" +
            "|&#947;|&#948;|&#949;|&#950;|&#951;|&#952;|&#953;|&#954;|&#955;|&#956;|&#957;|&#958;|&#959;|&#960;" +
            "|&#961;|&#962;|&#963;|&#964;|&#965;|&#966;|&#967;|&#968;|&#969;|&#977;|&#978;|&#982;|&#8194;|&#8195;" +
            "|&#8201;|&#8204;|&#8205;|&#8206;|&#8207;|&#8211;|&#8212;|&#8216;|&#8217;|&#8218;|&#8220;|&#8221;|&#8222;" +
            "|&#8224;|&#8225;|&#8226;|&#8230;|&#8240;|&#8242;|&#8243;|&#8249;|&#8250;|&#8254;|&#8260;|&#8364;|&#8465;" +
            "|&#8472;|&#8476;|&#8482;|&#8501;|&#8592;|&#8593;|&#8594;|&#8595;|&#8596;|&#8629;|&#8656;|&#8657;|&#8658;" +
            "|&#8659;|&#8660;|&#8704;|&#8706;|&#8707;|&#8709;|&#8711;|&#8712;|&#8713;|&#8715;|&#8719;|&#8721;|&#8722;" +
            "|&#8727;|&#8730;|&#8733;|&#8734;|&#8736;|&#8743;|&#8744;|&#8745;|&#8746;|&#8747;|&#8756;|&#8764;|&#8773;" +
            "|&#8776;|&#8800;|&#8801;|&#8804;|&#8805;|&#8834;|&#8835;|&#8836;|&#8838;|&#8839;|&#8853;|&#8855;|&#8869;" +
            "|&#8901;|&#8968;|&#8969;|&#8970;|&#8971;|&#9001;|&#9002;|&#9674;|&#9824;|&#9827;|&#9829;|&#9830;|&quot;" +
            "|&amp;|&apos;|&lt;|&gt;|&nbsp;|&iexcl;|&cent;|&pound;|&curren;|&yen;|&brvbar;|&sect;|&uml;|&copy;|&ordf;" +
            "|&laquo;|&not;|&shy;|&reg;|&macr;|&deg;|&plusmn;|&sup2;|&sup3;|&acute;|&micro;|&para;|&middot;|&cedil;" +
            "|&sup1;|&ordm;|&raquo;|&frac14;|&frac12;|&frac34;|&iquest;|&Agrave;|&Aacute;|&Acirc;|&Atilde;|&Auml;" +
            "|&Aring;|&AElig;|&Ccedil;|&Egrave;|&Eacute;|&Ecirc;|&Euml;|&Igrave;|&Iacute;|&Icirc;|&Iuml;|&ETH;" +
            "|&Ntilde;|&Ograve;|&Oacute;|&Ocirc;|&Otilde;|&Ouml;|&times;|&Oslash;|&Ugrave;|&Uacute;|&Ucirc;|&Uuml;" +
            "|&Yacute;|&THORN;|&szlig;|&agrave;|&aacute;|&acirc;|&atilde;|&auml;|&aring;|&aelig;|&ccedil;|&egrave;" +
            "|&eacute;|&ecirc;|&euml;|&igrave;|&iacute;|&icirc;|&iuml;|&eth;|&ntilde;|&ograve;|&oacute;|&ocirc;" +
            "|&otilde;|&ouml;|&divide;|&oslash;|&Ugrave;|&Uacute;|&Ucirc;|&Uuml;|&yacute;|&thorn;|&yuml;|&OElig;" +
            "|&oelig;|&Scaron;|&scaron;|&Yuml;|&fnof;|&circ;|&tilde;|&Alpha;|&Beta;|&Gamma;|&Delta;|&Epsilon;|&Zeta;" +
            "|&Eta;|&Theta;|&Iota;|&Kappa;|&Lambda;|&Mu;|&Nu;|&Xi;|&Omicron;|&Pi;|&Rho;|&Sigma;|&Tau;|&Upsilon;|&Phi;" +
            "|&Chi;|&Psi;|&Omega;|&alpha;|&beta;|&gamma;|&delta;|&epsilon;|&zeta;|&eta;|&theta;|&iota;|&kappa;" +
            "|&lambda;|&mu;|&nu;|&xi;|&omicron;|&pi;|&rho;|&sigmaf;|&sigma;|&tau;|&upsilon;|&phi;|&chi;|&psi;|&omega;" +
            "|&thetasym;|&Upsih;|&piv;|&ensp;|&emsp;|&thinsp;|&zwnj;|&zwj;|&lrm;|&rlm;|&ndash;|&mdash;|&lsquo;" +
            "|&rsquo;|&sbquo;|&ldquo;|&rdquo;|&bdquo;|&dagger;|&Dagger;|&bull;|&hellip;|&permil;|&prime;|&Prime;" +
            "|&lsaquo;|&rsaquo;|&oline;|&frasl;|&euro;|&image;|&weierp;|&real;|&trade;|&alefsym;|&larr;|&uarr;|&rarr;" +
            "|&darr;|&harr;|&crarr;|&lArr;|&UArr;|&rArr;|&dArr;|&hArr;|&forall;|&part;|&exist;|&empty;|&nabla;|&isin;" +
            "|&notin;|&ni;|&prod;|&sum;|&minus;|&lowast;|&radic;|&prop;|&infin;|&ang;|&and;|&or;|&cap;|&cup;|&int;" +
            "|&there4;|&sim;|&cong;|&asymp;|&ne;|&equiv;|&le;|&ge;|&sub;|&sup;|&nsub;|&sube;|&supe;|&oplus;|&otimes;" +
            "|&perp;|&sdot;|&lceil;|&rceil;|&lfloor;|&rfloor;|&lang;|&rang;|&loz;|&spades;|&clubs;|&hearts;|&diams;)";

    public static final int SPANNABLE_FLAGS = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE;

    /**
     * Apply styling rules to {@code content}.
     */
    public static void styleHtmlForDisplay(@NonNull Spannable content) {
        styleHtmlForDisplay(content, 0, content.length());
    }

    /**
     * Apply styling rules to {@code content} inside the range from {@code start} to {@code end}.
     *
     * @param content the Spannable to apply style rules to
     * @param start the index in {@code content} to start styling from
     * @param end the index in {@code content} to style until
     */
    public static void styleHtmlForDisplay(@NonNull Spannable content, int start, int end) {
        if (Build.VERSION.RELEASE.equals("4.1") || Build.VERSION.RELEASE.equals("4.1.1")) {
            // Avoids crashing bug in Android 4.1 and 4.1.1 triggered when spanned text is line-wrapped
            // AOSP issue: https://code.google.com/p/android/issues/detail?id=35466
            return;
        }

        applySpansByRegex(content, start, end, REGEX_HTML_TAGS);
        applySpansByRegex(content, start, end, REGEX_HTML_ATTRIBUTES);
        applySpansByRegex(content, start, end, REGEX_HTML_COMMENTS);
        applySpansByRegex(content, start, end, REGEX_HTML_ENTITIES);
    }

    /**
     * Applies styles to {@code content} from {@code start} to {@code end}, based on rule {@code regex}.
     * @param content the Spannable to apply style rules to
     * @param start the index in {@code content} to start styling from
     * @param end the index in {@code content} to style until
     * @param regex the pattern to match for styling
     */
    private static void applySpansByRegex(Spannable content, int start, int end, String regex) {
        if (content == null || start < 0 || end < 0 || start > content.length() || end > content.length()) {
            AppLog.d(AppLog.T.EDITOR, "applySpansByRegex() received invalid input");
            return;
        }

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content.subSequence(start, end));

        while (matcher.find()) {
            int matchStart = matcher.start() + start;
            int matchEnd = matcher.end() + start;
            switch(regex) {
                case REGEX_HTML_TAGS:
                    content.setSpan(new ForegroundColorSpan(TAG_COLOR), matchStart, matchEnd, SPANNABLE_FLAGS);
                    break;
                case REGEX_HTML_ATTRIBUTES:
                    content.setSpan(new ForegroundColorSpan(ATTRIBUTE_COLOR), matchStart, matchEnd, SPANNABLE_FLAGS);
                    break;
                case REGEX_HTML_COMMENTS:
                    content.setSpan(new ForegroundColorSpan(ATTRIBUTE_COLOR), matchStart, matchEnd, SPANNABLE_FLAGS);
                    content.setSpan(new StyleSpan(Typeface.ITALIC), matchStart, matchEnd, SPANNABLE_FLAGS);
                    content.setSpan(new RelativeSizeSpan(0.75f), matchStart, matchEnd, SPANNABLE_FLAGS);
                    break;
                case REGEX_HTML_ENTITIES:
                    content.setSpan(new ForegroundColorSpan(TAG_COLOR), matchStart, matchEnd, SPANNABLE_FLAGS);
                    content.setSpan(new StyleSpan(Typeface.BOLD), matchStart, matchEnd, SPANNABLE_FLAGS);
                    content.setSpan(new RelativeSizeSpan(0.75f), matchStart, matchEnd, SPANNABLE_FLAGS);
                    break;
            }
        }
    }

    /**
     * Clears all relevant spans in {@code content} from {@code start} to {@code end}. Relevant spans are the subclasses
     * of {@link CharacterStyle} applied by {@link HtmlStyleUtils#applySpansByRegex(Spannable, int, int, String)}.
     * @param content the Spannable to clear styles from
     * @param spanStart the index in {@code content} to start clearing styles from
     * @param spanEnd the index in {@code content} to clear styles until
     */
    public static void clearSpans(Spannable content, int spanStart, int spanEnd) {
        CharacterStyle[] spans = content.getSpans(spanStart, spanEnd, CharacterStyle.class);

        for (CharacterStyle span : spans) {
            if (span instanceof ForegroundColorSpan || span instanceof StyleSpan || span instanceof RelativeSizeSpan) {
                content.removeSpan(span);
            }
        }
    }
}
