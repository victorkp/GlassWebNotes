package com.victor.kaiser.pendergrast.glass.notes;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;

import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.victor.kaiser.pendergrast.glass.notes.auth.AuthTokenJsonParser;
import com.victor.kaiser.pendergrast.glass.notes.auth.RefreshAuthTokenTask;
import com.victor.kaiser.pendergrast.glass.notes.preferences.PreferenceConstants;
import com.victor.kaiser.pendergrast.glass.notes.api.GetNotesTask;

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
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		case R.id.menu_add_note:
			Intent addIntent = new Intent(this, AddActivity.class);
			startActivity(addIntent);
			finish();
			break;
		case R.id.menu_sign_out:
			// Clear all the saved tokens and codes
			// and then close this Activity
			mPrefs.edit()
				.putString(PreferenceConstants.AUTH_TOKEN, "")
				.putString(PreferenceConstants.REFRESH_TOKEN, "")
				.putString(PreferenceConstants.DEVICE_CODE, "")
				.commit();
			finish();
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onResponse(boolean success, String response) {
		if(success){
			AuthTokenJsonParser parser = new AuthTokenJsonParser(response);
			
			if(parser.hasError()){
				Log.e(TAG, parser.getError());
				
				// Show failure to sync
				setContentView(R.layout.card_full_image);
				((TextView) findViewById(R.id.card_title)).setText(getString(R.string.text_failed_to_sign_in));
				((TextView) findViewById(R.id.card_subtitle)).setText(getString(R.string.text_check_internet));
				((ImageView) findViewById(R.id.card_image)).setImageResource(R.drawable.ic_warning_50);
				
			}else{
				parser.writeToPreferences(mPrefs);
				String authToken = parser.getAuthToken();

				// Now sync the notes
				Log.i(TAG, "Starting GetNotesTask");

				GetNotesTask task = new GetNotesTask();
				task.setListener(new GetNotesTask.OnGetNotesListener(){
					@Override
					public void onReceiveNotes(boolean success, String response){
						Log.i(TAG, "onGetNotes: " + success);
						if(success){
							// Display the notes
							Log.i(TAG, response);
						}else{
							// TODO Show failure to sync
							
						}
					}
				});
				
				task.execute(authToken);
				
			}
		}
	}
	
	

}

