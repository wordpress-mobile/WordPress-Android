package org.wordpress.android.models;

public class MediaFile {
	protected String filePath = null; //path of the file into disk
	protected String fileName = null; //name of the file into the server
	protected String title = null;
	protected String caption = null;
	protected String description = null;
	protected String fileURL = null;
	protected boolean verticalAligment = false; //false = bottom, true = top
	protected int width, height;
	protected String MIMEType = ""; //do not store this value
	protected String videoPressShortCode = null;
	
	
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
	
}

