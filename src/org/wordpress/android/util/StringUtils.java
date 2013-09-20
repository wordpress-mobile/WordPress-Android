package org.wordpress.android.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.text.Html;
import android.util.Log;

public class StringUtils {

    public static String[] mergeStringArrays(String array1[], String array2[]) {
        if (array1 == null || array1.length == 0)
            return array2;
        if (array2 == null || array2.length == 0)
            return array1;
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

    public static BigInteger getMd5IntHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            return number;
        } catch (NoSuchAlgorithmException e) {
            Log.e("MD5", e.getLocalizedMessage());
            return null;
        }
    }

    public static String getMd5Hash(String input) {
        BigInteger number = getMd5IntHash(input);
        String md5 = number.toString(16);
        while (md5.length() < 32)
            md5 = "0" + md5;
        return md5;
    }

    public static String unescapeHTML(String html) {
        if (html != null)
            return Html.fromHtml(html).toString();
        else
            return "";
    }

    /*
     * returns empty string if passed string is null, otherwise returns passed string
     */
    public static String notNullStr(String s) {
        if (s==null)
            return "";
        return s;
    }

    /*
     * capitalizes the first letter in the passed string - based on Apache commons/lang3/StringUtils
     * http://svn.apache.org/viewvc/commons/proper/lang/trunk/src/main/java/org/apache/commons/lang3/StringUtils.java?revision=1497829&view=markup
     */
    public static String capitalize(final String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0)
            return str;

        char firstChar = str.charAt(0);
        if (Character.isTitleCase(firstChar))
            return str;

        return new StringBuilder(strLen)
                .append(Character.toTitleCase(firstChar))
                .append(str.substring(1))
                .toString();
    }
}
