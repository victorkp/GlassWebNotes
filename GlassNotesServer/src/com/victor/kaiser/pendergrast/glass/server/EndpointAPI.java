package com.victor.kaiser.pendergrast.glass.server;

import java.io.IOException;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.oauth.OAuthRequestException;
import com.google.appengine.api.users.User;
import com.google.apphosting.datastore.EntityV4.PartitionId.Constants;
import com.victor.kaiser.pendergrast.glass.server.data.UserData;
import com.victor.kaiser.pendergrast.glass.server.data.UserDatabase;


@Api(name = "endpoint", version = "v1")
public class EndpointAPI {
	
	/**
	 * All of the authentication constants 
	 */
	private static class Constants {
		public static final String WEB_CLIENT_ID = "81905218945-ahl3tchl11rcsviiivne7unpl5mnijfh.apps.googleusercontent.com";
		
		public static final String DEVICE_CLIENT_ID = "81905218945.apps.googleusercontent.com";
		
		//public static final String ANDROID_CLIENT_ID1 = "965606050172-6ovcrmugbs5na7p9i1et1tjbf4isv08p.apps.googleusercontent.com";
		//public static final String ANDROID_CLIENT_ID2 = "965606050172.apps.googleusercontent.com";

		public static final String ANDROID_AUDIENCE = WEB_CLIENT_ID;

		public static final String SCOPE_EMAIL = "https://www.googleapis.com/auth/userinfo.email";
		public static final String SCOPE_PLUS_PROFILE = "https://www.googleapis.com/auth/plus.me";
	}
	
	/**
	 * Used by the client to retrieve notes 
	 */
	@ApiMethod(name = "notes.list", path = "notes_get", httpMethod = HttpMethod.GET, scopes = { Constants.SCOPE_EMAIL }, clientIds = { Constants.WEB_CLIENT_ID, Constants.DEVICE_CLIENT_ID }, audiences = { Constants.WEB_CLIENT_ID })
	public UserData getReadArticles(User user) throws OAuthRequestException, IOException {
		
		// Check that the user is signed in
		if (user == null) {
			throw new OAuthRequestException("Couldn't authenticate");
		}

		//UserData userData = UserDatabase.getUserByEmail(user.getEmail());
		UserData userData = UserDatabase.getUserFromDatabaseByEmail(user.getEmail());

		if(userData == null){
			return new UserData(user.getEmail() + " not in datastore", "", 0);
		}
		
		return userData;
	}
	
	/**
	 * Used by the client to set notes
	 */
	@ApiMethod(name = "notes.put", path="notes_put", httpMethod = HttpMethod.PUT, scopes = { Constants.SCOPE_EMAIL }, clientIds = { Constants.WEB_CLIENT_ID, Constants.DEVICE_CLIENT_ID}, audiences = { Constants.ANDROID_AUDIENCE} )
	public UserData putReadArticles(User user, UserData data) throws OAuthRequestException, IOException, IllegalArgumentException {
		
		if (user == null) {
			throw new OAuthRequestException("Couldn't authenticate");
		}
		
		if(data == null){
			return new UserData("null", "No data received", System.currentTimeMillis());
		}
		
		try{
			// Add the data to the datastore
			
			//UserDatabase.putUser(data);
			UserDatabase.putUserIntoDatastore(data);

		} catch (Exception e){
			e.printStackTrace();
			return new UserData(user.getEmail(), "Failed to put into database", System.currentTimeMillis());
		}
		
		// Success
		data.setNotes("Successfully saved: \"" + data.getNotes() + "\"");
		
		return data;
	}

	/**
	 * Called by a cron job to put the contents of the write cache in the datastore.
	 */
	@ApiMethod(name = "persist_cache", path="persist_cache", httpMethod = HttpMethod.GET)
	public UserData persistCache() throws IOException, IllegalArgumentException {
		
		try{
			// Add the data to the datastore
			UserDatabase.writeCacheToDatastore();
		} catch (Exception e){
			e.printStackTrace();
			return new UserData("Server", "Failed to persist write cache", System.currentTimeMillis());
		}
		
		return new UserData("Server", "Persisted the write cache", System.currentTimeMillis());
	}

}
