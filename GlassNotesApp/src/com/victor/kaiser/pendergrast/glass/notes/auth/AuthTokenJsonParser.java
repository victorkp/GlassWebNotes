package com.victor.kaiser.pendergrast.glass.notes.auth;

import org.json.JSONObject;

import com.victor.kaiser.pendergrast.glass.notes.preferences.PreferenceConstants;

import android.content.SharedPreferences;

public class AuthTokenJsonParser {

	/**
	 * The fields that are expected in the JSON response
	 */
	private static class JSONFields {
		public static final String ERROR = "error";

		public static final String ACCESS_TOKEN = "access_token";
		public static final String TOKEN_TYPE = "token_type";
		public static final String EXPIRES_IN = "expires_in";
		public static final String REFRESH_TOKEN = "refresh_token";
	}

	
	private String mError;

	private String mToken;
	private String mTokenType;
	private long mExpiration;
	private String mRefreshToken;

	public AuthTokenJsonParser(String jsonResponse) {

		try {
			JSONObject obj = new JSONObject(jsonResponse);
			
			if(obj.has(JSONFields.ERROR)){
				mError = obj.getString(JSONFields.ERROR);
			} else {
				mToken = obj.getString(JSONFields.ACCESS_TOKEN);
				mTokenType = obj.getString(JSONFields.TOKEN_TYPE);
				mExpiration = obj.getLong(JSONFields.EXPIRES_IN);
				mRefreshToken = obj.getString(JSONFields.REFRESH_TOKEN);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void writeToPreferences(SharedPreferences prefs){
		prefs.edit()
			.putString(PreferenceConstants.AUTH_TOKEN, mToken)
			.putString(PreferenceConstants.REFRESH_TOKEN, mRefreshToken)
			.commit();
	}

	public boolean hasError(){
		return mError != null;
	}

	public String getError(){
		return mError;
	}
	
	public String getAuthToken(){
		return mToken;
	}
	
	public String getTokenType(){
		return mTokenType;
	}
	
	public String getRefreshToken(){
		return mRefreshToken;
	}
	
	public long getExpirationTime(){
		return mExpiration;
	}
	

}
