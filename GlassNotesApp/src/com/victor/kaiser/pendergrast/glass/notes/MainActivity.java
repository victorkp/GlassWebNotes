package com.victor.kaiser.pendergrast.glass.notes;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;

import com.victor.kaiser.pendergrast.glass.notes.auth.RefreshAuthTokenTask;
import com.victor.kaiser.pendergrast.glass.notes.preferences.PreferenceConstants;

public class MainActivity extends Activity  implements RefreshAuthTokenTask.OnGetTokenListener {
	private static final String TAG = "MainActivity";

	private SharedPreferences mPrefs;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		// Check to make sure that there is a Refresh Token
		if(mPrefs.getString(PreferenceConstants.REFRESH_TOKEN, "").isEmpty()){
			// Launch the authentication immersion
			Intent i = new Intent(this, AuthActivity.class);
			startActivity(i);
		}
		
		Log.d(TAG, "Auth token: \"" + mPrefs.getString(PreferenceConstants.AUTH_TOKEN, "") + "\"");
		Log.d(TAG, "Refresh token: \"" + mPrefs.getString(PreferenceConstants.REFRESH_TOKEN, "") + "\"");
		
		// Get a new auth token and get the most recent notes
		RefreshAuthTokenTask refreshAuthTask = new RefreshAuthTokenTask();
		refreshAuthTask.setListener(this);
		refreshAuthTask.execute(mPrefs.getString(PreferenceConstants.REFRESH_TOKEN, ""));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onResponse(boolean success, String response) {
		if(success){
			
		}
	}
	
	

}

