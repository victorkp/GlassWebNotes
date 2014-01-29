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

import com.google.android.glass.widget.CardScrollView;
import com.victor.kaiser.pendergrast.glass.notes.auth.AuthTokenJsonParser;
import com.victor.kaiser.pendergrast.glass.notes.auth.RefreshAuthTokenTask;
import com.victor.kaiser.pendergrast.glass.notes.preferences.PreferenceConstants;
import com.victor.kaiser.pendergrast.glass.notes.api.GetNotesTask;
import com.victor.kaiser.pendergrast.glass.notes.api.NotesJsonParser;
import com.victor.kaiser.pendergrast.glass.notes.content.NoteAdapter;

public class MainActivity extends Activity  implements RefreshAuthTokenTask.OnGetTokenListener {
	private static final String TAG = "MainActivity";

	private SharedPreferences mPrefs;

	private CardScrollView mScrollView;
	private NoteAdapter mAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		// Check to make sure that there is a Refresh Token
		if(mPrefs.getString(PreferenceConstants.REFRESH_TOKEN, "").isEmpty()){
			// Launch the authentication immersion
			Intent authSetupIntent  = new Intent(this, AuthActivity.class);
			startActivity(authSetupIntent);
			finish();
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
			mPrefs.edit()
				.putString(PreferenceConstants.AUTH_TOKEN, "")
				.putString(PreferenceConstants.REFRESH_TOKEN, "")
				.putString(PreferenceConstants.DEVICE_CODE, "")
				.commit();
			
			// Start the AuthActivity to do setup
			Intent authSetupIntent = new Intent(this, AuthActivity.class);
			startActivity(authSetupIntent);

			// Close this Activity
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
				displayFailureToSync();
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
							// Display the notes contained in the response
							Log.i(TAG, response);

							// First extract the notes from the response
							NotesJsonParser notesParser = new NotesJsonParser(response);
							String notes = notesParser.getNotes();

							if(notes.isEmpty()){
								displayNoNotes();
							}else{
								// List the notes
								displayNotes(notes);
							}

						}else{
							// Failed to get the notes
							displayFailureToSync();
						}
					}
				});
				
				task.execute(authToken);
				
			}
		}
	}
	
	private void displayFailureToSync(){
		// Show failure to sync
		setContentView(R.layout.card_full_image);
		((TextView) findViewById(R.id.card_title)).setText(getString(R.string.text_failed_to_get_notes));
		((TextView) findViewById(R.id.card_subtitle)).setText(getString(R.string.text_check_internet));
		((ImageView) findViewById(R.id.card_image)).setImageResource(R.drawable.ic_warning_50);
	}
	
	private void displayNoNotes(){
		// Show failure to sync
		setContentView(R.layout.card_full_image);
		((TextView) findViewById(R.id.card_title)).setText(getString(R.string.text_no_notes));
		((TextView) findViewById(R.id.card_subtitle)).setText(getString(R.string.text_suggest_add_notes));
		((ImageView) findViewById(R.id.card_image)).setImageResource(R.drawable.ic_pen_50);
	}

	private void displayNotes(String notes){
		mScrollView = new CardScrollView(this);
		
		// Create the adapter and set it, allow interaction
		mAdapter = new NoteAdapter(this, notes);
		mScrollView.setAdapter(mAdapter);

		// Show the CardScrollView
		mScrollView.activate();
		setContentView(mScrollView);
	}

}

