package org.wordpress.android.ui.posts;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.wordpress.android.ui.posts.ContentType.POST;

@Retention(RetentionPolicy.SOURCE)
@IntDef({POST})
public @interface ContentType {
    int POST = 0;
    int PAGE = 1;
    int PORTFOLIO = 2;
}
