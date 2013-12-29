package com.bignerdranch.android.photogallery;

import android.app.SearchManager;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;


public class PhotoGalleryActivity extends SingleFragmentActivity {
	private static final String TAG = "PhotoGalleryActivity";

	@Override
	public Fragment getFragment() {
		return new PhotoGalleryFragment();
	}
	
	/*
	 * Receive the search intent and refresh PhotoGalleryFragment
	 */
	@Override
	public void onNewIntent(Intent intent) {
		PhotoGalleryFragment fragment = (PhotoGalleryFragment)
				getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
		
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			Log.i(TAG, "Received a new search query " + query);
			
			PreferenceManager.getDefaultSharedPreferences(this)					//Save the value for all in the app to access
							 .edit()											//Get an instance of SharedPreferences.Editor
							 .putString(FlickrFetchr.PREF_SEARCH_QUERY, query)	
							 .commit();											//Make the changes visible to other users of SharedPreferences file
		}
		
		fragment.updateItems(0);
	}
	
	@Override
	public boolean onSearchRequested() {
		String initialQuery = PreferenceManager.getDefaultSharedPreferences(this)			
				   .getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
		startSearch(initialQuery, true, null, false);
		return true;
	}
}
