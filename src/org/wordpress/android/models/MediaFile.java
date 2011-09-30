package org.wordpress.android.models;

import org.wordpress.android.WordPressDB;

import android.content.Context;

public class MediaFile {
	
	protected int id;
	protected long postID;
	protected String filePath = null; //path of the file into disk
	protected String fileName = null; //name of the file into the server
	protected String title = null;
	protected String caption = null;
	protected String description = null;
	protected String fileURL = null;
	protected int horizontalAlignment; //0 = left, 1 = center, 2 = right 
	protected boolean verticalAligment = false; //false = bottom, true = top
	protected int width, height;
	protected String MIMEType = ""; //do not store this value
	protected String videoPressShortCode = null;
	protected boolean featured = false;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public boolean isFeatured() {
		return featured;
	}

	public void setFeatured(boolean featured) {
		this.featured = featured;
	}

	public long getPostID() {
		return postID;
	}

	public void setPostID(long postID) {
		this.postID = postID;
	}
	
	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getFileURL() {
		return fileURL;
	}

	public void setFileURL(String fileURL) {
		this.fileURL = fileURL;
	}

	public boolean isVerticalAlignmentOnTop() {
		return verticalAligment;
	}

	public void setVerticalAlignmentOnTop(boolean verticalAligment) {
		this.verticalAligment = verticalAligment;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getMIMEType() {
		return MIMEType;
	}

	public void setMIMEType(String type) {
		MIMEType = type;
	}

	public String getVideoPressShortCode() {
		return videoPressShortCode;
	}

	public void setVideoPressShortCode(String videoPressShortCode) {
		this.videoPressShortCode = videoPressShortCode;
	}
	
	public int getHorizontalAlignment() {
		return horizontalAlignment;
	}

	public void setHorizontalAlignment(int horizontalAlignment) {
		this.horizontalAlignment = horizontalAlignment;
	}
	
	public void save(Context ctx) {
		WordPressDB wpDB = new WordPressDB(ctx);
		wpDB.saveMediaFile(ctx, this);
	}
	
}

