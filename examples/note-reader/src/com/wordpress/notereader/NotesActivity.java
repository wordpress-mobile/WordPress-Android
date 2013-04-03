package com.wordpress.notereader;

import android.app.Activity;
import android.os.Bundle;

import com.wordpress.rest.Oauth;

import android.util.Log;

public class NotesActivity extends Activity
{
    public static final String TAG="WPNotes";
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
    
    @Override
    public void onStart(){
        super.onStart();
        Log.d(TAG, "Starting");
        Oauth oauth = new Oauth();
    }
}
