package com.victor.kaiser.pendergrast.glass.notes;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardScrollView;
import com.victor.kaiser.pendergrast.glass.notes.api.GetNotesTask;
import com.victor.kaiser.pendergrast.glass.notes.api.NotesJsonMaker;
import com.victor.kaiser.pendergrast.glass.notes.api.NotesJsonParser;
import com.victor.kaiser.pendergrast.glass.notes.api.PutNotesTask;
import com.victor.kaiser.pendergrast.glass.notes.auth.AuthTokenJsonParser;
import com.victor.kaiser.pendergrast.glass.notes.auth.RefreshAuthTokenTask;
import com.victor.kaiser.pendergrast.glass.notes.content.NoteAdapter;
import com.victor.kaiser.pendergrast.glass.notes.preferences.PreferenceConstants;

public class MainActivity extends Activity  implements RefreshAuthTokenTask.OnGetTokenListener, OnItemClickListener {
	private static final String TAG = "MainActivity";

	private SharedPreferences mPrefs;

	private CardScrollView mScrollView;
	private NoteAdapter mAdapter;

	private boolean mNotesShown = false;
	private int mItemClicked = -1;

	private String mAuthToken;
	private String mEmail;

	private TextView mTitle;
	private TextView mSubtitle;
	private ImageView mImage;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		// Keep and turn the screen on
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


		// A basic card-style layout with a 
		// full screen image in the background
		setupLayout();

		// This is a fairly generic layout,
		// one customization this app does is it moves
		// the text from being left aligned to the center
		mTitle.setGravity(Gravity.CENTER);
		mSubtitle.setGravity(Gravity.CENTER_HORIZONTAL);

		// Show a "Loading..." message
		mTitle.setText(getString(R.string.text_signing_in));
		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		// Check to make sure that there is a Refresh Token
		if(mPrefs.getString(PreferenceConstants.REFRESH_TOKEN, "").isEmpty()){
			// Launch the authentication immersion
			Intent authSetupIntent  = new Intent(this, AuthActivity.class);
			startActivity(authSetupIntent);
			finish();
		}
		
