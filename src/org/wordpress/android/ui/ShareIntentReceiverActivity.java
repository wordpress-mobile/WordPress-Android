package org.wordpress.android.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.internal.widget.IcsAdapterView;
import com.actionbarsherlock.internal.widget.IcsAdapterView.OnItemSelectedListener;
import com.actionbarsherlock.internal.widget.IcsSpinner;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.accounts.NewAccountActivity;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.util.StringUtils;

/**
 * An activity to handle share intents, since there are multiple actions possible. 
 * If there are multiple blogs, it lets the user choose which blog to share to.
 * It lists what actions that the user can perform and redirects them to the activity,
 * along with the content passed in the intent
 */
public class ShareIntentReceiverActivity extends SherlockFragmentActivity implements OnItemSelectedListener {

    private IcsSpinner mBlogSpinner;
    private IcsSpinner mActionSpinner;
    private int mAccountIDs[];
    private TextView mBlogSpinnerTitle;
    private int mActionIndex;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
  
        setContentView(R.layout.share_intent_receiver_dialog);     
        Context themedContext = getSupportActionBar().getThemedContext();
        
        mBlogSpinnerTitle = (TextView) findViewById(R.id.blog_spinner_title);
        mBlogSpinner = (IcsSpinner) findViewById(R.id.blog_spinner);
        
        String[] blogNames = getBlogNames();
        if (blogNames != null) {
            
            if (blogNames.length == 1) {
                // one blog
                mBlogSpinner.setVisibility(View.GONE);
                mBlogSpinnerTitle.setVisibility(View.GONE);
            } else {
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(themedContext, R.layout.sherlock_spinner_dropdown_item, blogNames);
                mBlogSpinner.setAdapter(adapter);
                mBlogSpinner.setOnItemSelectedListener(this);
            }
            
        }
        
        String[] actions = new String[] { getString(R.string.share_action_post), getString(R.string.share_action_media) };
        mActionSpinner = (IcsSpinner) findViewById(R.id.action_spinner);
        ArrayAdapter<String> actionAdapter = new ArrayAdapter<String>(themedContext, R.layout.sherlock_spinner_dropdown_item, actions);
        mActionSpinner.setAdapter(actionAdapter);
        mActionSpinner.setOnItemSelectedListener(this);
        
        getSupportActionBar().hide();
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
        
    private String[] getBlogNames() {
        List<Map<String, Object>> accounts = WordPress.wpDB.getAccounts();
        
        if (accounts.size() > 0) {

            final String blogNames[] = new String[accounts.size()];
            mAccountIDs = new int[accounts.size()];

            Blog blog;
            
            for (int i = 0; i < accounts.size(); i++) {

                Map<String, Object> curHash = accounts.get(i);
                try {
                    blogNames[i] = StringUtils.unescapeHTML(curHash.get("blogName").toString());
                } catch (Exception e) {
                    blogNames[i] = curHash.get("url").toString();
                }
                mAccountIDs[i] = (Integer) curHash.get("id");
                try {
                    blog = new Blog(mAccountIDs[i]);
                } catch (Exception e) {
                    showBlogErrorAndFinish();
                    return null;
                }
            }

            return blogNames;
        } else {
            // no account, load main view to load new account view
            Toast.makeText(this, getResources().getText(R.string.no_account), Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, NewAccountActivity.class));
            finish();
            return null;
        }
    }

    private void showBlogErrorAndFinish() {
        Toast.makeText(this, getResources().getText(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onItemSelected(IcsAdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == R.id.blog_spinner) {
            try {
                WordPress.currentBlog = new Blog(mAccountIDs[position]);
            } catch (Exception e) {
                showBlogErrorAndFinish();
            }
            WordPress.wpDB.updateLastBlogId(WordPress.currentBlog.getId());
        } else if (parent.getId() == R.id.action_spinner){
            mActionIndex = position;
        }
    }
    
    public void onShareClicked (View view) {

        String action = getIntent().getAction();
        
        Intent intent = null; 
        
        if (mActionIndex == 0) { 
            // new post
            intent = new Intent(this, EditPostActivity.class);
        } else if (mActionIndex == 1) {
            // add to media gallery
            intent = new Intent(this, MediaBrowserActivity.class);
        }

        if (intent != null) {
            intent.setAction(action);
            intent.setType(getIntent().getType());
            
            intent.putExtra(Intent.EXTRA_TEXT, getIntent().getStringExtra(Intent.EXTRA_TEXT));
            intent.putExtra(Intent.EXTRA_SUBJECT, getIntent().getStringExtra(Intent.EXTRA_SUBJECT));
            
            if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                ArrayList<Uri> extra = getIntent().getParcelableArrayListExtra((Intent.EXTRA_STREAM));
                intent.putExtra(Intent.EXTRA_STREAM, extra);
            } else {
                Uri extra = (Uri) getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
                intent.putExtra(Intent.EXTRA_STREAM, extra);
            }
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onNothingSelected(IcsAdapterView<?> parent) {
        // TODO Auto-generated method stub
        
    }
}
