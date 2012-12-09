package com.danbowtell.justintimer;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;

public class Timer
{
	String am = Context.ALARM_SERVICE;
	private Timer timerT;
	TextView t;
	private Handler mHandler = new Handler();
	private long startTime;

	private static TimerAdapter t_adapter;
	private int id;
	private String name;
	private long length;
	private long timerEnd;
	private long remaining;
	private long start;
	private boolean running;
	private boolean paused;
	private View v;
	private boolean customTone;
	private String customToneUri;
	private int last_state;
	private boolean displayCurrentName;

	public Timer(){}

	public Timer(int id, String name, long time)
    {
		this.id = id;
		this.name = name;
		this.length = time;
	}

	public int getId()
    {
		return id;
	}

	public void setId(int id)
    {
		this.id = id;
	}

	public String getTimerName()
    {
		return this.name;
	}

	public void setTimerName(String timerName)
    {
		this.name = timerName;
		this.displayCurrentName = false;
	}

	public Long getTimerTime()
    {
		return this.length;
	}

	public void setTimerTime(long timerTime)
    {
		this.length = timerTime;
	}

	public Long getEnd()
    {
		return this.timerEnd;
	}

	public void setEnd(Long end)
    {
		this.timerEnd = end;
	}

	public boolean getRunning()
    {
		return this.running;
	}

	public boolean isRunning()
    {
		return this.running && this.timerEnd > System.currentTimeMillis() ?
            true : false;
	}

	public boolean isEnded()
    {
		return (this.running && this.timerEnd < System.currentTimeMillis()) ?
            true : false;
	}

	public void setRunning(boolean running)
    {
		this.running = running;
	}

	public boolean getPaused()
    {
		return this.paused;
	}

	public void pause(Context context)
    {
		this.remaining=this.timerEnd - System.currentTimeMillis();
		this.running = false;
		this.paused = true;
		this.start = 0;
		this.cancelAlarm(context);
		this.cancelTimerHandle();

		updateWidget(context);
	}

