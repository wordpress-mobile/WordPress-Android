package org.wordpress.android.util;

import android.text.TextUtils;

import java.io.File;

public class FileUtils {
    /**
     * Returns the length of the file denoted by this abstract pathname.
     * The return value is unspecified if this pathname denotes a directory.
     *
     * @return  The length, in bytes, of the file denoted by this abstract
     *          pathname, or <code>-1L</code> if the file does not exist, or an
     *          exception is thrown accessing the file.
     *          Some operating systems may return <code>0L</code> for pathnames
     *          denoting system-dependent entities such as devices or pipes.
     */
    public static long length(String path) {
        // File not found
        File file = new File(path);
        try {
            if (!file.exists()) {
                AppLog.w(AppLog.T.MEDIA, "Can't access the file. It doesn't exists anymore?");
                return -1L;
            }

            return file.length();
        } catch (SecurityException e) {
            AppLog.e(AppLog.T.MEDIA, "Can't access the file.", e);
            return -1L;
        }
    }

    /**
     * Given the full file path, or the filename with extension (i.e. my-picture.jpg), returns the filename part only (my-picture).
     *
     * @param filePath The path to the file or the full filename
     * @return filename part only or null
     */
    public static String getFileNameFromPath(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }
        if (filePath.contains("/")) {
            if (filePath.lastIndexOf("/") + 1 >= filePath.length()) {
                filePath = filePath.substring(0, filePath.length() - 1);
            }
            filePath = filePath.substring(filePath.lastIndexOf("/") + 1);
        }

        String filename;
        int dotPos = filePath.indexOf('.');
        if (dotPos > 0) {
            filename = filePath.substring(0, dotPos);
        } else {
            filename = filePath;
        }
        return filename;
    }
}
