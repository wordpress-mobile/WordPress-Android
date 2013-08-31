package org.wordpress.android.util;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.text.Html;
import android.util.Log;

public class StringUtils {

    public static ArrayList<String> mergeArrayList(ArrayList<String> a1, ArrayList<String> a2) {
        if(a1.isEmpty())
            return a2;
        if(a2.isEmpty())
            return a1;
        ArrayList<String> result = new ArrayList<String>(a1);
        ArrayList<String> tmp = new ArrayList<String>(a1);
        tmp.retainAll(a2);
        result.addAll(a2);
        return result;
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
        String wrappedHTML = "";
        if (asploded.length > 0) {
            for (int i = 0; i < asploded.length; i++) {
                if (asploded[i].trim().length() > 0)
                    wrappedHTML += "<p>" + asploded[i].trim() + "</p>";
            }
        } else {
            wrappedHTML = source;
        }
        wrappedHTML = wrappedHTML.replace("<br />", "<br>").replace("<br/>", "<br>");
        wrappedHTML = wrappedHTML.replace("<br>\n", "<br>").replace("\n", "<br>");
        return wrappedHTML;
    }
    
    public static String getMd5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            String md5 = number.toString(16);

            while (md5.length() < 32)
                md5 = "0" + md5;

            return md5;
        } catch (NoSuchAlgorithmException e) {
            Log.e("MD5", e.getLocalizedMessage());
            return null;
        }
    }

    public static String unescapeHTML(String html) {
        if (html != null)
            return Html.fromHtml(html).toString();
        else
            return "";
    }
}
