package com.bignerdranch.android.photogallery;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.util.Log;

/*
 * A generic fragment that hides foreground notifications
 */
public class VisibleFragment extends Fragment {
	public static final String TAG = "VisibleFragment";
	
	private BroadcastReceiver mOnShowNotification = new BroadcastReceiver() {			//Dynamic receiver
		@Override
		public void onReceive(Context context, Intent intent) {							//It's called on the main thread
			//If we receive this, we're visible, so cancel the notification
			Log.i(TAG, "canceling notification");
			setResultCode(Activity.RESULT_CANCELED);
		}
	};
	
	@Override
	public void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter(PollService.ACTION_SHOW_NOTIFICATION);	//Express IntentFilter in code
		//Only this app is allowed to trigger the receiver with PollService.PERM_PRIVATE
		getActivity().registerReceiver(mOnShowNotification, filter, PollService.PERM_PRIVATE, null); //Register dynamic receiver
	}
	
	@Override
	public void onPause() {
		super.onPause();
		getActivity().unregisterReceiver(mOnShowNotification);							//Unregister dynamic receiver
	}
}
