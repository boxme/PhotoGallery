package com.bignerdranch.android.photogallery;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;

public class PhotoGalleryFragment extends Fragment {
	public static final String TAG = "PhotoGalleryFragment";
	private GridView mGridView;
	private ArrayList<GalleryItem> mItems;
	private ThumbnailDownloader<ImageView> mThumbnailThread;						//Background thread to download thumbnails
	
	private int current_page;
	private boolean isRefreshed;
	private String searchResultNum;
			
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		setHasOptionsMenu(true);
		
		current_page = 0;
		updateItems();
		
		mThumbnailThread = new ThumbnailDownloader<ImageView>(new Handler());		//Start customized background thread
		//Handler is started here in the main thread so it is automatically associated with this thread
		mThumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>() {
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
	
	public void updateItems() {
		new FetchItemsTask().execute(++current_page);								//Fire up background thread
	}
	
	public void updateItems(int reset) {
		current_page = reset;
		isRefreshed = true;
		mThumbnailThread.clearQueue();
		new FetchItemsTask().execute(++current_page);
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
	@TargetApi(11)
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_photo_gallery, menu);
		
		//Using Actionbar SearchView on newer devices
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			//Pull out the SearchView
			MenuItem searchItem = menu.findItem(R.id.menu_item_search);
			SearchView searchView = (SearchView) searchItem.getActionView();
			
			//Get the data from our searchable.xml as a Searchable Info
			SearchManager searchManager = (SearchManager) getActivity()
										.getSystemService(Context.SEARCH_SERVICE);
			ComponentName name = getActivity().getComponentName();
			SearchableInfo searchinInfo = searchManager.getSearchableInfo(name);		//Contains my search configuration
			
			searchView.setSearchableInfo(searchinInfo);
			String query = PreferenceManager.getDefaultSharedPreferences(getActivity())			
					   .getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
			if (query != null) {
				searchView.setQuery(query, false);
			}
		}
	}
	
	@Override
	@TargetApi(11)
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_search:
			getActivity().onSearchRequested();									//Starts search
			return true;
		case R.id.menu_item_clear:												//Clear search
			PreferenceManager.getDefaultSharedPreferences(getActivity())
							 .edit()
							 .putString(FlickrFetchr.PREF_SEARCH_QUERY, null)	//Clear saved item in PreferenceManager
							 .commit();
			setAdapterToNull();
			updateItems(0);
			return true;
		case R.id.menu_item_toggle_polling:
			boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
			PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
			
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) 
				getActivity().invalidateOptionsMenu();							//Inform post-3.0 devices to update actionbar 
				
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	/*
	 * Check whether the alarm is on, and then changes the text of menu_item_toggle_polling
	 * to show the appropriate feedback to the user
	 * This method is called every time the menu needs to be updated
	 */
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
		MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
		if (PollService.isServiceAlarmOn(getActivity())) {
			toggleItem.setTitle(R.string.stop_polling);
		} else {
			toggleItem.setTitle(R.string.start_polling);
		}
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
			setAdapterToNull();
		}
	}
	
	private void setAdapterToNull() {
		mGridView.setAdapter(null);
	}
	
	/*
	 * Inner class of subclass AsyncTask. Override AsyncTask.doInBackground(..)
	 * Utility class creates a background thread to call doInBackground(). 
	 * To get next page of download
	 */
	private class FetchItemsTask extends AsyncTask<Integer, Void, ArrayList<GalleryItem>> {
		@Override
		protected ArrayList<GalleryItem> doInBackground(Integer... params) {
			Activity activity = getActivity();
			if (activity == null)
				return new ArrayList<GalleryItem>();
			
			String query = PreferenceManager.getDefaultSharedPreferences(activity)			//Retrieve value stored previously
						   .getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
			if (query != null) {
				FlickrFetchr flickrFetchr = new FlickrFetchr();
				ArrayList<GalleryItem> arrayList = flickrFetchr.search(query, params[0]);
				searchResultNum = flickrFetchr.getNumResult();
				return arrayList;
			}
			return new FlickrFetchr().fetchItems(params[0]);
		}
		
		//onPostExecute(..) is run after doInBackground(..) and it runs on the main thread
		@Override
		protected void onPostExecute(ArrayList<GalleryItem> items) {
			if (mItems == null || isRefreshed) {
				mItems = items;
				isRefreshed = false;
				setAdapterToNull();
				setupAdapter();
				Toast.makeText(getActivity(), searchResultNum, Toast.LENGTH_SHORT).show();	//Toast to show number of results
			} else {
				mItems.addAll(items);
				setupAdapter();										//Safe to update the UI
			}
		}
	}
	
	/*
	 * Customize the ArrayAdapter whose getView() returns an ImageView
	 */
	private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {
		public GalleryItemAdapter(ArrayList<GalleryItem> items) {
			super(getActivity(), 0, items);
		}
		
		//AdapterView (GridView here) calls getView(..) on its adapter
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
				//Preload 10 images before and after the current image
				//Putting it here will prevent the preloading from hogging the background thread to preload
				for (int i = Math.max(0, pos-10); i < Math.min(mItems.size()-1, pos+10); i++) {
					mThumbnailThread.queuePreload(mItems.get(i).getUrl());
				}
			}
		
			return convertView;
		}
	}
}
