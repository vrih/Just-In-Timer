package com.danbowtell.justintimer;

import java.util.ArrayList;

import android.app.AlarmManager;
import android.app.Activity;
//import android.app.ListActivity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import android.util.DisplayMetrics; // get screen size

import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.GridView;

import android.widget.RelativeLayout;
import android.widget.TextView;

import com.danbowtell.justintimer.Timer;

public class JustInTimer extends Activity
{
    /** Called when the activity is first created. */
    private TimerAdapter t_adapter;
    private JustInTimerProvider mDbHelper;
    private GestureDetector gestureDetector;
    private OnTouchListener mGestureListener;
    private ListView lv;
    private GridView gv;
    private View sv;

    private TextView tvB;

    private int scrollDistance;
    private boolean mIsScrolling = false;
    private boolean mVScroll = false;
    private int position;
    //private final String DEBUG = "JustInTimer";
    private final static int EDIT_DONE = 4;
    private final static int NEW_ADDED = 5;

    //  SDK Version
    private int sdkVersion;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    //  Debug.startMethodTracing();
        this.sdkVersion = android.os.Build.VERSION.SDK_INT;

    // Remove title bar on earlier versions of Android
        if(sdkVersion < 11)
        {
	        requestWindowFeature(getWindow().FEATURE_NO_TITLE);
        }

        setContentView(R.layout.main);

        mDbHelper = new JustInTimerProvider(this);

        // Set up display metrics
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

	final float scale = dm.density;

	final float scaled_width = dm.widthPixels / (scale);
	final float scaled_height = dm.heightPixels / (scale);

