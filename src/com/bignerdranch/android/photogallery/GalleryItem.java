package com.bignerdranch.android.photogallery;

public class GalleryItem {
	private String mCaption;
	private String mID;
	private String mUrl;
	
	public void setID(String ID) {
		mID = ID;
	}
	
	public void setCaption(String caption) {
		mCaption = caption;
	}
	
	public void setUrl(String url) {
		mUrl = url;
	}
	
	public String toString() {
		return mCaption;
	}
	
	public String getID() {
		return mID;
	}
	
	public String getUrl() {
		return mUrl;
	}
}
