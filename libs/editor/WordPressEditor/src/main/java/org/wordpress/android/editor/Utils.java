package org.wordpress.android.editor;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Patterns;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.HTTPUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
            AppLog.e(AppLog.T.EDITOR, "Unable to load editor HTML (is the assets symlink working?): " + e.getMessage());
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
        if (html != null) {
            html = html.replace("\\", "\\\\");
            html = html.replace("\"", "\\\"");
            html = html.replace("'", "\\'");
            html = html.replace("\r", "\\r");
            html = html.replace("\n", "\\n");

            // Escape invisible line separator (U+2028) and paragraph separator (U+2029) characters
            // https://github.com/wordpress-mobile/WordPress-Editor-Android/issues/405
            html = html.replace("\u2028", "\\u2028");
            html = html.replace("\u2029", "\\u2029");
        }
        return html;
    }

    public static String decodeHtml(String html) {
        if (html != null) {
            try {
                html = URLDecoder.decode(html, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                AppLog.e(AppLog.T.EDITOR, "Unsupported encoding exception while decoding HTML.");
            }
        }
        return html;
    }

    public static String escapeQuotes(String text) {
        if (text != null) {
            text = text.replace("'", "\\'").replace("\"", "\\\"");
        }
        return text;
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
     * Splits a delimited string of value pairs (of the form identifier=value) into a set of strings.
     * @param string the delimited string to split
     * @param delimiter the string delimiter
     * @param identifiers the identifiers to match for in the string
     */
    public static Set<String> splitValuePairDelimitedString(String string, String delimiter, List<String> identifiers) {
        String identifierSegment = "";
        for (String identifier : identifiers) {
            if (identifierSegment.length() != 0) {
                identifierSegment += "|";
            }
            identifierSegment += identifier;
        }

        String regex = delimiter + "(?=(" + identifierSegment + ")=)";

        return new HashSet<>(Arrays.asList(string.split(regex)));
    }

    /**
     * Accepts a set of strings, each string being a key-value pair (<code>id=5</code>,
     * <code>name=content-filed</code>). Returns a map of all the key-value pairs in the set.
     * @param keyValueSet the set of key-value pair strings
     */
    public static Map<String, String> buildMapFromKeyValuePairs(Set<String> keyValueSet) {
        Map<String, String> selectionArgs = new HashMap<>();
        for (String pair : keyValueSet) {
            int delimLoc = pair.indexOf("=");
            if (delimLoc != -1) {
                selectionArgs.put(pair.substring(0, delimLoc), pair.substring(delimLoc + 1));
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

    public static Uri downloadExternalMedia(Context context, Uri imageUri, Map<String, String> headers) {
        if(context != null && imageUri != null) {
            File cacheDir = null;

            if (context.getApplicationContext() != null) {
                cacheDir = context.getCacheDir();
            }

            try {
                InputStream inputStream;
                if (imageUri.toString().startsWith("content://")) {
                    inputStream = context.getContentResolver().openInputStream(imageUri);
                    if (inputStream == null) {
                        AppLog.e(AppLog.T.UTILS, "openInputStream returned null");
                        return null;
                    }
                } else {
                    if (headers == null) {
                        headers = Collections.emptyMap();
                    }

                    HttpURLConnection conn = HTTPUtils.setupUrlConnection(imageUri.toString(), headers);

                    // If the HTTP response is a redirect, follow it
                    int responseCode = conn.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM
                                || responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                                || responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                            conn = HTTPUtils.setupUrlConnection(conn.getHeaderField("Location"), headers);
                        }
                    }

                    inputStream = conn.getInputStream();
                }

                String fileName = "thumb-" + System.currentTimeMillis();

                File f = new File(cacheDir, fileName);
                FileOutputStream output = new FileOutputStream(f);
                byte[] data = new byte[1024];

                int count;
                while ((count = inputStream.read(data)) != -1) {
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                inputStream.close();
                return Uri.fromFile(f);
            } catch (IOException e) {
                AppLog.e(AppLog.T.UTILS, e);
            }

            return null;
        } else {
            return null;
        }
    }

    /**
     * Checks the Clipboard for text that matches the {@link Patterns#WEB_URL} pattern.
     *
     * @return the URL text in the clipboard, if it exists; otherwise null
     */
    public static String getUrlFromClipboard(Context context) {
        if (context == null) return null;
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData data = clipboard != null ? clipboard.getPrimaryClip() : null;
        if (data == null || data.getItemCount() <= 0) return null;
        String clipText = String.valueOf(data.getItemAt(0).getText());
        return Patterns.WEB_URL.matcher(clipText).matches() ? clipText : null;
    }
}
