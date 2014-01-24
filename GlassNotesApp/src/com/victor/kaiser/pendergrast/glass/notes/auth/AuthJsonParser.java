package com.victor.kaiser.pendergrast.glass.notes.auth;

import org.json.JSONObject;

import com.victor.kaiser.pendergrast.glass.notes.preferences.PreferenceConstants;

import android.content.SharedPreferences;

public class AuthJsonParser {

	/**
	 * The fields that are expected in the JSON response
	 */
	private static class JSONFields {
		public static final String DEVICE_CODE = "device_code";
		public static final String USER_CODE = "user_code";
		public static final String VERIFICATION_URL = "verification_url";
		public static final String EXPIRES_IN = "expires_in";
		public static final String INTERVAL = "interval";
	}

	
	private String mDeviceCode;
	private String mUserCode;
	private String mUrl;
	private long mExpiration;
	private int mInterval;

	public AuthJsonParser(String jsonResponse) {

		try {
			JSONObject obj = new JSONObject(jsonResponse);
			
			mDeviceCode = obj.getString(JSONFields.DEVICE_CODE);
			mUserCode = obj.getString(JSONFields.USER_CODE);
			mUrl = obj.getString(JSONFields.VERIFICATION_URL);
			mExpiration = obj.getLong(JSONFields.EXPIRES_IN);
			mInterval = obj.getInt(JSONFields.INTERVAL);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void writeToPreferences(SharedPreferences prefs){
		prefs.edit()
			.putString(PreferenceConstants.DEVICE_CODE, mDeviceCode)
			.commit();
	}
	
	public String getDeviceCode(){
		return mDeviceCode;
	}
	
	public String getUserCode(){
		return mUserCode;
	}
	
	public String getUrl(){
		return mUrl;
	}
	
	public long getExpirationTime(){
		return mExpiration;
	}
	
	public int getRequestInteral(){
		return mInterval;
	}

}
