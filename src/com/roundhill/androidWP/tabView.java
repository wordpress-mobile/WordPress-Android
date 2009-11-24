package com.roundhill.androidWP;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.TabHost;

public class tabView extends TabActivity {
	private String id = "";
	private String accountName = "";
	private String activateTab = "";
	
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
         }
         
         Intent tab1 = new Intent(this, viewPosts.class);
         Intent tab2 = new Intent(this, viewLocalDrafts.class);
         Intent tab3 = new Intent(this, moderateComments.class);
         Intent tab4 = new Intent(this, viewPages.class);
         
        Bundle bundle = new Bundle();
 		bundle.putString("accountName", accountName);
 		bundle.putString("id", id);
 		
 		tab1.putExtras(bundle);
 		tab2.putExtras(bundle);
 		tab3.putExtras(bundle);
 		tab4.putExtras(bundle);
         
         TabHost host = getTabHost();  
         
         host.addTab(host.newTabSpec("one").setIndicator("On My Blog", getResources().getDrawable(R.drawable.on_blog)).setContent(tab1));  
         host.addTab(host.newTabSpec("two").setIndicator("Local Drafts", getResources().getDrawable(R.drawable.on_device)).setContent(tab2));
         host.addTab(host.newTabSpec("three").setIndicator("Comments", getResources().getDrawable(R.drawable.comments_tab)).setContent(tab3));
         host.addTab(host.newTabSpec("four").setIndicator("Pages", getResources().getDrawable(R.drawable.comments_tab)).setContent(tab4));
         if (activateTab != null){
        	 if (activateTab.equals("drafts")){
        	 host.setCurrentTab(1);
        	 }
         }
         

     }  
 }
