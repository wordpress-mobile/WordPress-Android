package org.wordpress.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;



public class selectMedia extends Activity {
	String accountName, postID = "";
	int commentID = 0;
	public String SD_CARD_TEMP_DIR = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		setContentView(R.layout.select_media);
		setTitle(getResources().getText(R.string.add_media));
		
		Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
         accountName = extras.getString("accountName");  
         commentID = extras.getInt("commentID");
         postID = extras.getString("postID");
        } 
		
		final customButton selectPhoto = (customButton) findViewById(R.id.selectPhoto);
        final customButton takePhoto = (customButton) findViewById(R.id.takePhoto);
        
        takePhoto.setOnClickListener(new customButton.OnClickListener() {
            public void onClick(View v) {
            	
            	String state = android.os.Environment.getExternalStorageState();
                if(!state.equals(android.os.Environment.MEDIA_MOUNTED))  {
                    try {
						throw new IOException("SD Card is not mounted.  It is " + state + ".");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                }

            	
            	SD_CARD_TEMP_DIR = Environment.getExternalStorageDirectory() + File.separator + "wordpress" + File.separator + "wp-" + System.currentTimeMillis() + ".jpg";
            	Intent takePictureFromCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            	takePictureFromCameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new
            	                File(SD_CARD_TEMP_DIR)));
            	
            	// make sure the directory we plan to store the recording in exists
                File directory = new File(SD_CARD_TEMP_DIR).getParentFile();
                if (!directory.exists() && !directory.mkdirs()) {
                  try {
					throw new IOException("Path to file could not be created.");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                }

            	
            	startActivityForResult(takePictureFromCameraIntent, 0); 
            	
            	
            	
            }
        });   
        
        selectPhoto.setOnClickListener(new customButton.OnClickListener() {
            public void onClick(View v) {
            	
            	Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            	photoPickerIntent.setType("image/*");
            	
            	startActivityForResult(photoPickerIntent, 1);
            	
            }
        });
		
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null)
		{
        if (requestCode == 0) {
                if (resultCode == Activity.RESULT_OK) {

                        // http://code.google.com/p/android/issues/detail?id=1480

                        // on activity return
                        File f = new File(SD_CARD_TEMP_DIR);
                        try {
                            Uri capturedImage =
                                Uri.parse(android.provider.MediaStore.Images.Media.insertImage(getContentResolver(),
                                                f.getAbsolutePath(), null, null));


                                Log.i("camera", "Selected image: " + capturedImage.toString());

                            //f.delete();
                            
                            Bundle bundle = new Bundle();
                            
                            bundle.putString("imageURI", capturedImage.toString());
                            bundle.putInt("returnType", 0);
                            Intent mIntent = new Intent();
                            mIntent.putExtras(bundle);
                            setResult(RESULT_OK, mIntent);
                        } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        
                        
                        finish();


                }
                else {
                        Log.i("Camera", "Result code was " + resultCode);

                }
        }
        else{
        	Bundle bundle = new Bundle();
            bundle.putInt("returnType", 1);
            data.putExtras(bundle);
        	setResult(RESULT_OK, data);
        	finish();
        }
     }//end null check
     else{
        finish();	
     }
	} 
	

}
