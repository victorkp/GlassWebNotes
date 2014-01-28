package com.victor.kaiser.pendergrast.glass.notes.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import com.victor.kaiser.pendergrast.glass.notes.auth.AuthConstants;
import com.victor.kaiser.pendergrast.glass.notes.auth.GetAuthTokenTask.OnGetTokenListener;

import android.os.AsyncTask;
import android.util.Log;

/**
 * An AsyncTask to get the user's notes from the server
 * Should be executed with the Auth Token as the first parameter
 */
public class PutNotesTask extends AsyncTask<String, Integer, Integer> {

	private static final String TAG = "GetNotesTask";
	private static final int SUCCESS = 0;
	private static final int FAILURE_UNKNOWN = 1;
	private static final int FAILURE_NO_AUTH_TOKEN = 2;
	private static final int FAILURE_NO_JSON = 3;
	private static final int FAILURE_BAD_RESPONSE = 4;

	/**
	 * The URL used to get the user's notes on the server
	 */
	private static final String URL_GET_NOTES = "https://glass-notes-app.appspot.com/_ah/api/endpoint/v1/notes_get";
	
	/**
	 * A simple functional interface that gets called once the token is received
	 * or if there's a problem
	 */
	public static interface OnPutNotesListener {
		public void onResponse(boolean success, String response);
	}
	

	/**
	 * The response received from the server
	 */
	private String mResponse = "";

	/**
	 * The JSON to send to the server
	 */
	private String mJSON = "";

	/**
	 * Listener to call once we're done
	 */
	private OnPutNotesListener mListener;

	/**
	 * Set the listener that will be called once the
	 * notes are put to the server
	 */
	public void setListener(OnPutNotesListener listener) {
		mListener = listener;
	}

	/**
	 * Set the JSON to send to the server
	 */
	public void setJSON(String json){
		mJSON = json;
	}

	@Override
	protected Integer doInBackground(String... params) {

		if (params.length != 1) {
			return FAILURE_NO_AUTH_TOKEN;
		}

		String authToken = params[0];

		if (authToken.isEmpty()) {
			return FAILURE_NO_AUTH_TOKEN;
		}

		if(mJSON.isEmpty()){
			return FAILURE_NO_JSON;
		}

		try {
			URL urlObject = new URL(URL_GET_NOTES);

			HttpsURLConnection con = (HttpsURLConnection) urlObject.openConnection();

			con.setRequestMethod("POST");
			con.setRequestProperty("Authorization", "Bearer " + authToken);
			con.setRequestProperty("Content-Type", "application/json");
			
			con.setDoOutput(true);

			con.connect();

			// Write the JSON
			OutputStreamWriter output = new OutputStreamWriter(con.getOutputStream());
			output.write(mJSON);
			output.flush();
			output.close();

			int serverCode = con.getResponseCode();

			Log.i(TAG, "HttpURLConnection response: " + serverCode);

			if (serverCode != 200) {
				// Response is bad
				BufferedReader reader = new BufferedReader(new InputStreamReader(con.getErrorStream()));

				for (String line = reader.readLine(); line != null; line = reader.readLine()) {
					Log.e(TAG, line);
				}

				return FAILURE_BAD_RESPONSE;
			} else {
				// Response is good
				BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

				for (String line = reader.readLine(); line != null; line = reader.readLine()) {
					mResponse += line;
				}

				Log.i(TAG, "Response: \"\n" + mResponse + "\n\"");

				return SUCCESS;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return FAILURE_UNKNOWN;
		}
	}

	@Override
	protected void onPostExecute(Integer result) {
		callListener(result.intValue() == SUCCESS);
	}

	private void callListener(boolean success) {
		if (mListener != null) {
			mListener.onResponse(success, mResponse);
		}
	}

}
