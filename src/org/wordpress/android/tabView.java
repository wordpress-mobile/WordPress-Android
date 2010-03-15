package org.wordpress.android;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Window;
import android.widget.TabHost;

public class tabView extends TabActivity {
	private String id = "";
	private String accountName = "";
	private String activateTab = "";
	boolean fromNotification = false;
	
	     @Override  
     public void onCreate(Bundle savedInstanceState) {  
         super.onCreate(savedInstanceState);
         requestWindowFeature(Window.FEATURE_NO_TITLE);
         Bundle extras = getIntent().getExtras();
         if(extras !=null)
         {
          id = extras.getString("id");
          accountName = extras.getString("accountName");
          activateTab = extras.getString("activateTab");
          fromNotification = extras.getBoolean("fromNotification", false);   
         }
         
         Intent tab1 = new Intent(this, moderateCommentsTab.class);
         Intent tab2 = new Intent(this, viewPosts.class);
         Intent tab3 = new Intent(this, viewPosts.class);
         
        Bundle bundle = new Bundle();
 		bundle.putString("accountName", accountName);
 		bundle.putString("id", id);
 		
 		if (fromNotification){
 			bundle.putBoolean("fromNotification", true);
 		}
 		
 		tab1.putExtras(bundle);
 		tab2.putExtras(bundle);		
 		bundle.putBoolean("viewPages", true); 		
 		tab3.putExtras(bundle);
         
         TabHost host = getTabHost();  
         
         host.addTab(host.newTabSpec("one").setIndicator(getResources().getText(R.string.tab_comments), getResources().getDrawable(R.layout.comment_tab_selector)).setContent(tab1));  
         host.addTab(host.newTabSpec("two").setIndicator(getResources().getText(R.string.tab_posts), getResources().getDrawable(R.layout.posts_tab_selector)).setContent(tab2));
         host.addTab(host.newTabSpec("three").setIndicator(getResources().getText(R.string.tab_pages), getResources().getDrawable(R.layout.pages_tab_selector)).setContent(tab3));

         if (activateTab != null){
        	 if (activateTab.equals("drafts")){
        	 host.setCurrentTab(1);
        	 }
         }
         

     }  
	     
	     @Override
	     public void onConfigurationChanged(Configuration newConfig) {
	       //ignore orientation change
	       super.onConfigurationChanged(newConfig);
	     } 
 }
