package com.danbowtell.justintimer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class JITPreferences extends PreferenceActivity{
	
	SharedPreferences prefs;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);		
	}
}
