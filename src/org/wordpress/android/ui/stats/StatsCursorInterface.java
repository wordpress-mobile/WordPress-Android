
package org.wordpress.android.ui.stats;

import android.database.Cursor;
import android.net.Uri;

public interface StatsCursorInterface {

    public void onCursorLoaded(Uri uri, Cursor cursor);
    
}
