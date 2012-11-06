package com.danbowtell.justintimer;

import android.app.Notification;
import android.media.MediaPlayer;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class AlarmNotify extends BroadcastReceiver {
	NotificationManager nm;

	 @Override
	 public void onReceive(Context context, Intent intent) {
		 Uri path = null;
		 nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		 PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				 new Intent(context, JustInTimer.class), 0);
		 
		 CharSequence tickerText = intent.getStringExtra("Name");
		 CharSequence contentTitle = intent.getStringExtra("Name");
		 int id = intent.getIntExtra("Id",0);
		 CharSequence contentText = context.getString(R.string.finished);
		 Notification notification = new Notification(R.drawable.statusicon,
				 tickerText, System.currentTimeMillis());


		 notification.defaults |= Notification.DEFAULT_VIBRATE;
		 
		 if(intent.getStringExtra("RT") != null){
			 path = Uri.parse(intent.getStringExtra("RT"));
		 } else {
			 path = null;
		 }
		 
		 
		 TelephonyManager mTM = 
			 (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		 
		 // TODO Screen on notification
		 Intent screenIntent = new Intent(context, JustInTimer.class);
		 screenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		 PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
		 PowerManager.WakeLock wl = pm.newWakeLock(
				 PowerManager.ACQUIRE_CAUSES_WAKEUP
                 | PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                 | PowerManager.ON_AFTER_RELEASE,
                 "TAG");		
		 if (pm.isScreenOn() == false){
			 wl.acquire();
			 context.startActivity(screenIntent);
	//		 wl.release();
		 }
		 
		 // Play sound in ear piece if phone conversation 
		 // is happening otherwise play standard notification
		 if (mTM.getCallState() == TelephonyManager.CALL_STATE_IDLE){
			 /**
			  * Create repeating notification sound
			  */
			 
			if(path != null){
				 notification.sound = path;	 
			 }	else {
				 
			 }
			 
			 notification.flags |= Notification.FLAG_INSISTENT;
		 } else {
			 /**
			  * Set up media player so we can hear notification on call
			  */	 
			 try{
				 Uri beepPath = Uri.parse("android.resource://com.danbowtell.justintimer/" + R.raw.beep9);
				 MediaPlayer mMediaManager = MediaPlayer.create(context,beepPath);
				 mMediaManager.start();
			 } catch (IllegalStateException e){
				 Log.i("MediaPlayer","IllegalStateException - " + e);
			 }
		 }
		 
		 notification.flags |= Notification.FLAG_AUTO_CANCEL;
		 notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent); 
		 nm.notify(id, notification);
	 }
}
