/*
 * Customised HandlerThread for a background thread
 */
package com.bignerdranch.android.photogallery;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;

public class ThumbnailDownloader<Token> extends HandlerThread {			//HandlerThread prepares a Looper
	private static final String TAG = "ThumbnailDownloader";
	private static final int MESSAGE_DOWNLOAD = 0;
	private static final int MESSAGE_PRELOAD = 1;
	
	private Handler mHandler;													//Handler
	private Handler mResponseHandler;											//Handler to hold a Handler passed from a main thread
	private Map<Token, String> requestMap = 									//Synchronized HashMap
			Collections.synchronizedMap(new HashMap<Token, String>());
	private Listener<Token> mListener;
	
	private static final int CACHE_SIZE = 400;
	private LruCache<String, Bitmap> mCache;
	
	/*
	 * Own implementation of Listener interface, to be implemented when
	 * an instance of ThumbnailDownloader is created. To do the UI work in main thread
	 */
	public interface Listener<Token> {
		void onThumbnailDownloaded(Token token, Bitmap thumbnail);
	}
	
	public void setListener(Listener<Token> listener) {
		mListener = listener;
	}
	
	public ThumbnailDownloader(Handler responseHandler) {
		super(TAG);
		mResponseHandler = responseHandler;								//Handler from main thread
		mCache = new LruCache<String, Bitmap>(CACHE_SIZE);
	}
	
	public Bitmap checkCache(String url) {
		if (url == null) return null;
		return mCache.get(url);
	}
	
	//Preload into the cache 
	public void queuePreload(String url) {
		if (url == null) return;
		if (mCache.get(url) != null) return;							//Already exists in cache
		
		mHandler.obtainMessage(MESSAGE_PRELOAD, url)
				.sendToTarget();
	}
	
	public void queueThumbnail(Token token, String url) {
//		Log.i(TAG, "Got an url " + url);
		requestMap.put(token, url);
		
		mHandler.obtainMessage(MESSAGE_DOWNLOAD, token)			//Recycling the use of Message objects
				.sendToTarget();								//Send the message to its handler
	}															//The handler will put the Message on the end of Looper's queue
	
	/*
	 * HandlerThread.onLooperPrepared() is called before the Looper checks the queue
	 * for the first time
	 */
	@SuppressLint("HandlerLeak")								//Handler will always be kept alive by its Looper
	@Override
	protected void onLooperPrepared() {
		mHandler = new Handler() {
			/*
			 * Override implementation of message handling by the handler
			 */
			@Override
			public void handleMessage(Message msg) {			//Message obj are handled here
				if (msg.what == MESSAGE_DOWNLOAD) {
					@SuppressWarnings("unchecked")
					Token token = (Token) msg.obj;				//Type erasure
					handleRequest(token);						//Download the image here
				} 
				else if (msg.what == MESSAGE_PRELOAD) {
					String url = (String) msg.obj;
					preload(url);
				}
			}
		};
	}
	
	private Bitmap getBitmap(String url) {
		try {
			if (url == null) return null;
			byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);		//DL bytes from URL and convert into a bitmap
			if (bitmapBytes == null) return null;
			final Bitmap bitmap = BitmapFactory
							  .decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
			return bitmap;
		} catch (IOException ioe) {
			Log.e(TAG, "Error downloading image", ioe);
		}
		return null;
	}
	
	private void preload(String url) {
		if (url == null) 	return;
		if (mCache.get(url) != null) 
			return;

		Bitmap bitmap = getBitmap(url);
		
		if (bitmap != null) 
			mCache.put(url, bitmap);
	}
		
	private void handleRequest(final Token token) {
		final String url = requestMap.get(token);
		if (url == null) 
			return;
		
		if (mCache.get(url) == null) 									//If image doesn't exist in cache, DL it
			preload(url);
		
		final Bitmap bitmap = mCache.get(url);
		mResponseHandler.post(new Runnable() {							//Handler.post(Runnable) sets the Message.callback
			@Override													//Runnable is called instead of the Message's Handler
			public void run() {
				if (requestMap.get(token) != url) 
					return;
				
				requestMap.remove(token);
				mListener.onThumbnailDownloaded(token, bitmap);
			}
		});
	}
	
	/*
	 * Clean all requests out of the queue
	 */
	public void clearQueue() {
		mHandler.removeMessages(MESSAGE_DOWNLOAD);
		requestMap.clear();
	}
}
