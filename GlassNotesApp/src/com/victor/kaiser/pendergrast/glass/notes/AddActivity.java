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
	private String mEmail;
	private String mNewNote;

	private String mAuthToken;

	private TextView mTitle;
	private TextView mSubtitle;
	private ImageView mImage;

	private boolean mStartedVoice = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Show a View with a small amount of explanation
		// of sign in. On tap, we'll start the authentication
		setContentView(R.layout.card_full_image);

		mTitle = (TextView) findViewById(R.id.card_title);
		mSubtitle = (TextView) findViewById(R.id.card_subtitle);
		mImage = (ImageView) findViewById(R.id.card_image);

		mImage.setScaleType(ScaleType.CENTER_INSIDE);

		mTitle.setText(R.string.text_saving);

		mTitle.setGravity(Gravity.CENTER);
		mSubtitle.setGravity(Gravity.CENTER_HORIZONTAL);

		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		// Check to make sure that there is a Refresh Token
		if (mPrefs.getString(PreferenceConstants.REFRESH_TOKEN, "").isEmpty()) {
			// The user must not be signed in
			// Launch the authentication immersion
			Intent i = new Intent(this, AuthActivity.class);
			startActivity(i);
			finish();
		}

	}

	@Override
	protected void onResume() {
		super.onResume();

		Intent i = getIntent();
		if (i == null || i.getExtras() == null) {
			// This must have not been launched from the "ok, glass" home menu
			// so launch our own voice recognizer
			startSpeechRecognizer();
			return;
		}

		ArrayList<String> notes = i.getExtras().getStringArrayList(RecognizerIntent.EXTRA_RESULTS);

		if (notes == null || notes.size() == 0) {
			// This must have not been launched from the "ok, glass" home menu
			// so launch our own voice recognizer
			startSpeechRecognizer();
		} else {
			// Keep this note: it will be put on the server once
			// all of the auth and sync is done
			mNewNote = notes.get(0);

			Log.d(TAG, "Starting note sync process");
			startNoteSync();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == VOICE_RECOGNIZER_REQUEST && resultCode == RESULT_OK) {
			List<String> notes = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

			mNewNote = notes.get(0);

			Log.d(TAG, "Starting note sync process");
			startNoteSync();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Start speech recognition if it has not been
	 * started already
	 */
	private void startSpeechRecognizer(){
		if(!mStartedVoice){
			Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			startActivityForResult(intent, VOICE_RECOGNIZER_REQUEST);
			mStartedVoice = true;
		}
	}

	/**
	 * Starts a new RefreshAuthTokenTask,
	 * if it successfully gets a new auth token,
	 * the existing notes are retrieved, the new note
	 * is added and then sent to the server
	 */
	private void startNoteSync(){
		// Get a new auth token and get the most recent notes
		RefreshAuthTokenTask refreshAuthTask = new RefreshAuthTokenTask();
		refreshAuthTask.setListener(this);
		refreshAuthTask.execute(mPrefs.getString(PreferenceConstants.REFRESH_TOKEN, ""));

	}

	/**
	 * Called when an Auth token is received, or
	 * if there is a problem getting an auth token
	 */
	@Override
	public void onResponse(boolean success, String response) {
		if (success) {
			AuthTokenJsonParser parser = new AuthTokenJsonParser(response);

			if (parser.hasError()) {
				Log.e(TAG, parser.getError());

				// Show failure to sync
				displayFailureToSignIn();

			} else {
				parser.writeToPreferences(mPrefs);
				mAuthToken = parser.getAuthToken();

				// Now sync the notes
				Log.i(TAG, "Starting GetNotesTask");

				GetNotesTask task = new GetNotesTask();
				task.setListener(new GetNotesTask.OnGetNotesListener() {
					@Override
					public void onReceiveNotes(boolean success, String response) {
						Log.i(TAG, "onGetNotes: " + success);
						if (success) {
							// Display the notes
							Log.i(TAG, response);
							NotesJsonParser notesParser = new NotesJsonParser(response);
							mNotes = notesParser.getNotes();
							mEmail = notesParser.getEmail();
							
							Log.d(TAG, "Existing notes: \"" + mNotes + "\"");
							Log.d(TAG, "Email: \"" + mEmail + "\"");

							// Now that we have the notes on the server,
							// add the new note
							addNote();
						} else {
							// Show failure to sync
							displayFailureToAdd();
						}
					}
				});

				task.execute(mAuthToken);

			}
		}
	}

	private void addNote() {
		// Add the note to the server
		PutNotesTask putTask = new PutNotesTask();

		// Use the NotesJsonMaker helper class to make 
		// JSON that can be interpretted by the server
		putTask.setJSON(NotesJsonMaker.makeJson(mNewNote + "|" + mNotes, mEmail));

		putTask.setListener(new PutNotesTask.OnPutNotesListener() {
			@Override
			public void onResponse(boolean success, String response) {
				if (success) {
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

	private void displayFailureToAdd() {
		// Play an error sound
		playErrorSound();

		mTitle.setText(R.string.text_failed_to_add_note);
		mSubtitle.setText(getString(R.string.text_check_internet));
		mImage.setImageResource(R.drawable.ic_warning_50);
	}

	private void displayFailureToSignIn() {
		// Play an error sound
		playErrorSound();

		mTitle.setText(R.string.text_failed_to_sign_in);
		mSubtitle.setText(getString(R.string.text_check_internet));
		mImage.setImageResource(R.drawable.ic_warning_50);
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
