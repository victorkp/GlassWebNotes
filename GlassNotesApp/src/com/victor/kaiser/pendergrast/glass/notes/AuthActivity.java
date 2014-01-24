package com.victor.kaiser.pendergrast.glass.notes;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;

import com.google.android.glass.app.Card;
import com.google.android.glass.app.Card.ImageLayout;
import com.victor.kaiser.pendergrast.glass.notes.auth.AuthTask;
import com.victor.kaiser.pendergrast.glass.notes.auth.AuthTask.OnAuthListener;

public class AuthActivity extends Activity {

	private boolean mAuthStarted = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Show a View with a small amount of explanation
		// of sign in. On tap, we'll start the authentication
		Card card = new Card(this);
		card.setText(R.string.text_sign_in_info);
		card.setFootnote(R.string.text_tap_to_begin);
		setContentView(card.toView());
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER){
			if(!mAuthStarted){
				mAuthStarted = true;
				
				// Start the authentication process
				AuthTask task = new AuthTask();
				
				task.setAuthListener(new OnAuthListener() {
					@Override
					public void onResponse(boolean success, String response) {
						if(success){
							// The response is in JSON and has to be parsed
						}else{
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
	
	private void displayFailure(){
		// Allow auth to happen again
		mAuthStarted = false;
		
		Card failureCard = new Card(this);
		
		failureCard.setText(R.string.text_auth_failure);
		failureCard.setFootnote(R.string.text_tap_to_try_again);
		failureCard.addImage(R.drawable.ic_warning_50);
		failureCard.setImageLayout(ImageLayout.LEFT);
		
		setContentView(failureCard.toView());
	}

}
