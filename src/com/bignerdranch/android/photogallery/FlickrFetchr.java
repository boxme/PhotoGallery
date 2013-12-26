package com.bignerdranch.android.photogallery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.net.Uri;
import android.util.Log;

public class FlickrFetchr {
	public static final String TAG = "FlickrFetchr";
	private static final String ENDPOINT = "http://api.flickr.com/services/rest/";
	private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
	private static final String PARAM_EXTRAS = "extras";								//Extra parameters
	private static final String EXTRA_SMALL_URL = "url_s";								//Include URL for the small version of the picture if possible
	private static final String API_KEY = "867afc6088c34a1cae2e31e3eb41cdb6";
	private static final String PAGE = "page";
	
	private static final String XML_PHOTO = "photo";									//Name of photo XML element
	
	/*
	 * Fetches raw data from URL and return as array of bytes
	 */
	public byte[] getUrlBytes(String urlSpec) throws IOException {
		URL url = new URL(urlSpec);													//URL obj from a string
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();	//Create connection obj to point at URL
		
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			InputStream in = connection.getInputStream();							//Connect to the endpoint (getOutputStream() for POST)
			
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) 
				return null;
			
			int byteRead = 0;
			byte[] buffer = new byte[1024];
			while ((byteRead = in.read(buffer)) > 0) {
				out.write(buffer, 0, byteRead);
			}
			out.close();
			return out.toByteArray();
		} finally {
			connection.disconnect();
		}
	}
	
	/*
	 * Converts the array of bytes into string
	 */
	public String getUrl(String urlSpec) throws IOException {
		return new String(getUrlBytes(urlSpec));
	}
	
	/*
	 * Fetch most recently uploaded Flickr photo using its API
	 */
	public ArrayList<GalleryItem> fetchItems(Integer page) {
		ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();
		
		try {
			String url = Uri.parse(ENDPOINT).buildUpon()						//Builds a valid URL to fetch its content
					.appendQueryParameter("method", METHOD_GET_RECENT)			//Auto escape query strings
					.appendQueryParameter("api_key", API_KEY)
					.appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
					.appendQueryParameter(PAGE, page.toString())
					.build().toString();
			String xmlString = getUrl(url);
			Log.i(TAG, "Received xml: " + xmlString);
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(new StringReader(xmlString));
			
			parseItem(items, parser);
		} catch (IOException ioe) {
			Log.e(TAG, "Failed to fetch item: ", ioe);
		} catch (XmlPullParserException xppe) {
			Log.e(TAG, "Failed to fetch item: ", xppe);
		}
		return items;
	}
	
	/*
	 * XmlPullParser to pull parse event off of a stream of XML
	 */
	public void parseItem(ArrayList<GalleryItem> items, XmlPullParser parser) 
					throws XmlPullParserException, IOException{
		int eventType = parser.next();
		
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG &&
				XML_PHOTO.equals(parser.getName())) {
				String id = parser.getAttributeValue(null, "id");
				String caption = parser.getAttributeValue(null, "title");
				String smallUrl = parser.getAttributeValue(null, EXTRA_SMALL_URL);
				
				GalleryItem item = new GalleryItem();							//Create an GalleryItem object
				item.setID(id);
				item.setCaption(caption);
				item.setUrl(smallUrl);
				items.add(item);
			}
			
			eventType = parser.next();
		}
	}
}
