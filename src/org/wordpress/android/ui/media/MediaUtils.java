package org.wordpress.android.ui.media;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class MediaUtils {

    public static boolean isValidImage(String url) {
        if (url.endsWith(".png") || url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".gif"))
            return true;
        return false;
    }
    
    /** E.g. Jul 2, 2013 @ 21:57 **/
    public static String getDate(long ms) {
        Date date = new Date(ms);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy '@' HH:mm");
        
        // The timezone on the website is at GMT
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        
        return sdf.format(date);
    }
    
}
