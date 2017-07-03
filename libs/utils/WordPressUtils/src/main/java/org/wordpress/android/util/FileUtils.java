package org.wordpress.android.util;

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
}
