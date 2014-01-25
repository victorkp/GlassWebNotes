package com.victor.kaiser.pendergrast.glass.notes.preferences;

/**
 * A class containing all the keys for preferences
 */
public class PreferenceConstants {

	/**
	 * Preference key for the device code retrieved when setting up
	 * authentication and used to get the first AUTH_TOKEN 
	 */
	public static final String DEVICE_CODE = "device_code";

	
	/**
	* Preference key for the last Auth Token
	*/
	public static final String AUTH_TOKEN = "auth_token";

	/**
 	* Preference key for the Refresh Token, which will be used
 	* to get new Auth Tokens when they expire
 	*/
	public static final String REFRESH_TOKEN = "refresh_token";

}
