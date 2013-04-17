package org.wordpress.android.ui.notifications;

import android.os.Bundle;
import android.os.Message;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.AsyncHttpClient;

import org.wordpress.android.R;
import org.wordpress.android.models.Note;

class NotificationsCommentFragment extends NotificationFragment {
    private static final String TAG="NoteComment";
    private static String LOREM="Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.\n\n";
    private TextView commentText;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state){
        View view = inflater.inflate(R.layout.notifications_comment, parent, false);
        commentText = (TextView) view.findViewById(R.id.note_text);
        return view;
    }
    @Override
    public void onStart(){
        super.onStart();
        // we should have been provided a note
        commentText.setText(Html.fromHtml(getNote().getCommentText()));
    }

}