package org.wordpress.android;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;

/**
 * Extends popup window to show the WordPress dashboard.
 */
public class QuickDashboard extends PopupWindows {
    private View mRootView;
    private LayoutInflater inflater;
    Context context;
    
    protected static final int ANIM_GROW_FROM_LEFT = 1;
    protected static final int ANIM_GROW_FROM_RIGHT = 2;
    protected static final int ANIM_GROW_FROM_CENTER = 3;
    protected static final int ANIM_REFLECT = 4;
    public static final int ANIM_AUTO = 5;
    TextView commentBadge;
    
    private int animStyle;
    
    /**
     * Constructor.
     * 
     * @param context Context
     */
    public QuickDashboard(final Context ctx) {
        super(ctx);
        context = ctx;
        inflater        = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        setRootViewId(R.layout.dashboard_popover);
       
        animStyle       = ANIM_AUTO;
        
        commentBadge = (TextView) mRootView.findViewById(R.id.comment_badge);
        
        updateCommentBadge();
        
        Button writeButton = (Button) mRootView.findViewById(R.id.dashboard_write_btn);
        writeButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				 Intent i = new Intent(context, EditPost.class);
                 i.putExtra("id", WordPress.currentBlog.getId());
                 i.putExtra("isNew", true);
                 i.putExtra("option", "");
                 context.startActivity(i);
			}
		});

        Button postsButton = (Button) mRootView.findViewById(R.id.dashboard_posts_btn);
        postsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(context, ViewPosts.class);
                context.startActivity(i);
            }
        });

        Button pagesButton = (Button) mRootView.findViewById(R.id.dashboard_pages_btn);
        pagesButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(context, ViewPosts.class);
                i.putExtra("id", WordPress.currentBlog.getId());
                i.putExtra("isNew", true);
                i.putExtra("viewPages", true);
                context.startActivity(i);
            }
        });

        /*
         * Button draftsButton = (Button)
         * findViewById(R.id.dashboard_drafts_btn);
         * draftsButton.setOnClickListener(new View.OnClickListener() {
         * public void onClick(View v) { Intent i = new
         * Intent(Dashboard.this, ViewDrafts.class); i.putExtra("id", id);
         * i.putExtra("blavatar", blavatar_url); i.putExtra("isNew", true);
         * startActivityForResult(i, 0); } });
         */

        Button commentsButton = (Button) mRootView.findViewById(R.id.dashboard_comments_btn);
        commentsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(context, ViewComments.class);
                i.putExtra("id", WordPress.currentBlog.getId());
                i.putExtra("isNew", true);
                context.startActivity(i);
            }
        });

        Button statsButton = (Button) mRootView.findViewById(R.id.dashboard_stats_btn);
        statsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(context, ViewStats.class);
                i.putExtra("id", WordPress.currentBlog.getId());
                i.putExtra("isNew", true);
                context.startActivity(i);
            }
        });

        Button settingsButton = (Button) mRootView.findViewById(R.id.dashboard_settings_btn);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(context, Settings.class);
                i.putExtra("id", WordPress.currentBlog.getId());
                i.putExtra("isNew", true);
                context.startActivity(i);
            }
        });
        
        Button subsButton = (Button) mRootView.findViewById(R.id.dashboard_subs_btn);
        subsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(context, About.class);
                i.putExtra("id", WordPress.currentBlog.getId());
                i.putExtra("loadReader", true);
                context.startActivity(i);
            }
        });
        
        Button picButton = (Button) mRootView.findViewById(R.id.dashboard_picture_btn);
        picButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(context, EditPost.class);
				i.putExtra("option", "newphoto");
				i.putExtra("isNew", true);
				context.startActivity(i);					
			}
		});
        
        Button videoButton = (Button) mRootView.findViewById(R.id.dashboard_video_btn);
        videoButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(context, EditPost.class);
				i.putExtra("option", "newvideo");
				i.putExtra("isNew", true);
				context.startActivity(i);					
			}
		});

        Button quickpress = (Button) mRootView.findViewById(R.id.quickpress);

        quickpress.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(context, EditPost.class);
                i.putExtra("id", WordPress.currentBlog.getId());
                i.putExtra("isNew", true);
                i.putExtra("option", "");
                context.startActivity(i);
            }
        });
    }

    /**
     * Set root view.
     * 
     * @param id Layout resource id
     */
    public void setRootViewId(int id) {
        mRootView   = (ViewGroup) inflater.inflate(id, null);
        
        setContentView(mRootView);
    }
    
    /**
     * Set animation style
     * 
     * @param animStyle animation style, default is set to ANIM_AUTO
     */
    public void setAnimStyle(int animStyle) {
        this.animStyle = animStyle;
    }
    
    /**
     * Show popup window. Popup is automatically positioned, on top or bottom of anchor view.
     * 
     */
    public void show (View anchor) {
        preShow();
        
        int xPos, yPos;
        
        int[] location      = new int[2];
    
        anchor.getLocationOnScreen(location);

        Rect anchorRect     = new Rect(location[0], location[1], location[0] + anchor.getWidth(), location[1] 
                            + anchor.getHeight());

        mRootView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        mRootView.measure(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
    
        int rootHeight      = mRootView.getMeasuredHeight();
        
        int screenWidth     = mWindowManager.getDefaultDisplay().getWidth();
        int screenHeight    = mWindowManager.getDefaultDisplay().getHeight();
        
        //we always want the x coord at 0 for the dashboard of awesomeness
        xPos = 0;
        
        int dyTop           = anchorRect.top;
        int dyBottom        = screenHeight - anchorRect.bottom;

        boolean onTop       = (dyTop > dyBottom) ? true : false;

        if (onTop) {
            if (rootHeight > dyTop) {
                yPos            = 15;
            } else {
                yPos = anchorRect.top - rootHeight;
            }
        } else {
            yPos = anchorRect.bottom;
            
            if (rootHeight > dyBottom) { 
            }
        }
        
        
        setAnimationStyle(screenWidth, anchorRect.centerX(), onTop);
        
        mWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos);
        
        isShowing = true;
    }
    
    /**
     * Set animation style
     * 
     * @param screenWidth screen width
     * @param requestedX distance from left edge
     * @param onTop flag to indicate where the popup should be displayed. Set TRUE if displayed on top of anchor view
     *        and vice versa
     */
    private void setAnimationStyle(int screenWidth, int requestedX, boolean onTop) {

        switch (animStyle) {
        case ANIM_GROW_FROM_LEFT:
            mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Left : R.style.Animations_PopDownMenu_Left);
            break;
                    
        case ANIM_GROW_FROM_RIGHT:
            mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Right : R.style.Animations_PopDownMenu_Right);
            break;
                    
        case ANIM_GROW_FROM_CENTER:
            mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Center : R.style.Animations_PopDownMenu_Center);
        break;
            
        case ANIM_REFLECT:
            mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Reflect : R.style.Animations_PopDownMenu_Reflect);
        break;
        
        case ANIM_AUTO:

                mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Right : R.style.Animations_PopDownMenu_Right);
                    
            break;
        }
    }
    
    public void updateCommentBadge() {
        if (WordPress.currentBlog != null) {
            commentBadge.setText(String.valueOf(WordPress.currentBlog.getUnmoderatedCommentCount(context)));
        }
    }
}