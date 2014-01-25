package com.victor.kaiser.pendergrast.glass.notes;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.google.android.glass.media.Sounds;
import com.victor.kaiser.pendergrast.glass.notes.auth.AuthTokenJsonParser;
import com.victor.kaiser.pendergrast.glass.notes.auth.DeviceCodeJsonParser;
import com.victor.kaiser.pendergrast.glass.notes.auth.GetAuthTokenTask;
import com.victor.kaiser.pendergrast.glass.notes.auth.GetDeviceCodeTask;
import com.victor.kaiser.pendergrast.glass.notes.auth.GetDeviceCodeTask.OnGetDeviceCodeListener;

public class AuthActivity extends Activity {

	private static final String TAG = "AuthActivity";

	private static final int STATE_START = 0;
	private static final int STATE_GETTING_DEVICE_CODE = 1;
	private static final int STATE_GOT_DEVICE_CODE = 2;
	private static final int STATE_TESTING_AUTH = 3;
	private static final int STATE_DONE = 4;

	private int mState = STATE_START;

	private String mDeviceCode;

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

		mCardImage.setScaleType(ScaleType.CENTER_INSIDE);
		
		mCardTitle.setText(R.string.text_sign_in_info);
		mCardSubTitle.setText(R.string.text_tap_to_begin);

		mCardTitle.setGravity(Gravity.CENTER);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {

			switch (mState) {
			case STATE_START:
				// Get the device code

				displayLoading();
				playClickSound();

				// Start the authentication process
				GetDeviceCodeTask task = new GetDeviceCodeTask();

				task.setListener(new OnGetDeviceCodeListener() {
					@Override
					public void onResponse(boolean success, String response) {
						if (success) {
							// The response is in JSON and has to be parsed
							DeviceCodeJsonParser parser = new DeviceCodeJsonParser(
									response);

							String userCode = parser.getUserCode();
							if (userCode == null) {
								displayFailure();
								return;
							}

							displayUserCode(userCode, parser.getUrl());

							// Write the DEVICE_CODE to the preferences so an
							// OAuth token can be retrieved later
							parser.writeToPreferences(PreferenceManager
									.getDefaultSharedPreferences(mContext));

							mDeviceCode = parser.getDeviceCode();

							mState = STATE_GOT_DEVICE_CODE;

						} else {
							// Something went wrong
							displayFailure();
						}
					}
				});

				task.execute();
				break;

			case STATE_GETTING_DEVICE_CODE:
				// Disallow user input while the
				// Device Code is being fetched
				playDisallowedSound();
				break;

			case STATE_GOT_DEVICE_CODE:
				// Now test that the user actually
				// went to that URL and entered the code

				playClickSound();
				
				displayTesting();

				GetAuthTokenTask authTask = new GetAuthTokenTask();
				authTask.setListener(new GetAuthTokenTask.OnGetTokenListener() {
					@Override
					public void onResponse(boolean success, String response) {

						if (success) {

							AuthTokenJsonParser parser = new AuthTokenJsonParser(
									response);

							if (parser.hasError()) {
								Log.e(TAG, parser.getError());
								displayFailure();
							} else {
								parser.writeToPreferences(PreferenceManager
										.getDefaultSharedPreferences(mContext));

								displaySuccess();

								mState = STATE_DONE;
							}

						} else {
							displayFailure();
						}

					}
				});
				authTask.execute(mDeviceCode);

				mState = STATE_TESTING_AUTH;

				break;

			case STATE_TESTING_AUTH:
				// Disallow user input while
				// the auth is being tested
				playDisallowedSound();
				break;

			case STATE_DONE:
				// All done, finish this Activity
				finish();
				playSuccessSound();
				break;
			}

			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	private void displayLoading() {
		mCardTitle.setText(R.string.text_loading);
		mCardSubTitle.setText("");
		mCardImage.setImageBitmap(null);
	}

	private void displayTesting() {
		mCardTitle.setText(R.string.text_testing);
		mCardSubTitle.setText("");
		mCardImage.setImageBitmap(null);

		// Increase the text size again
		mCardTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, 70);
	}

	private void displayFailure() {
		// Allow auth to happen again
		mState = STATE_START;

		// Play an error sound
		playErrorSound();

		mCardTitle.setText(R.string.text_auth_failure);
		mCardSubTitle.setText(R.string.text_tap_to_try_again);
		mCardImage.setImageResource(R.drawable.ic_warning_50);
	}

	private void displaySuccess() {
		// Play a success sound
		playSuccessSound();

		mCardTitle.setText(R.string.text_all_done);
		mCardSubTitle.setText(R.string.text_tap_to_finish);
		mCardImage.setImageResource(R.drawable.ic_done_150);
	}

	private void displayUserCode(String code, String url) {
		playSuccessSound();

		mCardTitle.setText(Html.fromHtml(String.format(
				getString(R.string.text_user_code_enter_at_url), "<br><b>"
						+ url + "</b>", "<b>" + code + "</b>")));

		mCardSubTitle.setText(R.string.text_tap_to_continue);
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
	protected void playErrorSound() {
		AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audio.playSoundEffect(Sounds.ERROR);
	}

	/**
	 * Play the standard Glass disallowed sound
	 */
	protected void playDisallowedSound() {
		AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audio.playSoundEffect(Sounds.DISALLOWED);
	}

	/**
	 * Play the standard Glass tap sound
	 */
	protected void playClickSound() {
		AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audio.playSoundEffect(Sounds.TAP);
	}

}
