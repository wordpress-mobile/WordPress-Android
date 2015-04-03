package org.wordpress.android.editor;

public class Utils {
    public static String escapeHtml(String html) {
        html = html.replace("\\", "\\\\");
        html = html.replace("\"", "\\\"");
        html = html.replace("'", "\\'");
        html = html.replace("\r", "\\r");
        html = html.replace("\n", "\\n");
        return html;
    }
}
