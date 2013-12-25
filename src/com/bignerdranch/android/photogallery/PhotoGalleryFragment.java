package com.bignerdranch.android.photogallery;

import java.util.ArrayList;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;

public class PhotoGalleryFragment extends Fragment {
	private static final String TAG = "PhotoGalleryFragment";
	private GridView mGridView;
	private ArrayList<GalleryItem> mItems;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		new FetchItemsTask().execute();												//Fire up background thread
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater,  ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
		
		mGridView = (GridView) view.findViewById(R.id.gridView);
		
		setupAdapter();
		
		return view;
	}
	
	public void setupAdapter() {
		if (getActivity() == null || mGridView == null) return;
		
		if (mItems != null) {
			mGridView.setAdapter(new ArrayAdapter<GalleryItem>(						//GridView needs an adapter to feed it views to display
					getActivity(), android.R.layout.simple_gallery_item, mItems));
		} else {
			mGridView.setAdapter(null);
		}
	}
	
	/*
	 * Inner class of subclass AsyncTask. Override AsyncTask.doInBackground(..)
	 * Utility class creates a background thread to call doInBackground()
	 */
	private class FetchItemsTask extends AsyncTask<Void, Void, ArrayList<GalleryItem>> {
		@Override
		protected ArrayList<GalleryItem> doInBackground(Void... params) {
//			try {
//				String result = new FlickrFetchr().getUrl("http://google.com");		//Get data from website
//				Log.i(TAG, "Fetched contents of URL " + result);
//			} catch (IOException e) {
//				Log.e(TAG, "Failed to fetch URL: " + e);
//			}
			return new FlickrFetchr().fetchItems();
		}
		
		//onPostExecute(..) is run after doInBackground(..) and it runs on the main thread
		@Override
		protected void onPostExecute(ArrayList<GalleryItem> items) {
			mItems = items;
			setupAdapter();										//Safe to update the UI
		}
	}
}
