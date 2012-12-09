package com.danbowtell.justintimer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

public class TimerWidget extends AppWidgetProvider
{
    static void updateTimerWidget(Context context,
                                  AppWidgetManager appWidgetManager,
                                  int appWidgetId,
                                  String length,
                                  int clickAction,
                                  int timerId,
                                  String name)
    {
        // Send widget to home
        final RemoteViews views = new RemoteViews(context.getPackageName(),
                                                  R.layout.widget_layout);
        views.setTextViewText(R.id.title, name);
        views.setTextViewText(R.id.time, length);

        Intent clickIntent;
        PendingIntent clickPendingIntent;

        switch(clickAction)
        {
            case 0:
                clickIntent = new Intent("com.danbowtell.justintimer.RESET");
                break;
            case 1:
                clickIntent = new Intent("com.danbowtell.justintimer.START");
                break;
            case 2:
                clickIntent = new Intent("com.danbowtell.justintimer.PAUSE");
                break;
            default:
                clickIntent = new Intent("com.danbowtell.justintimer.ERROR");
                break;
        }

        clickIntent.putExtra("ID", timerId);
        clickIntent.putExtra("AppID",appWidgetId);
        clickPendingIntent = PendingIntent.getBroadcast(context,
            appWidgetId,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.time,clickPendingIntent);
        //views.setOnClickPendingIntent(R.id.layout,clickPendingIntent);
        //Tell the widget manager
        appWidgetManager.updateAppWidget(appWidgetId,views);
    }

    @Override
    public void onUpdate(Context context,
                         AppWidgetManager appWidgetManager,
                         int[] appWidgetIds)
    {
        int runningTimers = 0;
        final JustInTimerProvider mDbHelper = new JustInTimerProvider(context);
        mDbHelper.open();
        final int N = appWidgetIds.length;

        for(int i = 0; i < N; i++)
        {
            final int appWidgetId = appWidgetIds[i];

            final int timerId = (int) TimerWidgetConfigure.loadTimerPref(
                context,
                appWidgetId);
            if(timerId > 0)
            {
                final Timer t = mDbHelper.retrieveTimerById(timerId);
                if(t != null){
                    if(t.getPaused())
                    {
                        TimerWidget.updateTimerWidget(
                            context,
                            appWidgetManager,
                            appWidgetId,
                            "Paused",
                            1,
                            t.getId(),
                            t.getTimerName());
                    }
                    else if(t.isRunning())
                    {
                        runningTimers++;
                        TimerWidget.updateTimerWidget(
                            context,
                            appWidgetManager,
                            appWidgetId,
                            new FormattedNumber().getDisplay(t.getRemaining()),
                            2,
                            t.getId(),
                            t.getTimerName());
                    }
                    else if(t.isEnded())
                    {
                        TimerWidget.updateTimerWidget(
                            context,
                            appWidgetManager,
                            appWidgetId,
                            "Finished",
                            0,
                            t.getId(),
                            t.getTimerName());
                    }
                    else
                    {
                        long duration = t.getTimerTime();
                        TimerWidget.updateTimerWidget(
                            context,
                            appWidgetManager,
                            appWidgetId,
                            new FormattedNumber().getDisplay(duration),
                            1,
                            t.getId(),
                            t.getTimerName());
                    }
                }
                else
                {
                    TimerWidget.updateTimerWidget(
                        context,
                        appWidgetManager,
                        appWidgetId,
                        "False",
                        5,
                        0,
                        "False");
                }
            }
            else
            {
                TimerWidget.updateTimerWidget(context,
                                              appWidgetManager,
                                              appWidgetId,
                                              "False",
                                              5,
                                              0,
                                              "False");
            }
        }
        mDbHelper.close();

        // Update every second only if timers are running.
        if(runningTimers > 0)
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

    @Override
    public void onReceive(Context context, Intent intent)
    {
        super.onReceive(context, intent);

        // set update
        if("com.danbowtell.justintimer.TIMER_WIDGET_UPDATE".equals(
            intent.getAction()))
        {
            Bundle extras = intent.getExtras();
            if(extras != null)
            {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ComponentName thisAppWidget = new ComponentName(
                    context.getPackageName(),
                    TimerWidget.class.getName());
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
                onUpdate(context,appWidgetManager,appWidgetIds);
            }
        }
    }
}
