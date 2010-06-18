package org.wordpress.android;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.Toast;

public class tabView extends TabActivity {
	private String id = "";
	private String accountName = "";
	private String activateTab = "", action = "";
	boolean fromNotification = false;
	int uploadID = 0;
	
	     @Override  
     public void onCreate(Bundle savedInstanceState) {  
         super.onCreate(savedInstanceState);
         requestWindowFeature(Window.FEATURE_NO_TITLE);
         getWindow().setFormat(PixelFormat.RGBA_8888);
         getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
         Bundle extras = getIntent().getExtras();
         if(extras !=null)
         {
          id = extras.getString("id");
          accountName = extras.getString("accountName");
          activateTab = extras.getString("activateTab");
          fromNotification = extras.getBoolean("fromNotification", false);   
          action = extras.getString("action");
          uploadID = extras.getInt("uploadID");
         }
         
         Intent tab1 = new Intent(this, moderateCommentsTab.class);
         Intent tab2 = new Intent(this, viewPosts.class);
         Intent tab3 = new Intent(this, viewPosts.class);
         Intent tab4 = new Intent(this, viewStats.class);
         
        Bundle bundle = new Bundle();
 		bundle.putString("accountName", accountName);
 		bundle.putString("id", id);
 		
 		if (fromNotification){
 			bundle.putBoolean("fromNotification", true);
 		}
 		
 		tab1.putExtras(bundle);
 		tab4.putExtras(bundle);
 		
 		if (action != null){
 			bundle.putString("action", action);
 			bundle.putInt("uploadID", uploadID);
 		}
 		
 		tab2.putExtras(bundle);		
 		bundle.putBoolean("viewPages", true); 		
 		tab3.putExtras(bundle);
         
         TabHost host = getTabHost();  
         host.addTab(host.newTabSpec("one").setIndicator(getResources().getText(R.string.tab_comments), getResources().getDrawable(R.layout.comment_tab_selector)).setContent(tab1));  
         host.addTab(host.newTabSpec("two").setIndicator(getResources().getText(R.string.tab_posts), getResources().getDrawable(R.layout.posts_tab_selector)).setContent(tab2));
         host.addTab(host.newTabSpec("three").setIndicator(getResources().getText(R.string.tab_pages), getResources().getDrawable(R.layout.pages_tab_selector)).setContent(tab3));
         host.addTab(host.newTabSpec("four").setIndicator(getResources().getText(R.string.tab_stats), getResources().getDrawable(R.layout.stats_tab_selector)).setContent(tab4));
         if (activateTab != null){
        	 if(activateTab.equals("posts")){
        		 host.setCurrentTab(1);
        	 }
         }
         
         /*TabWidget tw = getTabWidget();
         tw.setStripEnabled(false);
         for (int i = 0; i < tw.getChildCount(); i++) { 
                     View v = tw.getChildAt(i); 
                     v.setBackgroundDrawable(getResources().getDrawable(R.drawable.tab_bg_selector)); 
                   } */
         
     }  
	     
	     @Override
	     public void onConfigurationChanged(Configuration newConfig) {
	       //ignore orientation change
	       super.onConfigurationChanged(newConfig);
	     } 
	     
	 	//Add settings to menu
	 	@Override
	 	public boolean onCreateOptionsMenu(Menu menu) {
	 		super.onCreateOptionsMenu(menu);
	 		menu.add(0, 0, 0, getResources().getText(R.string.blog_settings));
	 		MenuItem menuItem1 = menu.findItem(0);
	 		menuItem1.setIcon(R.drawable.ic_menu_prefs);
	 		menu.add(0, 1, 0, getResources().getText(R.string.remove_account));
	 		MenuItem menuItem2 = menu.findItem(1);
	 		menuItem2.setIcon(R.drawable.ic_menu_close_clear_cancel);
	 		return true;
	 	}
	 	
	 	//Menu actions
	 	@Override
	 	public boolean onOptionsItemSelected(final MenuItem item){
	 		switch (item.getItemId()) {
	 		case 0:

	 			Bundle bundle = new Bundle();
	 			bundle.putString("id", id);
	 			bundle.putString("accountName", accountName);
	 			Intent i = new Intent(this, settings.class);
	 			i.putExtras(bundle);
	 			startActivity(i);
	 			return true;
	 		case 1:
	 			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(tabView.this);
	 			dialogBuilder.setTitle(getResources().getText(R.string.remove_account));
	 			dialogBuilder.setMessage(getResources().getText(R.string.sure_to_remove_account));
	 			dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),  new
	 					DialogInterface.OnClickListener() {
	 				public void onClick(DialogInterface dialog, int whichButton) {
	 					// User clicked Accept so set that they've agreed to the eula.
	 					WordPressDB settingsDB = new WordPressDB(tabView.this);
	 					boolean deleteSuccess = settingsDB.deleteAccount(tabView.this, id);
	 					if (deleteSuccess)
	 					{
	 						Toast.makeText(tabView.this, getResources().getText(R.string.blog_removed_successfully),
	 								Toast.LENGTH_SHORT).show();
	 						finish();
	 					}
	 					else
	 					{
	 						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(tabView.this);
	 						dialogBuilder.setTitle(getResources().getText(R.string.error));
	 						dialogBuilder.setMessage(getResources().getText(R.string.could_not_remove_account));
	 						dialogBuilder.setPositiveButton("OK",  new
	 								DialogInterface.OnClickListener() {
	 							public void onClick(DialogInterface dialog, int whichButton) {
	 								// just close the dialog


	 							}
	 						});
	 						dialogBuilder.setCancelable(true);
	 						dialogBuilder.create().show();
	 					}

	 				}
	 			});
	 			dialogBuilder.setNegativeButton(getResources().getText(R.string.no), new
	 					DialogInterface.OnClickListener() {
	 				public void onClick(DialogInterface dialog, int whichButton) {
	 					//just close the window
	 				}
	 			});
	 			dialogBuilder.setCancelable(false);
	 			dialogBuilder.create().show();
	 			return true;
	 		}
	 		return false;	
	 	}
 }
