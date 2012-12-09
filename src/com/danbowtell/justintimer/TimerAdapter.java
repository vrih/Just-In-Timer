package com.danbowtell.justintimer;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

class TimerAdapter extends ArrayAdapter<Timer>
{
    private static FormattedNumber fn;
    private int RED;
    private int GREEN;
    private int YELLOW;
    private int BLACK;
    private int GREY;
    private ArrayList<Timer> items;
    private Context context;

    public TimerAdapter(Context context,
                        int textViewResourcesId,
                        ArrayList<Timer> items)
    {
        super(context, textViewResourcesId, items);
        fn = new FormattedNumber();
        this.context = context;
        this.items = items;
        Resources res = context.getResources();
        RED = res.getColor(R.color.red);
        GREEN = res.getColor(R.color.green);
        YELLOW = res.getColor(R.color.yellow);
        BLACK = res.getColor(R.color.black);
        GREY = res.getColor(R.color.grey);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View v = convertView;
        if(v == null)
        {
            LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.list_item, null);
        }

        Timer o = items.get(position);
        if(o != null)
        {
            TextView name = (TextView) v.findViewById(R.id.name);
            TextView time = (TextView) v.findViewById(R.id.time);

            if(name!=null)
            {
                name.setText(o.getTimerName());
            }

            if(o.getPaused())
            {
                    setColors(BLACK,
                              v,
                              YELLOW,
                              fn.getDisplay(o.getRemaining()),
                              name,
                              time);
            }
            else if(o.isRunning())
            {
                    setColors(BLACK,
                              v,
                              GREEN,
                              fn.getDisplay(o.getRemaining()),
                              name,
                              time);
            }
            else if(o.isEnded())
            {
                setColors(BLACK,v,RED, fn.getDisplay((long) 0),name,time);
            }
            else if(!o.getPaused() && !o.isRunning())
            {
                setColors(GREY,
                          v,
                          BLACK,
                          fn.getDisplay(o.getTimerTime()),
                          name,
                          time);
            }
        }
        return v;
    }

    private void setColors(int id,
                           View v,
                           int bgcol,
                           String text,
                           TextView name,
                           TextView time)
    {
        if( id != 0 && bgcol != 0)
        {
            time.setTextColor(id);
            name.setTextColor(id);
            v.setBackgroundColor(bgcol);
        }
        time.setText(text);
    }

    /**
     * Start timer running
     * @param context
     * @param position
     */
    public void startTimer(Context context, int position)
    {
        this.getItem(position).start(context);
    }

    public void resetTimer(Context context, int position)
    {
        this.getItem(position).reset(context);
        this.notifyDataSetChanged();
    }

    public void pauseTimer(Context context, int position)
    {
        this.getItem(position).pause(context);
        this.notifyDataSetChanged();
    }

    public void deleteTimer(Context context, int position)
    {
        Timer t = this.getItem(position);
        t.delete(context);
        this.remove(t);
        this.notifyDataSetChanged();
    }

    public  ArrayList<Timer> getAllTimers()
    {
        return this.items;
    }

    public void addAll(ArrayList<Timer> timers)
    {
        this.items.clear();
        this.items.addAll(timers);
        for(Timer t:this.items)
        {
            if(t.isRunning())
            {
                t.start(context);
            }
        }
        this.notifyDataSetChanged();
    }

    public void holdTimers()
    {
        for(Timer t:items)
        {
            t.stopTimer();
        }
    }

    public void unholdTimers(Context context)
    {
        for(Timer t:items)
        {
            t.startTimer(context);
        }
    }
}
