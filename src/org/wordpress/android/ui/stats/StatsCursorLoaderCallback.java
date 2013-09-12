package org.wordpress.android.ui.stats;

import android.net.Uri;
import android.os.Bundle;
import android.widget.CursorTreeAdapter;

/** 
 * An interface used by {@link CursorTreeAdapter} subclasses 
 * to communicate with the {@link StatsCursorTreeFragment}
 */
public interface StatsCursorLoaderCallback {

    static final String BUNDLE_DATE = "BUNDLE_DATE";
    static final String BUNDLE_GROUP_ID = "BUNDLE_GROUP_ID";
    
    public void onUriRequested(int id, Uri uri, Bundle bundle);
    
}
