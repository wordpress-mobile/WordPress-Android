package org.wordpress.android.util;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {
    public static final String DOCUMENTS_DIR = "documents";
    /**
     * Returns the length of the file denoted by this abstract pathname.
     * The return value is unspecified if this pathname denotes a directory.
     *
     * @return The length, in bytes, of the file denoted by this abstract
     * pathname, or <code>-1L</code> if the file does not exist, or an
     * exception is thrown accessing the file.
     * Some operating systems may return <code>0L</code> for pathnames
     * denoting system-dependent entities such as devices or pipes.
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
     * Given the full file path, or the filename with extension (i.e. my-picture.jpg), returns the filename part only
     * (my-picture).
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

    /**
     * This solution is based on https://stackoverflow.com/a/53021624
     * In certain cases we cannot load a file from the disk so we have to cache it instead using streams.
     * The helper methods are copied from - https://github.com/coltoscosmin/FileUtils/blob/master/FileUtils.java
     *  @param context The context.
     * @param uri The Uri to query.
     * @param file Target file
     */
    static String cacheFile(Context context, Uri uri, File file) {
        String destinationPath = null;
        if (file != null) {
            destinationPath = file.getAbsolutePath();
            saveFileFromUri(context, uri, destinationPath);
        }
        return destinationPath;
    }

    private static void saveFileFromUri(Context context, Uri uri, String destinationPath) {
        InputStream is = null;
        BufferedOutputStream bos = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            bos = new BufferedOutputStream(new FileOutputStream(destinationPath, false));
            byte[] buf = new byte[1024];
            is.read(buf);
            do {
                bos.write(buf);
            } while (is.read(buf) != -1);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
                if (bos != null) bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
