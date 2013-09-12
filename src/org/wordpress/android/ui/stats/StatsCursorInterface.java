package org.wordpress.android.ui.stats;

import android.database.Cursor;
import android.net.Uri;

/**
 * An interface to call when the cursor has been loaded. 
 * Used so that the {@link StatsAbsPagedViewFragment} can update its titles
 */
public interface StatsCursorInterface {

    public void onCursorLoaded(Uri uri, Cursor cursor);
    
}
