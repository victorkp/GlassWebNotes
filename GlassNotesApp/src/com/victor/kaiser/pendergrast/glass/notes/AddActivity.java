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

public class AddActivity extends Activity implements RefreshAuthTokenTask.OnGetTokenListener {

	private static final String TAG = "AddActivity";

	private static final int VOICE_RECOGNIZER_REQUEST = 9000;

	private SharedPreferences mPrefs;

	private String mNotes;
	private String mNewNote;

	private String mAuthToken;

	private TextView mCardTitle;
	private TextView mCardSubTitle;
	private ImageView mCardImage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Show a View with a small amount of explanation
		// of sign in. On tap, we'll start the authentication
		setContentView(R.layout.card_full_image);

		mCardTitle = (TextView) findViewById(R.id.card_title);
		mCardSubTitle = (TextView) findViewById(R.id.card_subtitle);
		mCardImage = (ImageView) findViewById(R.id.card_image);

		mCardImage.setScaleType(ScaleType.CENTER_INSIDE);
		
		mCardTitle.setText(R.string.text_sign_in_info);
		mCardSubTitle.setText(R.string.text_tap_to_begin);

		mCardTitle.setGravity(Gravity.CENTER);


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
	protected void onResume() {
		super.onResume();

		ArrayList<String> notes = getIntent().getExtras().getStringArrayList(
				RecognizerIntent.EXTRA_RESULTS);

		if (notes == null || notes.size() == 0) {

			// This must have not been launched from the "ok, glass" home menu
			// so launch our own voice recognizer
			Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			startActivityForResult(intent, VOICE_RECOGNIZER_REQUEST);

		} else {
			// Keep this note: it will be put on the server once
			// all of the auth and sync is done
			mNewNote = notes.get(0);
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

	@Override
	public void onResponse(boolean success, String response) {
		if(success){
			AuthTokenJsonParser parser = new AuthTokenJsonParser(response);
			
			if(parser.hasError()){
				Log.e(TAG, parser.getError());
				
				// TODO Show failure to sync
				
			}else{
				parser.writeToPreferences(mPrefs);
				mAuthToken = parser.getAuthToken();

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
							NotesJsonParser notesParser = new NotesJsonParser(response);
							mNotes = notesParser.getNotes();
							
							// Now that we have the notes on the server,
							// add the new note
							addNote();
						}else{
							// TODO Show failure to sync
							
						}
					}
				});
				
				task.execute(mAuthToken);
				
			}
		}
	}
	

	
	private void addNote(){
		// TODO add the note to local storage and send to server
		PutNotesTask putTask = new PutNotesTask();
		putTask.setJSON(JSONMaker.makeJSON(mNotes + mNewNote);
		putTask.setListener(new PutNotesTask.OnPutNotesListener(){
			@Override
			public void onResponse(boolean success, String response){
				if(success){
					// All done with putting notes
					finish();
				} else {
					// TODO Show failure to sync
				}
			}
		});

		putTask.execute(mAuthToken);
	}

}
