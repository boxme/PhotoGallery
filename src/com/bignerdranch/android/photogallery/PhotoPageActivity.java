package com.bignerdranch.android.photogallery;

import android.support.v4.app.Fragment;

public class PhotoPageActivity extends SingleFragmentActivity {

	@Override
	protected Fragment getFragment() {
		return new PhotoPageFragment();
	}
}
