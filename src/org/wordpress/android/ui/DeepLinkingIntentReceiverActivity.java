package org.wordpress.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import org.wordpress.android.R;
import org.wordpress.android.ui.reader_native.ReaderActivityLauncher;
import org.wordpress.android.util.ReaderLog;
import org.wordpress.android.util.ToastUtils;

/**
 * An activity to handle deep linking. 
 * 
 * wordpress://viewpost?blogId={blogId}&postId={postId}
 * 
 * Redirects users to the reader activity along with IDs passed in the intent
 * 
 * @todo make sure this works for logged out users
 */
public class DeepLinkingIntentReceiverActivity extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //read the parameters and launch the Reader Activity
        Intent intent = getIntent();
        String action = getIntent().getAction();
        Uri uri = intent.getData();

        // check if this intent is started via custom scheme link
        if (Intent.ACTION_VIEW.equals(action) && uri != null) {
            String strBlogId = uri.getQueryParameter("blogId");
            String strPostId = uri.getQueryParameter("postId");

            if (!TextUtils.isEmpty(strBlogId) && !TextUtils.isEmpty(strPostId)) {
                ReaderLog.i(String.format("opening blogId %s, postId %s", strBlogId, strPostId));
                try {
                    ReaderActivityLauncher.showReaderPostDetail(this, Long.parseLong(strBlogId), Long.parseLong(strPostId));
                } catch (NumberFormatException e) {
                    ReaderLog.e(e);
                }
            } else {
                ToastUtils.showToast(this, R.string.error_generic);
            }
        }
        
        finish();
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
