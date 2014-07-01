package org.wordpress.android.util;

public class ABTestingUtils {
    public enum Feature {
        HELPSHIFT
    }

    public static boolean isFeatureEnabled(Feature feature) {
        switch (feature) {
            case HELPSHIFT:
                return true;
        }
        return false;
    }
}
