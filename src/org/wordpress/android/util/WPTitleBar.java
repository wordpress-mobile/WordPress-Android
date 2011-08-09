
package org.wordpress.android.util;

import org.wordpress.android.ActionItem;
import org.wordpress.android.QuickAction;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.Blog;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

public class WPTitleBar extends LinearLayout {

    QuickAction qa;
    public QuickAction qaBlogs;
    public ArrayList<String> defBlogNames = new ArrayList<String>();
    public int[] blogIDs;
    public Vector<?> accounts;
    private Context context;
    Button blogTitle;
    public ImageButton refreshButton;
    OnBlogChangedListener onBlogChangedListener = null;

    public WPTitleBar(final Context ctx, AttributeSet attrs) {
        super(ctx, attrs);

        context = ctx;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        final WordPressDB settingsDB = new WordPressDB(context);
        accounts = settingsDB.getAccounts(context);

        defBlogNames.clear();
        blogIDs = new int[accounts.size()];

        qaBlogs = new QuickAction(context);
        qaBlogs.setAnimStyle(QuickAction.ANIM_AUTO);

        for (int i = 0; i < accounts.size(); i++) {
            HashMap<?, ?> defHash = (HashMap<?, ?>) accounts.get(i);
            String curBlogName = EscapeUtils.unescapeHtml(defHash.get("blogName").toString());

            defBlogNames.add(curBlogName);
            blogIDs[i] = Integer.valueOf(defHash.get("id").toString());

            ActionItem blogIA = new ActionItem();

            blogIA.setTitle(curBlogName);
            blogIA.setIcon(getResources().getDrawable(R.drawable.wp_logo));
            qaBlogs.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
                @Override
                public void onItemClick(int pos) {
                    blogTitle.setText(defBlogNames.get(pos));
                    WordPress.currentBlog = new Blog(blogIDs[pos], context);
                    settingsDB.updateLastBlogID(context, blogIDs[pos]);
                    if (onBlogChangedListener != null) {
                        onBlogChangedListener.OnBlogChanged();
                    }

                }
            });

            blogTitle = (Button) findViewById(R.id.blog_title);
            blogTitle.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    if (qaBlogs != null)
                    {
                        qaBlogs.show(v);
                    }
                }
            });

            qaBlogs.addActionItem(blogIA);

        }

        int lastBlogID = settingsDB.getLastBlogID(context);
        if (lastBlogID != -1) {
            try {
                boolean matchedID = false;
                for (int i = 0; i < blogIDs.length; i++) {
                    if (blogIDs[i] == lastBlogID) {
                        matchedID = true;
                        WordPress.currentBlog = new Blog(blogIDs[i], context);
                    }
                }
                if (!matchedID) {
                    WordPress.currentBlog = new Blog(blogIDs[0], context);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (blogIDs.length > 0)
                WordPress.currentBlog = new Blog(blogIDs[0], context);
        }

        if (WordPress.currentBlog != null) {

            refreshButton = (ImageButton) findViewById(R.id.action_refresh);

            blogTitle.setText(WordPress.currentBlog.getBlogName());

            final ActionItem newpost = new ActionItem();

            newpost.setTitle("Add New Post");
            newpost.setIcon(getResources().getDrawable(R.drawable.posts_tab));

            final ActionItem newpage = new ActionItem();

            newpage.setTitle("Add New Page");
            newpage.setIcon(getResources().getDrawable(R.drawable.pages_tab));

            final ActionItem addOldPhoto = new ActionItem();
            addOldPhoto.setTitle("Add Image From Gallery");
            addOldPhoto.setIcon(getResources().getDrawable(R.drawable.media));

            final ActionItem takeNewPhoto = new ActionItem();
            takeNewPhoto.setTitle("Take Photo");
            takeNewPhoto.setIcon(getResources().getDrawable(R.drawable.media));

            final ActionItem addOldVideo = new ActionItem();
            addOldVideo.setTitle("Add Video from Gallery");
            addOldVideo.setIcon(getResources().getDrawable(R.drawable.media));

            final ActionItem takeNewVideo = new ActionItem();
            takeNewVideo.setTitle("Take Video");
            takeNewVideo.setIcon(getResources().getDrawable(R.drawable.media));

            ImageButton add = (ImageButton) findViewById(R.id.add_small);

            add.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    qa = new QuickAction(context);
                    qa.addActionItem(newpost);
                    qa.addActionItem(newpage);
                    qa.addActionItem(addOldPhoto);
                    qa.addActionItem(takeNewPhoto);
                    qa.addActionItem(addOldVideo);
                    qa.addActionItem(takeNewVideo);
                    qa.setAnimStyle(QuickAction.ANIM_AUTO);
                    qa.show(v);
                }
            });
        }
    }

    public void addRefreshButton() {
        refreshButton.setVisibility(View.VISIBLE);
    }

    // Listener for when user changes blog in the ActionBar
    public interface OnBlogChangedListener {
        public abstract void OnBlogChanged();
    }

    public void setOnBlogChangedListener(OnBlogChangedListener listener) {
        onBlogChangedListener = listener;
    }
}
