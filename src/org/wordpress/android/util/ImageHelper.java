package org.wordpress.android.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

public class ImageHelper{

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
		//get image EXIF orientation if Android 2.0 or higher, using reflection
		//http://developer.android.com/resources/articles/backward-compatibility.html
		Method exif_getAttribute;
		Constructor<ExifInterface> exif_construct;
		String exifOrientation = "";
		
		int sdk_int = 0;
		try {
			sdk_int = Integer.valueOf(android.os.Build.VERSION.SDK);
		} catch (Exception e1) {
			sdk_int = 3; //assume they are on cupcake
		}
		if (sdk_int >= 5){
			try {
		           exif_construct = android.media.ExifInterface.class.getConstructor(new Class[] { String.class } );
		           Object exif = exif_construct.newInstance(path);
		           exif_getAttribute = android.media.ExifInterface.class.getMethod("getAttribute", new Class[] { String.class } );
		           try {
		        	   exifOrientation = (String) exif_getAttribute.invoke(exif, android.media.ExifInterface.TAG_ORIENTATION);	
		        	   if (exifOrientation != null){
			        	    if (exifOrientation.equals("1")){
								orientation = "0";
							}
							else if (exifOrientation.equals("3")){
								orientation = "180";
							}
							else if (exifOrientation.equals("6")){
								orientation = "90";
							}
							else if (exifOrientation.equals("8")){
								orientation = "270";
							}
		        	   }
		        	   else{
		        		   orientation = "0";
		        	   }
		           } catch (InvocationTargetException ite) {
		               /* unpack original exception when possible */
		               orientation = "0";
		           } catch (IllegalAccessException ie) {
		               System.err.println("unexpected " + ie);
		               orientation = "0";
		           }
		           /* success, this is a newer device */
		       } catch (NoSuchMethodException nsme) {
		    	   orientation = "0";
		       } catch (IllegalArgumentException e) {
		    	   orientation = "0";
			} catch (InstantiationException e) {
				orientation = "0";
			} catch (IllegalAccessException e) {
				orientation = "0";
			} catch (InvocationTargetException e) {
				orientation = "0";
			}


		}
		return orientation;
	}
	
	//borrowed from http://android-developers.blogspot.com/2010/07/multithreading-for-performance.html
	class BitmapDownloaderTask extends AsyncTask<String, Void, Bitmap> {
	    private String url;
	    private final WeakReference<ImageView> imageViewReference;

	    public BitmapDownloaderTask(ImageView imageView) {
	        imageViewReference = new WeakReference<ImageView>(imageView);
	    }

	    @Override
	    // Actual download method, run in the task thread
	    protected Bitmap doInBackground(String... params) {
	         // params comes from the execute() call: params[0] is the url.
	         return downloadBitmap(params[0]);
	    }

	    @Override
	    // Once the image is downloaded, associates it to the imageView
	    protected void onPostExecute(Bitmap bitmap) {
	        if (isCancelled()) {
	            bitmap = null;
	        }

	        if (imageViewReference != null) {
	            ImageView imageView = imageViewReference.get();
	            if (imageView != null && bitmap != null) {
	                imageView.setImageBitmap(bitmap);
	            }
	        }
	    }
	}
	
	static Bitmap downloadBitmap(String url) {
	    final AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
	    final HttpGet getRequest = new HttpGet(url);

	    try {
	        HttpResponse response = client.execute(getRequest);
	        final int statusCode = response.getStatusLine().getStatusCode();
	        if (statusCode != HttpStatus.SC_OK) { 
	            Log.w("ImageDownloader", "Error " + statusCode + " while retrieving bitmap from " + url); 
	            return null;
	        }
	        
	        final HttpEntity entity = response.getEntity();
	        if (entity != null) {
	            InputStream inputStream = null;
	            try {
	                inputStream = entity.getContent(); 
	                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
	    			BitmapDrawable bd = new BitmapDrawable(bitmap);
	    			bitmap = com.commonsware.cwac.thumbnail.ThumbnailAdapter.getRoundedCornerBitmap(bd.getBitmap());
	                return bitmap;
	            } finally {
	                if (inputStream != null) {
	                    inputStream.close();  
	                }
	                entity.consumeContent();
	            }
	        }
	    } catch (Exception e) {
	        // Could provide a more explicit error message for IOException or IllegalStateException
	        getRequest.abort();
	        Log.w("ImageDownloader", "Error while retrieving bitmap from " + url);
	    } finally {
	        if (client != null) {
	            client.close();
	        }
	    }
	    return null;
	}
	
}
