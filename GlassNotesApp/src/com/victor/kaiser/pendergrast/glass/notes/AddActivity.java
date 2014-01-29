package com.victor.kaiser.pendergrast.glass.notes;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.google.android.glass.media.Sounds;
import com.victor.kaiser.pendergrast.glass.notes.api.GetNotesTask;
import com.victor.kaiser.pendergrast.glass.notes.api.NotesJsonMaker;
import com.victor.kaiser.pendergrast.glass.notes.api.NotesJsonParser;
import com.victor.kaiser.pendergrast.glass.notes.api.PutNotesTask;
import com.victor.kaiser.pendergrast.glass.notes.auth.AuthTokenJsonParser;
import com.victor.kaiser.pendergrast.glass.notes.auth.RefreshAuthTokenTask;
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
			
			mNewNote = notes.get(0);
			addNote();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onResponse(boolean success, String response) {
		if(success){
			AuthTokenJsonParser parser = new AuthTokenJsonParser(response);
			
			if(parser.hasError()){
				Log.e(TAG, parser.getError());
				
				// Show failure to sync
				displayFailureToSignIn();
				
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
							// Show failure to sync
							displayFailureToAdd();
						}
					}
				});
				
				task.execute(mAuthToken);
				
			}
		}
	}

	
	private void addNote(){
		// Add the note to the server
		PutNotesTask putTask = new PutNotesTask();
		putTask.setJSON(NotesJsonMaker.makeJson(mNewNote + "|" + mNotes));
		putTask.setListener(new PutNotesTask.OnPutNotesListener(){
			@Override
			public void onResponse(boolean success, String response){
				if(success){
					// All done with putting notes
					playSuccessSound();
					finish();
				} else {
					// Show failure to sync
					displayFailureToAdd();
				}
			}
		});

		putTask.execute(mAuthToken);
	}

	private void displayFailure() {
		// Play an error sound
		playErrorSound();

		mCardTitle.setText(R.string.text_auth_failure);
		mCardSubTitle.setText(R.string.text_tap_to_try_again);
		mCardImage.setImageResource(R.drawable.ic_warning_50);
	}

	private void displayFailureToAdd() {
		// Play an error sound
		playErrorSound();

		mCardTitle.setText(R.string.text_failed_to_add_note);
		mCardSubTitle.setText(getString(R.string.text_check_internet));
		mCardImage.setImageResource(R.drawable.ic_warning_50);
	}

	private void displayFailureToSignIn() {
		// Play an error sound
		playErrorSound();

		mCardTitle.setText(R.string.text_failed_to_sign_in);
		mCardSubTitle.setText(getString(R.string.text_check_internet));
		mCardImage.setImageResource(R.drawable.ic_warning_50);
	}

	/**
	 * Play the standard Glass success sound
	 */
	protected void playSuccessSound() {
		AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audio.playSoundEffect(Sounds.SUCCESS);
	}

	/**
	 * Play the standard Glass failure sound
	 */
	protected void playErrorSound() {
		AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audio.playSoundEffect(Sounds.ERROR);
	}

}
