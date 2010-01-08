//copyright Dan Roundhill, 11/10/2008
package org.wordpress.android;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;


public class splashScreen extends Activity {
    /** Called when the activity is first created. */
	
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        setContentView(R.layout.splashscreen);
        
        //Display the current version number
        PackageManager pm = getPackageManager();
        try {
			PackageInfo pi = pm.getPackageInfo("org.wordpress.android", 0);
			TextView versionNumber = (TextView) findViewById(R.id.versionNumber);           
            versionNumber.setText("Version " + pi.versionName);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        
        
        
        new Handler().postDelayed(new Runnable(){ 
            public void run() { 
                 /* Create an Intent that will start the Menu-Activity. */ 
                 Intent mainIntent = new Intent(splashScreen.this,wpAndroid.class); 
                 splashScreen.this.startActivity(mainIntent); 
                 splashScreen.this.finish(); 
            } 
       }, 3500);
        
}
}



