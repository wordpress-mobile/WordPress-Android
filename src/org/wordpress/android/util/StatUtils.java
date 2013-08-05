package org.wordpress.android.util;

import android.annotation.SuppressLint;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class StatUtils {
    
    /** Converts date in the form of 2013-07-18 to ms **/
    @SuppressLint("SimpleDateFormat")
	public static long toMs(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return sdf.parse(date).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
}
