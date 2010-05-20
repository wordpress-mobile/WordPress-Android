package org.wordpress.android;

import java.util.HashMap;
import java.util.Vector;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;


public class Preferences extends Activity {
    /** Called when the activity is first created. */
	public Vector accounts;
	public Vector accountNames = new Vector();
	private String selectedID = "";
	public int checkCtr = 0;
	protected static Intent svc = null;
	
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
      
        displayAccounts();

    }
    
public void displayAccounts(){
	//settings time!
    final WordPressDB settingsDB = new WordPressDB(this);
	accounts = settingsDB.getAccounts(this);
	HashMap notificationOptions = settingsDB.getNotificationOptions(this);
	boolean sound = false, vibrate = false, light = false, taglineValue = false;
	String tagline = "";
	
	if (notificationOptions != null){
		if (notificationOptions.get("sound").toString().equals("1")){
			sound = true;
		}
		if (notificationOptions.get("vibrate").toString().equals("1")){
			vibrate = true;
		}
		if (notificationOptions.get("light").toString().equals("1")){
			light = true;
		}
		if (notificationOptions.get("tagline_flag").toString().equals("1")){
			taglineValue = true;
		}
		tagline = notificationOptions.get("tagline").toString();
	}
	
	if (accounts.size() > 0){
		ScrollView sv = new ScrollView(this);
		sv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));
		sv.setBackgroundColor(Color.parseColor("#FFF5F5F5"));
		LinearLayout layout = new LinearLayout(this);
		
		layout.setPadding(14, 14, 14, 14);
		layout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));

		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setGravity(Gravity.LEFT);
		
		final LinearLayout cbLayout = new LinearLayout(this);
		
		cbLayout.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));

		cbLayout.setOrientation(LinearLayout.VERTICAL);
		
		LinearLayout.LayoutParams section1Params = new LinearLayout.LayoutParams 
        (LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		
		section1Params.setMargins(0, 0, 0, 20);
		
		LinearLayout section1 = new LinearLayout(this);
		section1.setBackgroundDrawable(getResources().getDrawable(R.drawable.content_bg));
		section1.setLayoutParams(section1Params);
		section1.setOrientation(LinearLayout.VERTICAL);
		
        TextView textView = new TextView(this);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setPadding(0, -2, 0, 0);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setShadowLayer(1, 0, 2, Color.parseColor("#FFFFFFFF"));
        textView.setText("Comment Notifications");

        section1.addView(textView);
        
        LinearLayout.LayoutParams cbParams = new LinearLayout.LayoutParams 
        (LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cbParams.setMargins(0, 0, 0, 6);
        
        for (int i = 0; i < accounts.size(); i++) {
            
        	HashMap curHash = (HashMap) accounts.get(i);
        	String curBlogName = curHash.get("blogName").toString();
        	String curUsername = curHash.get("username").toString();
        	String accountID = curHash.get("id").toString();
        	int runService = Integer.valueOf(curHash.get("runService").toString());
        	accountNames.add(i, curBlogName);
            
            final CheckBox checkBox = new CheckBox(this);
            checkBox.setTextColor(Color.parseColor("#444444"));
            checkBox.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            checkBox.setText(escapeUtils.unescapeHtml(curBlogName));
            checkBox.setId(Integer.valueOf(accountID));  
            checkBox.setLayoutParams(cbParams);
            
            if (runService == 1){
            	checkBox.setChecked(true);
            }

            cbLayout.addView(checkBox);  
        } 
        
        if (cbLayout.getChildCount() > 0){
        	section1.addView(cbLayout);
        }
        
        //add spinner and buttons
        TextView textView2 = new TextView(this);
        textView2.setTextColor(Color.parseColor("#444444"));
        textView2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView2.setText(getResources().getText(R.string.notifications_interval));

        section1.addView(textView2);
        
        final Spinner sInterval = new Spinner(this);
        sInterval.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));
	    ArrayAdapter<Object> sIntervalArrayAdapter = new ArrayAdapter<Object>(this,
	    		R.layout.spinner_textview,
	            new String[] { "5 Minutes", "10 Minutes", "15 Minutes", "30 Minutes" , "1 Hour", "3 Hours", "6 Hours", "12 Hours", "Daily"});
	    sIntervalArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    sInterval.setAdapter(sIntervalArrayAdapter);
	    String interval = settingsDB.getInterval(this);
	    
	    if (interval != ""){
            sInterval.setSelection(sIntervalArrayAdapter.getPosition(interval));
	    }
	    
	    section1.addView(sInterval);
	    
	    final LinearLayout nOptionsLayout = new LinearLayout(this);
		
	    nOptionsLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));

	    nOptionsLayout.setOrientation(LinearLayout.VERTICAL);
	    
	    CheckBox soundCB = new CheckBox(this);
	    soundCB.setTag("soundCB");
	    soundCB.setTextColor(Color.parseColor("#444444"));
	    soundCB.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
	    soundCB.setText(getResources().getText(R.string.notification_sound));
        soundCB.setLayoutParams(cbParams);
        soundCB.setChecked(sound);
        
        nOptionsLayout.addView(soundCB);
        
        CheckBox vibrateCB = new CheckBox(this);
        vibrateCB.setTag("vibrateCB");
        vibrateCB.setTextColor(Color.parseColor("#444444"));
        vibrateCB.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        vibrateCB.setText(getResources().getText(R.string.notification_vibrate));
        vibrateCB.setLayoutParams(cbParams);
        vibrateCB.setChecked(vibrate);
        
        nOptionsLayout.addView(vibrateCB);
        
        CheckBox lightCB = new CheckBox(this);
        lightCB.setTag("lightCB");
        lightCB.setTextColor(Color.parseColor("#444444"));
        lightCB.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        lightCB.setText(getResources().getText(R.string.notification_blink));
        lightCB.setLayoutParams(cbParams);
        lightCB.setChecked(light);
        
        nOptionsLayout.addView(lightCB);
        
        section1.addView(nOptionsLayout);
        
        layout.addView(section1);
        
        final LinearLayout section2 = new LinearLayout(this);
		section2.setBackgroundDrawable(getResources().getDrawable(R.drawable.content_bg));
		section2.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams section2Params = new LinearLayout.LayoutParams 
        (LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        section2Params.setMargins(0, 0, 0, 20);
		section2.setLayoutParams(section2Params);
		
		TextView section2lbl = new TextView(this);
		section2lbl.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
		section2lbl.setPadding(0, -2, 0, 0);
		section2lbl.setTypeface(Typeface.DEFAULT_BOLD);
		section2lbl.setShadowLayer(1, 0, 2, Color.parseColor("#FFFFFFFF"));
		section2lbl.setText("Post Signature");
		
		section2.addView(section2lbl);
        
        CheckBox taglineCB = new CheckBox(this);
        taglineCB.setTag("taglineCB");
        taglineCB.setTextColor(Color.parseColor("#444444"));
        taglineCB.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        taglineCB.setText("Add a tag line to new posts:");
        taglineCB.setLayoutParams(cbParams);
        taglineCB.setChecked(taglineValue);
        
        section2.addView(taglineCB);
        
        EditText taglineET = new EditText(this);
        if (tagline != null){
        	if (tagline.equals("")){
            	taglineET.setText("Posted from WordPress for Android");
            }
        	else{
        		taglineET.setText(tagline);
        	}
        } 
        taglineET.setMinLines(2);
        
        section2.addView(taglineET);
        
        layout.addView(section2);
	    
	    final Button save = new Button(this);

	    save.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.FILL_PARENT));
	    save.setBackgroundDrawable(getResources().getDrawable(R.drawable.wp_button));
        save.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
	    save.setText("Save");
	    
	    save.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	boolean sound = false, vibrate = false, light = false, tagValue = false;
            	checkCtr = 0;
            	
            	int listItemCount = cbLayout.getChildCount();
            	for( int i=0;i<listItemCount;i++ ) {
            	    CheckBox cbox = (CheckBox) ((View)cbLayout.getChildAt(i));
            	    	int id = cbox.getId();
                	    if( cbox.isChecked() ) {   
                	    	checkCtr++;
                	        settingsDB.updateNotificationFlag(Preferences.this, id, true);
                	        Log.i("CommentService", "Service enabled for " + cbox.getText());
                	    }
                	    else{
                	        settingsDB.updateNotificationFlag(Preferences.this, id, false);
                	    }
            	}
            	
            	int noOptionsItemCount = nOptionsLayout.getChildCount();
            	for( int i=0;i<noOptionsItemCount;i++ ) {
            	    CheckBox cbox = (CheckBox) ((View)nOptionsLayout.getChildAt(i));
	        	    if (cbox.getTag().equals("soundCB")){
		    			sound = cbox.isChecked();
		    		}
		    		else if (cbox.getTag().equals("vibrateCB")){
		    			vibrate = cbox.isChecked();
		    		}
		    		else if (cbox.getTag().equals("lightCB")){
		    			light = cbox.isChecked();
		    		}
            	}
            	
            	CheckBox tagFlag = (CheckBox) ((View)section2.getChildAt(1));
            	tagValue = tagFlag.isChecked();
            	
            	EditText taglineET = (EditText) ((View)section2.getChildAt(2));
            	String taglineText = taglineET.getText().toString();

            	settingsDB.updateNotificationSettings(Preferences.this, sInterval.getSelectedItem().toString(), sound, vibrate, light, tagValue, taglineText);
            	
            	if (checkCtr > 0){

        	        String updateInterval = sInterval.getSelectedItem().toString();
        	        int UPDATE_INTERVAL = 3600000;
        	        
        	      //configure time interval
        	         if (updateInterval.equals("5 Minutes")){
        	        	 UPDATE_INTERVAL = 300000;
        	         }
        	         else if (updateInterval.equals("10 Minutes")){
        	        	 UPDATE_INTERVAL = 600000;
        	         }
        	         else if (updateInterval.equals("15 Minutes")){
        	        	 UPDATE_INTERVAL = 900000;
        	         }
        	         else if (updateInterval.equals("30 Minutes")){
        	        	 UPDATE_INTERVAL = 1800000;
        	         }
        	         else if (updateInterval.equals("1 Hour")){
        	        	 UPDATE_INTERVAL = 3600000;
        	         }
        	         else if (updateInterval.equals("3 Hours")){
        	        	 UPDATE_INTERVAL = 10800000;
        	         }
        	         else if (updateInterval.equals("6 Hours")){
        	        	 UPDATE_INTERVAL = 21600000;
        	         }
        	         else if (updateInterval.equals("12 Hours")){
        	        	 UPDATE_INTERVAL = 43200000;
        	         }
        	         else if (updateInterval.equals("Daily")){
        	        	 UPDATE_INTERVAL = 86400000;
        	         }
        	        
        	        Intent intent = new Intent(Preferences.this, broadcastReceiver.class);
                	PendingIntent pIntent = PendingIntent.getBroadcast(Preferences.this, 0, intent, 0);
                	
                	AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                	
                	alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (5 * 1000), UPDATE_INTERVAL, pIntent);
        			  
        		}
        		else{

        						Intent stopIntent = new Intent(Preferences.this, broadcastReceiver.class);
                            	PendingIntent stopPIntent = PendingIntent.getBroadcast(Preferences.this, 0, stopIntent, 0);
                            	AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                            	alarmManager.cancel(stopPIntent); 
                            	
                            	Intent service = new Intent(Preferences.this, commentService.class);
                            	stopService(service);

            	}
            	
            	finish();
            }
        });
	    
	    
	    layout.addView(save);
        
        sv.addView(layout);
        
        setContentView(sv);
	}
}


}