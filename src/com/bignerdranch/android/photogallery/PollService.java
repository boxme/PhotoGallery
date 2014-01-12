/*
 * A service that will poll for search results
 */
package com.bignerdranch.android.photogallery;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class PollService extends IntentService {
	private static final String TAG = "PollService";
	private static final int POLL_INTERVAL = 1000*15; 						//15 sec
	public static final String PREF_IS_ALARM_ON = "isAlarmOn";
	public static final String ACTION_SHOW_NOTIFICATION = "com.bignerdranch.android.photogallery.SHOW_NOTIFICATION";
	public static final String PERM_PRIVATE = "com.bignerdranch.android.photogallery.PRIVATE";
	
	public PollService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		ConnectivityManager cm = (ConnectivityManager) 
				getSystemService(Context.CONNECTIVITY_SERVICE);
		
		@SuppressWarnings("deprecation")
		boolean isNetworkAvailable = (cm.getBackgroundDataSetting() &&		//Verify with CM that the network is available
					cm.getActiveNetworkInfo() != null);
		
		if (!isNetworkAvailable) return;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String query = prefs.getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
		String lastResultId = prefs.getString(FlickrFetchr.PREF_LAST_RESULT_ID, null);
		
		ArrayList<GalleryItem> items;
		if (query != null) {
			items = new FlickrFetchr().search(query, 1);
		} else {
			items = new FlickrFetchr().fetchItems(1);
		}
		
		if (items.size() == 0) 
			return;
		
		String resultId = items.get(0).getID();
		
		if (!resultId.equals(lastResultId)) {
			Log.i(TAG, "Got a new result " + resultId);
			
			//Notify the user that a new result is ready 
			Resources res = getResources();
			PendingIntent pi = PendingIntent
					.getActivity(this, 0, new Intent(this, PhotoGalleryActivity.class), 0);
					
			Notification notification = new NotificationCompat.Builder(this)
						.setTicker(res.getString(R.string.new_picture_title))				//Configure ticker text
						.setSmallIcon(android.R.drawable.ic_menu_report_image)				//Configure small icon
						.setContentTitle(res.getString(R.string.new_picture_title))			//Configure the appearance
						.setContentText(res.getString(R.string.new_picture_title))
						.setContentIntent(pi)	//pi will be fired when user presses this notification in the drawer
						.setAutoCancel(true)	//Notification will be deleted from the drawer when the user presses it
						.build();
			
//			NotificationManager notificationManager = (NotificationManager)
//					getSystemService(NOTIFICATION_SERVICE);
//			
//			notificationManager.notify(0, notification);								//The ID should be unqiue across the app
//			sendBroadcast(new Intent(ACTION_SHOW_NOTIFICATION), PERM_PRIVATE);			//Send a broadcast intent set in private
			showBackgroundNotification(0, notification);
		} else {
			Log.i(TAG, "got an old result: " + resultId);
		}
		
		prefs.edit().putString(FlickrFetchr.PREF_LAST_RESULT_ID, resultId).commit();
	}

	/*
	 * Turns off/on an alarm
	 * What is pendingIntent: http://stackoverflow.com/questions/2808796/what-is-pending-intent
	 */
	public static void setServiceAlarm(Context context, boolean isOn) {
		Intent intent = new Intent(context, PollService.class);
		PendingIntent pi = PendingIntent.getService(context, 0, intent, 0);		//PendingIntent to start PollService
		
		AlarmManager alarmManager = (AlarmManager) context						//AlarmManager is a system service that can send Intents
								.getSystemService(Context.ALARM_SERVICE);
		
		if (isOn) {																//Set alarm
			Log.i(TAG, "Start Alarm");
			alarmManager.setRepeating(AlarmManager.RTC, 
					System.currentTimeMillis(), POLL_INTERVAL, pi);
		} else {																//Cancel alarm
			Log.i(TAG, "Cancel Alarm");
			alarmManager.cancel(pi);
			pi.cancel();														//Cancel PendingIntent
		}
		
		PreferenceManager.getDefaultSharedPreferences(context)
						 .edit()
						 .putBoolean(PREF_IS_ALARM_ON, isOn)					//Save the state of the alarm
						 .commit();
	}
	
	/*
	 * Return true if pendingIntent already exists. If pendingIntent already exists, 
	 * alarm is active and returns true
	 */
	public static boolean isServiceAlarmOn(Context context) {
		Intent intent = new Intent(context, PollService.class);
		PendingIntent pi = PendingIntent
					.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE);	//Check that pendingIntent doesnt already exist
		return pi != null;
	}
	
	/*
	 * Send an ordered broadcast
	 */
	public void showBackgroundNotification(int requestCode, Notification notification) {
		Intent intent = new Intent(ACTION_SHOW_NOTIFICATION);
		intent.putExtra("REQUEST_CODE", requestCode);
		intent.putExtra("NOTIFICATION", notification);
		
		sendOrderedBroadcast(intent, PERM_PRIVATE, null, null, Activity.RESULT_OK, null, null);
	}
}
