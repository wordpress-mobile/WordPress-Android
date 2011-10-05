package org.wordpress.android;

import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Comment;

import android.app.Application;
import android.text.Spannable;

public class WordPress extends Application {
    public static Blog currentBlog;
	public static Spannable richPostContent;
	public static Comment currentComment;
}
