//Add WordPress image fields to ImageSpan object

package org.wordpress.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.style.ImageSpan; //import android.util.Log;

public class WPImageSpan extends ImageSpan {
	
		private Uri imageSource = null;

		private int width = 500;
		private String title = "";
		private String description = "";
		private String caption = "";
		private int horizontalAlignment = 0;
		private boolean isVideo;
		private boolean featured = false;
		private boolean featuredInPost = false;

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public int getHorizontalAlignment() {
			return horizontalAlignment;
		}

		public void setHorizontalAlignment(int horizontalAlignment) {
			this.horizontalAlignment = horizontalAlignment;
		}

		public void setImageSource(Uri imageSource) {
			this.imageSource = imageSource;
		}

		public WPImageSpan(Context context, Bitmap b, Uri src) {
		super(context, b);
		this.imageSource = src;
		// TODO Auto-generated constructor stub
	}

        public Uri getImageSource() {
                return imageSource;
        }

        public void setSrc(Uri src) {
                this.imageSource = src;
        }
        
        public int getWidth() {
			return width;
		}

		public void setWidth(int width) {
			this.width = width;
		}

		public String getCaption() {
			return caption;
		}

		public void setCaption(String caption) {
			this.caption = caption;
		}

		public boolean isFeatured() {
			return featured;
		}

		public void setFeatured(boolean featured) {
			this.featured = featured;
		}
		
		public boolean isFeaturedInPost() {
			return featuredInPost;
		}

		public void setFeaturedInPost(boolean featuredInPost) {
			this.featuredInPost = featuredInPost;
		}
		
		public boolean isVideo() {
			return isVideo;
		}

		public void setVideo(boolean isVideo) {
			this.isVideo = isVideo;
		}

}
