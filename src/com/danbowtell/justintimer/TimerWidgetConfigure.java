package com.danbowtell.justintimer;

import java.util.ArrayList;
import android.app.ListActivity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class TimerWidgetConfigure extends ListActivity{
	static final String TAG="TimerWidgetConfigure";
	private static final String PREFS_NAME = "com.danbowtell.justintimer.JITPreferences";
	private static final String PREF_PREFIX_KEY = "prefix_";
	private static ListView lv;
			
	int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	EditText mAppWidgetPrefix;
	
	public TimerWidgetConfigure(){
		super();
	}
	
	@Override
	public void onCreate(Bundle icicle){
		super.onCreate(icicle);
			JustInTimerProvider mDbHelper = new JustInTimerProvider(this); 
		mDbHelper.open();
		ArrayList<Timer> timers = new ArrayList<Timer>();
		//Get list of all timers
		timers.addAll(mDbHelper.fetchListTimersForWidget());
		mDbHelper.close();
		// Set the result to cancelled. This will call this widget host to cancel
		// out of the widget placement if they press the back button.
		setResult(RESULT_CANCELED);
		
		// Set the view layout resource to use.
		setContentView(R.layout.timerwidget_configure);
		
		ListAdapter adapter = new TimerAdapter(this, R.layout.list_item, timers);
		setListAdapter(adapter);
		lv = getListView();
		lv.setClickable(true);
		lv.setOnItemClickListener(mOnClickListener);
		
		// Get widget id from bundle
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if(extras != null){
			mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
		}
	}
	
	AdapterView.OnItemClickListener mOnClickListener = new AdapterView.OnItemClickListener(){
		public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
			final Context context = TimerWidgetConfigure.this;
			Timer t = (Timer) lv.getItemAtPosition(position);
			
			saveTimerChoice(context,mAppWidgetId,t.getId());
			// Push widget update to surface
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			TimerWidget.updateTimerWidget(context,appWidgetManager,mAppWidgetId,t.getTimeString(),1,t.getId(),t.getTimerName());
			
			Intent resultValue = new Intent();
			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
			setResult(RESULT_OK,resultValue);
			
			//  Pass back necessary information to 
			finish();
		}
	};
	
	static void saveTimerChoice(Context context, int appWidgetId, int timerId){
		SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
		prefs.putLong(PREF_PREFIX_KEY + appWidgetId,timerId);
		prefs.commit();
	}
	
	private class TimerAdapter extends ArrayAdapter<Timer> {
    	
    	private ArrayList<Timer> items;
    	
    	/**
    	 * Constructor for List Adapter
    	 * @param context
    	 * @param textViewResourcesId
    	 * @param items List of timers
    	 */
    	public TimerAdapter(Context context, int textViewResourcesId, ArrayList<Timer> items){
    		super(context, textViewResourcesId, items);
    		this.items = items;
    	}
    	
    	@Override
    	public View getView(int position, View convertView, ViewGroup parent){
    		View v = convertView;
    		if(v == null) {
    			LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    			v = vi.inflate(R.layout.list_item, null);
    		}
    		Timer o = items.get(position);
    		if(o != null){
    			TextView name = (TextView) v.findViewById(R.id.name);
    			TextView time = (TextView) v.findViewById(R.id.time);
    			
    			if(name != null)
    				name.setText(o.getTimerName());
    			
    			if(time != null){
    				time.setText(new FormattedNumber().getDisplay(o.getTimerTime())); 
    			}    			
    		}
    		return v;
    	}
    }
 	 
	static long loadTimerPref(Context context, int appWidgetId){
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getLong(PREF_PREFIX_KEY + appWidgetId, 0);
	}
}