package org.wordpress.android.fluxc;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class UnitTestUtils {
    public static final int DEFAULT_TIMEOUT_MS = 30000;

    public static void waitFor(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            AppLog.e(T.API, "Thread interrupted");
        }
    }

    public static void waitForNetworkCall() {
        waitFor(DEFAULT_TIMEOUT_MS);
    }

    public static String getStringFromResourceFile(Class clazz, String filename) {
        try {
            InputStream is = clazz.getClassLoader().getResourceAsStream(filename);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            StringBuilder buffer = new StringBuilder();
            String lineString;

            while ((lineString = bufferedReader.readLine()) != null) {
                buffer.append(lineString);
            }

            bufferedReader.close();
            return buffer.toString();
        } catch (IOException e) {
            AppLog.e(T.TESTS, "Could not load response JSON file.");
            return null;
        }
    }
}

