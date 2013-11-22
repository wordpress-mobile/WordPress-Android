package org.wordpress.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.ui.reader.ReaderActivity;
import org.wordpress.android.ui.reader_native.NativeReaderActivity;

/**
 * An activity to handle deep linking. 
 * 
 * wordpress://viewpost?blogId={blogId}&postId={postId}
 * 
 * Redirects users to the reader activity along with IDs passed in the intent
 * 
 * @todo make sure this works work logged out users
 */
public class DeepLinkingIntentReceiverActivity extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //read the parameters and launch the Reader Activity
        Intent intent = getIntent();
        String action = getIntent().getAction();
        
        // check if this intent is started via custom scheme link
        if (Intent.ACTION_VIEW.equals(action)) {
            Uri uri = intent.getData();
            
            String blogId = uri.getQueryParameter("blogId");
            String postId = uri.getQueryParameter("postId"); 
            
            if ( blogId != null && blogId.isEmpty() == false && postId != null && postId.isEmpty() == false ) {
                Bundle bundle = new Bundle();
                bundle.putString("blogID", blogId);
                bundle.putString("postID", postId);
                
                Intent newIntent;
                if (Constants.ENABLE_NATIVE_READER) {
                    newIntent = new Intent(this, NativeReaderActivity.class);
                } else {
                    newIntent = new Intent(this, ReaderActivity.class);
                }
               
                newIntent.putExtras(bundle);
                startActivity(newIntent);
            } else {
                Toast.makeText(
                        this,
                        getResources().getText(R.string.error_generic),
                        Toast.LENGTH_SHORT).show();
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
