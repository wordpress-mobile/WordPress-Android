package android.text;

/**
 * Allows unit tests to call methods that rely on {@link android.text.TextUtils} without requiring mocks
 * of the Android framework (i.e., no need to use Robolectric).
 */
public class TextUtils {
    /**
     * Duplicates {@link android.text.TextUtils#isEmpty(CharSequence)}.
     */
    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }
}
