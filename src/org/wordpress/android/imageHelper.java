package org.wordpress.android;

import android.os.Build;

public abstract class imageHelper {

	    /**
	     * Static singleton instance of {@link ContactAccessor} holding the
	     * SDK-specific implementation of the class.
	     */
	    private static imageHelper sInstance;

	    public static imageHelper getInstance() {
	        if (sInstance == null) {
	            String className;

	            /*
	             * Check the version of the SDK we are running on. Choose an
	             * implementation class designed for that version of the SDK.
	             *
	             * Unfortunately we have to use strings to represent the class
	             * names. If we used the conventional ContactAccessorSdk5.class.getName()
	             * syntax, we would get a ClassNotFoundException at runtime on pre-Eclair SDKs.
	             * Using the above syntax would force Dalvik to load the class and try to
	             * resolve references to all other classes it uses. Since the pre-Eclair
	             * does not have those classes, the loading of ContactAccessorSdk5 would fail.
	             */
	            @SuppressWarnings("deprecation")
	            int sdkVersion = Integer.parseInt(Build.VERSION.SDK);       // Cupcake style
	            if (sdkVersion < Build.VERSION_CODES.DONUT) {
	                className = "org.wordpress.android.imageHelper_v3";
	            } else {
	                className = "org.wordpress.android.imageHelper_v4";
	            }

	            /*
	             * Find the required class by name and instantiate it.
	             */
	            try {
	                Class<? extends imageHelper> clazz = Class.forName(className).asSubclass(imageHelper.class);
	                sInstance = clazz.newInstance();
	            } catch (Exception e) {
	                throw new IllegalStateException(e);
	            }
	        }

	        return sInstance;
	    }
	    
	    public abstract byte[] createThumbnail(byte[] bytes, String sMaxImageWidth, String orientation, boolean tiny);

	    /**
	     * Loads contact data for the supplied URI. The actual queries will differ for different APIs
	     * used, but the result is the same: the {@link #mDisplayName} and {@link #mPhoneNumber}
	     * fields are populated with correct data.
	     */
	    public abstract String getExifOrientation(String path, String orientation);
}
