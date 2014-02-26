package org.wordpress.android.util;

import android.content.Context;

import org.wordpress.android.util.AppLog.T;

public class SystemServiceFactory {
    public static SystemServiceFactoryAbstract sFactory;

    public static Object get(Context context, String name) {
        if (sFactory == null) {
            sFactory = new SystemServiceFactoryDefault();
        }
        AppLog.v(T.UTILS, "instantiate SystemService using sFactory: " + sFactory.getClass());
        return sFactory.get(context, name);
    }
}
