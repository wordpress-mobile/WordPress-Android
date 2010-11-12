package com.commonsware.cwac.thumbnail;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;

import com.commonsware.cwac.adapter.AdapterWrapper;
import com.commonsware.cwac.cache.SimpleWebImageCache;

public class ThumbnailAdapter extends AdapterWrapper {
	private static final String TAG="ThumbnailAdapter";
	private int[] imageIds;
	private SimpleWebImageCache<ThumbnailBus, ThumbnailMessage> cache=null;
	private Activity host=null;
	Bitmap bdRounded;

	/**
		* Constructor wrapping a supplied ListAdapter
    */
	public ThumbnailAdapter(Activity host,
													ListAdapter wrapped,
													SimpleWebImageCache<ThumbnailBus, ThumbnailMessage> cache,
													int[] imageIds) {
		super(wrapped);
		
		this.host=host;
		this.imageIds=imageIds;
		this.cache=cache;
		
		cache.getBus().register(getBusKey(), onCache);
	}
	
	public void close() {
		cache.getBus().unregister(onCache);
	}

	/**
		* Get a View that displays the data at the specified
		* position in the data set. In this case, if we are at
		* the end of the list and we are still in append mode,
		* we ask for a pending view and return it, plus kick
		* off the background task to append more data to the
		* wrapped adapter.
		* @param position Position of the item whose data we want
		* @param convertView View to recycle, if not null
		* @param parent ViewGroup containing the returned View
    */
	@Override
	public View getView(int position, View convertView,
											ViewGroup parent) {
		View result=super.getView(position, convertView, parent);
		
		processView(result);
		
		return(result);
	}
	
	public void processView(View row) {
		for (int imageId : imageIds) {
			ImageView image=(ImageView)row.findViewById(imageId);
			
			if (image!=null && image.getTag()!=null) {
				ThumbnailMessage msg=cache
																.getBus()
																.createMessage(getBusKey());
																
				msg.setImageView(image);
				msg.setUrl(image.getTag().toString());
				
				try {
					cache.notify(msg.getUrl(), msg);
				}
				catch (Throwable t) {
					Log.e(TAG, "Exception trying to fetch image", t);
				}
			}
		}
	}
	
	private String getBusKey() {
		return(toString());
	}
	
	private ThumbnailBus.Receiver<ThumbnailMessage> onCache=
		new ThumbnailBus.Receiver<ThumbnailMessage>() {
		public void onReceive(final ThumbnailMessage message) {
			final ImageView image=message.getImageView();
			
			host.runOnUiThread(new Runnable() {
				public void run() {
					if (image.getTag()!=null &&
							image.getTag().toString().equals(message.getUrl())) {
						
							BitmapDrawable bd = (BitmapDrawable) cache.get(message.getUrl());
							bdRounded = getRoundedCornerBitmap(bd.getBitmap());
							if (bdRounded != null){
								image.setImageBitmap(bdRounded);
							}
							else{
								image.setImageDrawable(cache.get(message.getUrl()));
							}
					}
				}
			});
		}
	};
	
	public static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
		
		if (bitmap != null)
		{
	    Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),bitmap.getHeight(), Config.ARGB_8888);
	    Canvas canvas = new Canvas(output);
	 
	    final int color = 0xff424242;
	    final Paint paint = new Paint();
	    final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
	    final RectF rectF = new RectF(rect);
	    final float roundPx = 8;
	 
	    paint.setAntiAlias(true);
	    canvas.drawARGB(0, 0, 0, 0);
	    paint.setColor(color);
	    canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
	 
	    paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
	    canvas.drawBitmap(bitmap, rect, rect, paint);
	 
	    return output;
		}
		else 
		{
			return bitmap;
		}
	  }
}