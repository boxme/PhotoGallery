package com.bignerdranch.android.photogallery;

public class GalleryItem {
	private String mCaption;
	private String mID;
	private String mUrl;
	private String mOwner;
	
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
	
	public String getOwner() {
		return mOwner;
	}
	
	public void setOwner(String owner) {
		mOwner = owner;
	}
	
	public String getPhotoPageUrl() {
		return "http://www.flickr.com/photos/" + mOwner + "/" + mID;
	}
}
