package org.wordpress.android.util;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import android.util.Log;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES;
import android.text.Spanned;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.text.style.ForegroundColorSpan;

public class Emoticons {
    public static final int EMOTICON_COLOR=0xFF21759B;
    private static final boolean HAS_EMOJI=SDK_INT >= VERSION_CODES.JELLY_BEAN;
    private static final Map<String, String> wpSmilies;
    static {
        Map<String, String> smilies = new HashMap<String, String>();
        smilies.put("icon_mrgreen.gif",   HAS_EMOJI ? "\uD83D\uDE00" : ":mrgreen:" );
        smilies.put("icon_neutral.gif",   HAS_EMOJI ? "\uD83D\uDE14" : ":|" );
        smilies.put("icon_twisted.gif",   HAS_EMOJI ? "\uD83D\uDE16" : ":twisted:" );
        smilies.put("icon_arrow.gif",     HAS_EMOJI ? "\u27A1" : ":arrow:" );
        smilies.put("icon_eek.gif",       HAS_EMOJI ? "\uD83D\uDE32" : "8-O" );
        smilies.put("icon_smile.gif",     HAS_EMOJI ? "\uD83D\uDE0A" : ":)" );
        smilies.put("icon_confused.gif",  HAS_EMOJI ? "\uD83D\uDE15" : ":?" );
        smilies.put("icon_cool.gif",      HAS_EMOJI ? "\uD83D\uDE0A" : "8)" );
        smilies.put("icon_evil.gif",      HAS_EMOJI ? "\uD83D\uDE21" : ":evil:" );
        smilies.put("icon_biggrin.gif",   HAS_EMOJI ? "\uD83D\uDE03" : ":D" );
        smilies.put("icon_idea.gif",      HAS_EMOJI ? "\uD83D\uDCA1" : ":idea:" );
        smilies.put("icon_redface.gif",   HAS_EMOJI ? "\uD83D\uDE33" : ":oops:" );
        smilies.put("icon_razz.gif",      HAS_EMOJI ? "\uD83D\uDE1D" : ":P" );
        smilies.put("icon_rolleyes.gif",  HAS_EMOJI ? "\uD83D\uDE0F" : ":roll:" );
        smilies.put("icon_wink.gif",      HAS_EMOJI ? "\uD83D\uDE09" : ";)" );
        smilies.put("icon_cry.gif",       HAS_EMOJI ? "\uD83D\uDE22" : ":'(" );
        smilies.put("icon_surprised.gif", HAS_EMOJI ? "\uD83D\uDE32" : ":o" );
        smilies.put("icon_lol.gif",       HAS_EMOJI ? "\uD83D\uDE03" : ":lol:" );
        smilies.put("icon_mad.gif",       HAS_EMOJI ? "\uD83D\uDE21" : ":x" );
        smilies.put("icon_sad.gif",       HAS_EMOJI ? "\uD83D\uDE1E" : ":(" );
        smilies.put("icon_exclaim.gif",   HAS_EMOJI ? "\u2757" : ":!:" );
        smilies.put("icon_question.gif",  HAS_EMOJI ? "\u2753" : ":?:" );
        
        
        wpSmilies = Collections.unmodifiableMap(smilies);
    }
    public static String lookupImageSmiley(String url){
        return lookupImageSmiley(url, "");
    }
    public static String lookupImageSmiley(String url, String ifNone){
        String file = url.substring(url.lastIndexOf("/") + 1);
        Log.d("Smilies", String.format("Looking for %s", file));
        if (wpSmilies.containsKey(file)) {
            return wpSmilies.get(file);
        }
        return ifNone;
    }
    public static Spanned replaceEmoticonsWithEmoji(SpannableStringBuilder html){
        ImageSpan imgs[] = html.getSpans(0, html.length(), ImageSpan.class);
        for (ImageSpan img : imgs) {
            String emoticon = Emoticons.lookupImageSmiley(img.getSource());
            if (!emoticon.equals("")) {
                int start = html.getSpanStart(img);
                html.replace(start, html.getSpanEnd(img), emoticon);
                html.setSpan(new ForegroundColorSpan(EMOTICON_COLOR), start,
                             start + emoticon.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                html.removeSpan(img);
            }
        }
        return html;
    }
}