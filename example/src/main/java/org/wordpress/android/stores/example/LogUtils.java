package org.wordpress.android.stores.example;

import java.lang.reflect.Field;

public class LogUtils {
    public static String toString(Object o) {
        StringBuilder result = new StringBuilder();
        String newLine = System.getProperty("line.separator");

        result.append(o.getClass().getName());
        result.append(" Object {");
        result.append(newLine);

        //determine fields declared in this class only (no fields of superclass)
        Field[] fields = o.getClass().getDeclaredFields();

        //print field names paired with their values
        for (Field field : fields) {
            result.append("  ");
            try {
                result.append(field.getName());
                result.append(": ");
                // Make the field public
                Field f = o.getClass().getDeclaredField(field.getName());
                f.setAccessible(true);
                //requires access to private field:
                result.append(f.get(o));
            } catch (IllegalAccessException ex) {
                // A panda just died
                result.append("(✪㉨✪)");
            } catch (NoSuchFieldException e) {
                // A panda just died
                result.append("ʕ◉ᴥ◉ʔ");
            }
            result.append(newLine);
        }
        result.append("}");

        return result.toString();
    }
}
