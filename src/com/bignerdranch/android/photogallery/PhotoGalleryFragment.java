package com.bignerdranch.android.photogallery;

import java.util.ArrayList;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;

public class PhotoGalleryFragment extends Fragment {
	private static final String TAG = "PhotoGalleryFragment";
	private GridView mGridView;
	private ArrayList<GalleryItem> mItems;
	
	private int current_page;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		current_page = 0;
		new FetchItemsTask().execute(++current_page);												//Fire up background thread
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
	
	public void setupAdapter() {
		if (getActivity() == null || mGridView == null) return;						//Ensure that this fragment is attached
		
		if (mItems != null) {														//Array is filled with PhotoGallery obj
			if (mGridView.getAdapter() == null) {
				mGridView.setAdapter(new ArrayAdapter<GalleryItem>(					//GridView needs an adapter to feed it views to display
						getActivity(), android.R.layout.simple_gallery_item, mItems));
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
//			try {
//				String result = new FlickrFetchr().getUrl("http://google.com");		//Get data from website
//				Log.i(TAG, "Fetched contents of URL " + result);
//			} catch (IOException e) {
//				Log.e(TAG, "Failed to fetch URL: " + e);
//			}
			return new FlickrFetchr().fetchItems(params[0]);
		}
		
		//onPostExecute(..) is run after doInBackground(..) and it runs on the main thread
		@Override
		protected void onPostExecute(ArrayList<GalleryItem> items) {
			mItems.addAll(items);
			setupAdapter();										//Safe to update the UI
		}
	}
}
