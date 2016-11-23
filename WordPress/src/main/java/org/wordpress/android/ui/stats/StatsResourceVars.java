package org.wordpress.android.ui.stats;

import android.content.Context;
import android.content.res.Resources;

import org.wordpress.android.R;

/*
 * class which holds all resource-based variables used in Stats
 */
class StatsResourceVars {
    final int headerAvatarSizePx;

    StatsResourceVars(Context context) {
        Resources resources = context.getResources();

        headerAvatarSizePx = resources.getDimensionPixelSize(R.dimen.avatar_sz_small);
    }
}
