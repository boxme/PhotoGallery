package com.bignerdranch.android.photogallery;

import android.support.v4.app.Fragment;


public class PhotoGalleryActivity extends SingleFragmentActivity {

	@Override
	public Fragment getFragment() {
		return new PhotoGalleryFragment();
	}
}
