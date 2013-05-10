package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.util.EscapeUtils;
import org.wordpress.android.util.StringHelper;

public class ViewPostFragment extends Fragment {
    /** Called when the activity is first created. */

    private OnDetailPostActionListener onDetailPostActionListener;
    PostsActivity parentActivity;

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

    }

    @Override
    public void onResume() {
        super.onResume();

        if (WordPress.currentPost != null)
            loadPost(WordPress.currentPost);

        parentActivity = (PostsActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.viewpost, container, false);

        // button listeners here
        ImageButton editPostButton = (ImageButton) v
                .findViewById(R.id.editPost);
        editPostButton.setOnClickListener(new ImageButton.OnClickListener() {
            public void onClick(View v) {
                if (WordPress.currentPost != null && !parentActivity.isRefreshing) {
                    onDetailPostActionListener.onDetailPostAction(
                            PostsActivity.POST_EDIT, WordPress.currentPost);
                    Intent i = new Intent(
                            getActivity().getApplicationContext(),
                            EditPostActivity.class);
                    i.putExtra("isPage", WordPress.currentPost.isPage());
                    i.putExtra("postID", WordPress.currentPost.getId());
                    i.putExtra("localDraft", WordPress.currentPost.isLocalDraft());
                    startActivityForResult(i, 0);
                }

            }
        });

        ImageButton shareURLButton = (ImageButton) v
                .findViewById(R.id.sharePostLink);
        shareURLButton.setOnClickListener(new ImageButton.OnClickListener() {
            public void onClick(View v) {

                if (!parentActivity.isRefreshing)
                    onDetailPostActionListener.onDetailPostAction(PostsActivity.POST_SHARE, WordPress.currentPost);

            }
        });

        ImageButton deletePostButton = (ImageButton) v
                .findViewById(R.id.deletePost);
        deletePostButton.setOnClickListener(new ImageButton.OnClickListener() {
            public void onClick(View v) {

                if (!parentActivity.isRefreshing)
                    onDetailPostActionListener.onDetailPostAction(PostsActivity.POST_DELETE, WordPress.currentPost);

            }
        });

        ImageButton viewPostButton = (ImageButton) v
                .findViewById(R.id.viewPost);
        viewPostButton.setOnClickListener(new ImageButton.OnClickListener() {
            public void onClick(View v) {

                if (!parentActivity.isRefreshing)
                    loadPostPreview();

            }
        });
        
        ImageButton addCommentButton = (ImageButton) v
                .findViewById(R.id.addComment);
        addCommentButton.setOnClickListener(new ImageButton.OnClickListener() {
            public void onClick(View v) {

                if (!parentActivity.isRefreshing)
                    onDetailPostActionListener.onDetailPostAction(PostsActivity.POST_COMMENT, WordPress.currentPost);

            }
        });

        return v;

    }

    protected void loadPostPreview() {

        if (WordPress.currentPost != null) {
            if (WordPress.currentPost.getPermaLink() != null && !WordPress.currentPost.getPermaLink().equals("")) {
                Intent i = new Intent(getActivity(), PreviewPostActivity.class);
                startActivity(i);
            }
        }

    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // check that the containing activity implements our callback
            onDetailPostActionListener = (OnDetailPostActionListener) activity;
        } catch (ClassCastException e) {
            activity.finish();
            throw new ClassCastException(activity.toString()
                    + " must implement Callback");
        }
    }
    
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

       private final int SWIPE_MIN_DISTANCE = 50;
       private final int SWIPE_THRESHOLD_VELOCITY = 60;
   
       @Override
       public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
          if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {                
           // Right to left
          swipePost(1);				//Pass 1 for nextpost
             return true;
          } 
          else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) >  SWIPE_THRESHOLD_VELOCITY) {
           // Left to right
          swipePost(0);				//Pass 0 for prevpost
                  return true;
            }
                  
                  return false;
           }
        }
    
    
    public void swipePost(int motionEvent){
        ViewPostsFragment vpf = new ViewPostsFragment();
        String prevpostid,nextpostid;
        long newid;
        
        if(motionEvent==0){
            prevpostid = vpf.getprevID();
            int previd=  Integer.parseInt(prevpostid);
            newid = (long)previd;
        }
        else{
            nextpostid = vpf.getnextID();
            int nextid=  Integer.parseInt(nextpostid);
            newid=(long)nextid;
               }
        Post post = new Post(WordPress.currentBlog.getId(),newid
                , false);
        loadPost(post);
        }
    
    
    public GestureDetector gesturedetector;

    public void loadPost(Post post) {

        // Don't load if the Post object of title are null, see #395
        if (post == null || post.getTitle() == null)
            return;
	gesturedetector = new GestureDetector(new GestureListener());
        TextView title = (TextView) getActivity().findViewById(R.id.postTitle);
        if (post.getTitle().equals(""))
            title.setText("(" + getResources().getText(R.string.untitled) + ")");
        else
            title.setText(EscapeUtils.unescapeHtml(post.getTitle()));
	RelativeLayout layout = (RelativeLayout)getActivity().findViewById(R.id.postHead);
        WebView webView = (WebView) getActivity().findViewById(
                R.id.viewPostWebView);
        TextView tv = (TextView) getActivity().findViewById(
                R.id.viewPostTextView);
        ImageButton shareURLButton = (ImageButton) getActivity().findViewById(
                R.id.sharePostLink);
        ImageButton viewPostButton = (ImageButton) getActivity().findViewById(
                R.id.viewPost);
        ImageButton addCommentButton = (ImageButton) getActivity().findViewById(
                R.id.addComment);
        layout.setOnTouchListener(new OnTouchListener() {
    
	    @Override
	    public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
			gesturedetector.onTouchEvent(event);
			return true;
	    }
            
            
        });        
        webView.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
	    gesturedetector.onTouchEvent(event);
	    return true;
	    }
    
    
	});
	
	

        tv.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        String html = StringHelper.addPTags(post.getDescription()
                + "\n\n" + post.getMt_text_more());

        String htmlText = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"webview.css\" /></head><body><div id=\"container\">"
                + html + "</div></body></html>";
        webView.loadDataWithBaseURL("file:///android_asset/", htmlText,
                "text/html", "utf-8", null);

        if (post.isLocalDraft()) {
            shareURLButton.setVisibility(View.GONE);
            viewPostButton.setVisibility(View.GONE);
            addCommentButton.setVisibility(View.GONE);
        } else {
            shareURLButton.setVisibility(View.VISIBLE);
            viewPostButton.setVisibility(View.VISIBLE);
            if (post.isMt_allow_comments()) {
                addCommentButton.setVisibility(View.VISIBLE);
            } else {
                addCommentButton.setVisibility(View.GONE);
            }
        }

    }

    public interface OnDetailPostActionListener {
        public void onDetailPostAction(int action, Post post);
    }

    public void clearContent() {
        TextView title = (TextView) getActivity().findViewById(R.id.postTitle);
        title.setText("");
        WebView webView = (WebView) getActivity().findViewById(
                R.id.viewPostWebView);
        TextView tv = (TextView) getActivity().findViewById(
                R.id.viewPostTextView);
        tv.setText("");
        String htmlText = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"webview.css\" /></head><body><div id=\"container\"></div></body></html>";
        webView.loadDataWithBaseURL("file:///android_asset/", htmlText,
                "text/html", "utf-8", null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }

}
