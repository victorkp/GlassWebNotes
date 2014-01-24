package com.victor.kaiser.pendergrast.glass.notes;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;

import com.victor.kaiser.pendergrast.glass.notes.preferences.PreferenceConstants;

public class MainActivity extends Activity {

	private SharedPreferences mPrefs;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		if(mPrefs.getString(PreferenceConstants.AUTH_TOKEN, "").isEmpty()){
			// Launch the authentication immersion
			
		}
		
		// Get the most recent notes
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}

