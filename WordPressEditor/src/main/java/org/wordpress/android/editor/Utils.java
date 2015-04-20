package org.wordpress.android.editor;

import android.app.Activity;
import android.content.res.AssetManager;

import org.wordpress.android.util.AppLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class Utils {

    public static String getHtmlFromFile(Activity activity, String filename) {
        try {
            AssetManager assetManager = activity.getAssets();
            InputStream in = assetManager.open(filename);
            return getStringFromInputStream(in);
        } catch (IOException e) {
            AppLog.e(AppLog.T.EDITOR, e.getMessage());
            return null;
        }
    }

    public static String getStringFromInputStream(InputStream inputStream) throws IOException {
        InputStreamReader is = new InputStreamReader(inputStream);
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(is);
        String read = br.readLine();
        while (read != null) {
            sb.append(read);
            sb.append('\n');
            read = br.readLine();
        }
        return sb.toString();
    }

    public static String escapeHtml(String html) {
        html = html.replace("\\", "\\\\");
        html = html.replace("\"", "\\\"");
        html = html.replace("'", "\\'");
        html = html.replace("\r", "\\r");
        html = html.replace("\n", "\\n");
        return html;
    }

    /**
     * Splits a delimited string into a set of strings.
     * @param string the delimited string to split
     * @param delimiter the string delimiter
     */
    public static Set<String> splitDelimitedString(String string, String delimiter) {
        Set<String> splitString = new HashSet<>();

        StringTokenizer stringTokenizer = new StringTokenizer(string, delimiter);
        while (stringTokenizer.hasMoreTokens()) {
            splitString.add(stringTokenizer.nextToken());
        }

        return splitString;
    }

    /**
     * Accepts a set of strings, each string being a key-value pair (<code>id=5</code>,
     * <code>name=content-filed</code>). Returns a map of all the key-value pairs in the set.
     * @param keyValueSet the set of key-value pair strings
     */
    public static Map<String, String> buildMapFromKeyValuePairs(Set<String> keyValueSet) {
        Map<String, String> selectionArgs = new HashMap<>();
        for (String pair : keyValueSet) {
            String[] splitString = pair.split("=");
            if (splitString.length == 2) {
                selectionArgs.put(splitString[0], splitString[1]);
            }
        }
        return selectionArgs;
    }

    /**
     * Compares two <code>Sets</code> and returns a <code>Map</code> of elements not contained in both
     * <code>Sets</code>. Elements contained in <code>oldSet</code> but not in <code>newSet</code> will be marked
     * <code>false</code> in the returned map; the converse will be marked <code>true</code>.
     * @param oldSet the older of the two <code>Sets</code>
     * @param newSet the newer of the two <code>Sets</code>
     * @param <E> type of element stored in the <code>Sets</code>
     * @return a <code>Map</code> containing the difference between <code>oldSet</code> and <code>newSet</code>, and whether the
     * element was added (<code>true</code>) or removed (<code>false</code>) in <code>newSet</code>
     */
    public static <E> Map<E, Boolean> getChangeMapFromSets(Set<E> oldSet, Set<E> newSet) {
        Map<E, Boolean> changeMap = new HashMap<>();

        Set<E> additions = new HashSet<>(newSet);
        additions.removeAll(oldSet);

        Set<E> removals = new HashSet<>(oldSet);
        removals.removeAll(newSet);

        for (E s : additions) {
            changeMap.put(s, true);
        }

        for (E s : removals) {
            changeMap.put(s, false);
        }

        return changeMap;
    }
}