	public void widgetStart(Context context)
    {
		if(this.start == 0)
        {
			this.start = System.currentTimeMillis();
		}

		long finish = this.remaining > 0 ? this.remaining + this.start :
            this.length + this.start;

		// Get default path
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

		String path = (pref.getBoolean("default_tone",false)) ?
				pref.getString("ringtone_preference",
                              "android.resource://com.danbowtell.justintimer/"
                                + R.raw.alarm_951)
				: "android.resource://com.danbowtell.justintimer/"
                    + R.raw.alarm_951;

		Intent intent = new Intent(context, AlarmNotify.class);
		intent.putExtra("Name",this.name);
		String pathb = (this.isCustomTone() == true ) ? this.customToneUri :
            path;
		intent.putExtra("RT",pathb);
		intent.putExtra("Id",this.id);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
            this.id,
            intent,
            PendingIntent.FLAG_ONE_SHOT);
		AlarmManager xAlarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		xAlarmManager.set(AlarmManager.RTC_WAKEUP, finish, pendingIntent);
	}

	public void start(Context context)
    {
		//this.start = System.currentTimeMillis();
		if(this.start == 0)
        {
			this.start = System.currentTimeMillis();
		}

		long finish = this.remaining > 0 ? this.remaining + this.start
            : this.length + this.start;

		// Get default path
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

		String path = (pref.getBoolean("default_tone",false)) ?
				pref.getString("ringtone_preference","android.resource://com.danbowtell.justintimer/" + R.raw.alarm_951)
				: "android.resource://com.danbowtell.justintimer/" + R.raw.alarm_951;

		this.remaining = 0;
		this.timerEnd = finish;
		this.running  = true;
		this.paused = false;
		Intent intent = new Intent(context, AlarmNotify.class);
		intent.putExtra("Name",this.name);
		// TODO edit path
		// Get timer specific path
		this.startTimerHandle(finish);

		String pathb = (this.isCustomTone() == true ) ? this.customToneUri : path;
		intent.putExtra("RT",pathb);
		intent.putExtra("Id",this.id);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, this.id, intent, PendingIntent.FLAG_ONE_SHOT);
		AlarmManager xAlarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		xAlarmManager.set(AlarmManager.RTC_WAKEUP, finish, pendingIntent);

		updateWidget(context);
	}

	public void startTimer(Context context)
    {
		if(this.isRunning())
        {
			long finish = this.remaining > 0 ? this.remaining + this.start
                : this.length + this.start;
			this.remaining = 0;
			this.startTimerHandle(finish);
		}
	}

	public void stopTimer()
    {
		this.cancelTimerHandle();
	}

	public void reset(Context context)
    {
		this.cancelTimerHandle();
		if(this.running)
        {
			this.cancelAlarm(context);
			this.cancelNotification(context);
		}
		this.running = false;
		this.paused = false;
		this.timerEnd = 0;
		this.start = 0;
		this.remaining = 0;
		updateWidget(context);
	}

	public void setPaused(boolean paused)
    {
		this.paused = paused;
		this.cancelTimerHandle();
	}

	public Long getRemaining()
    {
		if(this.paused)
        {
			return this.remaining;
		}
        else if(this.running)
        {
			return this.timerEnd - System.currentTimeMillis();
		}
        else
        {
			return this.length;
		}
	}

	public Long getStorageRemaining()
    {
		if(this.paused)
        {
			return this.remaining;
		}
        else
        {
			return (long) 0;
		}
	}

	public void setRemaining(Long remaining)
    {
		this.remaining = remaining;
	}

	public View getView()
    {
		return this.v;
	}

	public void setView(View v)
    {
		this.v = v;
	}

	public boolean isCustomTone()
    {
		return customTone;
	}

	public void setCustomTone(boolean customTone)
    {
		this.customTone = customTone;
	}

	public String getCustomToneUri()
    {
		return customToneUri;
	}

	public void setCustomToneUri(String customeToneUri)
    {
		this.customToneUri = customeToneUri;
	}

	/**
	 * Cancel any pending alarms
	 * @param context
	 * @param id
	 */
	private void cancelAlarm(Context context)
    {
		PendingIntent alarmIntent = PendingIntent.getBroadcast(context,this.getId(),new Intent(context,AlarmNotify.class),PendingIntent.FLAG_ONE_SHOT);
		AlarmManager xAlarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		xAlarmManager.cancel(alarmIntent);
		alarmIntent.cancel();
	}

	public String getTimeString()
    {
		FormattedNumber fn = new FormattedNumber();
		return fn.getDisplay(this.length);
	}

	/**
	 * Cancel the notification	 *
	 * @param context
	 * @param id
	 */
	// Cancel notification
	private void cancelNotification(Context context)
    {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
		mNotificationManager.cancel(this.id);
	}

	public void delete(Context context)
    {
		this.cancelAlarm(context);
		this.cancelTimerHandle();
	}

	// Make sure widget timers are running
	private void updateWidget(Context context)
    {
		Intent updateIntent = new Intent("com.danbowtell.justintimer.TIMER_WIDGET_UPDATE");
		PendingIntent widgetIntent = PendingIntent.getBroadcast(context,0,updateIntent,PendingIntent.FLAG_ONE_SHOT);
		AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, widgetIntent);
	}

	static void setAdapter(TimerAdapter adapter)
    {
		Timer.t_adapter = adapter;
	}

	private Runnable updateView = new Runnable ()
    {
		public void run()
        {
			final long start = startTime;
			long millis = SystemClock.uptimeMillis() - startTime;
			int seconds = (int) millis / 1000;
			t_adapter.notifyDataSetChanged();

			mHandler.removeCallbacks(updateView);
			if(timerT.isRunning())
            {
				mHandler.postAtTime(this,start + (seconds + 1) * 1000);
			}
			else
            {
				mHandler.removeCallbacks(updateView);
				t_adapter.notifyDataSetChanged();
			}
		}
	};

	public void startTimerHandle (long millisinFuture)
    {
		timerT = this;
		startTime = SystemClock.uptimeMillis();
		mHandler.removeCallbacks(updateView);
		mHandler.postDelayed(updateView,100);
	}

	public void cancelTimerHandle()
    {
		mHandler.removeCallbacks(updateView);
	}

	public long getStart()
    {
		return this.start;
	}

	public void setStart(long start)
    {
		this.start = start;
	}

	public int getLastState()
    {
		return last_state;
	}

	public void setLastState(int state)
    {
		this.last_state = state;
	}

	public void setDisplayCurrentName(boolean value)
    {
		this.displayCurrentName = value;
	}

	public boolean isDisplayCurrentName()
    {
		return this.displayCurrentName;
	}
}
