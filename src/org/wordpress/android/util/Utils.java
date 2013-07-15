package org.wordpress.android.util;

import java.util.Comparator;
import java.util.Map;

public class Utils {

    public static Comparator<Object> BlogNameComparator = new Comparator<Object>() {
        
        public int compare(Object blog1, Object blog2) {
 
            Map<Object, Object> blogMap1 = (Map<Object, Object>)blog1;
            Map<Object, Object> blogMap2 = (Map<Object, Object>)blog2;
            
            String blogName1 = blogMap1.get("blogName").toString();
            if (blogName1.length() == 0) {
                blogName1 = blogMap1.get("url").toString();
            }
            
            String blogName2 = blogMap2.get("blogName").toString();
            if (blogName2.length() == 0) {
                blogName2 = blogMap2.get("url").toString();
            }
            
          return blogName1.compareToIgnoreCase(blogName2);
 
        }
 
    };
    
}