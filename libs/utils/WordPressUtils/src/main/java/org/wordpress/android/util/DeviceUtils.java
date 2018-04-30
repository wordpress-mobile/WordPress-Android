package org.wordpress.android.util;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import org.wordpress.android.util.AppLog.T;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DeviceUtils {
    private static final String APP_RUNTIME_ON_CHROME_FLAG = "org.chromium.arc.device_management";

    private static DeviceUtils instance;
    private boolean mIsKindleFire = false;

    public boolean isKindleFire() {
        return mIsKindleFire;
    }

    public static DeviceUtils getInstance() {
        if (instance == null) {
            instance = new DeviceUtils();
        }
        return instance;
    }

    private DeviceUtils() {
        mIsKindleFire = android.os.Build.MODEL.equalsIgnoreCase("kindle fire") ? true : false;
    }

    /**
     * Checks camera availability recursively based on API level.
     *
     * TODO: change "android.hardware.camera.front" and "android.hardware.camera.any" to
     * {@link PackageManager#FEATURE_CAMERA_FRONT} and {@link PackageManager#FEATURE_CAMERA_ANY},
     * respectively, once they become accessible or minSdk version is incremented.
     *
     * @param context The context.
     * @return Whether camera is available.
     */
    public boolean hasCamera(Context context) {
        final PackageManager pm = context.getPackageManager();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)
                   || pm.hasSystemFeature("android.hardware.camera.front");
        }

        return pm.hasSystemFeature("android.hardware.camera.any");
    }

    public String getDeviceName(Context context) {
        String manufacturer = Build.MANUFACTURER;
        String undecodedModel = Build.MODEL;
        String model = null;

        try {
            Properties prop = new Properties();
            InputStream fileStream;
            // Read the device name from a precomplied list:
            // see http://making.meetup.com/post/29648976176/human-readble-android-device-names
            fileStream = context.getAssets().open("android_models.properties");
            prop.load(fileStream);
            fileStream.close();
            String decodedModel = prop.getProperty(undecodedModel.replaceAll(" ", "_"));
            if (decodedModel != null && !decodedModel.trim().equals("")) {
                model = decodedModel;
            }
        } catch (IOException e) {
            AppLog.e(T.UTILS, "Can't read `android_models.properties` file from assets, or it's in the wrong form.", e);
            AppLog.d(T.UTILS,
                 "If you need more info about the file, please check the reference implementation available here: "
                 + "https://github.com/wordpress-mobile/WordPress-Android/blob/dd989429bd701a66bcba911de08f2e8d336798ef"
                 + "/WordPress/src/main/assets/android_models.properties");
        }

        if (model == null) { // Device model not found in the list
            if (undecodedModel.startsWith(manufacturer)) {
                model = capitalize(undecodedModel);
            } else {
                model = capitalize(manufacturer) + " " + undecodedModel;
            }
        }
        return model;
    }

    public boolean isDeviceLocked(Context context) {
        KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return myKM.inKeyguardRestrictedInputMode();
    }

    /**
     * Checks if the current device runtime is ARC which effectively means it is a chromebook.
     *
     * @param context The context.
     * @return Whether the device is a chromebook.
     */
    public boolean isAppRuntimeForChrome(Context context) {
        return context.getPackageManager().hasSystemFeature(APP_RUNTIME_ON_CHROME_FLAG);
    }

    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }
}
