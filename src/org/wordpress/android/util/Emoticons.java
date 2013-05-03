package org.wordpress.android.util;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import android.util.Log;

public class Emoticons {
    
    private static final Map<String, String> wpSmilies;
    static {
        Map<String, String> smilies = new HashMap<String, String>();
        smilies.put("icon_mrgreen.gif",   ":mrgreen:" );
        smilies.put("icon_neutral.gif",   ":neutral:" );
        smilies.put("icon_twisted.gif",   ":twisted:" );
        smilies.put("icon_arrow.gif",     ":arrow:" );
        smilies.put("icon_eek.gif",       ":shock:" );
        smilies.put("icon_smile.gif",     ":smile:" );
        smilies.put("icon_confused.gif",  ":???:" );
        smilies.put("icon_cool.gif",      ":cool:" );
        smilies.put("icon_evil.gif",      ":evil:" );
        smilies.put("icon_biggrin.gif",   ":grin:" );
        smilies.put("icon_idea.gif",      ":idea:" );
        smilies.put("icon_redface.gif",   ":oops:" );
        smilies.put("icon_razz.gif",      ":razz:" );
        smilies.put("icon_rolleyes.gif",  ":roll:" );
        smilies.put("icon_wink.gif",      ":wink:" );
        smilies.put("icon_cry.gif",       ":cry:" );
        smilies.put("icon_surprised.gif", ":eek:" );
        smilies.put("icon_lol.gif",       ":lol:" );
        smilies.put("icon_mad.gif",       ":mad:" );
        smilies.put("icon_sad.gif",       ":sad:" );
        smilies.put("icon_cool.gif",      "8-)" );
        smilies.put("icon_eek.gif",       "8-O" );
        smilies.put("icon_sad.gif",       ":-(" );
        smilies.put("icon_smile.gif",     ":-)" );
        smilies.put("icon_confused.gif",  ":-?" );
        smilies.put("icon_biggrin.gif",   ":-D" );
        smilies.put("icon_razz.gif",      ":-P" );
        smilies.put("icon_surprised.gif", ":-o" );
        smilies.put("icon_mad.gif",       ":-x" );
        smilies.put("icon_neutral.gif",   ":-|" );
        smilies.put("icon_wink.gif",      ";-)" );
        smilies.put("icon_cool.gif",      "8)" );
        smilies.put("icon_eek.gif",       "8O" );
        smilies.put("icon_sad.gif",       ":(" );
        smilies.put("icon_smile.gif",     ":)" );
        smilies.put("icon_confused.gif",  ":?" );
        smilies.put("icon_biggrin.gif",   ":D" );
        smilies.put("icon_razz.gif",      ":P" );
        smilies.put("icon_surprised.gif", ":o" );
        smilies.put("icon_mad.gif",       ":x" );
        smilies.put("icon_neutral.gif",   ":|" );
        smilies.put("icon_wink.gif",      ";)" );
        smilies.put("icon_exclaim.gif",   ":!:" );
        smilies.put("icon_question.gif", ":?:" );
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
    
}