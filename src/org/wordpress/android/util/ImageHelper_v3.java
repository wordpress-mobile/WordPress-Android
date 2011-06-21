package org.wordpress.android.util;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

public class ImageHelper_v3 extends ImageHelper {

	public byte[] createThumbnail(byte[] bytes, String sMaxImageWidth, String orientation, boolean tiny) {
		//creates a thumbnail and returns the bytes
		
		int finalHeight = 0;
		BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
        
        int width = opts.outWidth;
        int height = opts.outHeight; 
        
        int finalWidth = 500;  //default to this if there's a problem
        //Change dimensions of thumbnail
        
        if (tiny){
        	finalWidth = 150;
        }
        
        byte[] finalBytes;
        
        if (sMaxImageWidth.equals("Original Size")){
        	if (bytes.length > 2000000) //it's a biggie! don't want out of memory crash
        	{
        		float finWidth = 1000;
        		int sample = 0;

        		float fWidth = width;
                sample= new Double(Math.ceil(fWidth / finWidth)).intValue();
                
        		if(sample == 3){
                    sample = 4;
        		}
        		else if(sample > 4 && sample < 8 ){
                    sample = 8;
        		}
        		
        		opts.inSampleSize = sample;
        		opts.inJustDecodeBounds = false;
        		
        		float percentage = (float) finalWidth / width;
        		float proportionateHeight = height * percentage;
        		finalHeight = (int) Math.rint(proportionateHeight);
        	
                bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();  
                bm.compress(Bitmap.CompressFormat.JPEG, 90, baos);
                
                bm.recycle(); //free up memory
                
                finalBytes = baos.toByteArray();
        	}
        	else
        	{
        	finalBytes = bytes;
        	} 
        	
        }
        else
        {
           	finalWidth = Integer.parseInt(sMaxImageWidth);
        	if (finalWidth > width){
        		//don't resize
        		finalBytes = bytes;
        	}
        	else
            {
            		int sample = 0;

            		float fWidth = width;
                    sample= new Double(Math.ceil(fWidth / 1200)).intValue();
                    
            		if(sample == 3){
                        sample = 4;
            		}
            		else if(sample > 4 && sample < 8 ){
                        sample = 8;
            		}
            		
            		opts.inSampleSize = sample;
            		opts.inJustDecodeBounds = false;
            		
                    bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
                    
                    float percentage = (float) finalWidth / bm.getWidth();
            		float proportionateHeight = bm.getHeight() * percentage;
            		finalHeight = (int) Math.rint(proportionateHeight);
            		
            		float scaleWidth = ((float) finalWidth) / bm.getWidth(); 
        	        float scaleHeight = ((float) finalHeight) / bm.getHeight(); 

                    
        	        float scaleBy = Math.min(scaleWidth, scaleHeight);
        	        
        	        // Create a matrix for the manipulation 
        	        Matrix matrix = new Matrix(); 
        	        // Resize the bitmap 
        	        matrix.postScale(scaleBy, scaleBy); 
        	        if ((orientation != null) && (orientation.equals("90") || orientation.equals("180") || orientation.equals("270"))){
        	        matrix.postRotate(Integer.valueOf(orientation));
        	        }

        	        Bitmap resized = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();  
                    resized.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                    
                    bm.recycle(); //free up memory
                    resized.recycle();
                    
                    finalBytes = baos.toByteArray();
            	}
		
	}
        
        return finalBytes;

}

	public String getExifOrientation(String path, String orientation) {
		
		return orientation;
	}
	
}