		// Get a new auth token 
		// Once an auth token is recieved,
		// get the notes from the server
		RefreshAuthTokenTask refreshAuthTask = new RefreshAuthTokenTask();
		refreshAuthTask.setListener(this);
		refreshAuthTask.execute(mPrefs.getString(PreferenceConstants.REFRESH_TOKEN, ""));
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event){
		// Open the menu when taps are detected
		if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER){
			playClickSound();
			openOptionsMenu();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int index, long id){
		// An item was clicked
		playClickSound();

		// Keep the index of the click for possible
		// later use
		mItemClicked = index;

		// Invalidate the menu: an item has been
		// clicked, so we know that the CardScrollView
		// is being shown and that there are notes
		invalidateOptionsMenu();
		openOptionsMenu();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu
		getMenuInflater().inflate(R.menu.main, menu);

		// If the CardScrollView is shown,
		// then there should be a "Delete Note" option
		// The "Delete Note" item defaults to not bein
		// visible
		if(mNotesShown){
			MenuItem delete = menu.findItem(R.id.menu_delete_note);
			delete.setVisible(true);
		}

		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		case R.id.menu_delete_note:
			Log.d(TAG, "menu_delete_note");

			// Delete this note from the adapter
			mAdapter.deleteNote(mItemClicked);

			if(mAdapter.getCount() == 0){
				// If there aren't any notes
				// left, display the "no notes"
				// view
				displayNoNotes();
			}else{
				// Show the new notes,
				// notifyDataSetChanged causes
				// the application to crash immediately
				// because the CardScrollView 
				// had activate() called
				displayNotes(mAdapter.getNotes());
			}

			// Push the notes to the server
			syncNotes(mAdapter.getNotes());

			return true;
		case R.id.menu_add_note:
			Log.d(TAG, "menu_add_note");

			// Go to the AddActivity to add notes
			Intent addIntent = new Intent(this, AddActivity.class);
			finish();
			startActivity(addIntent);
			return true;
		case R.id.menu_sign_out:
			Log.d(TAG, "menu_sign_out");

			// Clear all the saved tokens and codes
			mPrefs.edit()
				.putString(PreferenceConstants.AUTH_TOKEN, "")
				.putString(PreferenceConstants.REFRESH_TOKEN, "")
				.putString(PreferenceConstants.DEVICE_CODE, "")
				.commit();
			
			// Start the AuthActivity to do setup again
			Intent authSetupIntent = new Intent(this, AuthActivity.class);
			startActivity(authSetupIntent);

			// Close this Activity
			finish();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * This is the listener for the RefreshAuthTokenTask
	 * that is started in onCreate
	 */
	@Override
	public void onResponse(boolean success, String response) {
		if(success){
			AuthTokenJsonParser parser = new AuthTokenJsonParser(response);
			
			// There may have been a response
			// with an error instead of an
			// auth token
			if(parser.hasError()){
				Log.e(TAG, parser.getError());
				displayFailureToSignIn();
			}else{
				// Show a "Getting notes" View
				displayGettingNotes();
				
				// Save this auth token for possible future use
				parser.writeToPreferences(mPrefs);
				mAuthToken = parser.getAuthToken();

				// Now sync the notes with those on the server
				GetNotesTask task = new GetNotesTask();
				task.setListener(new GetNotesTask.OnGetNotesListener(){
					@Override
					public void onReceiveNotes(boolean success, String response){
						Log.i(TAG, "onGetNotes: " + success);
						if(success){
							// Display the notes contained in the response,
							// First extract the notes from the response
							NotesJsonParser notesParser = new NotesJsonParser(response);
							mEmail = notesParser.getEmail();
							String notes = notesParser.getNotes();

							if(notes.isEmpty()){
								displayNoNotes();
							}else{
								// Show the notes in a 
								// CardScrollView
								displayNotes(notes);
							}

						}else{
							// Failed to get the notes
							displayFailureToSync();
						}
					}
				});
				
				// GetNotesTask takes the auth token
				// as a parameter in its execution
				task.execute(mAuthToken);
			}
		}else{
			// success was false for whatever reason,
			// so display a failed to sign-in message
			displayFailureToSignIn();
		}
	}

	private void syncNotes(String notes) {
		// Add the note to the server
		PutNotesTask putTask = new PutNotesTask();

		// Use the NotesJsonMaker helper class to make 
		// JSON in a way that the server expects
		putTask.setJSON(NotesJsonMaker.makeJson(notes, mEmail));

		putTask.setListener(new PutNotesTask.OnPutNotesListener() {
			@Override
			public void onResponse(boolean success, String response) {
				if (success) {
					// All done with putting notes

				} else {
					// Show failure to sync
					displayFailureToSet();
				}
			}
		});

		putTask.execute(mAuthToken);
	}

	private void displayGettingNotes(){
		// Show getting notes
		mTitle.setText(getString(R.string.text_getting_notes));
		mSubtitle.setText("");
		mImage.setImageDrawable(null);
	}

	private void displayFailureToSignIn(){
		setupLayout();

		// Show failure to sign in
		mTitle.setText(getString(R.string.text_failed_to_sign_in));
		mSubtitle.setText(getString(R.string.text_check_internet));
		mImage.setImageResource(R.drawable.ic_warning_50);
	}

	private void displayFailureToSet() {
		setupLayout();

		// Play an error sound
		playErrorSound();

		mTitle.setText(R.string.text_failed_to_add_note);
		mSubtitle.setText(getString(R.string.text_check_internet));
		mImage.setImageResource(R.drawable.ic_warning_50);
	}

	private void displayFailureToSync(){
		setupLayout();

		// Show failure to sync
		mTitle.setText(getString(R.string.text_failed_to_get_notes));
		mSubtitle.setText(getString(R.string.text_check_internet));
		mImage.setImageResource(R.drawable.ic_warning_50);
	}
	
	private void displayNoNotes(){
		setupLayout();

		// Show failure to sync
		mTitle.setText(getString(R.string.text_no_notes));
		mSubtitle.setText(getString(R.string.text_suggest_add_notes));
		mImage.setImageResource(R.drawable.ic_pen_50);
	}

	private void displayNotes(String notes){
		mScrollView = new CardScrollView(this);
		
		// Create the adapter and set it, allow interaction
		mAdapter = new NoteAdapter(this, notes);
		mScrollView.setAdapter(mAdapter);

		// Set the OnClickListener
		mScrollView.setOnItemClickListener(this);

		// Show the CardScrollView
		mScrollView.activate();
		setContentView(mScrollView);

		mNotesShown = true;
	}

	/**
	 * If the layout hasn't already been setup,
	 * then set it up. This is useful when
	 * the user had notes, but deleted all of them.
	 * In that case, a CardScrollView was shown,
	 * and the normal layout was deleted
	 */
	private void setupLayout(){
		if(findViewById(R.id.card_title) == null){
			setContentView(R.layout.card_full_image);

			mTitle = (TextView) findViewById(R.id.card_title);
			mSubtitle = (TextView) findViewById(R.id.card_subtitle);
			mImage = (ImageView) findViewById(R.id.card_image);
		}
	}

	/**
	 * Play the standard Glass tap sound
	 */
	protected void playClickSound() {
		AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audio.playSoundEffect(Sounds.TAP);
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

