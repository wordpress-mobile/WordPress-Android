package android.text;

import java.util.Iterator;

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

    /**
     * Duplicates {@link android.text.TextUtils#join(CharSequence, Iterable)}.
     */
    public static String join(CharSequence delimiter, Iterable tokens) {
        StringBuilder sb = new StringBuilder();
        Iterator<?> it = tokens.iterator();
        if (it.hasNext()) {
            sb.append(it.next());
            while (it.hasNext()) {
                sb.append(delimiter);
                sb.append(it.next());
            }
        }
        return sb.toString();
    }
}
