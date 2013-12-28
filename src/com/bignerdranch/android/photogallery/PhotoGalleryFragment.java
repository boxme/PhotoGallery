package com.bignerdranch.android.photogallery;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class PhotoGalleryFragment extends Fragment {
	private static final String TAG = "PhotoGalleryFragment";
	private GridView mGridView;
	private ArrayList<GalleryItem> mItems;
	private ThumbnailDownloader<ImageView> mThumbnailThread;
	
	private int current_page;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		current_page = 0;
		new FetchItemsTask().execute(++current_page);								//Fire up background thread
		
		mThumbnailThread = new ThumbnailDownloader<ImageView>(new Handler());		//Start customized background thread
		mThumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>() {//Handler is started here in the main thread
			@Override
			public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail) {	//Listener to set the returned bitmap
				if (isVisible()) {														//on the ImageView handle
					imageView.setImageBitmap(thumbnail);
				}
			}
		});
		mThumbnailThread.start();													//Ensure that the thread is ready before proceeding
		mThumbnailThread.getLooper();
		Log.i(TAG, "Background thread started");		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater,  ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
		mItems = new ArrayList<GalleryItem>();
		
		mGridView = (GridView) view.findViewById(R.id.gridView);
		mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {						//Infinite scroll listview
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				if (firstVisibleItem + visibleItemCount == totalItemCount &&					//When the scroll reaches the end
						totalItemCount > 0) {
					new FetchItemsTask().execute(++current_page);
				}
			}
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				//
			}
		});
		
		setupAdapter();		
		return view;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mThumbnailThread.quit();													//Shut down background thread
		Log.i(TAG, "Background thread destroyed");
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mThumbnailThread.clearQueue();												//Clean out when view is destroyed
	}
	
	public void setupAdapter() {
		if (getActivity() == null || mGridView == null) return;						//Ensure that this fragment is attached
		
		if (mItems != null) {														//Array is filled with PhotoGallery obj
			if (mGridView.getAdapter() == null) {
//				mGridView.setAdapter(new ArrayAdapter<GalleryItem>(					//GridView needs an adapter to feed it views to display
//						getActivity(), android.R.layout.simple_gallery_item, mItems));
				mGridView.setAdapter(new GalleryItemAdapter(mItems));
			} else {																			//Ensure the scroll bar doesnt reset
				((ArrayAdapter<GalleryItem>) mGridView.getAdapter()).notifyDataSetChanged();	//Not adding a new ArrayAdapter
			}																					//Just update it
		} else {
			mGridView.setAdapter(null);
		}
	}
	
	/*
	 * Inner class of subclass AsyncTask. Override AsyncTask.doInBackground(..)
	 * Utility class creates a background thread to call doInBackground()
	 */
	private class FetchItemsTask extends AsyncTask<Integer, Void, ArrayList<GalleryItem>> {
		@Override
		protected ArrayList<GalleryItem> doInBackground(Integer... params) {
			return new FlickrFetchr().fetchItems(params[0]);
		}
		
		//onPostExecute(..) is run after doInBackground(..) and it runs on the main thread
		@Override
		protected void onPostExecute(ArrayList<GalleryItem> items) {
			mItems.addAll(items);
			setupAdapter();										//Safe to update the UI
		}
	}
	
	/*
	 * Customize the ArrayAdapter whose getView() returns an ImageView
	 */
	private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {
		public GalleryItemAdapter(ArrayList<GalleryItem> items) {
			super(getActivity(), 0, items);
		}
		
		@Override
		public View getView(int pos, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater()
						.inflate(R.layout.gallery_item, parent, false);
			}
			
			ImageView imageView = (ImageView) convertView
						.findViewById(R.id.gallery_item_imageView);
			imageView.setImageResource(R.drawable.brian_up_close);
			
			GalleryItem item = getItem(pos);							//Retrieve the correct item with ArrayAdapter.getItem(..)	
			Bitmap bitmap = mThumbnailThread.checkCache(item.getUrl());
			
			if (bitmap != null) {
				imageView.setImageBitmap(bitmap);							//If image is already in cache
			} else {
				mThumbnailThread.queueThumbnail(imageView, item.getUrl());	//Pass the url and imageView
			}
			
			//Preload 10 images before and after the current image
			for (int i = Math.max(0, pos-10); i < Math.min(mItems.size()-1, pos+10); i++) {
				mThumbnailThread.queuePreload(mItems.get(i).getUrl());
			}
			
			return convertView;
		}
	}
}
