package org.wordpress.android.util;

import android.content.Context;

public class SystemServiceFactory {
    private static SystemServiceFactoryAbstract sFactory;

    public static Object get(Context context, String name) {
        if (sFactory == null) {
            sFactory = new SystemServiceFactoryDefault();
        }
        return sFactory.get(context, name);
    }
}
