package com.victor.kaiser.pendergrast.glass.notes.auth;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import android.os.AsyncTask;
import android.util.Log;

/**
 * A class to get authentication initially setup 
 * so that later we can get OAuth tokens
 * See https://developers.google.com/accounts/docs/OAuth2ForDevices
 * for more detail
 */
public class AuthTask extends AsyncTask<String, Integer, Integer> {
	
	private static final String TAG = "AuthTask";
	
	private static final Integer SUCCESS = 1;
	private static final Integer FAILURE = 0;
	
	/**
	 * Listener to call once we're done
	 */
	private OnAuthListener mListener;
	
	/**
	 * The response received from the server
	 */
	private String mResponse;
	
	/**
	 * Wrapper class for OAuth client ID that was setup on 
	 * the Google Developer Console
	 */
	private class AuthConstants {
		public static final String CLIENT_ID = "81905218945.apps.googleusercontent.com";
		public static final String CLIENT_SECRET = "2-J8_qD_2b2fzaYrQqV-LhsL";
	}

	/**
	 * A simple functional interface that gets called
	 * once 
	 */
	public static interface OnAuthListener {
		public void onResponse(boolean success, String response);
	}
	
	public void setAuthListener(OnAuthListener listener){
		mListener = listener;
	}
	
	@Override
	protected Integer doInBackground(String... params) {

		try {
			URL urlObject = new URL("accounts.google.com/o/oauth2/token");

			HttpsURLConnection con = (HttpsURLConnection) urlObject.openConnection();
			
			con.setRequestMethod("PUT");
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			con.setRequestProperty("client_id", AuthConstants.CLIENT_ID);
			con.setRequestProperty("scope", "email%20profile");

			con.setDoOutput(true);

			con.connect();
			
			//Log.i(TAG, "Getting outputStream");
			OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
			//Log.i(TAG, "Got outputStream");
			
			out.flush();
			out.close();
			
			//Log.i(TAG, "Wrote PUT data");

			int serverCode = con.getResponseCode();

			Log.i(TAG, "HttpURLConnection response: " + serverCode);

			if (serverCode != 200) {
				// Response is bad
				BufferedReader reader = new BufferedReader(new InputStreamReader(con.getErrorStream()));

				for (String line = reader.readLine(); line != null; line = reader.readLine()) {
					Log.e(TAG, line);
				}

				return FAILURE;
			} else {
				// Response is good
				BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

				String response = "";
				
				for (String line = reader.readLine(); line != null; line = reader.readLine()) {
					response += line;
				}
				
				Log.i(TAG, "Response:\"\n" + response + "\n\"");
				
				
				return SUCCESS;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return FAILURE;
		}
	}

	@Override
	protected void onPostExecute(Integer result) {
		callListener(result.intValue() == SUCCESS);
	}
	
	private void callListener(boolean success){
		if(mListener != null){
			mListener.onResponse(success, mResponse);
		}
	}
	
	

}
