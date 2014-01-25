package com.victor.kaiser.pendergrast.glass.notes;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.glass.app.Card;
import com.google.android.glass.app.Card.ImageLayout;
import com.google.android.glass.media.Sounds;
import com.victor.kaiser.pendergrast.glass.notes.auth.AuthJsonParser;
import com.victor.kaiser.pendergrast.glass.notes.auth.GetDeviceCodeTask;
import com.victor.kaiser.pendergrast.glass.notes.auth.GetDeviceCodeTask.OnAuthListener;

public class AuthActivity extends Activity {

	private boolean mAuthStarted = false;
	private boolean mAuthFinished = false;

	private Context mContext;

	private TextView mCardTitle;
	private TextView mCardSubTitle;
	private ImageView mCardImage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mContext = this;

		// Show a View with a small amount of explanation
		// of sign in. On tap, we'll start the authentication
		setContentView(R.layout.card_full_image);
		
		mCardTitle = (TextView) findViewById(R.id.card_title);
		mCardSubTitle = (TextView) findViewById(R.id.card_subtitle);
		mCardImage = (ImageView) findViewById(R.id.card_image);

		mCardTitle.setText(R.string.text_sign_in_info);
		mCardSubTitle.setText(R.string.text_tap_to_begin);
		
		mCardTitle.setGravity(Gravity.CENTER);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			
			// Check to see if we're already done
			if(mAuthFinished){
				finish();
				playSuccessSound();
				return true;
			}
			
			// Disallow multiple taps after asking for
			// a device code once
			if (!mAuthStarted) {
				mAuthStarted = true;

				displayLoading();

				playClickSound();

				// Start the authentication process
				GetDeviceCodeTask task = new GetDeviceCodeTask();

				task.setAuthListener(new OnAuthListener() {
					@Override
					public void onResponse(boolean success, String response) {
						if (success) {
							// The response is in JSON and has to be parsed
							AuthJsonParser parser = new AuthJsonParser(response);

							String userCode = parser.getUserCode();
							if (userCode == null) {
								displayFailure();
								return;
							}

							displayUserCode(userCode, parser.getUrl());
							
							mAuthFinished = true;

							// Write the DEVICE_CODE to the preferences so an
							// OAuth token can be retrieved later
							parser.writeToPreferences(PreferenceManager
									.getDefaultSharedPreferences(mContext));

						} else {
							// Something went wrong
							displayFailure();
						}
					}
				});

				task.execute();

			}
		}

		return super.onKeyDown(keyCode, event);
	}

	private void displayLoading() {
		mCardTitle.setText(R.string.text_loading);
		mCardSubTitle.setText("");
		mCardImage.setImageBitmap(null);
	}

	private void displayFailure() {
		// Allow auth to happen again
		mAuthStarted = false;
		
		// Play an error sound
		playFailureSound();

		mCardTitle.setText(R.string.text_auth_failure);
		mCardSubTitle.setText(R.string.text_tap_to_try_again);
		mCardImage.setImageResource(R.drawable.ic_warning_50);
	}

	private void displayUserCode(String code, String url) {
		playSuccessSound();
		
		mCardTitle.setText(Html.fromHtml(String.format(
				getString(R.string.text_user_code_enter_at_url),
								"<br><b>" + url + "</b>",
								"<b>" + code + "</b>")));
		
		mCardSubTitle.setText(R.string.text_tap_to_finish);
		mCardImage.setImageBitmap(null);
		
		// Have to reduce the text size a bit
		// so that the url fits on one line
		mCardTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, 40);
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
	protected void playFailureSound() {
		AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audio.playSoundEffect(Sounds.ERROR);
	}

	/**
	 * Play the standard Glass tap sound
	 */
	protected void playClickSound() {
		AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audio.playSoundEffect(Sounds.TAP);
	}

}
