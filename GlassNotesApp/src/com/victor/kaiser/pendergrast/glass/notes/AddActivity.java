package com.victor.kaiser.pendergrast.glass.notes;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.util.Log;

import com.victor.kaiser.pendergrast.glass.notes.preferences.PreferenceConstants;

public class AddActivity extends Activity {

	private static final String TAG = "AddActivity";

	private static final int VOICE_RECOGNIZER_REQUEST = 9000;

	private SharedPreferences mPrefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
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
		
		
	}

	@Override
	protected void onResume() {
		super.onResume();

		ArrayList<String> notes = getIntent().getExtras().getStringArrayList(
				RecognizerIntent.EXTRA_RESULTS);

		if (notes == null) {

			// This must have not been launched from the "ok, glass" home menu
			// so launch our own voice recognizer
			Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			startActivityForResult(intent, VOICE_RECOGNIZER_REQUEST);

		} else {

			for (String note : notes) {
				Log.i(TAG, "Note: \"" + note + "\"");
			}

			addNote(notes.get(0));
			
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == VOICE_RECOGNIZER_REQUEST && resultCode == RESULT_OK) {
			List<String> notes = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			addNote(notes.get(0));
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	private void addNote(String note){
		// TODO add the note to local storage and send to server
		
	}

}