	// Code for phones
        if(scaled_width < 600 ||
           scaled_height < 600 ||
           this.sdkVersion < 11)
        {
	    // set up listview gesture detector
            gestureDetector = new GestureDetector(new MyGestureDetector());
            mGestureListener = new View.OnTouchListener()
            {
                public boolean onTouch(View v, MotionEvent event)
                {
                    if (gestureDetector.onTouchEvent(event)) return true;

                    if(event.getAction() == MotionEvent.ACTION_UP)
                    {
                        if(mIsScrolling || mVScroll)
                        {
                            mIsScrolling  = false;
                            //mVscroll - false;
                            handleScrollFinished(event);
                        };
                    }
                    return false;
                }
            };

            // Create view and add to add to list footer
            final View vbuf = getLayoutInflater().inflate(R.layout.list_footer, null);
            final RelativeLayout rlfooter = (RelativeLayout) vbuf.findViewById(R.id.list_footer);
            rlfooter.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View arg0)
                {
                    Intent launchNewTimerIntent = new Intent().setClass(getApplicationContext(),ItemDetailsActivity.class);
                    startActivityForResult(launchNewTimerIntent, NEW_ADDED);
                }
            });

            lv = (ListView) findViewById(R.id.list);
            // Only add foorter where there is no actionbar
            // Add footer on earlier versions of android
            if(this.sdkVersion < 11)
            {
                lv.addFooterView(rlfooter);
            }

            this.t_adapter = new TimerAdapter(this,
                                              R.layout.list_item,
                                              new ArrayList<Timer>());
            lv.setAdapter(this.t_adapter);
            Timer.setAdapter(this.t_adapter);
            registerForContextMenu(lv);

            lv.setOnTouchListener(mGestureListener);
        }
        else
        {
            // Tabete layout (don't deal with below sdk 11)
            // set up listview gesture detector
            gestureDetector = new GestureDetector(new GridGestureDetector());
            mGestureListener = new View.OnTouchListener()
            {
                public boolean onTouch(View v, MotionEvent event)
                {
                    if (gestureDetector.onTouchEvent(event)) return true;

                    if(event.getAction() == MotionEvent.ACTION_UP)
                    {
                        if(mIsScrolling || mVScroll)
                        {
                            mIsScrolling  = false;
                            //mVscroll - false;
                            handleScrollFinished(event);
                        };
                    }
                    return false;
                }
            };

            // Create view and add to add to list footer
            gv = (GridView) findViewById(R.id.grid);

            this.t_adapter = new TimerAdapter(this,
                                              R.layout.list_item,
                                              new ArrayList<Timer>());
            gv.setAdapter(this.t_adapter);
            Timer.setAdapter(this.t_adapter);
            registerForContextMenu(gv);

            gv.setOnTouchListener(mGestureListener);
        }
    }

    // Reload all timers when app resumes
    @Override
    public void onResume()
    {
        super.onResume();
        t_adapter.clear();
        getTimers();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        // Commit timer changes to database;
        t_adapter.holdTimers();
        ArrayList<Timer> t = t_adapter.getAllTimers();
        mDbHelper.open();
        mDbHelper.updateAllTimers(t);
        mDbHelper.close();
        final Context context = getApplicationContext();
        final Intent updateIntent = new Intent("com.danbowtell.justintimer.TIMER_WIDGET_UPDATE");
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(context,0,updateIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        final AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, pendingIntent);
        t = null;
        //Debug.stopMethodTracing();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
        switch(item.getItemId())
        {
            case R.id.delete_timer:
                removeTimer(getApplicationContext(), info.id);
                return true;
            case R.id.reset_timer:
                resetTimer(getApplicationContext(), info.id);
                return true;
            case R.id.edit_timer:
                final Timer t_timer = t_adapter.getItem((int) info.id);
                Intent launchEditTimerIntent = new Intent().setClass(getApplicationContext(), ItemDetailsActivity.class);
                launchEditTimerIntent.putExtra("id",t_timer.getId());
                startActivityForResult(launchEditTimerIntent, EDIT_DONE);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
        switch (item.getItemId())
        {
            case R.id.new_timer:
                Intent launchNewTimerIntent = new Intent().setClass(getApplicationContext(), ItemDetailsActivity.class);
                startActivityForResult(launchNewTimerIntent, NEW_ADDED);
                return true;
            case R.id.menu_preferences:
                Intent launchPreferencesIntent = new Intent().setClass(getApplicationContext(), JITPreferences.class);
                startActivityForResult(launchPreferencesIntent, 1);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data)
    {
        if(data == null) return;
        Intent intent = data;
        int id = intent.getIntExtra("id",-1);
        String name = intent.getStringExtra("name");
        long length = intent.getLongExtra("length", 0);
        boolean toneCB = intent.getBooleanExtra("toneCB",false);
        String toneuri = intent.getStringExtra("toneuri");
        // See which child activity is calling us back.
        switch (requestCode)
        {
            case EDIT_DONE:
                if(id > -1)
                {
                    for(Timer ta : t_adapter.getAllTimers())
                    {
                        if(ta.getId() == id)
                        {
                            ta.reset(getApplicationContext());
                            ta.setTimerName(name);
                            ta.setTimerTime(length);
                            ta.setCustomTone(toneCB);
                            ta.setCustomToneUri(toneuri);
                            break;
                        }
                    }
                }
                break;
            case NEW_ADDED:
                Timer t = new Timer(id,name,length);
                t.setCustomTone(toneCB);
                t.setCustomToneUri(toneuri);
                t_adapter.add(t);
                t_adapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
        t_adapter.notifyDataSetChanged();
    }

    /*    private Runnable returnRes = new Runnable(){
      @Override
      public void run() {
      //if(t_timers != null && t_timers.size() > 0){
      if(t_adapter.getCount() > 0){
      t_adapter.clear();
      t_adapter.notifyDataSetChanged();
      for(Timer ta : t_adapter.getAllTimers())
      t_adapter.add(ta);
      }
      t_adapter.notifyDataSetChanged();

      //lv.post(startTimers);
      }
      };*/

    private void getTimers()
    {
        mDbHelper.open();
        t_adapter.addAll(mDbHelper.fetchAllTimers());
        mDbHelper.close();
    }

    /**
     * Take string time and return number of milliseconds
     * @param time String of time duration in hh:mm:ss format
     * @return number of milliseconds in duration
     */
    public Long parseTime(String time)
    {
        Long millis = null;
        String[] b = time.split(":");
        if(b.length == 2)
        {
            Long minutes = Long.parseLong(b[0].trim());
            Long seconds = Long.parseLong(b[1].trim());
            millis = (minutes * 60 + seconds) * 1000;
        }
        else if (b.length == 3)
        {
            Long hours = Long.parseLong(b[0].trim());
            Long minutes = Long.parseLong(b[1].trim());
            Long seconds = Long.parseLong(b[2].trim());
            millis = (hours * 60 * 60 + minutes * 60 + seconds) * 1000;
        }
        return millis;
    }

    /**
     * Reset timer
     * @param rowId
     */
    public void resetTimer(Context context, long rowId)
    {
        t_adapter.resetTimer(context, (int) rowId);
    }

    public void removeTimer(Context context, long rowId)
    {
        final Timer t = t_adapter.getItem((int) rowId);
        t_adapter.deleteTimer(context, (int) rowId);
        mDbHelper.open();
        mDbHelper.deleteTimer(t.getId());
        mDbHelper.close();
    }

    class MyGestureDetector extends SimpleOnGestureListener
    {
        private static final int SWIPE_MIN_DISTANCE = 50;
        private static final int SWIPE_MAX_OFF_PATH = 50;
        private Context context;
    //     private static final int SWIPE_THRESHOLD_VELOCITY = 1000;

        @Override
        public boolean onScroll(MotionEvent e1,
                                MotionEvent e2,
                                float distanceX,
                                float distanceY)
        {
            position = lv.pointToPosition(Math.round(e1.getX()),
                                          Math.round(e1.getY()));
            t_adapter.holdTimers();

            final int firstVisible = lv.getFirstVisiblePosition();
            final int positionB = position - firstVisible;

            distanceY = Math.abs(Math.round(e1.getY()- e2.getY()));

            if(mIsScrolling)
                distanceY = 0;

            sv = lv.getChildAt(positionB);
            if(sv == null)
                return false;

            tvB = (TextView)sv.findViewById(R.id.drawer);
            final LinearLayout rL = (LinearLayout) sv.findViewById(R.id.li_layout);

            // Handle vertical scroll and swipe to right
            if(distanceY > SWIPE_MAX_OFF_PATH ||
                 Math.round(e2.getX() - e1.getX()) > SWIPE_MIN_DISTANCE)
            {
                mVScroll = true;
                return false;
            }

            // right to left swipe
            final int trueDistance = Math.round(e1.getX() - e2.getX());

            if(trueDistance > SWIPE_MIN_DISTANCE)
            {
                distanceY = 0;
                mIsScrolling = true;

                scrollDistance = (int) trueDistance;
                final int alphaValue = (trueDistance > 500 ? 255
                                        : (int) trueDistance / 2);

                rL.scrollBy((int)distanceX, 0);
                tvB.setBackgroundColor(Color.argb(alphaValue,0, 255, 0));
                tvB.setTextColor(Color.argb(alphaValue,0, 0, 0));
                return true;
            }
            mVScroll = true;
            return true;
        }

        public boolean onDown(MotionEvent e)
        {
            return true;
        }

        public void onLongPress(MotionEvent e)
        {
            final int positionB = getPosition(e);
            if(positionB >= 0)
            {
                final int firstVisible = lv.getFirstVisiblePosition();
                final int position = positionB - firstVisible;
                openContextMenu(lv.getChildAt(position));
            }
        }

        public boolean onSingleTapConfirmed(MotionEvent e)
        {
            context = getApplicationContext();
            final int positionB = getPosition(e);
            if(positionB < 0)
                return false;

            // Get the timer
            final Timer t = t_adapter.getItem(positionB);

            if(t.isRunning())
            {
                t_adapter.pauseTimer(context, positionB);
            }
            else if (!t.getRunning())
            {
                t_adapter.startTimer(context, positionB);
            }
            else if(t.isEnded())
            {
                t_adapter.resetTimer(context, positionB);
            }

            t_adapter.notifyDataSetChanged();
            return true;
        }
    }

    public void handleScrollFinished(MotionEvent e)
    {
        t_adapter.unholdTimers(getApplicationContext());
        if(mVScroll == true)
        {
            mVScroll = false;
            //    return;
        }

        if(scrollDistance > 200)
            t_adapter.resetTimer(getApplicationContext(), position);

        mIsScrolling = false;
        final LinearLayout rL = (LinearLayout)sv.findViewById(R.id.li_layout);
        final int negScroll = 0 - (rL.getScrollX());
        rL.scrollBy(negScroll, 0);
        // TODO: Handle somewhere else
        final TextView tvB = (TextView)sv.findViewById(R.id.drawer);
        tvB.setBackgroundColor(Color.argb(0, 0, 255, 0));
        tvB.setTextColor(Color.argb(0, 0, 0, 0));
        t_adapter.notifyDataSetChanged();
    }

    // Return the position from the grid view
    public int getGridPosition(MotionEvent e)
    {
        return (int) gv.pointToPosition(Math.round(e.getX()),
                                        Math.round(e.getY()));
    }

    public int getPosition(MotionEvent e)
    {
        return (int) lv.pointToPosition(Math.round(e.getX()),
                                        Math.round(e.getY()));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        return (gestureDetector.onTouchEvent(event) ? true : false);
    }


    class GridGestureDetector extends SimpleOnGestureListener
    {
        private static final int SWIPE_MIN_DISTANCE = 50;
        private static final int SWIPE_MAX_OFF_PATH = 50;
        private Context context;
    //     private static final int SWIPE_THRESHOLD_VELOCITY = 1000;

        @Override
        public boolean onScroll(MotionEvent e1,
                                MotionEvent e2,
                                float distanceX,
                                float distanceY)
        {
            position = gv.pointToPosition(Math.round(e1.getX()),
                                          Math.round(e1.getY()));
            t_adapter.holdTimers();

            final int firstVisible = gv.getFirstVisiblePosition();
            final int positionB = position - firstVisible;

            distanceY = Math.abs(Math.round(e1.getY()- e2.getY()));

            if(mIsScrolling)
                distanceY = 0;

            sv = gv.getChildAt(positionB);
            if(sv == null)
                return false;

            tvB = (TextView)sv.findViewById(R.id.drawer);
            final LinearLayout rL = (LinearLayout) sv.findViewById(R.id.li_layout);

            // Handle vertical scroll and swipe to right
            if(distanceY > SWIPE_MAX_OFF_PATH ||
               Math.round(e2.getX() - e1.getX()) > SWIPE_MIN_DISTANCE)
            {
                mVScroll = true;
                return false;
            }

            // right to left swipe
            final int trueDistance = Math.round(e1.getX() - e2.getX());

            if(trueDistance > SWIPE_MIN_DISTANCE)
            {
                distanceY = 0;
                mIsScrolling = true;

                scrollDistance = (int) trueDistance;
                final int alphaValue = (trueDistance > 500 ? 255
                    : (int) trueDistance / 2);

                rL.scrollBy((int)distanceX, 0);
                tvB.setBackgroundColor(Color.argb(alphaValue,0, 255, 0));
                tvB.setTextColor(Color.argb(alphaValue,0, 0, 0));
                return true;
            }
            mVScroll = true;
            return true;
        }

        public boolean onDown(MotionEvent e)
        {
            return true;
        }

        public void onLongPress(MotionEvent e)
        {
            final int positionB = getGridPosition(e);
            if(positionB >= 0)
            {
                final int firstVisible = gv.getFirstVisiblePosition();
                final int position = positionB - firstVisible;
                openContextMenu(gv.getChildAt(position));
            }
        }

        public boolean onSingleTapConfirmed(MotionEvent e)
        {
            context = getApplicationContext();
            final int positionB = getGridPosition(e);
            if(positionB < 0)
                return false;

            // Get the timer
            final Timer t = t_adapter.getItem(positionB);

            if(t.isRunning())
            {
                t_adapter.pauseTimer(context, positionB);

            }
            else if (!t.getRunning())
            {
                t_adapter.startTimer(context, positionB);
            }
            else if(t.isEnded())
            {
                t_adapter.resetTimer(context, positionB);
            }

            t_adapter.notifyDataSetChanged();
            return true;
        }
    }
}
