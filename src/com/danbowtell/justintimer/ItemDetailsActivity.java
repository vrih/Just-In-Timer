package com.danbowtell.justintimer;

import kankan.wheel.widget.NumericWheelAdapter;
import kankan.wheel.widget.OnWheelChangedListener;
import kankan.wheel.widget.OnWheelScrollListener;
import kankan.wheel.widget.WheelView;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.NumberPicker;

public class ItemDetailsActivity extends Activity{
    private boolean timeChanged = false;
    private boolean timeScrolled = false;
    private static Context mCxt;
    private Long millis;
    private String name;
    private String toneUri;
    private int id;
    boolean cancel;
    // New for v11
    private NumberPicker npHours;
    private NumberPicker npMinutes;
    private NumberPicker npSeconds;
    // Deprecated. Remove once majority above v11
    private WheelView hours;
    private WheelView mins;
    private WheelView secs;

    private JustInTimerProvider mDbHelper;
    private final static int SET_RINGTONE = 1;
    private String mTitle;
    private Uri mUri;
    private CheckBox toneCB;
    private int sdkVersion;


    private static final String[] MEDIA_COLUMNS = new String[]
    {
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.TITLE
    };


    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        sdkVersion = android.os.Build.VERSION.SDK_INT;
        setContentView(R.layout.new_timer);
        Intent intent = getIntent();
        id = intent.getIntExtra("id",-1);

        mCxt = getApplicationContext();
        setContentView(R.layout.new_timer);
        setTitle("Select Duration");

        // Initialise Timer selectors
        if(sdkVersion < 11){
            hours = (WheelView) findViewById(R.id.w_hours);
            hours.setAdapter(new NumericWheelAdapter(0, 23));

            mins = (WheelView) findViewById(R.id.w_minutes);
            mins.setAdapter(new NumericWheelAdapter(0, 59, "%02d"));
            mins.setCyclic(true);

            secs = (WheelView) findViewById(R.id.w_seconds);
            secs.setAdapter(new NumericWheelAdapter(0, 59, "%02d"));
            secs.setCyclic(true);

            int curHours = 0;
            int curMinutes = 0;
            int curSecs = 0;

            hours.setCurrentItem(curHours);
            mins.setCurrentItem(curMinutes);
            secs.setCurrentItem(curSecs);

            addChangingListener(mins, "");
            addChangingListener(hours, "");
            addChangingListener(secs, "");

            OnWheelChangedListener wheelListener = new OnWheelChangedListener()
            {
                public void onChanged(WheelView wheel,
                                      int oldValue,
                                      int newValue)
                {
                    if (!timeScrolled)
                    {
                        timeChanged = true;
                        timeChanged = false;
                    }
                }
            };

        hours.addChangingListener(wheelListener);
        mins.addChangingListener(wheelListener);
        secs.addChangingListener(wheelListener);

        OnWheelScrollListener scrollListener = new OnWheelScrollListener()
        {
            public void onScrollingStarted(WheelView wheel)
            {
                timeScrolled = true;
            }
            public void onScrollingFinished(WheelView wheel)
            {
                timeScrolled = false;
                timeChanged = true;
                timeChanged = false;
            }
        };

        hours.addScrollingListener(scrollListener);
        mins.addScrollingListener(scrollListener);
        secs.addScrollingListener(scrollListener);
    }
    else
    {
        // Set up Honeycomb Number Spinners
        npHours = (NumberPicker) findViewById(R.id.numberPickerHours);
        npHours.setMaxValue(100);

        npMinutes = (NumberPicker) findViewById(R.id.numberPickerMinutes);
        npMinutes.setMaxValue(59);
        npMinutes.setFormatter(new NumberPicker.Formatter()
        {
            @Override
            public String format(int value)
            {
                if(value > 9)
                {
                    return Integer.toString(value);
                }
                else
                {
                    return "0" + Integer.toString(value);
                }
            }
        });

        npSeconds = (NumberPicker) findViewById(R.id.numberPickerSeconds);
        npSeconds.setMaxValue(59);
        npSeconds.setFormatter(new NumberPicker.Formatter()
        {
            @Override
            public String format(int value)
            {
                if(value > 9)
                {
                    return Integer.toString(value);
                }
                else
                {
                    return "0" + Integer.toString(value);
                }
            }
        });
    }

    final TextView ringtoneUriTV = (TextView)findViewById(R.id.ringtone_display);
    //        if(toneUri ==  null)
    //            ringtoneUriTV.setText("Default alarm");
    //        else
    ringtoneUriTV.setText(toneUri);
    // Set up on click listener for ringtone
    final Button ringtoneTV = (Button)findViewById(R.id.ringtone);

    // Initialise toneCB
    toneCB = (CheckBox)findViewById(R.id.toneCB);
    ringtoneTV.setEnabled(toneCB.isChecked());
    ringtoneUriTV.setEnabled(toneCB.isChecked());

    toneCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
    {
        @Override
        public void onCheckedChanged(CompoundButton arg0, boolean isChecked)
        {
            ringtoneTV.setEnabled(isChecked);
            ringtoneUriTV.setEnabled(isChecked);
        }
    });

    if(id > -1)
    {
        setTitle("Edit Timer");
        mDbHelper = new JustInTimerProvider(this);
        mDbHelper.open();
        final Timer t = mDbHelper.retrieveTimerById(id);
        mDbHelper.close();
        final FormattedNumber fm = new FormattedNumber();
        fm.calculateTime(t.getTimerTime());
        final TextView tName = (TextView)findViewById(R.id.tTname);
        if(this.sdkVersion < 11)
        {
            hours.setCurrentItem(fm.getHours());
            mins.setCurrentItem(fm.getMinutes());
            secs.setCurrentItem(fm.getSeconds());
        }
        else
        {
            this.npHours.setValue(fm.getHours());
            this.npMinutes.setValue(fm.getMinutes());
            this.npSeconds.setValue(fm.getSeconds());
        }

        tName.setText(t.getTimerName());
        toneCB.setChecked(t.isCustomTone());
        final TextView toneName = (TextView) findViewById(R.id.ringtone_display);
        String toneU = t.getCustomToneUri();
        Uri uri = (toneU != null) ?
            Uri.parse(t.getCustomToneUri()):
            null;
        final String uriText = (uri != null)?
            getTitle(mCxt,Uri.parse(t.getCustomToneUri()),true):
            (String) mCxt.getResources().getText(R.string.silent);
        toneName.setText(uriText);
        }
    }

    public void onRingtoneButtonClick(View arg0)
    {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "@string/ringtone_title");
        if(toneUri != null)
        {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,Uri.parse(toneUri));
        }
        else
        {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,(Uri)null);
        }
        startActivityForResult(intent, SET_RINGTONE);
    }

    public void okButtonClick(View v)
    {
        final TextView tHour = (TextView)findViewById(R.id.tTname);
        int selHour;
        int selMin;
        int selSec;
        if(this.sdkVersion < 11)
        {
            selHour = hours.getCurrentItem();
            selMin = mins.getCurrentItem();
            selSec = secs.getCurrentItem();
        }
        else
        {
            selHour = this.npHours.getValue();
            selMin = this.npMinutes.getValue();
            selSec = this.npSeconds.getValue();
        }

        mDbHelper = new JustInTimerProvider(this);
        mDbHelper.open();

        millis = new Long((selSec * 1000) + (selMin * 60 * 1000) + (selHour * 60 * 60 * 1000));
        name = tHour.getText().toString();
        if(id > -1)
        {
        // TODO add ringtone into null
            mDbHelper.editTimer(id,name,millis, toneCB.isChecked(),toneUri);
        }
        else
        {
            mDbHelper.createTimer(name,millis,toneCB.isChecked(),toneUri);
        }
        Intent data = new Intent();
        data.putExtra("id", id);
        data.putExtra("name", this.name);
        data.putExtra("length",millis);
        data.putExtra("toneCB",toneCB.isChecked());
        data.putExtra("toneuri",toneUri);
        if (getParent() == null)
        {
            setResult(Activity.RESULT_OK, data);
        }
        else
        {
            getParent().setResult(Activity.RESULT_OK, data);
        }
        mDbHelper.close();
        finish();
    }

    public void cancelButtonClick(View view)
    {
        finish();
    }

    /**
     * Adds changing listener for wheel that updates the wheel label
     * @param wheel the wheel
     * @param label the wheel label
     */
    private void addChangingListener(final WheelView wheel, final String label) {
    wheel.addChangingListener(new OnWheelChangedListener() {
        public void onChanged(WheelView wheel, int oldValue, int newValue) {
            wheel.setLabel(newValue != 1 ? label + "" : label);
        }
        });
    }

    public void populate(int id, Long time, String name)
    {
        final FormattedNumber fm = new FormattedNumber();
        fm.calculateTime(time);
        final TextView tName = (TextView)findViewById(R.id.tTname);
        if(this.sdkVersion < 11)
        {
            hours.setCurrentItem(fm.getHours());
            mins.setCurrentItem(fm.getMinutes());
            secs.setCurrentItem(fm.getSeconds());
        }
        else
        {
            this.npHours.setValue(fm.getHours());
            this.npMinutes.setValue(fm.getMinutes());
            this.npSeconds.setValue(fm.getSeconds());
        }
        tName.setText(name);
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public Long getMillis()
    {
        return millis;
    }

    public int getId()
    {
        return this.id;
    }

    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data)
    {
        final TextView ringtoneUriTV = (TextView)findViewById(R.id.ringtone_display);

        switch(requestCode){
            case SET_RINGTONE:
                if(resultCode == RESULT_OK)
                {
                    final Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if(uri != null)
                    {
                        toneUri = uri.toString();
                        final String uriText = getTitle(mCxt,uri,true);
                        ringtoneUriTV.setText(uriText);
                    }
                    else
                    {
                    // TODO Display ringtone title
                    //                    Ringtone.getTitle(uri);

                        ringtoneUriTV.setText((String) mCxt.getResources().getText(R.string.silent));
                    }
                }
            break;
        }
    }
    /**
     * Returns a human-presentable title for ringtone. Looks in media and DRM
     * content providers. If not in either, uses the filename
     *
     * @param context A context used for querying.
     */
    public String getTitle(Context context)
    {
        return (mTitle != null) ? mTitle : getTitle(context, mUri, true);
    }

    private static String getTitle(Context context,
                                   Uri uri,
                                   boolean followSettingsUri)
    {
        Cursor cursor = null;
        final ContentResolver res = context.getContentResolver();

        String title = null;

        if (uri != null)
        {
            final String authority = uri.getAuthority();
            if (Settings.AUTHORITY.equals(authority))
            {
                if (followSettingsUri)
                {
                    final Uri actualUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.getDefaultType(uri));
                    final String actualTitle = getTitle(context, actualUri, false);
                    title = (String) mCxt.getResources().getText(R.string.default_tone);
                }
            }
            else
            {
                if (MediaStore.AUTHORITY.equals(authority))
                {
                    cursor = res.query(uri, MEDIA_COLUMNS, null, null, null);
                }

                if (cursor != null && cursor.getCount() == 1)
                {
                    cursor.moveToFirst();
                    return cursor.getString(2);
                }
                else
                {
                    title = uri.getLastPathSegment();
                }
            }
        }
        return (title != null) ? title : "";
    }
}
