package com.danbowtell.justintimer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class TimerActionReceiver extends BroadcastReceiver
{
    private Timer t;
    private int id = 0;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        JustInTimerProvider mDbHelper = new JustInTimerProvider(context);
        Bundle extras = intent.getExtras();
        mDbHelper.open();

        if(extras != null)
        {
                id = extras.getInt("ID");

                t = new Timer();
                t = mDbHelper.retrieveTimerById(id);
        }
        else
        {
            return;
        }

        // Start Action
        if("com.danbowtell.justintimer.START".equals(intent.getAction()))
        {
            mDbHelper.updateTimerEnd(t.getId(),
                                     t.getRemaining() +
                                        System.currentTimeMillis(),
                                     System.currentTimeMillis());
            t.widgetStart(context);
            updateWidget(context);
            //t.updateTimerEnd();
        }

        // Pause Action
        if("com.danbowtell.justintimer.PAUSE".equals(intent.getAction()))
        {
            mDbHelper.pauseTimer(t.getId(), t.getRemaining());
            t.pause(context);
            updateWidget(context);
            //t.pause(context);
        }

        // Reset Action
        if("com.danbowtell.justintimer.RESET".equals(intent.getAction()))
        {
            mDbHelper.resetTimer(t.getId());
            t.reset(context);
            updateWidget(context);
            //t.reset(context);
        }

        if("com.danbowtell.justintimer.ERROR".equals(intent.getAction()))
        {
            Toast.makeText(context, "Please delete widget", Toast.LENGTH_LONG);
        }
        mDbHelper.close();
    }

    private void updateWidget(Context context)
    {
        Intent updateIntent = new Intent(
            "com.danbowtell.justintimer.TIMER_WIDGET_UPDATE");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            updateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager)context.getSystemService(
            Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC,
               System.currentTimeMillis() + 1000,
               pendingIntent);
    }
}
