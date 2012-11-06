package com.danbowtell.justintimer;

import java.util.ArrayList;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class JustInTimerProvider{
	private static final String TAG = "JustInTimerProvider";

	private static final String DATABASE_NAME = "justintimer.db";
	private static final int DATABASE_VERSION = 17;
	private static final String DATABASE_TABLE = "timers";
	private static final String COLUMN_ID = "_id" ;
	private static final String COLUMN_ID_CONST = " INTEGER PRIMARY KEY AUTOINCREMENT, "; 
	private static final String COLUMN_NAME = "name";
	private static final String COLUMN_NAME_CONST = " TEXT NOT NULL, ";
	private static final String COLUMN_TIME = "time";
	private static final String COLUMN_TIME_CONST = " LONG NOT NULL, ";
	private static final String COLUMN_END = "end";
	private static final String COLUMN_END_CONST = " LONG, ";
	private static final String COLUMN_RUNNING = "running";
	private static final String COLUMN_RUNNING_CONST = " BOOLEAN DEFAULT 0, ";
	private static final String COLUMN_PAUSED = "paused";
	private static final String COLUMN_PAUSED_CONST = " BOOLEAN DEFAULT 0, ";
	private static final String COLUMN_REMAINING = "remaining";
	private static final String COLUMN_REMAINING_CONST = " LONG";
	private static final String COLUMN_SOUNDN = "s";
	private static final String COLUMN_SOUNDN_CONST = " BOOLEAN DEFAULT 0";
	private static final String COLUMN_SOUND = "sound";
	private static final String COLUMN_SOUND_CONST = " TEXT";
	private static final String COLUMN_START_TIME = "start_timer";
	private static final String COLUMN_START_TIME_CONST = " LONG";


	private static final String DATABASE_CREATE = "CREATE TABLE "
		+ DATABASE_TABLE + " ("
		+ COLUMN_ID + COLUMN_ID_CONST
		+ COLUMN_NAME + COLUMN_NAME_CONST
		+ COLUMN_TIME + COLUMN_TIME_CONST
		+ COLUMN_END + COLUMN_END_CONST
		+ COLUMN_RUNNING + COLUMN_RUNNING_CONST
		+ COLUMN_PAUSED + COLUMN_PAUSED_CONST 
		+ COLUMN_REMAINING + COLUMN_REMAINING_CONST + ", "
		+ COLUMN_SOUNDN + COLUMN_SOUNDN_CONST + ", " 
		+ COLUMN_START_TIME + COLUMN_START_TIME_CONST + ", "
		+ COLUMN_SOUND + COLUMN_SOUND_CONST + ");";

	private final Context mCtx;
	private SQLiteDatabase mDb;
	private DatabaseHelper mDbHelper;

	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context){
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to"
					+ newVersion + ", which will destroy any old data");
			if (newVersion > oldVersion) {
				boolean success = true;
				for (int i = oldVersion ; i < newVersion ; ++i) {
					int nextVersion = i + 1;
					switch (nextVersion) {
						case 15:
							Log.i("JustInTimer","15");
						case 16:
							Log.i("JustInTimer","16");
							success = upgradeToVersion16(db);
							break;
						case 17:
							success = upgradeToVersion17(db);
							break;
					}
		
					if (!success) {
						break;
					}
				}
			}
		}

		private boolean upgradeToVersion17(SQLiteDatabase db) throws SQLiteException {
			Log.i("JustInTimer","Database upgraded");
			db.execSQL("ALTER TABLE " + DATABASE_TABLE +
					" ADD COLUMN " + COLUMN_START_TIME + COLUMN_START_TIME_CONST +" ;");
			return true;
		}
		
		private boolean upgradeToVersion16(SQLiteDatabase db) throws SQLiteException {
			Log.i("JustInTimer","Database upgraded");
			db.execSQL("ALTER TABLE "
					+ DATABASE_TABLE
					+ " ADD COLUMN " + COLUMN_SOUNDN + COLUMN_SOUNDN_CONST + " ;");
			db.execSQL("ALTER TABLE "
					+ DATABASE_TABLE
					+ " ADD COLUMN " + COLUMN_SOUND + COLUMN_SOUND_CONST + " ;");
	
			return true;
		}
	}
	// Constructor to pass context
	public JustInTimerProvider(Context ctx){
		this.mCtx = ctx;
	}

	public JustInTimerProvider open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close(){
		mDbHelper.close();
	}

	// Create new timer
	public long createTimer(String title, long time, boolean soundn, String sound){
		ContentValues initialValues = new ContentValues();
		initialValues.put(COLUMN_NAME, title);
		initialValues.put(COLUMN_TIME,time);
		initialValues.put(COLUMN_SOUNDN,soundn);
		initialValues.put(COLUMN_SOUND,sound);

		return mDb.insert(DATABASE_TABLE, null, initialValues);
	}

	public ArrayList<Timer> fetchAllTimers(){
		ArrayList<Timer> tList = new ArrayList<Timer>();
		
		Cursor c = mDb.query(DATABASE_TABLE, new String[] {
				COLUMN_ID, COLUMN_NAME,COLUMN_TIME,COLUMN_END,
				COLUMN_RUNNING,COLUMN_PAUSED,COLUMN_REMAINING,COLUMN_SOUNDN,COLUMN_SOUND,COLUMN_START_TIME}
		, null, null, null, null, null);
		
		
		if(c != null){
			c.moveToFirst();
			while (c.isAfterLast() == false) {
		    // The Cursor is now set to the right position
				Timer t = new Timer();
				t.setId(c.getInt(0));
				t.setTimerName(c.getString(1));
				t.setTimerTime(c.getLong(2));
				
				t.setEnd(c.getLong(3));	
				t.setRemaining(c.getLong(6));
				t.setRunning(c.getInt(4) == 1 ? true : false);
				t.setPaused((c.getInt(5) == 1) ? true : false);
				t.setCustomTone((c.getInt(7)== 1) ? true : false);
				t.setCustomToneUri((c.getString(8)));
				t.setStart(c.getLong(9));
				
				tList.add(t);
				t = null;
				c.moveToNext();
			}
		}
		c.close();
		return tList;
	}

	public boolean deleteTimer(int id){
		return mDb.delete(DATABASE_TABLE, COLUMN_ID + " = \"" + id + "\"", null) > 0;
	}

	public int pauseTimer(int id, Long remaining){
		ContentValues cv = new ContentValues();
		cv.put(COLUMN_END, 0);
		cv.put(COLUMN_RUNNING, 0);
		cv.put(COLUMN_START_TIME,0);
		cv.put(COLUMN_PAUSED, 1);
		cv.put(COLUMN_REMAINING, remaining);
		return mDb.update(DATABASE_TABLE, cv, COLUMN_ID + " = \""
				+ id + "\"", null);
	}

	public int updateTimerEnd(int id, Long endMillis, Long startMillis){
		ContentValues cv = new ContentValues();
		cv.put(COLUMN_END, endMillis);
		cv.put(COLUMN_START_TIME,startMillis);
		cv.put(COLUMN_RUNNING, 1);
		cv.put(COLUMN_PAUSED, 0);
		cv.put(COLUMN_REMAINING, endMillis-System.currentTimeMillis());
		return mDb.update(DATABASE_TABLE, cv, COLUMN_ID + " = \""  
				+ id + "\"", null);

	}

	public int editTimer(int id, String name, Long time, Boolean soundn, String sound){
		ContentValues cv = new ContentValues();
		cv.put(COLUMN_NAME, name);
		cv.put(COLUMN_TIME, time);
		cv.put(COLUMN_END, "NULL");
		cv.put(COLUMN_RUNNING, false);
		cv.put(COLUMN_PAUSED, false);
		cv.put(COLUMN_REMAINING, "NULL");
		cv.put(COLUMN_START_TIME,0);
		cv.put(COLUMN_SOUNDN,soundn);
		cv.put(COLUMN_SOUND,sound);
		return mDb.update(DATABASE_TABLE, cv, COLUMN_ID + " = \""
				+ id + "\"", null);
	}

	public int resetTimer(int id){
		ContentValues cv = new ContentValues();
		cv.put(COLUMN_END, "NULL");
		cv.put(COLUMN_RUNNING, false);
		cv.put(COLUMN_PAUSED, false);
		cv.put(COLUMN_START_TIME,0);
		cv.put(COLUMN_REMAINING, "NULL");
		return mDb.update(DATABASE_TABLE, cv, COLUMN_ID + " = \"" 
				+ id + "\"", null);

	}

	public Timer retrieveTimerById(long rowId) throws SQLException {
		Cursor mCursor =
			mDb.query(true, DATABASE_TABLE, new String[] {
					COLUMN_ID, COLUMN_NAME,COLUMN_TIME, COLUMN_SOUNDN,
					COLUMN_SOUND, COLUMN_RUNNING, COLUMN_PAUSED, COLUMN_END, COLUMN_REMAINING, COLUMN_START_TIME}, 
					COLUMN_ID + "=" + (int)rowId, 
					null,
					null, 
					null, 
					null, 
					null);

		if (mCursor != null)
			mCursor.moveToFirst();
		else
			return null;

		if(mCursor.getCount() == 0){
			return null;
		}
		Log.i("JustInTimer",mCursor.getInt(0)+ mCursor.getString(1) + mCursor.getLong(2));

		Timer t = new Timer();
		t.setId(mCursor.getInt(0));
		t.setTimerName(mCursor.getString(1));
		t.setTimerTime(mCursor.getLong(2));
		t.setCustomTone((mCursor.getInt(3)==1) ? true : false);
		t.setCustomToneUri(mCursor.getString(4));
		t.setRunning(mCursor.getInt(5) == 1 ? true : false);
		t.setPaused(mCursor.getInt(6) == 1 ? true : false);
		t.setEnd(mCursor.getLong(7));
		t.setRemaining(mCursor.getLong(8));
		t.setStart(mCursor.getLong(9));
		
		mCursor.close();
		return t;
	}
	
	public ArrayList<Timer> fetchListTimersForWidget(){
		ArrayList<Timer> t_timers = new ArrayList<Timer>();
		Cursor mCursor = mDb.query(DATABASE_TABLE, new String[] {COLUMN_ID,COLUMN_NAME,COLUMN_TIME}, null, null, null, null, null);
		
		if(mCursor != null)
			mCursor.moveToFirst();
		
		while (mCursor.isAfterLast() == false) {
		    // The Cursor is now set to the right position
			Timer t = new Timer();
			t.setId(mCursor.getInt(0));
			t.setTimerName(mCursor.getString(1));
			t.setTimerTime(mCursor.getLong(2));
			 			    			    			
		    t_timers.add(t);
		    t = null;
		    mCursor.moveToNext();
		}
		return t_timers;
	}
	
	public void updateAllTimers(ArrayList<Timer> timers){
		for(Timer t:timers){
			ContentValues cv = new ContentValues();
			cv.put(COLUMN_NAME, t.getTimerName());
			cv.put(COLUMN_TIME, t.getTimerTime());
			cv.put(COLUMN_END, t.getEnd());
			cv.put(COLUMN_RUNNING, t.getRunning());
			cv.put(COLUMN_PAUSED, t.getPaused());
			cv.put(COLUMN_REMAINING, t.getStorageRemaining());
			cv.put(COLUMN_START_TIME, t.getStart());
		
			mDb.update(DATABASE_TABLE, cv, COLUMN_ID + " = \"" + t.getId() + "\"", null);
		}
	}
}